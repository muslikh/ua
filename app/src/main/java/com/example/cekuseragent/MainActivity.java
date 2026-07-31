package com.example.cekuseragent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Versi: " + BuildConfig.VERSION_NAME);

        MaterialCardView menuUserAgent = findViewById(R.id.menuUserAgent);
        MaterialCardView menuDeviceInfo = findViewById(R.id.menuDeviceInfo);
        MaterialCardView menuTimestamp = findViewById(R.id.menuTimestamp);
        MaterialCardView menuQrCode = findViewById(R.id.menuQrCode);

        menuUserAgent.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserAgentActivity.class));
        });

        menuDeviceInfo.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeviceInfoActivity.class));
        });

        menuTimestamp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TimestampActivity.class));
        });

        menuQrCode.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, QrCodeActivity.class));
        });

        // Panggil auto-update saat aplikasi dibuka
        new UpdateHelper(this).checkForUpdate();
    }
}
