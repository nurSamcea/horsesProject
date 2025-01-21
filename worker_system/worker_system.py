import json
import logging
from time import sleep, time

import paho.mqtt.client as mqtt
from gpiozero import LED, PWMLED, Button, Buzzer

NUM_HORSES = 10

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),
        logging.StreamHandler()
    ]
)

# stable led ->
# Patilla larga (ánodo): conectada a la resistencia y la resistencia al pin GPIO22.
# Patilla corta (cátodo): conectada a tierra (GND).
stable_led = LED(22)

# Configuración de LEDs y sensores
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

buzzer = Buzzer(18)
button = Button(17)

currentHorse = -1
horses_array = [("green", time.time())] * NUM_HORSES

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
    "negro": (0, 0, 0),
    "purple": (1, 0, 1)
}


def toggle_stable_led(state):
    if state:
        stable_led.on()
    else:
        stable_led.off()


def set_rgb_color(r, g, b):
    red_pwm.value = r
    green_pwm.value = g
    blue_pwm.value = b


def display_number(num):
    for segment in segments.values():
        segment.off()
    for segment in numbers.get(num, []):
        segments[segment].on()


def process_message(message):
    try:
        data = json.loads(message)

        device_name = data.get("deviceName", "Ningun dispositivo")

        if (device_name == "stable"):
            stable_state = data.get("stable_led", False)
            toggle_stable_led(stable_state)

        led_color = data.get("led", "negro")
        horse_number = data.get("horse", 0)
        buzzer_state = data.get("buzzer", False)


        # Actualizar LEDs
        color = colors.get(led_color, colors["negro"])
        set_rgb_color(*color)

        # Mostrar número del caballo
        display_number(str(horse_number))

        # Actualizar estado del buzzer
        if buzzer_state:
            buzzer.on()
        else:
            buzzer.off()

        logging.info(
            f"Actualizado: Caballo {horse_number} // deviceName = {device_name}, LED {led_color}, Buzzer {'ON' if buzzer_state else 'OFF'}, Stable LED {'ON' if stable_state else 'OFF'}")

    except Exception as e:
        logging.error(f"Error procesando mensaje MQTT: {e}")


def handle_button_press():
    start_time = time()
    while button.is_pressed:
        sleep(0.1)
    press_duration = time() - start_time

    if press_duration < 1:
        # Restablecer a 0
        logging.info("Botón presionado brevemente: Restableciendo a estado inicial.")
        display_number('0')
        set_rgb_color(*colors["verde"])
        buzzer.off()
    else:
        # Llamar al veterinario
        alert_message = {"alert": True, "horse": currentHorse}
        client.publish(topic_to_publish, json.dumps(alert_message), qos=1)
        logging.info("Mensaje publicado: Llamar al veterinario.")


# Configuración de MQTT
broker = "srv-iot.diatel.upm.es"
port = 8883
topic_to_subscribe = "v1/devices/me/attributes"
topic_to_publish = "v1/devices/me/telemetry"
access_token = "7jpi6hyp0jzolihttq45"

client = mqtt.Client()
client.username_pw_set(access_token)
client.tls_set()


def on_connect(client, userdata, flags, rc):
    if rc == 0:
        logging.info("Conexión al broker MQTT exitosa.")
        client.subscribe(topic_to_subscribe, qos=1)
    else:
        logging.error(f"Error al conectar con el broker, código: {rc}")


def on_message(client, userdata, msg):
    logging.info(f"Mensaje recibido: {msg.payload.decode()}")
    process_message(msg.payload.decode())


client.on_connect = on_connect
client.on_message = on_message

try:
    logging.info(f"Conectando al broker MQTT {broker}:{port}...")
    client.connect(broker, port, 60)
    client.loop_start()
except Exception as e:
    logging.error(f"Error al conectar con el broker MQTT: {e}")


def main():
    logging.info("Sistema de monitoreo iniciado.")
    button.when_pressed = handle_button_press
    try:
        while True:
            sleep(1)
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
