package com.example.cekuseragent;

import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class DeviceInfoActivity extends AppCompatActivity {
    private String fullDeviceInfoString = "";
    private String userAgentString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvDeviceModelHeader = findViewById(R.id.tvDeviceModelHeader);
        TextView tvAndroidVersionHeader = findViewById(R.id.tvAndroidVersionHeader);
        TextView tvHardwareInfo = findViewById(R.id.tvHardwareInfo);
        TextView tvUserAgent = findViewById(R.id.tvUserAgent);
        TextView tvLiveStatusInfo = findViewById(R.id.tvLiveStatusInfo);
        Button btnCopyUa = findViewById(R.id.btnCopyUa);
        MaterialButton btnCopy = findViewById(R.id.btnCopy);

        String manufacturer = Build.MANUFACTURER.toUpperCase();
        String model = Build.MODEL;
        tvDeviceModelHeader.setText(manufacturer + " " + model);
        tvAndroidVersionHeader.setText("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");

        // User Agent
        try {
            userAgentString = WebSettings.getDefaultUserAgent(this);
        } catch (Exception e) {
            userAgentString = System.getProperty("http.agent");
            if (userAgentString == null) userAgentString = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + model + ")";
        }
        tvUserAgent.setText(userAgentString);

        // Hardware details
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        int densityDpi = dm.densityDpi;

        StringBuilder hw = new StringBuilder();
        hw.append("• Pabrikan     : ").append(manufacturer).append("\n");
        hw.append("• Model        : ").append(model).append("\n");
        hw.append("• Perangkat    : ").append(Build.DEVICE).append(" (").append(Build.PRODUCT).append(")\n");
        hw.append("• Device ID    : ").append(deviceId != null ? deviceId : "-").append("\n");
        hw.append("• Board        : ").append(Build.BOARD).append("\n");
        hw.append("• Hardware     : ").append(Build.HARDWARE).append("\n");
        hw.append("• CPU ABI      : ").append(Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "-").append("\n");
        hw.append("• Layar        : ").append(screenWidth).append(" x ").append(screenHeight).append(" px (").append(densityDpi).append(" DPI)\n");
        hw.append("• Bootloader   : ").append(Build.BOOTLOADER).append("\n");
        hw.append("• Fingerprint  : ").append(Build.FINGERPRINT);
        tvHardwareInfo.setText(hw.toString());

        // Live battery, RAM, and Network
        StringBuilder live = new StringBuilder();

        // Battery
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int temp = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            int batteryPct = (level >= 0 && scale > 0) ? (int) ((level / (float) scale) * 100) : -1;
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            live.append("• Baterai      : ").append(batteryPct).append("% (").append(isCharging ? "Sedang Mengisi Daya ⚡" : "Menggunakan Baterai").append(")\n");
            if (temp > 0) {
                live.append("• Suhu Baterai : ").append(temp / 10.0).append(" °C\n");
            }
        }

        // RAM
        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.getMemoryInfo(mi);
                long totalMb = mi.totalMem / (1024 * 1024);
                long availMb = mi.availMem / (1024 * 1024);
                live.append("• RAM Sistem   : ").append(availMb).append(" MB Bebas / ").append(totalMb).append(" MB Total\n");
            }
        } catch (Exception ignored) {}

        // Network
        live.append("• Koneksi      : ").append(getNetworkType()).append("\n");
        live.append("• IP Lokal     : ").append(getLocalIpAddress());
        tvLiveStatusInfo.setText(live.toString());

        // Full info text for copy
        StringBuilder full = new StringBuilder();
        full.append("=== INFORMASI PERANGKAT LENGKAP ===\n\n");
        full.append(hw).append("\n\n");
        full.append("=== STATUS DAYA & JARINGAN ===\n\n");
        full.append(live).append("\n\n");
        full.append("=== USER AGENT ===\n\n");
        full.append(userAgentString);
        fullDeviceInfoString = full.toString();

        btnCopyUa.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("User Agent", userAgentString));
                Snackbar.make(v, "User Agent berhasil disalin!", Snackbar.LENGTH_SHORT).show();
            }
        });

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Info Perangkat & UA", fullDeviceInfoString));
                Snackbar.make(v, "Semua informasi perangkat berhasil disalin!", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private String getNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null && ni.isConnected()) {
                    if (ni.getType() == ConnectivityManager.TYPE_WIFI) return "Wi-Fi";
                    if (ni.getType() == ConnectivityManager.TYPE_MOBILE) return "Data Seluler (" + ni.getSubtypeName() + ")";
                    if (ni.getType() == ConnectivityManager.TYPE_VPN) return "VPN";
                    return ni.getTypeName();
                }
            }
        } catch (Exception ignored) {}
        return "Terputus";
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) { // IPv4
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}
