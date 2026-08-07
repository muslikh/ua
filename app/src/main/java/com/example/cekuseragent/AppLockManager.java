package com.example.cekuseragent;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppLockManager {
    private static final String PREF_NAME = "mytools_app_lock";
    private static final String KEY_LOCKED_PACKAGES = "locked_packages";
    private static final String KEY_PIN = "security_pin";
    private static final String KEY_LOCK_ENABLED = "lock_service_enabled";

    private final SharedPreferences prefs;
    private final Context context;

    private static String lastUnlockedPackage = "";
    private static long lastUnlockedTime = 0;

    public AppLockManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLockEnabled() {
        return prefs.getBoolean(KEY_LOCK_ENABLED, false);
    }

    public void setLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply();
        if (enabled) {
            startLockService();
        } else {
            stopLockService();
        }
    }

    public void startLockService() {
        try {
            Intent intent = new Intent(context, AppLockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopLockService() {
        try {
            Intent intent = new Intent(context, AppLockService.class);
            context.stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<String> getLockedPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_LOCKED_PACKAGES, new HashSet<>()));
    }

    public int getLockedCount() {
        return getLockedPackages().size();
    }

    public boolean isPackageLocked(String packageName) {
        return getLockedPackages().contains(packageName);
    }

    public void setPackageLocked(String packageName, boolean locked) {
        Set<String> current = getLockedPackages();
        if (locked) {
            current.add(packageName);
        } else {
            current.remove(packageName);
            if (packageName.equals(lastUnlockedPackage)) {
                clearTemporaryUnlock();
            }
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply();
    }

    public String getPin() {
        return prefs.getString(KEY_PIN, "1234");
    }

    public void setPin(String newPin) {
        prefs.edit().putString(KEY_PIN, newPin).apply();
    }

    public boolean checkPin(String inputPin) {
        return getPin().equals(inputPin);
    }

    public static void setTemporarilyUnlocked(String packageName) {
        lastUnlockedPackage = packageName;
        lastUnlockedTime = System.currentTimeMillis();
    }

    public static String getLastUnlockedPackage() {
        return lastUnlockedPackage;
    }

    public static void clearTemporaryUnlock() {
        lastUnlockedPackage = "";
        lastUnlockedTime = 0;
    }

    public static void clearTemporaryUnlockIfNot(String pkg) {
        if (!lastUnlockedPackage.equals(pkg)) {
            clearTemporaryUnlock();
        }
    }

    public static boolean isTemporarilyUnlocked(String packageName) {
        if (packageName.equals(lastUnlockedPackage)) {
            // Unlocked as long as session is within 10 minutes and user hasn't switched away
            return (System.currentTimeMillis() - lastUnlockedTime) < 600000;
        }
        return false;
    }

    public boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo service : runningServices) {
            if (service.getId() != null && service.getId().contains(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUsageStatsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return true;
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return Settings.canDrawOverlays(context);
    }

    public static Intent getAccessibilityIntent() {
        return new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
    }

    public static Intent getUsageStatsIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    public static Intent getOverlayIntent(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.getPackageName()));
        }
        return new Intent(Settings.ACTION_SETTINGS);
    }
}
