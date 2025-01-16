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
    private TextView temperatureView, oximetryView, hrView, horseNameView;

    String TAG1 = "FRONT MESSAGE mqtt";

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceName = intent.getStringExtra("horseName");
            double temperature = intent.getDoubleExtra("temperature", -1);
            double oximetry = intent.getDoubleExtra("oximetry", -1);
            int heartRate = intent.getIntExtra("heartRate", -1);

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
            }
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horse_detail);

        String horseName = getIntent().getStringExtra("horseName");
        horseIndex = getIntent().getIntExtra("horseIndex", -1);

        horseNameView = findViewById(R.id.horse_name);
        temperatureView = findViewById(R.id.temperature);
        oximetryView = findViewById(R.id.oximetry);
        hrView = findViewById(R.id.heart_rate);

        horseNameView.setText(horseName);

        // Recuperar datos del mapa y actualizarlos
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
        } else {
            temperatureView.setText("Temperatura: N/A");
            oximetryView.setText("Oxímetro: N/A");
            hrView.setText("Frecuencia Cardíaca: N/A");
        }

        // Registrar receptor de difusión
        registerReceiver(updateReceiver, new IntentFilter("UPDATE_HORSE_DETAILS"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Desregistrar receptor de difusión
        unregisterReceiver(updateReceiver);
    }
}
