package com.example.cekuseragent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

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
        MaterialButton btnCopy = findViewById(R.id.btnCopy);
        
        StringBuilder info = new StringBuilder();
        info.append("=== INFO PERANGKAT ===\n\n");
        info.append("Pabrikan      : ").append(Build.MANUFACTURER.toUpperCase()).append("\n");
        info.append("Model         : ").append(Build.MODEL).append("\n");
        info.append("Perangkat     : ").append(Build.DEVICE).append("\n");
        info.append("Versi Android : ").append(Build.VERSION.RELEASE).append("\n");
        info.append("API Level     : ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Board         : ").append(Build.BOARD).append("\n");
        info.append("Hardware      : ").append(Build.HARDWARE).append("\n\n");
        
        info.append("=== USER AGENT ===\n\n");
        String userAgent = System.getProperty("http.agent");
        if (userAgent != null) {
            info.append(userAgent);
        } else {
            info.append("Tidak diketahui");
        }
        
        String finalInfo = info.toString();
        tvDeviceInfo.setText(finalInfo);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Info Perangkat & User Agent", finalInfo);
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "Semua info berhasil disalin!", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
