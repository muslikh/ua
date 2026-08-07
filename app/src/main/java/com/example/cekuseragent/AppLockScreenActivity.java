package com.example.cekuseragent;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AppLockScreenActivity extends AppCompatActivity {
    private String lockedPackage = "";
    private AppLockManager lockManager;
    private final StringBuilder enteredPin = new StringBuilder();

    private View dot1, dot2, dot3, dot4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock_screen);

        lockManager = new AppLockManager(this);
        lockedPackage = getIntent().getStringExtra("locked_package");
        if (lockedPackage == null) lockedPackage = "";

        ImageView ivAppIcon = findViewById(R.id.ivLockedAppIcon);
        TextView tvAppName = findViewById(R.id.tvLockedAppName);

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
        int activeColor = ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = Color.parseColor("#CCCCCC");

        dot1.setBackgroundTintList(ColorStateList.valueOf(length >= 1 ? activeColor : inactiveColor));
        dot2.setBackgroundTintList(ColorStateList.valueOf(length >= 2 ? activeColor : inactiveColor));
        dot3.setBackgroundTintList(ColorStateList.valueOf(length >= 3 ? activeColor : inactiveColor));
        dot4.setBackgroundTintList(ColorStateList.valueOf(length >= 4 ? activeColor : inactiveColor));
    }

    private void checkEnteredPin() {
        if (lockManager.checkPin(enteredPin.toString())) {
            AppLockManager.setTemporarilyUnlocked(lockedPackage);
            finish();
        } else {
            Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show();
            enteredPin.setLength(0);
            updatePinDots();
        }
    }

    private void goToHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        finish();
    }
}
