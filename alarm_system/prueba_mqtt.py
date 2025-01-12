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

# Habilitar TLS con certificado predeterminado
client.tls_set()  # Esto usa los certificados predeterminados del sistema

# Callback para la conexión
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("Conexión al broker MQTT exitosa.")
    else:
        print(f"Error al conectar con el broker, código: {rc}")

# Callback para publicación
def on_publish(client, userdata, mid):
    print("Mensaje publicado correctamente.")

# Asignar callbacks
client.on_connect = on_connect
client.on_publish = on_publish

try:
    # Conectar al broker
    print(f"Conectando al broker MQTT {broker}:{port}...")
    client.connect(broker, port, 60)

    # Iniciar el loop para manejar eventos
    client.loop_start()

    # Publicar mensaje
    data = {"temperature": 55}
    print(f"Enviando datos: {json.dumps(data)}")
    result = client.publish(topic, json.dumps(data), qos=1)

    # Asegurarse de que el mensaje se publique antes de cerrar
    result.wait_for_publish()
    print("Mensaje enviado.")

except Exception as e:
    print(f"Error al conectar o enviar datos: {e}")

finally:
    # Detener el loop y desconectar
    client.loop_stop()
    client.disconnect()
    print("Conexión cerrada.")
