package com.example.cekuseragent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;
    private TextView tvDegree, tvDirection;
    private ImageView ivCompass;
    private float[] gravity, geomagnetic;
    private float currentDegree = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvDegree = findViewById(R.id.tvDegree);
        tvDirection = findViewById(R.id.tvDirection);
        ivCompass = findViewById(R.id.ivCompass);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                tvDegree.setText(String.format(java.util.Locale.US, "%.0f°", azimuth));
                tvDirection.setText(getDirection(azimuth));

                float rotation = -azimuth;
                ivCompass.setRotation(rotation);
                currentDegree = rotation;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private String getDirection(float degree) {
        if (degree >= 337.5 || degree < 22.5) return "Utara (N)";
        if (degree >= 22.5 && degree < 67.5) return "Timur Laut (NE)";
        if (degree >= 67.5 && degree < 112.5) return "Timur (E)";
        if (degree >= 112.5 && degree < 157.5) return "Tenggara (SE)";
        if (degree >= 157.5 && degree < 202.5) return "Selatan (S)";
        if (degree >= 202.5 && degree < 247.5) return "Barat Daya (SW)";
        if (degree >= 247.5 && degree < 292.5) return "Barat (W)";
        return "Barat Laut (NW)";
    }
}
