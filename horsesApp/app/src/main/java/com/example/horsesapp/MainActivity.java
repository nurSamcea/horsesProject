// VeterinaryOperatorApp.java
// Código actualizado para una app exclusiva para veterinarios.
// Lista de caballos con detalles y alertas emergentes.

package com.example.horsesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final String[] horses = {"Caballo 1", "Caballo 2", "Caballo 3", "Caballo 4"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void simulateAlert() {
        // Simular una alerta para Caballo 1
        new AlertDialog.Builder(this)
                .setTitle("Alerta: Caballo 1")
                .setMessage("Temperatura alta detectada en Caballo 1.")
                .setPositiveButton("Voy en camino", (dialog, which) -> {
                    Toast.makeText(this, "Marcado como en camino.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
