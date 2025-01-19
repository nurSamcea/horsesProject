// VeterinaryOperatorApp.java
// Código actualizado para una app exclusiva para veterinarios.
// Lista de caballos con detalles y alertas emergentes.

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

    public static Map<String, HorseData> horseDataMap = new HashMap<>();

    public static class HorseData {
        public double temperature = -1; // Temperatura predeterminada
        public double oximetry = -1;    // Oxímetro predeterminado
        public int heartRate = -1;      // Frecuencia cardíaca predeterminada
        public String lastUpdated = "Sin actualizar"; // Hora de última actualización
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

        // Inicializar la lista de caballos
        ListView horseListView = findViewById(R.id.horse_list_view);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                horses
        );
        horseListView.setAdapter(adapter);

        // Configurar clics en la lista
        horseListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedHorse = horses[position];
            Intent intent = new Intent(MainActivity.this, HorseDetailActivity.class);
            intent.putExtra("horseName", selectedHorse);
            intent.putExtra("horseIndex", position); // Pasa el índice del caballo
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
                .sslWithDefaultConfig() // Configuración para usar TLS
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
                        double temperature = jsonMessage.optDouble("temperature", -1);
                        double oximetry = jsonMessage.optDouble("oximetry", -1);
                        int heartRate = jsonMessage.optInt("HR", -1);
                        boolean alert = jsonMessage.optBoolean("alert", false); // Detectar si hay una alerta

                        // Obtener hora actual
                        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                        // Actualizar datos en el mapa
                        MainActivity.HorseData horseData = MainActivity.horseDataMap.getOrDefault(deviceName, new MainActivity.HorseData());
                        if (temperature != -1) horseData.temperature = temperature;
                        if (oximetry != -1) horseData.oximetry = oximetry;
                        if (heartRate != -1) horseData.heartRate = heartRate;
                        horseData.lastUpdated = currentTime;

                        MainActivity.horseDataMap.put(deviceName, horseData);

                        // Enviar difusión
                        Intent intent = new Intent("UPDATE_HORSE_DETAILS");
                        intent.putExtra("horseName", deviceName);
                        intent.putExtra("temperature", temperature);
                        intent.putExtra("oximetry", oximetry);
                        intent.putExtra("heartRate", heartRate);
                        intent.putExtra("lastUpdated", currentTime);
                        sendBroadcast(intent);

                        // Mostrar alerta si alert=true
                        if (alert) {
                            runOnUiThread(() -> showAlert(deviceName, temperature, oximetry, heartRate));
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                    }
                })
                .send();
    }

    private void showAlert(String deviceName, double temperature, double oximetry, int heartRate) {
        String alertMessage = String.format(
                "¡Alerta detectada para %s!\n\nTemperatura: %.1f °C\nOxímetro: %.1f%%\nFrecuencia cardíaca: %d bpm",
                deviceName,
                temperature,
                oximetry,
                heartRate
        );

        new AlertDialog.Builder(this)
                .setTitle("ALERTA CRÍTICA")
                .setMessage(alertMessage)
                .setCancelable(false)
                .setPositiveButton("Voy en camino", (dialog, which) -> {
                    Toast.makeText(this, "Marcado como en camino.", Toast.LENGTH_SHORT).show();

                    try {
                        // Crear el payload como un JSONObject
                        JSONObject payloadJson = new JSONObject();
                        payloadJson.put("speakerDeviceName", "");
                        payloadJson.put("status", true); // Agregar status como true

                        // Convertir el JSONObject a cadena
                        String payload = payloadJson.toString();

                        // Publicar el mensaje al tópico MQTT
                        publishMessage(payload);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error al construir el JSON del payload: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void publishMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            Log.d(TAG, "El mensaje está vacío. No se publicará.");
            return;
        }

        Log.d(TAG, "Publicando mensaje: " + payload); // Verificar el contenido del mensaje

        if (client == null || !client.getState().isConnected()) {
            Log.d(TAG, "El cliente MQTT no está conectado. No se puede publicar.");
            return;
        }

        client.publishWith()
                .topic(publishingTopic)
                .payload(payload.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Error publicando el mensaje en el tópico " + publishingTopic, throwable);
                    } else {
                        Log.d(TAG, "Mensaje publicado en el tópico: " + publishingTopic);
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
