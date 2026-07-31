package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialCardView menuDeviceInfo = findViewById(R.id.menuDeviceInfo);
        MaterialCardView menuTimestamp = findViewById(R.id.menuTimestamp);
        TextView tvVersion = findViewById(R.id.tvVersion);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText("Versi " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        menuDeviceInfo.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeviceInfoActivity.class));
        });

        menuTimestamp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TimestampActivity.class));
        });

        // Panggil auto-update saat aplikasi dibuka
        new UpdateHelper(this).checkForUpdate();
    }
}
