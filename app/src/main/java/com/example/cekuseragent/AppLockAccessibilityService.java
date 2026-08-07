package com.example.cekuseragent;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.accessibility.AccessibilityEvent;

public class AppLockAccessibilityService extends AccessibilityService {
    private AppLockManager lockManager;
    private String currentActivePackage = "";

    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                // Kunci ulang seluruh aplikasi saat layar HP mati
                AppLockManager.clearTemporaryUnlock();
                currentActivePackage = "";
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        lockManager = new AppLockManager(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver, filter);
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 10;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence pkgChar = event.getPackageName();
        if (pkgChar == null) return;
        String packageName = pkgChar.toString();

        // Abaikan jika event berasal dari aplikasi kita sendiri
        if (packageName.equals(getPackageName())) {
            return;
        }

        // Jika berpindah jendela/aplikasi (termasuk kembali ke Home/Launcher)
        if (!packageName.equals(currentActivePackage)) {
            // Jika user keluar dari aplikasi yang baru saja di-unlock, langsung reset status buka kunci
            if (!packageName.equals(AppLockManager.getLastUnlockedPackage())) {
                AppLockManager.clearTemporaryUnlock();
            }
            currentActivePackage = packageName;
        }

        // Jika aplikasi yang dibuka masuk dalam daftar kunci
        if (lockManager.isLockEnabled() && lockManager.isPackageLocked(packageName)) {
            if (!AppLockManager.isTemporarilyUnlocked(packageName)) {
                // Tampilkan layar PIN seketika (0ms delay)
                Intent lockIntent = new Intent(this, AppLockScreenActivity.class);
                lockIntent.putExtra("locked_package", packageName);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(lockIntent);
            }
        }
    }

    @Override
    public void onInterrupt() {}
}
