package com.example.horsesapp;


import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OperatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator);

        // Campos de entrada y botón para enviar alertas
        EditText alertInput = findViewById(R.id.alert_input);
        Button sendAlertButton = findViewById(R.id.btn_send_alert);

        // Configurar acción del botón
        sendAlertButton.setOnClickListener(v -> {
            String alertMessage = alertInput.getText().toString();
            if (!alertMessage.isEmpty()) {
                // TODO: Implementar envío de alerta a través de ThingsBoard
                Toast.makeText(this, "Alerta enviada: " + alertMessage, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Por favor, escribe un mensaje de alerta.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
