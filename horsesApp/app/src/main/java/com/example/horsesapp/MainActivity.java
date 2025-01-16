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
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public static Map<String, HorseData> horseDataMap = new HashMap<>();

    public static class HorseData {
        public double temperature = -1; // Temperatura predeterminada
        public double oximetry = -1;    // Oxímetro predeterminado
        public int heartRate = -1;      // Frecuencia cardíaca predeterminada
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
    String publishingTopic = "v1/devices/me/telemetry";

    Mqtt3AsyncClient client;

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

        // Simular una alerta (puedes integrarlo con datos reales más adelante)
        simulateAlert();
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

                        // Almacenar los datos en el mapa estático
                        MainActivity.HorseData horseData = MainActivity.horseDataMap.getOrDefault(deviceName, new MainActivity.HorseData());

                        // Solo actualizar si los valores son válidos
                        if (temperature != -1) horseData.temperature = temperature;
                        if (oximetry != -1) horseData.oximetry = oximetry;
                        if (heartRate != -1) horseData.heartRate = heartRate;

                        MainActivity.horseDataMap.put(deviceName, horseData);

                        // Enviar la difusión
                        Intent intent = new Intent("UPDATE_HORSE_DETAILS");
                        intent.putExtra("horseName", deviceName);
                        intent.putExtra("temperature", temperature);
                        intent.putExtra("oximetry", oximetry);
                        intent.putExtra("heartRate", heartRate);
                        sendBroadcast(intent);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON: " + e.getMessage());
                    }
                })
                .send();
    }

    private void publishMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            // Si el mensaje está vacío, no hacemos nada
            Log.d(TAG, "El mensaje está vacío. No se publicará.");
            return;
        }

        if (client == null || !client.getState().isConnected()) {
            // Verifica que el cliente MQTT esté conectado
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

    private void simulateAlert() {
        String alertMessage = "Temperatura alta detectada en Caballo 1.";

        new AlertDialog.Builder(this)
                .setTitle("Alerta: Caballo 1")
                .setMessage(alertMessage)
                .setPositiveButton("Voy en camino", (dialog, which) -> {
                    Toast.makeText(this, "Marcado como en camino.", Toast.LENGTH_SHORT).show();
                    String defaultPayload = "Caballo 1: Operario en camino. Alerta: " + alertMessage;
                    publishMessage(defaultPayload); // Publica un mensaje predefinido
                })
                .setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
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
