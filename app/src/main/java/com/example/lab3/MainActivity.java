package com.example.lab3;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Gravity, no air resistance is calculated
    static final float gravity = -(float)9.8;
    // max and min sensitivity multiplied with 10
    static final int minimumSensitivity = 8;
    static final int maximumSensitivity = 150;
    // Sensitivity value, default value is 10
    static int sensitivity = 10;
    // Frequency at which the GUI should be updated
    static int frequencyUpdateText = 30;
    // Interval of throw in milliseconds, the time where it looks for higher throws when detecting a throw
    static int interval = 250; // Time used to calculate highest throw value

    // Int representing last throw time in seconds to know if I am on the same throw as last time
    long currentTime;

    // Temporary time value used in async, had to be accessed in runOnUiThread
    long tempValue; // value for not updating GUI too often

    // Current highest speed value
    float currentHighest;

    // Gravity values
    float gravityValues[] = {0, 0, 0};

    // bools for checking if values should be changed
    boolean runChecks = true;
    boolean firstRun = true;

    private Sensor sensor, sensorGravity;
    SensorManager sensorManager;

    // Text views
    TextView heightText;
    TextView txtHighestPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Gets start time
        currentTime = System.currentTimeMillis();

        // Gets configuration value from disk if it is written
        getDataPhysically();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Accelerometer services to figure out user driven acceleration
        sensor = sensorManager.getDefaultSensor(sensor.TYPE_LINEAR_ACCELERATION);     // Complete acceleration
        //sensorGravity = sensorManager.getDefaultSensor(sensor.TYPE_GRAVITY);    // Gravity

        // Gets text views
        heightText = findViewById(R.id.txtHeight);
        txtHighestPoint = findViewById(R.id.txtHighestPoint);

        final Button transfer = findViewById(R.id.btnSettings);
        transfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // Goes to the options menu
                runChecks = false; // stops checking of movement while in settings menu
                startActivity(new Intent(MainActivity.this, Settings.class));
                runChecks = true;
            }
        });

        // Register listener
        sensorManager.registerListener(this, sensor, sensorManager.SENSOR_DELAY_NORMAL);
    }

    // Checks if slidingWindow should be reset or not and resets it if so
    boolean resetSliding() {
        // Resets time to now if more then x time have passed and starts throw
        if (System.currentTimeMillis() > currentTime + interval) {
            new updateScreenLoop().execute();

            return false;
        } else { // if still within the same time-frame

            return true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sen, int acc) {
        // Not in use
    }

    // When the sensor changes
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (runChecks) { // checks if the checks should run
            float linear_acceleration[] = {0, 0, 0};
            linear_acceleration[0] = event.values[0] - gravityValues[0];
            linear_acceleration[1] = event.values[1] - gravityValues[1];
            linear_acceleration[2] = event.values[2] - gravityValues[2];

            float speed = getSpeed(linear_acceleration[0], linear_acceleration[1], linear_acceleration[2]);
            if (speed >= sensitivity / 10.0f) {
                if (firstRun) { // resets time if this is the first detected value of this throw
                    currentTime = System.currentTimeMillis();
                    firstRun = false;
                }

                // resets slidingWindow if appropriate and updates currentTime
                resetSliding();

                // Sets current highest speed found
                if (speed > currentHighest) {
                    currentHighest = speed;
                }
            }
        }
    }

    // s = v0 * t + 1/2 * a * t^2
    // Code which runs asynchronously to count up and update the GUI when a throw is detected
    private class updateScreenLoop extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... values) {
            runChecks = false;                          // updates the value
            // new updateScreenAndRunSound().execute();    // Runs async task

            // Updates current time
            currentTime = System.currentTimeMillis();
            tempValue = currentTime;

            // starting time, const
            final long startTime1 = currentTime;
            // height, written here so the calculation is only made once
            final float highest = getHeight(currentHighest);
            // The time it takes to reach highest point in milliseconds
            final float timeTop = currentHighest / -gravity * 1000;

            // Loops until the correct amount of time have passed
            // Increases height until the highest point is reached
            while (timeTop > currentTime - startTime1) {
                currentTime = System.currentTimeMillis();

                // sleeeps for a specific amount of time
                SystemClock.sleep(frequencyUpdateText);
                // Checks if it is time to update the UiThread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Time mult value, value set here to have calculation only be made once per loop
                            float timeMult = (currentTime - startTime1) / 1000.0f;

                            heightText.setText("Current height: " + String.format("%.2f",
                                    (currentHighest * timeMult
                                            + gravity / 2 * Math.pow(timeMult, 2))
                            ) + "m");
                            tempValue = currentTime;
                        }
                    });
            }

            // Is on highest point, updates correct GUI element
            runOnUiThread(new Runnable() {
                public void run() {
                    // time to sleep Math.round(1000 * currentHighest / -gravity)
                    txtHighestPoint.setText("Highest point of throw: " + String.format("%.2f", highest) + "m");

                    // Media player
                    final MediaPlayer soundPlayer = MediaPlayer.create(MainActivity.this,
                            R.raw.inception_sound);
                    soundPlayer.start();
                    soundPlayer.setVolume(10, 10);

                }
            });
        // Ball falls down again
        // startTime for moving downwards
        final long startTime2 = System.currentTimeMillis();
        // sets currentTime to the new current time
        currentTime = System.currentTimeMillis();
        tempValue = currentTime;

        // Runs code until the ball have hit the ground
            while (timeTop > currentTime - startTime2) {
                currentTime = System.currentTimeMillis();

                SystemClock.sleep(frequencyUpdateText);
                runOnUiThread(new Runnable() {
                    public void run() {
                        float timeMult = (currentTime - startTime2) / 1000.0f;
                        Log.d("DEBUG",  timeTop + " > " + (currentTime - startTime2));

                        heightText.setText("Current height: " +
                                String.format("%.2f",
                                        (highest + gravity / 2 * Math.pow(timeMult, 2))
                                ) + "m");
                        tempValue = currentTime;
                    }
                });
            }
        // Sets the text to height 0 as the ball have hit the ground at this point
        runOnUiThread(new Runnable() {
            public void run() {
                heightText.setText("Current height: 0m");
            }
        });

        // Finishes asyncTask, reset booleans and other values
        currentHighest = 0;
        runChecks = firstRun = true;

            return null;
    }

        @Override
        protected void onProgressUpdate(Void... values) {

        }

        @Override
        protected void onPostExecute(Void voids) { // run after timer is done
        }
    }


    // Calculates speed based on x, y and z values
    protected float getSpeed(float x, float y, float z) {
        return (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    // Gets highest the height of the throw
    protected float getHeight(float startSpeed) {
        // s = v0 * t + 1/2 * a * t^2 // The distance covered
        // v0 = (float)Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2))
        // a = gravity
        // t = (v0 / -a) // the number of seconds it will take to reach 0 speed

        float timeValue = (startSpeed / -gravity);
        // returns the highest y coordinates
        return (startSpeed * timeValue + 0.5f * gravity * (float)Math.pow(timeValue, 2));
    }

    // Saves data to disk
    static public void saveDataPhysically(Context context) {
        // Writes to file
        String filename = "settingsLab3";
        String fileContents = sensitivity + "";
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loads data from disk(internal)
    public boolean getDataPhysically() {
        try {
            FileInputStream inputStream = getApplicationContext().openFileInput("settingsLab3");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String sensitivityValue = bufferedReader.readLine();

            if (!sensitivityValue.equals("")) {
                sensitivity = Integer.parseInt(sensitivityValue);
            }

            inputStreamReader.close(); // closes input stream

            return true;
        } catch (Exception e) { // An error occurred, sets all values to default
            e.printStackTrace();
            Log.d("DEBUG", "ERROR");
            return false;
        }
    }
}
