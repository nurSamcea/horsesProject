#include <Arduino.h>
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>

#define LED_GREEN 16

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
const char* mqtt_topic = "v1/devices/me/telemetry";
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
    } else {
      Serial.print("Failed, rc=");
      Serial.print(client.state());
      Serial.println(" Trying again in 5 seconds...");
      delay(5000);
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

  // Led stable status
  pinMode(LED_GREEN, OUTPUT);
  digitalWrite(LED_GREEN, LOW);

  // Init variables
  counter = 0.0;
  sum_temp = 0.0;
  sum_hum = 0.0;

  temperature = 23.0;
  humidity = 60.0;

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

    String payload = "{\"temperature\":" + String(avg_temp) + 
                    ",\"humidity\":" + String(avg_hum) + "}";

    if (client.publish(mqtt_topic, payload.c_str())) {
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
