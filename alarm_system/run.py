from time import sleep, time
from gpiozero import LED, PWMLED, Button, Buzzer
import logging

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("program_log.log"),  # Guardar logs en un archivo
        logging.StreamHandler()  # Mostrar logs en la consola
    ]
)

# TWO-COLOR LED -> S = GPIO17, MIDDLE = GPIO27, -=GND
green = LED(17)
red = LED(27)

# SEVEN FLASH LED -> S=GPIO13, -=GND
seven_flash_led = LED(13)

# RGB LED -> R: PIN 21, G: PIN 20, B: PIN 16
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

# SMALL SOUND SENSOR -> D0=GPIO22, +=3.3V, GND=GND
sound_sensor = Button(22)

# BUZZERS
active_buzzer = Buzzer(18)  # Buzzer activo en el pin 18
passive_buzzer = Buzzer(24)  # Buzzer pasivo en el pin 24

# Estado del LED para el sensor de sonido
led_state = False

# Colores predefinidos para el RGB
colors = {
    "naranja": (1, 0.5, 0),  # Rojo + Verde
    "fucsia": (1, 0, 1),  # Rojo + Azul
    "verde": (0, 1, 0),  # Verde
    "azul": (0, 0, 1),  # Azul
    "rojo": (1, 0, 0),  # Rojo
    "rosa": (1, 0.2, 0.6),  # Mezcla personalizada
    "amarillo": (1, 1, 0),  # Rojo + Verde
    "negro": (0, 0, 0),  # Apagado
    "blanco": (1, 1, 1),  # Máxima intensidad de todos los colores
}


def toggle_led():
    global led_state
    led_state = not led_state
    if led_state:
        logging.info("Encendiendo el SEVEN FLASH LED")
        seven_flash_led.on()
    else:
        logging.info("Apagando el SEVEN FLASH LED")
        seven_flash_led.off()


# Función para establecer un color RGB
def set_rgb_color(r, g, b):
    logging.info(f"Estableciendo color RGB: R={r}, G={g}, B={b}")
    red_pwm.value = r
    green_pwm.value = g
    blue_pwm.value = b


# Función para tocar una canción corta con el buzzer pasivo
def play_song():
    notes = [
        (523, 0.5),  # C5
        (523, 0.5),  # C5
        (784, 0.5),  # G5
        (784, 0.5),  # G5
        (880, 0.5),  # A5
        (880, 0.5),  # A5
        (784, 1),  # G5
        (0, 0.2),  # Pausa

        (659, 0.5),  # E5
        (659, 0.5),  # E5
        (587, 0.5),  # D5
        (587, 0.5),  # D5
        (523, 0.5),  # C5
        (523, 0.5),  # C5
        (784, 1),  # G5
        (0, 0.2),  # Pausa

        (784, 0.5),  # G5
        (784, 0.5),  # G5
        (659, 0.5),  # E5
        (659, 0.5),  # E5
        (587, 0.5),  # D5
        (587, 0.5),  # D5
        (523, 1),  # C5
    ]

    logging.info("Iniciando canción en buzzer pasivo")
    for i, (freq, duration) in enumerate(notes):
        logging.info(f"Nota {i + 1}: Frecuencia={freq}, Duración={duration}s")
        if freq > 0:
            passive_buzzer.beep(on_time=duration, off_time=0, n=1, background=False)
        else:
            sleep(duration)
    logging.info("Canción terminada")


try:
    start_time = time()  # Marca el tiempo inicial
    max_duration = 60  # Duración máxima del programa en segundos

    logging.info("Iniciando programa principal")
    while time() - start_time < max_duration:
        # Control del sensor de sonido
        sound_sensor.when_pressed = toggle_led

        # Control del TWO-COLOR LED
        logging.info("Encendiendo LED verde")
        green.on()
        sleep(2)
        logging.info("Apagando LED verde")
        green.off()
        sleep(2)

        logging.info("Encendiendo LED rojo")
        red.on()
        sleep(2)
        logging.info("Apagando LED rojo")
        red.off()
        sleep(2)

        # Control del RGB LED
        for color_name, (r, g, b) in colors.items():
            logging.info(f"Mostrando color RGB: {color_name}")
            set_rgb_color(r, g, b)
            sleep(2)

        # Activa el buzzer activo como alerta
        logging.info("Activando buzzer activo")
        active_buzzer.on()
        sleep(1)
        logging.info("Desactivando buzzer activo")
        active_buzzer.off()

        # Reproduce una canción corta con el buzzer pasivo
        logging.info("Reproduciendo canción con buzzer pasivo")
        play_song()

    logging.info("Duración máxima alcanzada. Finalizando programa.")

except KeyboardInterrupt:
    logging.warning("Programa interrumpido manualmente")

except Exception as e:
    logging.error(f"Se produjo un error: {e}")

finally:
    # Limpieza de recursos
    logging.info("Realizando limpieza de recursos")
    green.off()
    red.off()
    seven_flash_led.off()
    set_rgb_color(0, 0, 0)  # Apagar el RGB LED
    active_buzzer.off()
    logging.info("Programa finalizado correctamente")
