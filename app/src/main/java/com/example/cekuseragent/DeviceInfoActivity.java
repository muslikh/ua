package com.example.cekuseragent;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

public class DeviceInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        
        StringBuilder info = new StringBuilder();
        info.append("Pabrikan : ").append(Build.MANUFACTURER.toUpperCase()).append("\n");
        info.append("Model : ").append(Build.MODEL).append("\n");
        info.append("Perangkat : ").append(Build.DEVICE).append("\n");
        info.append("Versi Android : ").append(Build.VERSION.RELEASE).append("\n");
        info.append("API Level : ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Board : ").append(Build.BOARD).append("\n");
        info.append("Hardware : ").append(Build.HARDWARE);
        
        tvDeviceInfo.setText(info.toString());
    }
}
