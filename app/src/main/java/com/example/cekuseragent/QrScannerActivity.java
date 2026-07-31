package com.example.cekuseragent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class QrScannerActivity extends AppCompatActivity {
    private TextView tvScanResult;
    private Button btnOpenUrl;
    private String lastScannedUrl = "";

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show();
                } else {
                    String content = result.getContents();
                    tvScanResult.setText(content);
                    
                    if (Patterns.WEB_URL.matcher(content).matches()) {
                        lastScannedUrl = content;
                        if (!lastScannedUrl.startsWith("http")) {
                            lastScannedUrl = "http://" + lastScannedUrl;
                        }
                        btnOpenUrl.setVisibility(View.VISIBLE);
                    } else {
                        btnOpenUrl.setVisibility(View.GONE);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvScanResult = findViewById(R.id.tvScanResult);
        Button btnScan = findViewById(R.id.btnScan);
        btnOpenUrl = findViewById(R.id.btnOpenUrl);

        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
            options.setPrompt("Arahkan kamera ke QR Code / Barcode");
            options.setCameraId(0);
            options.setBeepEnabled(true);
            options.setBarcodeImageEnabled(false);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });

        btnOpenUrl.setOnClickListener(v -> {
            if (!lastScannedUrl.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lastScannedUrl));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka link", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
