from gpiozero import LED
from time import sleep

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

def display_number(num):
    """Enciende los segmentos para mostrar un número."""
    # Apagar todos los segmentos
    for segment in segments.values():
        segment.off()
    
    # Encender los segmentos necesarios
    for segment in numbers[num]:
        segments[segment].on()

try:
    while True:
        # Mostrar todos los números del 0 al 9
        for num in '0123456789':
            display_number(num)
            print(f"Mostrando: {num}")
            sleep(1)  # Mostrar cada número durante 1 segundo
except KeyboardInterrupt:
    print("Prueba interrumpida.")
finally:
    # Apagar todos los segmentos
    for segment in segments.values():
        segment.off()
