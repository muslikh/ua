package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private TextView tvLiveBadge, tvQuickLockStatus, tvQuickFirewallStatus, tvVersion;
    private AppLockManager lockManager;
    private AppBlockerManager blockerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lockManager = new AppLockManager(this);
        blockerManager = new AppBlockerManager(this);

        tvLiveBadge = findViewById(R.id.tvLiveBadge);
        tvQuickLockStatus = findViewById(R.id.tvQuickLockStatus);
        tvQuickFirewallStatus = findViewById(R.id.tvQuickFirewallStatus);
        tvVersion = findViewById(R.id.tvVersion);

        MaterialCardView menuDeviceInfo = findViewById(R.id.menuDeviceInfo);
        MaterialCardView menuTimestamp = findViewById(R.id.menuTimestamp);
        MaterialCardView menuAppLock = findViewById(R.id.menuAppLock);
        MaterialCardView menuAppBlocker = findViewById(R.id.menuAppBlocker);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            tvVersion.setText("MyTools Pro v" + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        menuDeviceInfo.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, DeviceInfoActivity.class)));
        menuTimestamp.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TimestampActivity.class)));
        menuAppLock.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AppLockActivity.class)));
        menuAppBlocker.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AppBlockerActivity.class)));

        // Panggil auto-update saat aplikasi dibuka
        new UpdateHelper(this).checkForUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatusBadges();
    }

    private void refreshStatusBadges() {
        boolean isLockOn = lockManager.isLockEnabled();
        int lockedCount = lockManager.getLockedCount();
        tvQuickLockStatus.setText(isLockOn ? "🔒 Kunci: " + lockedCount + " Aktif" : "🔒 Kunci: Nonaktif");

        boolean isFirewallOn = blockerManager.isFirewallEnabled();
        int blockedCount = blockerManager.getBlockedCount();
        tvQuickFirewallStatus.setText(isFirewallOn ? "🛡️ Firewall: " + blockedCount + " Diblokir" : "🛡️ Firewall: Nonaktif");

        int activeCount = (isLockOn ? 1 : 0) + (isFirewallOn ? 1 : 0);
        if (activeCount > 0) {
            tvLiveBadge.setText("🛡️ " + activeCount + " Proteksi Aktif");
        } else {
            tvLiveBadge.setText("⚡ Siaga");
        }
    }
}
