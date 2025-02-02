########################################
#           DATA AGGREGATION           #
########################################
#    ASP - MIoT - HorseCare Connect    #
#             Group 4                  #
########################################

import json
import logging
from time import sleep, time
import paho.mqtt.client as mqtt
from geopy.distance import geodesic
from pymongo import MongoClient
from math import radians, sin, cos, sqrt, atan2

#----------------------------------------------------------------------------
# CONFIGURATION and PARAMETERS 
#----------------------------------------------------------------------------
HIGH_TEMP = 38 # ºC 
HEART_LIMIT = 46 # bpm
OX_LIMIT = 96 # o2
THRESHOLD_METERS = 2 # m 
Z_AX_LIMIT = 5 # m/s2
R = 6371000 # radio tierra

# MQTT configuration---------------------------------------------------------
MQTT_BROKER = "srv-iot.diatel.upm.es"
MQTT_PORT = 8883
MQTT_BROKER_SUB = "v1/devices/me/attributes"
MQTT_BROKER_PUB = "v1/devices/me/telemetry"
ACCESS_TOKEN = "OcftH8NDooivVwEWG69f" # porcessor node - aggregation data 

# DATA ANALYTICS configuration ----------------------------------------------
# Number of horses 
NUM_HORSES = 10 
# each 5 msg data is proccessed - TEST PURPOSES
TEST_DATA = 5 
# dict to count messages and save params, for TEST PURPOSES --> real deployment: change to time --> import time
message_count_dict = {f"horse {i}": 0 for i in range(NUM_HORSES)}
horse_data_dict = {f"horse {i}": [] for i in range(NUM_HORSES)} 
last_location_dict = {f"horse {i}": [] for i in range(NUM_HORSES)} # location GPS

# Logging configuration-----------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("aggregation_data.log"),
        logging.StreamHandler()
    ]
)

# Database--------------------------------------------------------------------
# USER ASP_HorseCare_Connect_User1 Pwd: Xx4X0SvuYWIPwLYh
MONGO_URI = "mongodb+srv://ASP_HorseCare_Connect_User1:Xx4X0SvuYWIPwLYh@horsemonitoring.m3r6o.mongodb.net/?retryWrites=true&w=majority&appName=HorseMonitoring"
client_mongo = MongoClient(MONGO_URI)
db = client_mongo["HorseMonitoring"]  # Name of DB
collection_measurements = db["horse_data"]  # Measurements
collection_alerts = db["horse_alerts"]  # Alerts
#----------------------------------------------------------------------------
#----------------------------------------------------------------------------

# Function to save data in MongoDB-------------------------------------------
def save_to_mongo(collection, data):
    """Función para guardar datos en MongoDB"""
    collection.insert_one(data)
    logging.info(f"Data saved to MongoDB: {data}")
#----------------------------------------------------------------------------
#----------------------------------------------------------------------------

# temperature----------------------------------------------------------------
def evaluate_high_temp(horse_data):
    avg_temp = sum([data["temperature"] for data in horse_data if data["heart_rate"] is not None]) / len(horse_data)
    return avg_temp >= HIGH_TEMP

# oximetry -------------------------------------------------------------------
def evaluate_low_oxim(horse_data):
    avg_oximetry = sum([data["oximetry"] for data in horse_data if data["oximetry"] is not None]) / len(horse_data)
    return avg_oximetry < OX_LIMIT

# heartbeat-------------------------------------------------------------------
def evaluate_high_hr(horse_data):
    avg_heart_rate = sum([data["heart_rate"] for data in horse_data if data["heart_rate"] is not None]) / len(horse_data)
    return avg_heart_rate >= HEART_LIMIT

# too much time on the ground (5m/s2) if 3/5 measurements are true------------
def evaluate_acc_on_ground(horse_data):
    count_threshold = 0
    # check last 5 measurements 
    for data in horse_data[-5:]: 
        if data["z"] is not None and data["z"] < Z_AX_LIMIT:
            count_threshold += 1
    if count_threshold >= 3:
        fall_down = True
    else:
        fall_down = False
    return fall_down

# GPS -- # geopy libreary -----------------------------------------------------
def calculate_distance(lat1, lon1, lat2, lon2):
    # Convertir grados a radianes
    lat1 = radians(lat1)
    lon1 = radians(lon1)
    lat2 = radians(lat2)
    lon2 = radians(lon2)

    # Diferencias de latitud y longitud
    dlat = lat2 - lat1
    dlon = lon2 - lon1

    # Aplicar la fórmula de Haversine
    a = sin(dlat / 2)**2 + cos(lat1) * cos(lat2) * sin(dlon / 2)**2
    c = 2 * atan2(sqrt(a), sqrt(1 - a))

    # Calcular la distancia en metros
    distance = R * c
    return distance

