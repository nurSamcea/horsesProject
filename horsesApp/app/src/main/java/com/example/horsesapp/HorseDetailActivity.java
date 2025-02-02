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
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.IntentFilter;
import com.example.horsesapp.MainActivity;

public class HorseDetailActivity extends AppCompatActivity {
    private static final String TAG = "MQTT";
    private int horseIndex;
    private TextView temperatureView, oximetryView, hrView, horseNameView, lastUpdatedView;
    private TextView accelerationYView, accelerationXView, accelerationZView, latitudeView, longitudeView;
    private TextView msg_metadataView, msg_dataprocessorView;
    private Button activateSpeakerButton; // Button to activate the speaker

    String TAG1 = "FRONT MESSAGE mqtt";

    private MyReceiver myReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horse_detail);

        horseNameView = findViewById(R.id.horse_name);
        temperatureView = findViewById(R.id.temperature);
        oximetryView = findViewById(R.id.oximetry);
        hrView = findViewById(R.id.heart_rate);
        accelerationXView = findViewById(R.id.acceleration_x);
        accelerationYView = findViewById(R.id.acceleration_y);
        accelerationZView = findViewById(R.id.acceleration_z);
        latitudeView = findViewById(R.id.location_latitude);
        longitudeView = findViewById(R.id.location_longitude);
        lastUpdatedView = findViewById(R.id.last_updated);
        activateSpeakerButton = findViewById(R.id.activate_speaker_button);
        msg_metadataView = findViewById(R.id.alert_metadata);
        msg_dataprocessorView = findViewById(R.id.alert_msg_dataprocessor);

        // Retrieve initial data
        String horseName = getIntent().getStringExtra("horseName");
        horseIndex = getIntent().getIntExtra("horseIndex", -1);

        horseNameView.setText(horseName);

        MainActivity.HorseData horseData = MainActivity.horseDataMap.get(horseName);
        if (horseData != null) {
            if (horseData.temperature != MainActivity.DEFAULT_VAL_DOUBLE) {
                temperatureView.setText(String.format("Temperature: %.2f °C", horseData.temperature));
            }
            if (horseData.oximetry != -1) {
                oximetryView.setText(String.format("Oximetry: %d", horseData.oximetry));
            }
            if (horseData.heartRate != -1) {
                hrView.setText(String.format("Heart Rate: %d bpm", horseData.heartRate));
            }
            if (horseData.lastUpdated != null) {
                lastUpdatedView.setText(String.format("Last updated: %s", horseData.lastUpdated));
            }
            if (horseData.x != MainActivity.DEFAULT_VAL_DOUBLE && horseData.y != MainActivity.DEFAULT_VAL_DOUBLE && horseData.z != MainActivity.DEFAULT_VAL_DOUBLE) {
                accelerationXView.setText(String.format("Acceleration X: %.2f m/s²", horseData.x));
                accelerationYView.setText(String.format("Acceleration Y: %.2f m/s²", horseData.y));
                accelerationZView.setText(String.format("Acceleration Z: %.2f m/s²", horseData.z));
            }
            if (horseData.latitude != MainActivity.DEFAULT_VAL_DOUBLE && horseData.longitude != MainActivity.DEFAULT_VAL_DOUBLE) {
                latitudeView.setText(String.format("Latitude: %.6f", horseData.latitude));
                longitudeView.setText(String.format("Longitude: %.6f", horseData.longitude));
            }
            if(horseData.msg_dataprocessor != null) {
                msg_dataprocessorView.setText(String.format("Msg: %s", horseData.msg_dataprocessor));
                if (horseData.msg_metadata != null)
                    msg_metadataView.setText(String.format("Last msg from DATAPROCESSOR at: %s", horseData.msg_metadata));
            }
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
            payloadJson.put("status", true);

            String payload = payloadJson.toString();
            publishMessage(payload); // Call the method to publish the message

            Toast.makeText(this, "Speaker activated for " + horseName, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "Error building JSON: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        myReceiver = new MyReceiver(temperatureView, oximetryView, hrView, horseNameView,
                lastUpdatedView, accelerationXView, accelerationYView, accelerationZView,
                latitudeView, longitudeView, msg_metadataView, msg_dataprocessorView);
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_MY_BROADCAST);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(myReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(myReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (myReceiver != null) {
            unregisterReceiver(myReceiver);
        }
    }

    public void closeActivity(View view) {
        finish();
    }
}