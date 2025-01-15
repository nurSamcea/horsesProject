#include <Arduino.h>
#include <Thread.h>
#include <StaticThreadController.h>
#include <ThreadController.h>

#define LED_GREEN 16
#define LED_RED 21

#define TEMP_MAX 28
#define TEMP_MIN 18

#define HUM_MAX 80
#define HUM_MIN 40

class SensorThread: public Thread {
  private:
    float temperature;
    float humidity;
    int pin_led;

  public:
    SensorThread(int _pin_led): Thread() {
      temperature = 23.0;
      humidity = 60.0;

      pin_led = _pin_led;
      pinMode(pin_led, OUTPUT);
    }

    bool shouldRun(unsigned long time) {
      return Thread::shouldRun(time);
    }

    void run() {
      Thread::run();
      temperature += random(-10, 11) / 10.0;
      humidity += random(-10, 11) / 10.0;

      Serial.print("Temperatura: ");
      Serial.print(temperature);
      Serial.println(" °C");

      Serial.print("Humedad: ");
      Serial.print(humidity);
      Serial.println(" %");

      if (temperature > TEMP_MAX || temperature < TEMP_MIN || humidity > HUM_MAX || humidity < HUM_MIN) {
        digitalWrite(pin_led, HIGH);
      } else {
        digitalWrite(pin_led, LOW);
      }
    }

    float getTemp() {
      return temperature;
    }

    float getHum() {
      return humidity;
    }
};

SensorThread *sensor_thread;
ThreadController controller = ThreadController();

void setup() {
  Serial.begin(9600);

  pinMode(LED_GREEN, OUTPUT);
  digitalWrite(LED_GREEN, LOW);

  sensor_thread = new SensorThread(LED_RED);
  sensor_thread->setInterval(60000);
  controller.add(sensor_thread);
}

void loop() {
  controller.run();
  //Serial.println(" %");
}