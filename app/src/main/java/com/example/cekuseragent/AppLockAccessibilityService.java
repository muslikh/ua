package com.example.cekuseragent;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

public class AppLockAccessibilityService extends AccessibilityService {
    private AppLockManager lockManager;
    private String currentActivePackage = "";

    @Override
    public void onCreate() {
        super.onCreate();
        lockManager = new AppLockManager(this);
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

        // Ignore our own app, system UI / launcher keyboards / lockscreen
        if (packageName.equals(getPackageName()) ||
            packageName.equals("android") ||
            packageName.equals("com.android.systemui") ||
            packageName.contains("launcher") ||
            packageName.contains("inputmethod")) {
            return;
        }

        if (!packageName.equals(currentActivePackage)) {
            // Switched to a new app -> if user left the unlocked app, reset temporary unlock for other packages
            if (!packageName.equals(AppLockManager.getLastUnlockedPackage())) {
                AppLockManager.clearTemporaryUnlockIfNot(packageName);
            }
            currentActivePackage = packageName;
        }

        if (lockManager.isLockEnabled() && lockManager.isPackageLocked(packageName)) {
            if (!AppLockManager.isTemporarilyUnlocked(packageName)) {
                // Launch lock screen instantly
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
