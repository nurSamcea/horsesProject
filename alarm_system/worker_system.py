import logging
from time import sleep, time
import random
import json
import paho.mqtt.client as mqtt
from gpiozero import LED, PWMLED, Button, Buzzer

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),
        logging.StreamHandler()
    ]
)

# Configuración de MQTT
broker = "srv-iot.diatel.upm.es"
port = 8883
topic = "v1/devices/me/telemetry"
access_token = "7jpi6hyp0jzolihttq45"

# Crear cliente MQTT
client = mqtt.Client()
client.username_pw_set(access_token)
client.tls_set()

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        logging.info("Conexión al broker MQTT exitosa.")
    else:
        logging.error(f"Error al conectar con el broker, código: {rc}")

def on_publish(client, userdata, mid):
    logging.info("Mensaje publicado correctamente.")

client.on_connect = on_connect
client.on_publish = on_publish

try:
    logging.info(f"Conectando al broker MQTT {broker}:{port}...")
    client.connect(broker, port, 60)
    client.loop_start()
except Exception as e:
    logging.error(f"Error al conectar con el broker MQTT: {e}")

# Configuración de LEDs y sensores
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

buzzer = Buzzer(18)
button = Button(17)

segments = {
    'A': LED(25),
    'B': LED(12),
    'C': LED(13),
    'D': LED(19),
    'E': LED(26),
    'F': LED(24),
    'G': LED(23),
    'DP': LED(6)
}

numbers = {
    '0': ['A', 'B', 'C', 'D', 'E', 'F'],
    '1': ['B', 'C'],
    '2': ['A', 'B', 'G', 'E', 'D'],
    '3': ['A', 'B', 'C', 'D', 'G'],
    '4': ['B', 'C', 'F', 'G'],
    '5': ['A', 'C', 'D', 'F', 'G'],
    '6': ['A', 'C', 'D', 'E', 'F', 'G'],
    '7': ['A', 'B', 'C'],
    '8': ['A', 'B', 'C', 'D', 'E', 'F', 'G'],
    '9': ['A', 'B', 'C', 'D', 'F', 'G']
}

colors = {
    "verde": (0, 1, 0),
    "rojo": (1, 0, 0),
    "azul": (0, 0, 1),
    "amarillo": (1, 1, 0),
    "negro": (0, 0, 0)
}

def set_rgb_color(r, g, b):
    red_pwm.value = r
    green_pwm.value = g
    blue_pwm.value = b

def display_number(num):
    for segment in segments.values():
        segment.off()
    for segment in numbers[num]:
        segments[segment].on()

def set_number(color):
    color_map = {
        "verde": 0,
        "negro": 0,
        "rojo": 1,
        "azul": 2,
        "amarillo": 3
    }
    return color_map.get(color, 0)

def display_message():
    for _ in range(3):
        for segment in segments.values():
            segment.on()
        sleep(0.5)
        for segment in segments.values():
            segment.off()
        sleep(0.5)

def send_vet_call():
    """Envía el mensaje 'Llamar al veterinario' por MQTT."""
    message = {"alert": "Llamar al veterinario"}
    client.publish(topic, json.dumps(message), qos=1)
    logging.info("Mensaje enviado por MQTT: Llamar al veterinario")

def button_pressed():
    start_time = time()
    while button.is_pressed:
        sleep(0.1)
    press_duration = time() - start_time

    if press_duration < 2:
        logging.info("Botón presionado: Restableciendo pantalla y sonido.")
        buzzer.off()
        display_number('0')
        set_rgb_color(*colors["verde"])
    else:
        logging.info("Pulsación prolongada detectada: Llamar al veterinario.")
        display_message()
        send_vet_call()

def monitor_health():
    temperature = random.uniform(33.5, 44.5)
    oxygen = random.uniform(70, 100)
    heartbeat = random.uniform(100, 160)

    if temperature < 36.5 or temperature > 40.5:
        logging.warning(f"Temperatura fuera de rango: {temperature:.1f}°C")
        color = "rojo"
    elif oxygen < 80 or oxygen > 90:
        logging.warning(f"Nivel de oxígeno fuera de rango: {oxygen:.1f}%")
        color = "azul"
    elif heartbeat < 120 or heartbeat > 130:
        logging.warning(f"Latido fuera de rango: {heartbeat:.1f} BPM")
        color = "amarillo"
    else:
        logging.info(f"Estado normal: Temp={temperature:.1f}°C, Oxígeno={oxygen:.1f}%, Latidos={heartbeat:.1f} BPM")
        color = "verde"

    number = set_number(color)
    display_number(f"{number}")
    set_rgb_color(*colors[color])
    if number == 0:
        buzzer.off()
    else:
        buzzer.on()

def main():
    logging.info("Iniciando sistema de monitoreo de salud.")

    try:
        while True:
            monitor_health()
            button.when_pressed = button_pressed
            sleep(10)
    except KeyboardInterrupt:
        logging.warning("Programa interrumpido manualmente.")
    finally:
        logging.info("Apagando sistema.")
        for segment in segments.values():
            segment.off()
        buzzer.off()
        set_rgb_color(*colors["negro"])
        client.loop_stop()
        client.disconnect()
        logging.info("MQTT client desconectado.")

if __name__ == "__main__":
    main()
