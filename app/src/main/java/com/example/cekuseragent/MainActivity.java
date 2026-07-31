package com.example.cekuseragent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvUserAgent = findViewById(R.id.tvUserAgent);
        TextView tvVersion = findViewById(R.id.tvVersion);
        MaterialButton btnCopy = findViewById(R.id.btnCopy);
        
        // Tampilkan versi saat ini
        tvVersion.setText("Versi: " + BuildConfig.VERSION_NAME);
        
        // Ambil User Agent Android murni
        String userAgent = System.getProperty("http.agent");
        
        if (userAgent != null) {
            tvUserAgent.setText(userAgent);
        } else {
            tvUserAgent.setText("Tidak dapat memuat User Agent");
        }

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && userAgent != null) {
                ClipData clip = ClipData.newPlainText("User Agent", userAgent);
                clipboard.setPrimaryClip(clip);
                Snackbar.make(v, "User Agent berhasil disalin!", Snackbar.LENGTH_SHORT).show();
            }
        });

        // Panggil auto-update saat aplikasi dibuka
        new UpdateHelper(this).checkForUpdate();
    }
}
