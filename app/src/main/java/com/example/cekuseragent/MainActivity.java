package com.example.cekuseragent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("Versi: " + BuildConfig.VERSION_NAME);

        MaterialCardView menuUserAgent = findViewById(R.id.menuUserAgent);
        MaterialCardView menuDeviceInfo = findViewById(R.id.menuDeviceInfo);
        MaterialCardView menuTimestamp = findViewById(R.id.menuTimestamp);
        MaterialCardView menuQrCode = findViewById(R.id.menuQrCode);
        MaterialCardView menuQrScanner = findViewById(R.id.menuQrScanner);
        MaterialCardView menuIpInfo = findViewById(R.id.menuIpInfo);
        MaterialCardView menuBmi = findViewById(R.id.menuBmi);
        MaterialCardView menuDiscount = findViewById(R.id.menuDiscount);
        MaterialCardView menuUnitConverter = findViewById(R.id.menuUnitConverter);
        MaterialCardView menuCompass = findViewById(R.id.menuCompass);
        MaterialCardView menuAgeCalc = findViewById(R.id.menuAgeCalc);
        MaterialCardView menuStopwatch = findViewById(R.id.menuStopwatch);
        MaterialCardView menuFlashlight = findViewById(R.id.menuFlashlight);
        MaterialCardView menuNotes = findViewById(R.id.menuNotes);
        MaterialCardView menuCurrency = findViewById(R.id.menuCurrency);
        MaterialCardView menuLoanCalc = findViewById(R.id.menuLoanCalc);

        menuUserAgent.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserAgentActivity.class));
        });

        menuDeviceInfo.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeviceInfoActivity.class));
        });

        menuTimestamp.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TimestampActivity.class));
        });

        menuQrCode.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, QrCodeActivity.class));
        });

        menuQrScanner.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, QrScannerActivity.class));
        });

        menuIpInfo.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, IpInfoActivity.class));
        });

        menuBmi.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BmiCalculatorActivity.class));
        });

        menuDiscount.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DiscountCalculatorActivity.class));
        });

        menuUnitConverter.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UnitConverterActivity.class));
        });

        menuCompass.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CompassActivity.class));
        });

        menuAgeCalc.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AgeCalculatorActivity.class));
        });

        menuStopwatch.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StopwatchActivity.class));
        });

        menuFlashlight.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FlashlightActivity.class));
        });

        menuNotes.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NotesActivity.class));
        });

        menuCurrency.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CurrencyActivity.class));
        });

        menuLoanCalc.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoanCalculatorActivity.class));
        });

        // Panggil auto-update saat aplikasi dibuka
        new UpdateHelper(this).checkForUpdate();
    }
}
