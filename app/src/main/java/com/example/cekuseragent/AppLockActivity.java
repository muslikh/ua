package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppLockActivity extends AppCompatActivity {
    private AppLockManager lockManager;
    private SwitchMaterial switchMasterLock;
    private TextView tvLockStatus, tvPinPreview, tvAppCount;
    private MaterialCardView cardPermissions;
    private TextView tvOverlayStatus, tvAccessibilityStatus, tvUsageStatus;
    private Button btnPermOverlay, btnPermAccessibility, btnPermUsage;
    private RecyclerView rvApps;
    private ProgressBar progressBar;
    private TextInputEditText etSearchApp;
    private AppLockAdapter adapter;
    private final List<AppInfoItem> appList = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        lockManager = new AppLockManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchMasterLock = findViewById(R.id.switchMasterLock);
        tvLockStatus = findViewById(R.id.tvLockStatus);
        tvPinPreview = findViewById(R.id.tvPinPreview);
        tvAppCount = findViewById(R.id.tvAppCount);
        cardPermissions = findViewById(R.id.cardPermissions);

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        tvUsageStatus = findViewById(R.id.tvUsageStatus);

        btnPermOverlay = findViewById(R.id.btnPermOverlay);
        btnPermAccessibility = findViewById(R.id.btnPermAccessibility);
        btnPermUsage = findViewById(R.id.btnPermUsage);

        rvApps = findViewById(R.id.rvApps);
        progressBar = findViewById(R.id.progressBar);
        etSearchApp = findViewById(R.id.etSearchApp);
        MaterialButton btnChangePin = findViewById(R.id.btnChangePin);

        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppLockAdapter(appList, (item, isLocked) -> {
            lockManager.setPackageLocked(item.getPackageName(), isLocked);
            updateAppCountHeader();
        });
        rvApps.setAdapter(adapter);

        updateMasterSwitchUI();
        updatePinUI();

        switchMasterLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            lockManager.setLockEnabled(isChecked);
            updateMasterSwitchUI();
            if (isChecked && (!lockManager.hasOverlayPermission() || !lockManager.isAccessibilityEnabled())) {
                Toast.makeText(this, "Tips: Aktifkan izin Tampilan & Aksesibilitas di bawah agar proteksi berjalan maksimal!", Toast.LENGTH_LONG).show();
            }
        });

        btnChangePin.setOnClickListener(v -> showChangePinDialog());

        btnPermOverlay.setOnClickListener(v -> startActivity(AppLockManager.getOverlayIntent(this)));
        btnPermAccessibility.setOnClickListener(v -> showAccessibilityGuideDialog());
        btnPermUsage.setOnClickListener(v -> startActivity(AppLockManager.getUsageStatsIntent()));

        etSearchApp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadInstalledApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionCards();
    }

    private void refreshPermissionCards() {
        boolean overlayOk = lockManager.hasOverlayPermission();
        boolean accessOk = lockManager.isAccessibilityEnabled();
        boolean usageOk = lockManager.hasUsageStatsPermission();

        if (overlayOk) {
            tvOverlayStatus.setText("1. Tampilan di Atas Aplikasi (✅ Aktif)");
            tvOverlayStatus.setTextColor(Color.parseColor("#166534"));
            btnPermOverlay.setText("Terpasang");
            btnPermOverlay.setEnabled(false);
        } else {
            tvOverlayStatus.setText("1. Tampilan di Atas Aplikasi (⚠️ Wajib)");
            tvOverlayStatus.setTextColor(Color.parseColor("#991B1B"));
            btnPermOverlay.setText("Aktifkan");
            btnPermOverlay.setEnabled(true);
        }

        if (accessOk) {
            tvAccessibilityStatus.setText("2. Aksesibilitas (✅ Aktif - Deteksi 0ms)");
            tvAccessibilityStatus.setTextColor(Color.parseColor("#166534"));
            btnPermAccessibility.setText("Terpasang");
            btnPermAccessibility.setEnabled(false);
        } else {
            tvAccessibilityStatus.setText("2. Aksesibilitas (⚡ Sangat Dianjurkan)");
            tvAccessibilityStatus.setTextColor(Color.parseColor("#991B1B"));
            btnPermAccessibility.setText("Aktifkan");
            btnPermAccessibility.setEnabled(true);
        }

        if (usageOk) {
            tvUsageStatus.setText("3. Akses Penggunaan (✅ Aktif)");
            tvUsageStatus.setTextColor(Color.parseColor("#166534"));
            btnPermUsage.setText("Terpasang");
            btnPermUsage.setEnabled(false);
        } else {
            tvUsageStatus.setText("3. Akses Penggunaan (⚠️ Diperlukan)");
            tvUsageStatus.setTextColor(Color.parseColor("#92400E"));
            btnPermUsage.setText("Aktifkan");
            btnPermUsage.setEnabled(true);
        }

        if (overlayOk && accessOk && usageOk) {
            cardPermissions.setCardBackgroundColor(Color.parseColor("#F0FDF4"));
            cardPermissions.setStrokeColor(Color.parseColor("#86EFAC"));
        } else {
            cardPermissions.setCardBackgroundColor(Color.parseColor("#FFFBEB"));
            cardPermissions.setStrokeColor(Color.parseColor("#FCD34D"));
        }
    }

    private void updateMasterSwitchUI() {
        boolean enabled = lockManager.isLockEnabled();
        switchMasterLock.setChecked(enabled);
        tvLockStatus.setText(enabled ? "Status: 🛡️ Proteksi Siaga Aktif" : "Status: Nonaktif");
        tvLockStatus.setTextColor(enabled ? Color.parseColor("#166534") : Color.parseColor("#64748B"));
    }

    private void updatePinUI() {
        tvPinPreview.setText("🔐 PIN Saat Ini: " + lockManager.getPin());
    }

    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ubah PIN Keamanan");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final EditText input = new EditText(this);
        input.setHint("Masukkan 4 digit PIN baru...");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setTextSize(18f);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String newPin = input.getText().toString().trim();
            if (newPin.length() == 4) {
                lockManager.setPin(newPin);
                updatePinUI();
                Toast.makeText(this, "PIN berhasil diubah ke: " + newPin, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PIN harus tepat 4 digit angka!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void showAccessibilityGuideDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Izin Layanan Aksesibilitas ♿");
        builder.setMessage("Layanan Aksesibilitas diperlukan agar kunci aplikasi merespon secara instan (0ms delay).\n\n" +
                "⚠️ Jika di HP Anda tombol Aksesibilitas abu-abu / bertuliskan 'Pengaturan Dibatasi' (Android 13/14):\n" +
                "1. Ketuk 'Buka Info Aplikasi' di bawah\n" +
                "2. Ketuk titik tiga (⋮) di pojok kanan atas\n" +
                "3. Pilih 'Izinkan setelan terbatas'\n" +
                "4. Masukkan PIN layar HP Anda\n" +
                "5. Lalu kembali dan aktifkan 'MyTools Lock' di Aksesibilitas.");

        builder.setPositiveButton("Buka Aksesibilitas", (dialog, which) -> {
            startActivity(AppLockManager.getAccessibilityIntent());
        });

        builder.setNeutralButton("Buka Info Aplikasi", (dialog, which) -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Gagal membuka info aplikasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Tutup", null);
        builder.show();
    }

    private void loadInstalledApps() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfoItem> list = new ArrayList<>();
            Set<String> lockedSet = lockManager.getLockedPackages();

            for (ApplicationInfo appInfo : packages) {
                if (appInfo.packageName.equals(getPackageName())) continue;

                Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                if (launchIntent == null) continue;

                String name = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isLocked = lockedSet.contains(appInfo.packageName);

                list.add(new AppInfoItem(name, appInfo.packageName, icon, isLocked));
            }

            Collections.sort(list, (a, b) -> {
                if (a.isChecked() != b.isChecked()) {
                    return a.isChecked() ? -1 : 1;
                }
                return a.getAppName().compareToIgnoreCase(b.getAppName());
            });

            mainHandler.post(() -> {
                appList.clear();
                appList.addAll(list);
                adapter.updateData(appList);
                progressBar.setVisibility(View.GONE);
                updateAppCountHeader();
            });
        });
    }

    private void updateAppCountHeader() {
        int lockedCount = 0;
        for (AppInfoItem item : appList) {
            if (item.isChecked()) lockedCount++;
        }
        tvAppCount.setText("Daftar Aplikasi (" + appList.size() + " total, " + lockedCount + " terkunci 🔒)");
    }
}
