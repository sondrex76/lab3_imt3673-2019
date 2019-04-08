package com.example.lab3;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final TextView txtSensitivity = findViewById(R.id.txtSensitivity);
        txtSensitivity.setText("Minimum force: " +  MainActivity.sensitivity / 10.0f);

        SeekBar slider = findViewById(R.id.sensitivityBar);
        // Sets min and max correctly
        // Translation of values is needed as the slider needs its value to be 0
        slider.setMin(0);
        slider.setMax(MainActivity.maximumSensitivity - MainActivity.minimumSensitivity);
        slider.setProgress(MainActivity.sensitivity - MainActivity.minimumSensitivity);

        // Updates value
        slider.setOnSeekBarChangeListener(new  SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar slide) { }

            @Override
            public void onStopTrackingTouch(SeekBar slide) { }

            @Override
            public void onProgressChanged(SeekBar slide, int value, boolean bool) {
                MainActivity.sensitivity = value + MainActivity.minimumSensitivity;
                MainActivity.saveDataPhysically(getBaseContext());
                txtSensitivity.setText("Minimum force: " + (value + MainActivity.minimumSensitivity) / 10.0f);
            }
        });
    }
}
