
// HorseDetailActivity.java
// Actividad para mostrar los detalles de un caballo específico.

package com.example.horsesapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HorseDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_horse_detail);

        String horseName = getIntent().getStringExtra("horseName");

        // Configurar vista con los detalles del caballo
        TextView horseNameView = findViewById(R.id.horse_name);
        horseNameView.setText(horseName);

        TextView temperatureView = findViewById(R.id.temperature);
        TextView oximetryView = findViewById(R.id.oximetry);
        TextView hrView = findViewById(R.id.heart_rate);

        // Simular datos (puedes reemplazar con datos reales)
        temperatureView.setText("Temperatura: 38.5 °C");
        oximetryView.setText("Oxímetro: 95%");
        hrView.setText("Frecuencia Cardíaca: 72 bpm");
    }
}
