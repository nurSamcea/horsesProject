import json
import logging
import random
import time

import paho.mqtt.client as mqtt

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

# Lista de dispositivos
devices = [
    {"name": "WorkerSystem", "type": "pub", "access_token": "7jpi6hyp0jzolihttq45", "topic": "v1/devices/me/telemetry"},
    {"name": "horse real 10", "type": "sub", "access_token": "qTMksu1CPRUh6R9x9ZR4",
     "topic": "v1/devices/me/telemetry"},
    {"name": "horse sim 9", "type": "sub", "access_token": "iojQPjgsew6y4jtqY3Ga", "topic": "v1/devices/me/telemetry"},
    # Agrega más dispositivos según sea necesario...
]

# Configuración del broker MQTT
broker = "srv-iot.diatel.upm.es"
port = 8883

# Lista para almacenar clientes MQTT
clients = []


# Callback para manejar la conexión
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        logging.info(f"[{userdata['name']}] Conexión exitosa al broker.")
        if userdata["type"] == "sub":
            client.subscribe(userdata["topic"])
            logging.info(f"[{userdata['name']}] Suscrito al topic {userdata['topic']}")
    else:
        logging.error(f"[{userdata['name']}] Error al conectar: Código {rc}")


# Callback para manejar los mensajes recibidos
def on_message(client, userdata, msg):
    try:
        payload = msg.payload.decode("utf-8")
        data = json.loads(payload)
        logging.info(f"[{userdata['name']}] Mensaje recibido: {data}")
        logging.info(f"[{userdata['name']}] Detalle: Topic={msg.topic}, Payload={data}")
    except Exception as e:
        logging.error(f"[{userdata['name']}] Error al procesar mensaje: {e}")


# Callback para confirmar publicación
def on_publish(client, userdata, mid):
    logging.info(f"[{userdata['name']}] Mensaje publicado correctamente (MID={mid}).")


def main():
    # Configuración y manejo de clientes MQTT
    for device in devices:
        client = mqtt.Client()
        client.username_pw_set(device["access_token"])
        client.tls_set()

        # Pasar información del dispositivo como `userdata`
        client.user_data_set(device)

        # Conectar y almacenar cliente
        try:
            client.connect(broker, port, 60)
            client.loop_start()
            # Asignar callbacks
            client.on_connect = on_connect
            if device["type"] == "sub":
                client.on_message = on_message
            elif device["type"] == "pub":
                client.on_publish = on_publish

            clients.append({"client": client, "device": device})
            logging.info(f"[{device['name']}] Cliente configurado con tipo {device['type']}")
        except Exception as e:
            logging.error(f"[{device['name']}] Error al conectar cliente: {e}")

    # Enviar datos periódicamente desde dispositivos tipo "pub"
    try:
        while True:
            for client_entry in clients:
                dev_element = client_entry["device"]
                client = client_entry["client"]

                if dev_element["type"] == "pub":
                    # Simulación de datos
                    data = {"temperature": round(random.uniform(20.0, 40.0), 2)}
                    payload = json.dumps(data)

                    # Publicar datos
                    logging.info(f"[{dev_element['name']}] Enviando datos: {payload}")
                    client.publish(dev_element["topic"], payload, qos=1)

            time.sleep(10)  # Esperar 10 segundos antes del próximo envío
    except KeyboardInterrupt:
        logging.info("Finalizando programa...")
    finally:
        for client_entry in clients:
            client_entry["client"].loop_stop()
            client_entry["client"].disconnect()
        logging.info("Clientes MQTT desconectados.")


if __name__ == "__main__":
    main()
