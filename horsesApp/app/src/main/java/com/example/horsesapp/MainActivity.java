package com.example.horsesapp;
// VeterinaryOperatorApp.java
// Este archivo contiene la estructura inicial para la app en Android Studio
// con funcionalidades para el veterinario y el operario.


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Botón para acceder a la vista del veterinario
        Button vetButton = findViewById(R.id.btn_veterinarian);
        vetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VeterinarianActivity.class);
                startActivity(intent);
            }
        });

        // Botón para acceder a la vista del operario
        Button operatorButton = findViewById(R.id.btn_operator);
        operatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OperatorActivity.class);
                startActivity(intent);
            }
        });
    }
}
