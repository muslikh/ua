package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppBlockerActivity extends AppCompatActivity {
    private AppBlockerManager blockerManager;
    private SwitchMaterial switchMasterBlocker;
    private TextView tvBlockerStatus, tvAppCount;
    private RecyclerView rvApps;
    private ProgressBar progressBar;
    private TextInputEditText etSearchApp;
    private AppBlockerAdapter adapter;
    private final List<AppInfoItem> appList = new ArrayList<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> vpnPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    blockerManager.setFirewallEnabled(true);
                    updateMasterSwitchUI();
                    Toast.makeText(this, "Firewall Aktif! Internet aplikasi terpilih diblokir.", Toast.LENGTH_SHORT).show();
                } else {
                    switchMasterBlocker.setChecked(false);
                    Toast.makeText(this, "Izin VPN ditolak, firewall tidak dapat aktif.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_blocker);

        blockerManager = new AppBlockerManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchMasterBlocker = findViewById(R.id.switchMasterBlocker);
        tvBlockerStatus = findViewById(R.id.tvBlockerStatus);
        tvAppCount = findViewById(R.id.tvAppCount);
        rvApps = findViewById(R.id.rvApps);
        progressBar = findViewById(R.id.progressBar);
        etSearchApp = findViewById(R.id.etSearchApp);
        Button btnClearAllBlocks = findViewById(R.id.btnClearAllBlocks);

        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppBlockerAdapter(appList, (item, isBlocked) -> {
            blockerManager.setPackageBlocked(item.getPackageName(), isBlocked);
            updateAppCountHeader();
        });
        rvApps.setAdapter(adapter);

        updateMasterSwitchUI();

        switchMasterBlocker.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Intent vpnIntent = VpnService.prepare(this);
                if (vpnIntent != null) {
                    vpnPermissionLauncher.launch(vpnIntent);
                } else {
                    blockerManager.setFirewallEnabled(true);
                    updateMasterSwitchUI();
                }
            } else {
                blockerManager.setFirewallEnabled(false);
                updateMasterSwitchUI();
            }
        });

        btnClearAllBlocks.setOnClickListener(v -> {
            if (blockerManager.getBlockedPackages().isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Batal Semua Blokir")
                    .setMessage("Buka kembali akses internet untuk semua aplikasi?")
                    .setPositiveButton("Ya, Buka Semua", (d, w) -> {
                        blockerManager.setAllBlocked(new HashSet<>());
                        for (AppInfoItem item : appList) {
                            item.setChecked(false);
                        }
                        adapter.updateData(appList);
                        updateAppCountHeader();
                        Toast.makeText(this, "Semua blokir internet telah dilepas", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Batal", null)
                    .show();
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

    private void updateMasterSwitchUI() {
        boolean enabled = blockerManager.isFirewallEnabled();
        switchMasterBlocker.setChecked(enabled);
        tvBlockerStatus.setText(enabled ? "Status: Firewall Aktif Memblokir" : "Status: Nonaktif");
        tvBlockerStatus.setTextColor(enabled ? 0xFFD32F2F : 0xFF757575);
    }

    private void loadInstalledApps() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfoItem> list = new ArrayList<>();
            Set<String> blockedSet = blockerManager.getBlockedPackages();

            for (ApplicationInfo appInfo : packages) {
                if (appInfo.packageName.equals(getPackageName())) continue;

                Intent launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName);
                if (launchIntent == null) continue;

                String name = pm.getApplicationLabel(appInfo).toString();
                Drawable icon = pm.getApplicationIcon(appInfo);
                boolean isBlocked = blockedSet.contains(appInfo.packageName);

                list.add(new AppInfoItem(name, appInfo.packageName, icon, isBlocked));
            }

            // Sort: blocked apps first, then alphabetical
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
        int blockedCount = 0;
        for (AppInfoItem item : appList) {
            if (item.isChecked()) blockedCount++;
        }
        tvAppCount.setText("Daftar Aplikasi (" + appList.size() + " total, " + blockedCount + " diblokir internet)");
    }
}
