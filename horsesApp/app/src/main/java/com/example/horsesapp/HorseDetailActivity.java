package com.example.horsesapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HorseDetailActivity extends AppCompatActivity {

    private static final String TAG = "HorseDetailActivity";
    private int horseIndex;
    private TextView temperatureView, oximetryView, hrView, horseNameView, lastUpdatedView;

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
                    temperatureView.setText(String.format("Temperatura: %.1f °C", temperature));
                }
                if (oximetry != -1) {
                    oximetryView.setText(String.format("Oxímetro: %.1f%%", oximetry));
                }
                if (heartRate != -1) {
                    hrView.setText(String.format("Frecuencia Cardíaca: %d bpm", heartRate));
                }
                if (lastUpdated != null) {
                    lastUpdatedView.setText(String.format("Última actualización: %s", lastUpdated));
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

        // Obtener datos iniciales
        String horseName = getIntent().getStringExtra("horseName");
        horseIndex = getIntent().getIntExtra("horseIndex", -1);

        horseNameView.setText(horseName);

        MainActivity.HorseData horseData = MainActivity.horseDataMap.get(horseName);
        if (horseData != null) {
            if (horseData.temperature != -1) {
                temperatureView.setText(String.format("Temperatura: %.1f °C", horseData.temperature));
            }
            if (horseData.oximetry != -1) {
                oximetryView.setText(String.format("Oxímetro: %.1f%%", horseData.oximetry));
            }
            if (horseData.heartRate != -1) {
                hrView.setText(String.format("Frecuencia Cardíaca: %d bpm", horseData.heartRate));
            }
            lastUpdatedView.setText(String.format("Última actualización: %s", horseData.lastUpdated));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar receptor de difusión
        try {
            unregisterReceiver(updateReceiver);
            Log.d(TAG, "Receiver unregistered successfully.");
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver was not registered or already unregistered: " + e.getMessage());
        }
    }

}
