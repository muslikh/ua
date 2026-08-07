package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppLockActivity extends AppCompatActivity {
    private AppLockManager lockManager;
    private SwitchMaterial switchMasterLock;
    private TextView tvLockStatus, tvAppCount;
    private MaterialCardView cardPermissionWarning;
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
        tvAppCount = findViewById(R.id.tvAppCount);
        cardPermissionWarning = findViewById(R.id.cardPermissionWarning);
        rvApps = findViewById(R.id.rvApps);
        progressBar = findViewById(R.id.progressBar);
        etSearchApp = findViewById(R.id.etSearchApp);
        Button btnChangePin = findViewById(R.id.btnChangePin);
        Button btnGrantPermission = findViewById(R.id.btnGrantPermission);

        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppLockAdapter(appList, (item, isLocked) -> {
            lockManager.setPackageLocked(item.getPackageName(), isLocked);
            updateAppCountHeader();
        });
        rvApps.setAdapter(adapter);

        updateMasterSwitchUI();

        switchMasterLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !lockManager.hasUsageStatsPermission()) {
                switchMasterLock.setChecked(false);
                promptPermissionDialog();
                return;
            }
            lockManager.setLockEnabled(isChecked);
            updateMasterSwitchUI();
        });

        btnChangePin.setText("Ubah PIN (" + lockManager.getPin() + ")");
        btnChangePin.setOnClickListener(v -> showChangePinDialog(btnChangePin));

        btnGrantPermission.setOnClickListener(v -> {
            startActivity(AppLockManager.getUsageStatsIntent());
        });

        etSearchApp.addTextChangedWatcher(new TextWatcher() {
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
        checkPermissions();
    }

    private void checkPermissions() {
        if (!lockManager.hasUsageStatsPermission()) {
            cardPermissionWarning.setVisibility(View.VISIBLE);
        } else {
            cardPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void updateMasterSwitchUI() {
        boolean enabled = lockManager.isLockEnabled();
        switchMasterLock.setChecked(enabled);
        tvLockStatus.setText(enabled ? "Status: Aktif Memproteksi" : "Status: Nonaktif");
        tvLockStatus.setTextColor(enabled ? 0xFF2E7D32 : 0xFF757575);
    }

    private void promptPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Izin Akses Penggunaan Diperlukan")
                .setMessage("Agar fitur Kunci Aplikasi dapat mendeteksi aplikasi yang dibuka, silakan berikan izin 'Akses Penggunaan' di pengaturan.")
                .setPositiveButton("Buka Pengaturan", (d, w) -> {
                    startActivity(AppLockManager.getUsageStatsIntent());
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showChangePinDialog(Button btnChangePin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ubah PIN Keamanan");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setHint("Masukkan 4 digit PIN baru...");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(input);
        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String newPin = input.getText().toString().trim();
            if (newPin.length() == 4) {
                lockManager.setPin(newPin);
                btnChangePin.setText("Ubah PIN (" + newPin + ")");
                Toast.makeText(this, "PIN berhasil diubah ke " + newPin, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PIN harus tepat 4 angka!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Batal", null);
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
                // Don't show our own app
                if (appInfo.packageName.equals(getPackageName())) continue;

                // Check if it's a launchable app (has launcher intent)
                Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                if (launchIntent == null) continue;

                String name = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isLocked = lockedSet.contains(appInfo.packageName);

                list.add(new AppInfoItem(name, appInfo.packageName, icon, isLocked));
            }

            // Sort: locked apps first, then alphabetical by name
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
        tvAppCount.setText("Daftar Aplikasi (" + appList.size() + " total, " + lockedCount + " terkunci)");
    }
}
