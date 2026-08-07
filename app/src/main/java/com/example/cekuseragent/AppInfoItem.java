package com.example.cekuseragent;

import android.graphics.drawable.Drawable;

public class AppInfoItem {
    private final String appName;
    private final String packageName;
    private final Drawable icon;
    private boolean isChecked;

    public AppInfoItem(String appName, String packageName, Drawable icon, boolean isChecked) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.isChecked = isChecked;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
