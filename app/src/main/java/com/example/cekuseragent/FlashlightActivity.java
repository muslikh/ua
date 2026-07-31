package com.example.cekuseragent;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class FlashlightActivity extends AppCompatActivity {
    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashlight);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView ivFlashlight = findViewById(R.id.ivFlashlight);
        TextView tvStatus = findViewById(R.id.tvStatus);
        Button btnToggle = findViewById(R.id.btnToggle);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Perangkat tidak mendukung flash", Toast.LENGTH_SHORT).show();
        }

        btnToggle.setOnClickListener(v -> {
            if (isFlashOn) {
                turnOffFlash();
                tvStatus.setText("MATI");
                tvStatus.setTextColor(0xFFBDBDBD);
                ivFlashlight.setImageResource(android.R.drawable.btn_star_big_off);
                ivFlashlight.setColorFilter(0xFFBDBDBD);
            } else {
                turnOnFlash();
                tvStatus.setText("HIDUP");
                tvStatus.setTextColor(0xFFFFD600);
                ivFlashlight.setImageResource(android.R.drawable.btn_star_big_on);
                ivFlashlight.setColorFilter(0xFFFFD600);
            }
        });
    }

    private void turnOnFlash() {
        try {
            cameraManager.setTorchMode(cameraId, true);
            isFlashOn = true;
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Gagal menyalakan flash", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOffFlash() {
        try {
            cameraManager.setTorchMode(cameraId, false);
            isFlashOn = false;
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Gagal mematikan flash", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFlashOn) turnOffFlash();
    }
}
