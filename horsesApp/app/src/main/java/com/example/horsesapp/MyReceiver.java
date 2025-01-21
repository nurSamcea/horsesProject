package com.example.horsesapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;

public class MyReceiver extends BroadcastReceiver {
    private static final String TAG1 = "FRONT MESSAGE mqtt";
    private TextView temperatureView, oximetryView, hrView, horseNameView, lastUpdatedView;
    private TextView accelerationYView, accelerationXView, accelerationZView, latitudeView, longitudeView;

    // Constructor que recibe las referencias de los TextViews
    public MyReceiver(TextView temperatureView, TextView oximetryView, TextView hrView, TextView horseNameView,
                          TextView lastUpdatedView, TextView accelerationXView, TextView accelerationYView,
                          TextView accelerationZView, TextView latitudeView, TextView longitudeView) {
        this.temperatureView = temperatureView;
        this.oximetryView = oximetryView;
        this.hrView = hrView;
        this.horseNameView = horseNameView;
        this.lastUpdatedView = lastUpdatedView;
        this.accelerationXView = accelerationXView;
        this.accelerationYView = accelerationYView;
        this.accelerationZView = accelerationZView;
        this.latitudeView = latitudeView;
        this.longitudeView = longitudeView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG1, "onReceive called");

        String deviceName = intent.getStringExtra("horseName");
        double temperature = intent.getDoubleExtra("temperature", MainActivity.DEFAULT_VAL_DOUBLE);
        int oximetry = intent.getIntExtra("oximetry", -1);
        int heartRate = intent.getIntExtra("heartRate", -1);
        double x = intent.getDoubleExtra("x", MainActivity.DEFAULT_VAL_DOUBLE);
        double y = intent.getDoubleExtra("y", MainActivity.DEFAULT_VAL_DOUBLE);
        double z = intent.getDoubleExtra("z", MainActivity.DEFAULT_VAL_DOUBLE);
        double latitude = intent.getDoubleExtra("lat", MainActivity.DEFAULT_VAL_DOUBLE);
        double longitude = intent.getDoubleExtra("long", MainActivity.DEFAULT_VAL_DOUBLE);
        String lastUpdated = intent.getStringExtra("lastUpdated");

        if (deviceName != null && deviceName.equals(horseNameView.getText().toString())) {
            if (temperature != MainActivity.DEFAULT_VAL_DOUBLE) {
                temperatureView.setText(String.format("Temperature: %.2f °C", temperature));
            }
            if (oximetry != -1) {
                oximetryView.setText(String.format("Oximeter: %d", oximetry));
            }
            if (heartRate != -1) {
                hrView.setText(String.format("Heart Rate: %d bpm", heartRate));
            }
            if (lastUpdated != null) {
                lastUpdatedView.setText(String.format("Last updated: %s", lastUpdated));
            }
            if (x != MainActivity.DEFAULT_VAL_DOUBLE && y != MainActivity.DEFAULT_VAL_DOUBLE && z != MainActivity.DEFAULT_VAL_DOUBLE) {
                accelerationXView.setText(String.format("Acceleration X: %.2f m/s²", x));
                accelerationYView.setText(String.format("Acceleration Y: %.2f m/s²", y));
                accelerationZView.setText(String.format("Acceleration Z: %.2f m/s²", z));
            }
            if (latitude != MainActivity.DEFAULT_VAL_DOUBLE && longitude != MainActivity.DEFAULT_VAL_DOUBLE) {
                latitudeView.setText(String.format("Latitude: %.6f", latitude));
                longitudeView.setText(String.format("Longitude: %.6f", longitude));
            }
        }
    }
}
