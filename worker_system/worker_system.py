import json
import logging
from time import sleep, time

import paho.mqtt.client as mqtt
from gpiozero import LED, PWMLED, Button, Buzzer

NUM_HORSES = 10
COLOR = 0
BUZZER = 1
TIME = 2

# Logging configuration
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),
        logging.StreamHandler()
    ]
)

stable_led = LED(22)

# LED and sensor configuration
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

buzzer = Buzzer(18)
button = Button(17)

current_horse = -1
horses_array = [("black", False, time())] * NUM_HORSES

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
    "-": ['DP'],
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
    "green": (0, 1, 0),
    "red": (1, 0, 0),
    "blue": (0, 0, 1),
    "yellow": (1, 1, 0),
    "black": (0, 0, 0),
    "orange": (1, 0.5, 0),
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

def update_actuators():
    actual_horse = -1
    filtered_horses = [i for i, horse in enumerate(horses_array) if not "green" in horse[COLOR] and not "black" in horse[COLOR]]
    
    if filtered_horses:
        actual_horse = max(filtered_horses, key=lambda i: horses_array[i][TIME])
        led_color = horses_array[actual_horse][COLOR]
        buzzer_state = horses_array[actual_horse][BUZZER]
        logging.info(f"led_color: {led_color}, horse_number: {actual_horse}")

        # Update LEDs
        color = colors.get(led_color, colors["black"])
        set_rgb_color(*color)

        # Display horse number
        display_number(str(actual_horse))

        # Update buzzer state
        if buzzer_state:
            buzzer.on()
        else:
            buzzer.off()

        logging.info(
            f"Updated: Horse {actual_horse}, LED {led_color}, Buzzer {'ON' if buzzer_state else 'OFF'}")
    else:
        logging.info("No horses with alerts")
        display_number('-')
        set_rgb_color(*colors["green"])
        buzzer.off()

    return actual_horse

def process_message(message):
    try:
        data = json.loads(message)

        device_name = data.get("deviceName", "No device")

        if device_name == "stable":
            stable_state = data.get("ledStable", False)
            toggle_stable_led(stable_state)
            logging.info(f"stable_led: {stable_state}")
        elif "horse" in device_name:
            horse_number = data.get("horse", -1)
            if horse_number < 0:
                return

            # Update buffer
            horses_array[horse_number] = (data.get("led", "black"), data.get("buzzer", False), time())
            update_actuators()

    except Exception as e:
        logging.error(f"Error processing MQTT message: {e}")

def handle_button_press():
    global current_horse

    start_time = time()
    while button.is_pressed:
        sleep(0.1)
    press_duration = time() - start_time

    if press_duration < 1:
        # Reset to 0
        logging.info(f"Button briefly pressed: Clearing alert for horse {current_horse}")
        horses_array[current_horse] = ("green", False, time())
        logging.info(f"Button briefly pressed: Clearing alert for horse {current_horse}, horse_array: {horses_array}")
        current_horse = update_actuators()
    else:
        # Call the vet
        alert_message = {"alert": True, "horse": current_horse}
        client.publish(topic_to_publish, json.dumps(alert_message), qos=1)
        logging.info(f"Message published: horse {current_horse}")

# MQTT configuration
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
        logging.info("Successfully connected to MQTT broker.")
        client.subscribe(topic_to_subscribe, qos=1)
    else:
        logging.error(f"Error connecting to broker, code: {rc}")

def on_message(client, userdata, msg):
    logging.info(f"Message received: {msg.payload.decode()}")
    process_message(msg.payload.decode())

client.on_connect = on_connect
client.on_message = on_message

try:
    logging.info(f"Connecting to MQTT broker {broker}:{port}...")
    client.connect(broker, port, 60)
    client.loop_start()
except Exception as e:
    logging.error(f"Error connecting to MQTT broker: {e}")

def main():
    logging.info("Monitoring system started.")
    button.when_pressed = handle_button_press

    display_number('-')
    set_rgb_color(*colors["black"])
    buzzer.off()

    try:
        while True:
            sleep(1)
    except KeyboardInterrupt:
        logging.warning("Program manually interrupted.")
    finally:
        logging.info("Shutting down system.")
        for segment in segments.values():
            segment.off()
        buzzer.off()
        set_rgb_color(*colors["black"])
        client.loop_stop()
        client.disconnect()
        logging.info("MQTT client disconnected.")

if __name__ == "__main__":
    main()
