#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

#define NUM_HORSES 10
#define MAX_JSON 200

#define LED_GREEN 16
#define LED_YELLOW 19

#define TEMP_MAX 28
#define TEMP_MIN 18

#define HUM_MAX 80
#define HUM_MIN 40

#define FREC_READ 5000 // 5 seconds
#define FREC_PUBLISH 30000 // 30 seconds

// Credencials Wi-Fi
const char* ssid = "iPhone de Lara";
const char* password = "";

// Configuration MQTT
const char* mqtt_server = "srv-iot.diatel.upm.es";
const int mqtt_port = 8883;
const char* mqtt_topic_pub = "v1/devices/me/telemetry";
const char* mqtt_topic_sub = "v1/devices/me/attributes";
const char* mqtt_token = "7lXu9ibhq7fLlzXdawoH"; 

// Clients Wi-Fi y MQTT
WiFiClientSecure espClient;
PubSubClient client(espClient);

void setup_wifi() {
  delay(10);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected.");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP());

  // Verify server connectivity
  IPAddress serverIP;
  if (!WiFi.hostByName(mqtt_server, serverIP)) {
    Serial.println("Error resolving the MQTT server address");
  } else {
    Serial.print("MQTT server accessible: ");
    Serial.println(serverIP);
  }
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Connecting to MQTT...");
    if (client.connect("Esp32", mqtt_token, NULL)) {
      Serial.println("connected");

      client.subscribe(mqtt_topic_sub); 
      Serial.print("Subscribe to topic: ");
      Serial.println(mqtt_topic_sub);
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" Trying again in 5 seconds...");
      delay(5000);
    }
  }
}

bool horses[NUM_HORSES];

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received on topic: ");
  Serial.println(topic);

  if (strcmp(topic, mqtt_topic_sub) == 0) {    
    // Convert payload to string
    char message[length + 1];
    memcpy(message, payload, length);
    message[length] = '\0';

    StaticJsonDocument<MAX_JSON> doc;
    DeserializationError error = deserializeJson(doc, message);

    if (error) {
      Serial.print("deserializeJson() failed: ");
      Serial.println(error.f_str());
      return;
    }

    bool led = doc["led"];
    const char* deviceName = doc["deviceName"];
    int horse = doc["horse"];

    Serial.print("led: ");
    Serial.println(led);
    Serial.print("deviceName: ");
    Serial.println(deviceName);
    Serial.print("horse: ");
    Serial.println(horse);

    if (horse >= 0 && horse < NUM_HORSES) {
      horses[horse] = led;
      int status_led = LOW;

      for (int i = 0; i < NUM_HORSES; i++) {
        if (horses[i] == true) {
          status_led = HIGH;
          break;
        }
      }
      digitalWrite(LED_YELLOW, status_led);
    }
  }
}

float temperature;
float humidity;

int time_read;
int time_pub;

float counter;
float sum_temp;
float sum_hum;

void setup() {
  Serial.begin(9600);

  setup_wifi();
  espClient.setInsecure(); // Disable TLS
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);

  // Led stable status
  pinMode(LED_GREEN, OUTPUT);
  digitalWrite(LED_GREEN, LOW);

  // Led horses 
  pinMode(LED_YELLOW, OUTPUT);
  digitalWrite(LED_YELLOW, LOW);

  // Init variables
  counter = 0.0;
  sum_temp = 0.0;
  sum_hum = 0.0;

  temperature = 23.0;
  humidity = 60.0;

  for(int i = 0; i < NUM_HORSES; i++) {
    horses[i] = false;
  }

  time_read = millis();
  time_pub = millis();
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  if (millis()- time_read >= FREC_READ) {
    temperature += random(-10, 11) / 10.0;
    humidity += random(-10, 11) / 10.0;

    Serial.print("Temperature: ");
    Serial.print(temperature);
    Serial.println(" °C");

    Serial.print("Humidity: ");
    Serial.print(humidity);
    Serial.println(" %");

    if (temperature > TEMP_MAX || temperature < TEMP_MIN || humidity > HUM_MAX || humidity < HUM_MIN) {
      digitalWrite(LED_GREEN, HIGH);
    } else {
      digitalWrite(LED_GREEN, LOW);
    }

    // Update avg variables
    counter += 1.0;
    sum_temp += temperature;
    sum_hum += humidity;

    // Reset
    time_read = millis();
  }

  if (millis() - time_pub >= FREC_PUBLISH) {
    float avg_temp = sum_temp / counter;
    float avg_hum = sum_hum / counter;

    avg_temp = 39.8;

    String payload = "{\"temperature\":" + String(avg_temp) + 
                    ",\"humidity\":" + String(avg_hum) + "}";

    if (client.publish(mqtt_topic_pub, payload.c_str())) {
      Serial.println("Message published successfully");
      Serial.println(payload);
    } else {
      Serial.println("Error publishing the message");
    }

    // Reset
    counter = 0.0;
    sum_temp = 0.0;
    sum_hum = 0.0;
    time_pub = millis();
  }
}
