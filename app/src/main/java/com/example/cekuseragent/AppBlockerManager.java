package com.example.cekuseragent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

public class AppBlockerManager {
    private static final String PREF_NAME = "mytools_app_blocker";
    private static final String KEY_BLOCKED_PACKAGES = "blocked_packages";
    private static final String KEY_FIREWALL_ENABLED = "firewall_enabled";

    private final SharedPreferences prefs;
    private final Context context;

    public AppBlockerManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirewallEnabled() {
        return prefs.getBoolean(KEY_FIREWALL_ENABLED, false);
    }

    public void setFirewallEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FIREWALL_ENABLED, enabled).apply();
        if (enabled) {
            startFirewallService();
        } else {
            stopFirewallService();
        }
    }

    public void startFirewallService() {
        try {
            Intent intent = new Intent(context, AppBlockerVpnService.class);
            intent.setAction(AppBlockerVpnService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopFirewallService() {
        try {
            Intent intent = new Intent(context, AppBlockerVpnService.class);
            intent.setAction(AppBlockerVpnService.ACTION_STOP);
            context.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reloadFirewall() {
        if (isFirewallEnabled()) {
            startFirewallService();
        }
    }

    public Set<String> getBlockedPackages() {
        return new HashSet<>(prefs.getStringSet(KEY_BLOCKED_PACKAGES, new HashSet<>()));
    }

    public boolean isPackageBlocked(String packageName) {
        return getBlockedPackages().contains(packageName);
    }

    public void setPackageBlocked(String packageName, boolean blocked) {
        Set<String> current = getBlockedPackages();
        if (blocked) {
            current.add(packageName);
        } else {
            current.remove(packageName);
        }
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, current).apply();
        reloadFirewall();
    }

    public void setAllBlocked(Set<String> packageNames) {
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, packageNames).apply();
        reloadFirewall();
    }
}
