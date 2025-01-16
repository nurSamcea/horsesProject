// VeterinaryOperatorApp.java
// Código actualizado para una app exclusiva para veterinarios.
// Lista de caballos con detalles y alertas emergentes.

package com.example.horsesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

public class MainActivity extends AppCompatActivity {

    private final String[] horses = {"Horse 0", "Horse 1", "Horse 2", "Horse 3", "Horse 4",
            "Horse 5", "Horse 6", "Horse 7", "Horse 8", "Horse 9"};

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
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.d(TAG, "Problem subscribing to topic:");
                        Log.d(TAG, throwable.toString());
                    } else {
                        Log.d(TAG, "Subscribed to topic");
                    }
                });
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