def evaluate_little_gpslocation(horse_data, device_name):
    little_movement_alert = False
    latitude = horse_data[-1]["latitude"]
    longitude = horse_data[-1]["longitude"]
   
    if len(last_location_dict[device_name]) > 0:
        last_lat, last_lon = last_location_dict[device_name][-1]
        
        # función Haversine solo si las coordenadas son diferentes
        if latitude != last_lat or longitude != last_lon:
            distance = calculate_distance(last_lat, last_lon, latitude, longitude)
            # Si la distancia es mayor que el umbral (2 metros), entonces hay movimiento
            if distance > THRESHOLD_METERS:
                little_movement_alert = True
        else:
            # Si las coordenadas son exactamente iguales, activar la alerta si esto se repite
            same_location_count = last_location_dict[device_name].count((latitude, longitude))
            if same_location_count >= 3:  
                little_movement_alert = True  
    else:
        # Si no hay ubicaciones anteriores, marcamos el movimiento como estacionario por ahora
        little_movement_alert = True
    
    # Agregar la coordenada actual al historial
    if len(last_location_dict[device_name]) >= TEST_DATA:
        last_location_dict[device_name].pop(0)  # Eliminar la más antigua si la lista está llena
    last_location_dict[device_name].append((latitude, longitude))
    
    return little_movement_alert

#----------------------------------------------------------------------------
# Check possible issues------------------------------------------------------
def process_and_publish_data(client, device_name):
    logging.info(f"Processing parameters of {device_name} for the last 5 min")
    horse_data = horse_data_dict[device_name]

    # Evaluamos todas las condiciones
    temp_high_bool = evaluate_high_temp(horse_data)  # ¿Temperatura alta?
    heartrate_high_bool = evaluate_high_hr(horse_data)  # ¿Frecuencia cardíaca alta?
    oximetry_low_bool = evaluate_low_oxim(horse_data)  # ¿Oxigenación baja?
    horse_on_gnd_bool = evaluate_acc_on_ground(horse_data)  # ¿El caballo está en el suelo?
    little_movement_bool = evaluate_little_gpslocation(horse_data, device_name)  # ¿Movimientos muy pequeños?

    # Log de depuración
    logging.debug(f"Evaluando condiciones: Temp: {temp_high_bool}, HR: {heartrate_high_bool}, O2: {oximetry_low_bool}, Ground: {horse_on_gnd_bool}, Movement: {little_movement_bool}")

    alert_message = None  # Inicializamos la variable de la alerta como None

    # Primero, verifica el ataque al corazón. Esto debe tener prioridad
    if (heartrate_high_bool and oximetry_low_bool and horse_on_gnd_bool and little_movement_bool and not temp_high_bool):
        alert_message = "ALERT: Possible heart attack"
        logging.debug("Alerta de ataque al corazón activada")

    # Si no es un ataque al corazón, revisa las demás alertas
    elif (oximetry_low_bool and not temp_high_bool and not heartrate_high_bool):
        alert_message = "WARNING: Old horse is suffocating"
        logging.debug("Alerta de sufrimiento por falta de oxígeno activada")
    
    # Caso de infección, si la temperatura es alta y la oxigenación no está baja
    elif (temp_high_bool and not oximetry_low_bool and horse_on_gnd_bool and little_movement_bool):
        alert_message = "ALERT: Possible infection"
        logging.debug("Alerta de posible infección activada")
    
    # Caso de estrés, si la frecuencia cardíaca es alta y la oxigenación baja
    elif (oximetry_low_bool and heartrate_high_bool and not horse_on_gnd_bool and not little_movement_bool):
        alert_message = "WARNING: Possible stress detection"
        logging.debug("Alerta de posible estrés activada")
    
    # Caso de caballo pariendo, temperatura alta, oxigenación normal, etc.
    elif (temp_high_bool and not oximetry_low_bool and heartrate_high_bool and horse_on_gnd_bool and little_movement_bool):
        alert_message = "NEWS: Horse is giving birth"
        logging.debug("Alerta de caballo pariendo activada")
    
    # Caso de caballo muriendo, oxigenación baja, sin ritmo cardíaco alto, etc.
    elif (not temp_high_bool and oximetry_low_bool and not heartrate_high_bool and horse_on_gnd_bool and little_movement_bool):
        alert_message = "SAD: Horse is dying"
        logging.debug("Alerta de caballo muriendo activada")
    
    # Si hay una alerta, publica el mensaje y guárdalo en la base de datos
    if alert_message:
        payload = {
            "deviceName": device_name,
            "alert_data_processor": alert_message
        }
        client.publish(MQTT_BROKER_PUB, json.dumps(payload), qos=1)
        logging.info(f"Message published: horse {device_name}")
        
        alert_data = {
            "deviceName": device_name,
            "alert_data_processor": alert_message,
            "timestamp": time()
        }
        save_to_mongo(collection_alerts, alert_data)

    return None

