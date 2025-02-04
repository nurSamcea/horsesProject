import json
import logging
from time import sleep, time

import paho.mqtt.client as mqtt
from gpiozero import LED, PWMLED, Button, Buzzer

COLOR = 0
BUZZER = 1

# Logging configuration
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),
        logging.StreamHandler()
    ]
)


class RGBLed:
    def __init__(self):
        self.red = PWMLED(21)
        self.green = PWMLED(20)
        self.blue = PWMLED(16)

    def set_color(self, r, g, b):
        self.red.value = r
        self.green.value = g
        self.blue.value = b

    def turn_off(self):
        self.set_color(0, 0, 0)


class SevenSegmentDisplay:
    def __init__(self):
        self.segments = {
            'A': LED(25),
            'B': LED(12),
            'C': LED(13),
            'D': LED(19),
            'E': LED(26),
            'F': LED(24),
            'G': LED(23),
            'DP': LED(6)
        }
        self.numbers = {
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

    def off(self):
        for segment in self.segments.values():
            segment.off()

    def display(self, num):
        for segment in self.segments.values():
            segment.off()
        for segment in self.numbers.get(str(num), []):
            self.segments[segment].on()


class HealthMonitor:
    def __init__(self):
        self.num_horses = 10
        self.horses = [("black", False, time())] * self.num_horses
        self.current_horse = -1
        self.stable_led = LED(22)
        self.rgb_led = RGBLed()
        self.buzzer = Buzzer(18)
        self.display = SevenSegmentDisplay()
        self.button = Button(17)
        self.button.when_pressed = self.handle_button_press
        self.TIME = 2
        self.COLOR = 0
        self.BUZZER = 1

    def toggle_stable_led(self, state):
        if state:
            self.stable_led.on()
        else:
            self.stable_led.off()

    def update_actuators(self):
        filtered_horses = [i for i, horse in enumerate(self.horses) if horse[0] not in ["green", "black"]]
        if filtered_horses:
            self.current_horse = max(filtered_horses, key=lambda i: self.horses[i][self.TIME])
            color, buzzer_state, _ = self.horses[self.current_horse]
            self.rgb_led.set_color(*COLORS.get(color, COLORS["black"]))
            self.display.display(self.current_horse)
            if buzzer_state:
                self.buzzer.on()
            else:
                self.buzzer.off()
            logging.info(f"Updated: Horse {self.current_horse}, LED {color}, Buzzer {'ON' if buzzer_state else 'OFF'}")
        else:
            self.display.display('-')
            self.rgb_led.set_color(*COLORS["green"])
            self.buzzer.off()
            logging.info("No horses with alerts")

    def process_message(self, message):
        try:
            data = json.loads(message)
            device_name = data.get("deviceName", "No device")
            if device_name == "stable":
                self.toggle_stable_led(data.get("ledStable", False))
            elif "horse" in device_name:
                horse_number = data.get("horse", -1)
                if 0 <= horse_number < self.num_horses:
                    self.horses[horse_number] = (data.get("led", "black"), data.get("buzzer", False), time())
                    self.update_actuators()
        except Exception as e:
            logging.error(f"Error processing MQTT message: {e}")

    def handle_button_press(self):
        start_time = time()
        while self.button.is_pressed:
            sleep(0.1)
        press_duration = time() - start_time
        if press_duration < 1:
            self.horses[self.current_horse] = ("green", False, time())
            self.update_actuators()
        else:
            alert_message = {"alert": True, "horse": self.current_horse}
            client.publish(TOPIC_TO_PUBLISH, json.dumps(alert_message), qos=1)
            logging.info(f"Message published: horse {self.current_horse}")


# MQTT Configuration
BROKER = "srv-iot.diatel.upm.es"
PORT = 8883
TOPIC_TO_SUBSCRIBE = "v1/devices/me/attributes"
TOPIC_TO_PUBLISH = "v1/devices/me/telemetry"
ACCESS_TOKEN = "7jpi6hyp0jzolihttq45"

monitor = HealthMonitor()
client = mqtt.Client()
client.username_pw_set(ACCESS_TOKEN)
client.tls_set()


COLORS = {"green": (0, 1, 0),
          "red": (1, 0, 0),
          "blue": (0, 0, 1),
          "yellow": (1, 1, 0),
          "black": (0, 0, 0),
          "orange": (1, 0.5, 0),
          "purple": (1, 0, 1)}

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        logging.info("Successfully connected to MQTT broker.")
        client.subscribe(TOPIC_TO_SUBSCRIBE, qos=1)
    else:
        logging.error(f"Error connecting to broker, code: {rc}")


def on_message(client, userdata, msg):
    logging.info(f"Message received: {msg.payload.decode()}")
    monitor.process_message(msg.payload.decode())


client.on_connect = on_connect
client.on_message = on_message

try:
    logging.info(f"Connecting to MQTT broker {BROKER}:{PORT}...")
    client.connect(BROKER, PORT, 60)
    client.loop_start()
except Exception as e:
    logging.error(f"Error connecting to MQTT broker: {e}")


def main():
    logging.info("Monitoring system started.")
    monitor.button.when_pressed = monitor.handle_button_press

    monitor.display.display('-')
    monitor.rgb_led.set_color(*COLORS["black"])
    monitor.buzzer.off()

    try:
        while True:
            sleep(1)
    except KeyboardInterrupt:
        logging.warning("Program manually interrupted.")
    finally:
        logging.info("Shutting down system.")
        monitor.display.off()
        monitor.buzzer.off()
        monitor.rgb_led.turn_off()
        client.loop_stop()
        client.disconnect()
        logging.info("MQTT client disconnected.")


if __name__ == "__main__":
    main()
