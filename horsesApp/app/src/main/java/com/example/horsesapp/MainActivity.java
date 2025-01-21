// VeterinaryOperatorApp.java
// Updated code for a veterinarian-exclusive app.
// List of horses with details and pop-up alerts.

package com.example.horsesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public static double DEFAULT_VAL_DOUBLE = -200.0;
    public static final String ACTION_MY_BROADCAST = "com.example.horsesapp.ACTION_MY_BROADCAST";

    public static Map<String, HorseData> horseDataMap = new HashMap<>();

    public static class HorseData {
        public double temperature = DEFAULT_VAL_DOUBLE; // Default temperature
        public int oximetry = -1;    // Default oximetry
        public int heartRate = -1;      // Default heart rate
        public String lastUpdated = "Not updated"; // Last update time
        public double x = DEFAULT_VAL_DOUBLE, y = DEFAULT_VAL_DOUBLE, z = DEFAULT_VAL_DOUBLE;
        public double latitude = DEFAULT_VAL_DOUBLE, longitude = DEFAULT_VAL_DOUBLE;
    }

    private final String[] horses = {
            "horse real 0",
            "horse sim 1",
            "horse sim 2",
            "horse sim 3",
            "horse sim 4",
            "horse sim 5",
            "horse sim 6",
            "horse sim 7",
            "horse sim 8",
            "horse sim 9"};

    String TAG = "HORSE APP MQTT";
    String serverHost = "srv-iot.diatel.upm.es";
    int serverPort = 8883;
    String access_token = "n8qc2l1jo0cpt5w6bhm6";

    String subscriptionTopic = "v1/devices/me/attributes";
    public static String publishingTopic = "v1/devices/me/telemetry";

    public static Mqtt3AsyncClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createMQTTclient();
        connectToBroker();

        // Initialize the horse list
        ListView horseListView = findViewById(R.id.horse_list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                horses
        );
        horseListView.setAdapter(adapter);

        // Set click events for the list
        horseListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedHorse = horses[position];
            Intent intent = new Intent(MainActivity.this, HorseDetailActivity.class);
            intent.putExtra("horseName", selectedHorse);
            intent.putExtra("horseIndex", position); // Pass the horse index
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromBroker();
    }

    void createMQTTclient() {
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier("veterinaryApp")
                .serverHost(serverHost)
                .serverPort(serverPort)
                .sslWithDefaultConfig() // Configuration to use TLS
                .buildAsync();
    }

    void connectToBroker() {
        if (client != null) {
            client.connectWith()
                    .simpleAuth()
                    .username(access_token)
                    .applySimpleAuth()
                    .send()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            Log.d(TAG, "Problem connecting to server:");
                            Log.d(TAG, throwable.toString());
                        } else {
                            Log.d(TAG, "Connected to server");
                            subscribeToTopic();
                        }
                    });
        } else {
            Log.d(TAG, "Cannot connect client (null)");
        }
    }

    void subscribeToTopic() {
        client.subscribeWith()
                .topicFilter(subscriptionTopic)
                .callback(publish -> {
                    String receivedMessage = new String(publish.getPayloadAsBytes());
                    Log.d(TAG, "Message received: " + receivedMessage);

                    try {
                        JSONObject jsonMessage = new JSONObject(receivedMessage);

                        String deviceName = jsonMessage.optString("deviceName", "unknown");

                        if (deviceName.equals("stable")) {
                            int a = 1;
                        } else {
                            double temperature = jsonMessage.optDouble("temperature", DEFAULT_VAL_DOUBLE);
                            int oximetry = jsonMessage.optInt("oximetry", -1);
                            int heartRate = jsonMessage.optInt("HR", -1);
                            double x = jsonMessage.optDouble("x", DEFAULT_VAL_DOUBLE);
                            double y = jsonMessage.optDouble("y", DEFAULT_VAL_DOUBLE);
                            double z = jsonMessage.optDouble("z", DEFAULT_VAL_DOUBLE);
                            double latitude = jsonMessage.optDouble("lat", DEFAULT_VAL_DOUBLE);
                            double longitude = jsonMessage.optDouble("long", DEFAULT_VAL_DOUBLE);
                            boolean alert = jsonMessage.optBoolean("alert", false); // Detect if there's an alert

                            // Get current time
                            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                            // Update data in the map
                            MainActivity.HorseData horseData = MainActivity.horseDataMap.getOrDefault(deviceName, new MainActivity.HorseData());
                            if (temperature != DEFAULT_VAL_DOUBLE)
                                horseData.temperature = temperature;
                            if (oximetry != -1) horseData.oximetry = oximetry;
                            if (heartRate != -1) horseData.heartRate = heartRate;
                            if (x != DEFAULT_VAL_DOUBLE && y != DEFAULT_VAL_DOUBLE && z != DEFAULT_VAL_DOUBLE) {
                                horseData.x = x;
                                horseData.y = y;
                                horseData.z = z;
                            }
                            if (latitude != DEFAULT_VAL_DOUBLE && longitude != DEFAULT_VAL_DOUBLE) {
                                horseData.latitude = latitude;
                                horseData.longitude = longitude;
                            }
                            horseData.lastUpdated = currentTime;

                            MainActivity.horseDataMap.put(deviceName, horseData);

                            // Send broadcast
                            Intent intent = new Intent(ACTION_MY_BROADCAST);
                            intent.putExtra("horseName", deviceName);
                            intent.putExtra("temperature", temperature);
                            intent.putExtra("oximetry", oximetry);
                            intent.putExtra("heartRate", heartRate);
                            intent.putExtra("lastUpdated", currentTime);
                            intent.putExtra("x", x);
                            intent.putExtra("y", y);
                            intent.putExtra("z", z);
                            intent.putExtra("lat", latitude);
                            intent.putExtra("long", longitude);
                            sendBroadcast(intent);

                            // Show alert if alert=true
                            if (alert) {
                                runOnUiThread(() -> showAlert(deviceName, temperature, oximetry, heartRate));
                            }
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                    }
                })
                .send();
    }

    private void showAlert(String deviceName, double temperature, double oximetry, int heartRate) {
        String alertMessage = String.format(
                "Alert detected for %s!\n\nTemperature: %.1f °C\nOximetry: %.1f%%\nHeart rate: %d bpm",
                deviceName,
                temperature,
                oximetry,
                heartRate
        );

        new AlertDialog.Builder(this)
                .setTitle("CRITICAL ALERT")
                .setMessage(alertMessage)
                .setCancelable(false)
                .setPositiveButton("On my way", (dialog, which) -> {
                    Toast.makeText(this, "Marked as on the way.", Toast.LENGTH_SHORT).show();

                    try {
                        // Create the payload as a JSONObject
                        JSONObject payloadJson = new JSONObject();
                        payloadJson.put("speakerDeviceName", "");
                        payloadJson.put("status", true); // Add status as true

                        // Convert the JSONObject to a string
                        String payload = payloadJson.toString();

                        // Publish the message to the MQTT topic
                        publishMessage(payload);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error building the payload JSON: " + e.getMessage());
                    }
                })
                .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void publishMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            Log.d(TAG, "Message is empty. It will not be published.");
            return;
        }

        Log.d(TAG, "Publishing message: " + payload); // Verify the message content

        if (client == null || !client.getState().isConnected()) {
            Log.d(TAG, "MQTT client is not connected. Cannot publish.");
            return;
        }

        client.publishWith()
                .topic(publishingTopic)
                .payload(payload.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Error publishing the message to topic " + publishingTopic, throwable);
                    } else {
                        Log.d(TAG, "Message published to topic: " + publishingTopic);
                    }
                });
    }

    void disconnectFromBroker() {
        if (client != null) {
            client.disconnect()
                    .whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            Log.d(TAG, "Problem disconnecting from server:");
                            Log.d(TAG, throwable.toString());
                        } else {
                            Log.d(TAG, "Disconnected from server");
                        }
                    });
        } else {
            Log.d(TAG, "Cannot disconnect client (null)");
        }
    }

}