## Stable

ESP32

- sensors/publish: similated humidity and temperature -> **read workers system (led) and app (show)**
- Actuators:
  - Green led: alarms conditions of the stable.
  - Yellow led: **subcribe to horses**: Maintain an array of 10 horses, tracking the state of each one. If any of them is sick, the LED turns on.
- Attributes:
  - led: bool
  - horse: number
  - deviceName: String
- Limits:
  - TEMP_MAX 28
  - TEMP_MIN 18
  - HUM_MAX 80
  - HUM_MIN 40
    
## Worker system

Rasp Berry

- sensors/publish:
   - press buttom long -> **read by the app**
   - press buttom short -> reset alarms in the own board
- Actuators:
    - LED RGB: **subscribe to horses**: : Maintain an array of 10 horses, tracking the state of each one. If any of them is sick, the LED turns on, showing the most serious one (purple) or the last one to trigger an alarm.
    - LED: **subscribe to stable**
- Attributes:
  - led: string (color)
  - horse: number
  - deviceName: String
  - ledStable: bool
  - buzzer: bool
 
## Horse

Board
Simulated in node-red

- sensors/publish: temperature, HR, oximetry, x, y, z, lat, long
  - **sub by the app** all varaibles
  - **sub by the stable** status of a led
  - **sub by the worker system** status of the led RGB y buzzer
- Limits correct:
  - z > 0
  - 36 <= HR <= 40
  - 95 <= oxi <= 100
  - 37.5 <= temp <= 38.5
    
