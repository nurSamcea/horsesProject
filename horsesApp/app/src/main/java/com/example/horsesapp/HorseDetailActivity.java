package com.example.horsesapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class HorseDetailActivity extends AppCompatActivity {

    private static final String TAG = "MQTT";
    private int horseIndex;
    private TextView temperatureView, oximetryView, hrView, horseNameView, lastUpdatedView;
    private Button activateSpeakerButton; // Button to activate the speaker

    String TAG1 = "FRONT MESSAGE mqtt";

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceName = intent.getStringExtra("horseName");
            double temperature = intent.getDoubleExtra("temperature", -1);
            double oximetry = intent.getDoubleExtra("oximetry", -1);
            int heartRate = intent.getIntExtra("heartRate", -1);
            String lastUpdated = intent.getStringExtra("lastUpdated");

            if (deviceName != null && deviceName.equals(horseNameView.getText().toString())) {
                if (temperature != -1) {
                    temperatureView.setText(String.format("Temperature: %.1f °C", temperature));
                }
                if (oximetry != -1) {
                    oximetryView.setText(String.format("Oximeter: %.1f%%", oximetry));
                }
                if (heartRate != -1) {
                    hrView.setText(String.format("Heart Rate: %d bpm", heartRate));
                }
                if (lastUpdated != null) {
                    lastUpdatedView.setText(String.format("Last updated: %s", lastUpdated));
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horse_detail);

        horseNameView = findViewById(R.id.horse_name);
        temperatureView = findViewById(R.id.temperature);
        oximetryView = findViewById(R.id.oximetry);
        hrView = findViewById(R.id.heart_rate);
        lastUpdatedView = findViewById(R.id.last_updated);
        activateSpeakerButton = findViewById(R.id.activate_speaker_button);

        // Retrieve initial data
        String horseName = getIntent().getStringExtra("horseName");
        horseIndex = getIntent().getIntExtra("horseIndex", -1);

        horseNameView.setText(horseName);

        MainActivity.HorseData horseData = MainActivity.horseDataMap.get(horseName);
        if (horseData != null) {
            if (horseData.temperature != -1) {
                temperatureView.setText(String.format("Temperature: %.1f °C", horseData.temperature));
            }
            if (horseData.oximetry != -1) {
                oximetryView.setText(String.format("Oximeter: %.1f%%", horseData.oximetry));
            }
            if (horseData.heartRate != -1) {
                hrView.setText(String.format("Heart Rate: %d bpm", horseData.heartRate));
            }
            lastUpdatedView.setText(String.format("Last updated: %s", horseData.lastUpdated));
        }

        // Configure the button to activate the speaker
        activateSpeakerButton.setOnClickListener(v -> activateSpeaker(horseName));
    }

    private void publishMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            Log.d(TAG, "The message is empty. It will not be published.");
            return;
        }

        Log.d(TAG, "Publishing message from HorseDetailActivity: " + payload);

        // Use the static MQTT client from MainActivity
        if (MainActivity.client == null || !MainActivity.client.getState().isConnected()) {
            Log.d(TAG, "The MQTT client is not connected. Cannot publish.");
            return;
        }

        MainActivity.client.publishWith()
                .topic(MainActivity.publishingTopic) // Reuse the topic defined in MainActivity
                .payload(payload.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Error publishing message to the topic " + MainActivity.publishingTopic, throwable);
                    } else {
                        Log.d(TAG, "Message published to the topic: " + MainActivity.publishingTopic);
                    }
                });
    }

    private void activateSpeaker(String horseName) {
        try {
            JSONObject payloadJson = new JSONObject();
            payloadJson.put("speakerDeviceName", horseName);
            payloadJson.put("status", false);

            String payload = payloadJson.toString();
            publishMessage(payload); // Call the method to publish the message

            Toast.makeText(this, "Speaker activated for " + horseName, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast receiver
        try {
            unregisterReceiver(updateReceiver);
            Log.d(TAG, "Receiver unregistered successfully.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver was not registered or already unregistered: " + e.getMessage());
        }
    }

    public void closeActivity(View view) {
        finish();
    }
}