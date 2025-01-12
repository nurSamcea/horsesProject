import paho.mqtt.client as mqtt
import json

# Configuración
broker = "srv-iot.diatel.upm.es"
port = 8883
topic = "v1/devices/me/telemetry"
access_token = "7jpi6hyp0jzolihttq45"

# Crear cliente MQTT
client = mqtt.Client()
client.username_pw_set(access_token)

# Conectar al broker
client.connect(broker, port, 60)

# Publicar mensaje
data = {"temperature": 15}
client.publish(topic, json.dumps(data))
print("Mensaje enviado:", data)

# Cerrar conexión
client.disconnect()
