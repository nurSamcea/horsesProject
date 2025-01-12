import paho.mqtt.client as mqtt
import json

# Configuración del servidor MQTT
broker = "srv-iot.diatel.upm.es"
port = 8883  # Puerto para conexión con TLS
topic = "v1/devices/me/telemetry"
access_token = "7jpi6hyp0jzolihttq45"

# Crear cliente MQTT
client = mqtt.Client()
client.username_pw_set(access_token)

# Habilitar TLS
client.tls_set()

try:
    # Conectar al broker
    print(f"Conectando al broker MQTT {broker}:{port}...")
    client.connect(broker, port, 60)
    print("Conexión establecida.")
    
    # Publicar mensaje
    data = {"temperature": 55}
    client.publish(topic, json.dumps(data))
    print(f"Mensaje enviado: {data}")
    
except Exception as e:
    print(f"Error al conectar o enviar datos: {e}")

finally:
    # Cerrar conexión
    client.disconnect()
    print("Conexión cerrada.")
