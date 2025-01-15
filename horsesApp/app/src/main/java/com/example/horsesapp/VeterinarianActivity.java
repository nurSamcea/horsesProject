package com.example.horsesapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VeterinarianActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veterinarian);

        // ListView para mostrar los caballos y sus estados
        ListView horseListView = findViewById(R.id.horse_list_view);

        // Ejemplo: Datos simulados para probar
        String[] horseData = {"Caballo 1: Normal", "Caballo 2: Atención requerida", "Caballo 3: Emergencia"};

        // Adaptador para mostrar los datos en el ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                horseData
        );

        horseListView.setAdapter(adapter);

        // Manejar clics en los ítems (opcional)
        horseListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedHorse = horseData[position];
            Toast.makeText(this, "Seleccionaste: " + selectedHorse, Toast.LENGTH_SHORT).show();
        });
    }
}