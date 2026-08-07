package com.example.cekuseragent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class AppLockScreenActivity extends AppCompatActivity {
    private String lockedPackage = "";
    private AppLockManager lockManager;
    private final StringBuilder enteredPin = new StringBuilder();

    private View dot1, dot2, dot3, dot4;
    private View pinDotsContainer;
    private TextView tvLockPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on and show over lockscreen if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        setContentView(R.layout.activity_app_lock_screen);

        lockManager = new AppLockManager(this);
        lockedPackage = getIntent().getStringExtra("locked_package");
        if (lockedPackage == null) lockedPackage = "";

        ImageView ivAppIcon = findViewById(R.id.ivLockedAppIcon);
        TextView tvAppName = findViewById(R.id.tvLockedAppName);
        tvLockPrompt = findViewById(R.id.tvLockPrompt);
        pinDotsContainer = findViewById(R.id.pinDotsContainer);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);

        if (!lockedPackage.isEmpty()) {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(lockedPackage, 0);
                tvAppName.setText(pm.getApplicationLabel(appInfo));
                Drawable icon = pm.getApplicationIcon(appInfo);
                ivAppIcon.setImageDrawable(icon);
            } catch (Exception e) {
                tvAppName.setText(lockedPackage);
            }
        }

        setupKeypad();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToHome();
            }
        });
    }

    private void setupKeypad() {
        int[] keyIds = {
                R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3, R.id.btnKey4,
                R.id.btnKey5, R.id.btnKey6, R.id.btnKey7, R.id.btnKey8, R.id.btnKey9
        };

        for (int id : keyIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> appendDigit(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnKeyDelete).setOnClickListener(v -> deleteDigit());
        findViewById(R.id.btnKeyCancel).setOnClickListener(v -> goToHome());
    }

    private void appendDigit(String digit) {
        if (enteredPin.length() < 4) {
            enteredPin.append(digit);
            updatePinDots();

            if (enteredPin.length() == 4) {
                checkEnteredPin();
            }
        }
    }

    private void deleteDigit() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            updatePinDots();
        }
    }

    private void updatePinDots() {
        int length = enteredPin.length();
        int activeColor = Color.parseColor("#3B82F6"); // Electric Blue
        int inactiveColor = Color.parseColor("#334155"); // Dark slate

        dot1.setBackgroundTintList(ColorStateList.valueOf(length >= 1 ? activeColor : inactiveColor));
        dot2.setBackgroundTintList(ColorStateList.valueOf(length >= 2 ? activeColor : inactiveColor));
        dot3.setBackgroundTintList(ColorStateList.valueOf(length >= 3 ? activeColor : inactiveColor));
        dot4.setBackgroundTintList(ColorStateList.valueOf(length >= 4 ? activeColor : inactiveColor));
    }

    private void checkEnteredPin() {
        if (lockManager.checkPin(enteredPin.toString())) {
            AppLockManager.setTemporarilyUnlocked(lockedPackage);
            finish();
            overridePendingTransition(0, android.R.anim.fade_out);
        } else {
            vibrateError();
            tvLockPrompt.setText("PIN Salah! Coba lagi");
            tvLockPrompt.setTextColor(Color.parseColor("#EF4444"));

            // Shake animation
            if (pinDotsContainer != null) {
                pinDotsContainer.animate()
                        .translationXBy(20f)
                        .setDuration(50)
                        .withEndAction(() -> pinDotsContainer.animate()
                                .translationXBy(-40f)
                                .setDuration(50)
                                .withEndAction(() -> pinDotsContainer.animate()
                                        .translationXBy(20f)
                                        .setDuration(50)
                                        .start())
                                .start())
                        .start();
            }

            enteredPin.setLength(0);
            updatePinDots();
        }
    }

    private void vibrateError() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(200);
                }
            }
        } catch (Exception ignored) {}
    }

    private void goToHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        finish();
    }
}