#----------------------------------------------------------------------------

# MSG Handler. Function to extract all information from message received
def process_message (msg, client):
    logging.info(f"Porcessing message...")
    global horse_data_dict, message_count_dict, last_location_dict
    message = msg.payload.decode('utf-8')

    try:
        data = json.loads(message)

        device_name = data.get("deviceName", "unknown")
        temperature = data.get("temperature", None)
        oximetry = data.get("oximetry", None)
        heart_rate = data.get("HR", None)
        x = data.get("x", None)
        y = data.get("y", None)
        z = data.get("z", None)
        latitude = data.get("lat", None)
        longitude = data.get("long", None)

        # check devices 
        if device_name not in horse_data_dict:
            logging.error(f"Caballo desconocido: {device_name}")
            return
        
        # save parameters 
        measurements = {
            "device_name": device_name,
            "temperature": temperature,
            "oximetry": oximetry,
            "heart_rate": heart_rate,
            "x": x,
            "y": y,
            "z": z,
            "latitude": latitude,
            "longitude": longitude
        }
        
        # save_to_mongo DATABASE
        save_to_mongo(collection_measurements, measurements)

        horse_data_dict[device_name].append(measurements)

        # control msg for future proccessing
        message_count_dict[device_name] += 1
        logging.info(f"Parameters of {device_name} registered.")


        if message_count_dict[device_name] == TEST_DATA:          
            process_and_publish_data(client, device_name) # agg data
            # reset, remove .... CLEAN 
            message_count_dict[device_name] = 0  # reset counter 
            horse_data_dict[device_name] = [] # remove data 
            last_location_dict[device_name] = []

    except json.JSONDecodeError:
        logging.error("Error parsing JSON msg")


#----------------------------------------------------------------------------
#----------------------------------------------------------------------------
# MQTT functions 
def on_connect(client, userdata, flags, rc):
    logging.info(f"Connected with result code {rc}")
    if rc == 0:
        logging.info("Successfully connected to MQTT broker.")
        client.subscribe(MQTT_BROKER_SUB, qos = 1)
    else:
        logging.error(f"Error connecting to MQTT broker, code: {rc}")

def on_message(client, userdata, msg): 
    logging.info(f"Message received {userdata}")
    process_message(msg, client)

# ---------------------------------------------------------------------------

# MAIN
def main():
    horses = [
        {"name": "horse0", "deviceName": "horse real 0"},
        {"name": "horse1", "deviceName": "horse sim 1"},
        {"name": "horse2", "deviceName": "horse sim 2"},
        {"name": "horse3", "deviceName": "horse sim 3"},
        {"name": "horse4", "deviceName": "horse sim 4"},
        {"name": "horse5", "deviceName": "horse sim 5"},
        {"name": "horse6", "deviceName": "horse sim 6"},
        {"name": "horse7", "deviceName": "horse sim 7"},
        {"name": "horse8", "deviceName": "horse sim 8"},
        {"name": "horse9", "deviceName": "horse sim 9"},
    ]

     # Start dicc with the appropiate keys
    global horse_data_dict, message_count_dict, last_location_dict
    horse_data_dict = {horse["deviceName"]: [] for horse in horses}
    message_count_dict = {horse["deviceName"]: 0 for horse in horses}
    last_location_dict = {horse["deviceName"]: [] for horse in horses}

    # Connection to MQTT broker
    client = mqtt.Client()
    client.username_pw_set(ACCESS_TOKEN)
    client.tls_set()
    client.on_connect = on_connect
    client.on_message = on_message
    logging.info("Data analytics system starting...")

    client.connect(MQTT_BROKER, MQTT_PORT, 60)     
    client.loop_forever()

# ---------------------------------------------------------------------------

main()