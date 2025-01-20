## Stable

ESP32

- sensors/publish: similated humidity and temperature -> **reading workers system and app**
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
