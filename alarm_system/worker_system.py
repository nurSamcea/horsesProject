from gpiozero import LED, PWMLED, Button, Buzzer
from time import sleep, time
import logging

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[
        logging.FileHandler("health_monitor_log.log"),
        logging.StreamHandler()
    ]
)

# Configuración de LEDs y sensores
# LEDs para indicar estados
red_pwm = PWMLED(21)
green_pwm = PWMLED(20)
blue_pwm = PWMLED(16)

# Buzzer para alarmas
buzzer = Buzzer(18)

# Botón para control del sistema
button = Button(17)

# Configuración de pines GPIO para cada segmento
segments = {
    'A': LED(25),
    'B': LED(12),
    'C': LED(13),
    'D': LED(19),
    'E': LED(26),
    'F': LED(24),
    'G': LED(23),
    'DP': LED(6)  # Punto decimal (opcional)
}

# Mapas para formar números
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
    red_pwm.value = r
    green_pwm.value = g
    blue_pwm.value = b

def display_number(num):
    """Enciende los segmentos para mostrar un número."""
    for segment in segments.values():
        segment.off()
    for segment in numbers[num]:
        segments[segment].on()

def display_message():
    """Muestra un mensaje en el display (simulación de "LLAMAR VET")."""
    for _ in range(3):
        for segment in segments.values():
            segment.on()
        sleep(0.5)
        for segment in segments.values():
            segment.off()
        sleep(0.5)

def sound_alert_triggered():
    """Activa alarma visual, sonora y código en pantalla."""
    logging.warning("Actividad anormal detectada. Activando alarma.")
    set_rgb_color(*colors["rojo"])
    buzzer.on()
    display_number('1')
    sleep(2)
    buzzer.off()
    set_rgb_color(*colors["negro"])

def button_pressed():
    """Maneja el evento del botón: restablecer o llamar al veterinario."""
    start_time = time()
    while button.is_pressed:
        sleep(0.1)
    press_duration = time() - start_time

    if press_duration < 2:
        logging.info("Botón presionado: Restableciendo pantalla y sonido.")
        buzzer.off()
        display_number('0')
    else:
        logging.info("Pulsación prolongada detectada: Llamar al veterinario.")
        display_message()

def monitor_health():
    """Simulación de monitoreo de salud basado en condiciones aleatorias."""
    import random
    temperature = random.uniform(36.5, 40.5)  # Temperatura simulada
    if temperature < 37 or temperature > 39.5:
        sound_alert_triggered()
    else:
        logging.info(f"Temperatura normal: {temperature:.1f}°C")
        set_rgb_color(*colors["verde"])
        display_number('0')
        sleep(1)
        set_rgb_color(*colors["negro"])

def main():
    """Función principal para gestionar el sistema."""
    logging.info("Iniciando sistema de monitoreo de salud.")

    try:
        while True:
            monitor_health()
            button.when_pressed = button_pressed
            sleep(10)
    except KeyboardInterrupt:
        logging.warning("Programa interrumpido manualmente.")
    finally:
        logging.info("Apagando sistema.")
        for segment in segments.values():
            segment.off()
        buzzer.off()
        set_rgb_color(*colors["negro"])

if __name__ == "__main__":
    main()
