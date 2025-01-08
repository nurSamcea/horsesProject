import time

import RPi.GPIO as GPIO

GPIO.setmode(GPIO.BCM)
GPIO.setup(17, GPIO.OUT)
GPIO.setup(27, GPIO.OUT)

try:
    while True:
        GPIO.output(17, GPIO.HIGH)  # Encender LED rojo
        GPIO.output(27, GPIO.LOW)
        time.sleep(1)
        GPIO.output(17, GPIO.LOW)
        GPIO.output(27, GPIO.HIGH)  # Encender LED verde
        time.sleep(1)
except KeyboardInterrupt:
    GPIO.cleanup()
