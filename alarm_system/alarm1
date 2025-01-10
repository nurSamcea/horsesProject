from time import sleep, time
from gpiozero import LED, PWMLED, Button, Buzzer
import logging

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),  # Guardar logs en un archivo
        logging.StreamHandler()  # Mostrar logs en la consola
    ]
)

# Configuración de LEDs y sensores
# LEDs para indicar estados
health_led = LED(17)  # Verde: Salud estable
alert_led = LED(27)  # Rojo: Alerta detectada

# RGB LED -> Indicadores adicionales
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

# Buzzer para alarmas
buzzer = Buzzer(18)

# Sensor de sonido para actividad
sound_sensor = Button(22)

# Estado inicial
system_active = True

# Colores predefinidos para el RGB
colors = {
    "verde": (0, 1, 0),  # Verde: Estable
    "rojo": (1, 0, 0),  # Rojo: Alerta
    "azul": (0, 0, 1),  # Azul: Información adicional
    "amarillo": (1, 1, 0),  # Rojo + Verde: Precaución
    "negro": (0, 0, 0),  # Apagado
}

def set_rgb_color(r, g, b):
    """Establecer un color en el LED RGB."""
    logging.info(f"Estableciendo color RGB: R={r}, G={g}, B={b}")
    red_pwm.value = r
    green_pwm.value = g
    blue_pwm.value = b

def sound_alert_triggered():
    """Función activada cuando el sensor de sonido detecta actividad anormal."""
    logging.warning("Actividad anormal detectada. Activando alarma de sonido.")
    alert_led.on()
    set_rgb_color(*colors["rojo"])
    buzzer.on()
    sleep(2)
    buzzer.off()
    alert_led.off()
    set_rgb_color(*colors["negro"])

def monitor_health():
    """Simulación de monitoreo de salud basado en condiciones aleatorias."""
    logging.info("Verificando estado de salud...")
    # Simular condiciones de salud (en una implementación real, se tomarían datos de sensores)
    import random
    temperature = random.uniform(36.5, 40.5)  # Temperatura simulada
    activity_level = random.randint(0, 100)  # Nivel de actividad simulado

    if temperature < 37 or temperature > 39.5:
        logging.warning(f"Temperatura anormal detectada: {temperature} °C")
        alert_led.on()
        set_rgb_color(*colors["amarillo"])
        buzzer.on()
        sleep(3)
        buzzer.off()
        alert_led.off()
        set_rgb_color(*colors["negro"])
    else:
        logging.info(f"Temperatura normal: {temperature} °C")
        health_led.on()
        set_rgb_color(*colors["verde"])
        sleep(2)
        health_led.off()
        set_rgb_color(*colors["negro"])

    if activity_level < 20:
        logging.warning(f"Baja actividad detectada: {activity_level}")
        set_rgb_color(*colors["azul"])
        buzzer.on()
        sleep(2)
        buzzer.off()
        set_rgb_color(*colors["negro"])
    else:
        logging.info(f"Nivel de actividad adecuado: {activity_level}")

def main():
    """Función principal para gestionar el sistema de alarmas."""
    global system_active
    logging.info("Iniciando sistema de monitoreo de salud equina")

    # Configurar sensor de sonido
    sound_sensor.when_pressed = sound_alert_triggered

    try:
        while system_active:
            monitor_health()
            sleep(10)  # Intervalo entre chequeos
    except KeyboardInterrupt:
        logging.warning("Programa interrumpido manualmente.")
    except Exception as e:
        logging.error(f"Se produjo un error: {e}")
    finally:
        logging.info("Apagando sistema y limpiando recursos.")
        health_led.off()
        alert_led.off()
        set_rgb_color(*colors["negro"])
        buzzer.off()
        logging.info("Sistema apagado correctamente.")

if __name__ == "__main__":
    main()
