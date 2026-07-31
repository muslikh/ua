package com.example.cekuseragent;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvUserAgent = findViewById(R.id.tvUserAgent);
        Button btnCopy = findViewById(R.id.btnCopy);
        
        // Ambil User Agent Android murni (Dalvik)
        String userAgent = System.getProperty("http.agent");
        
        tvUserAgent.setText("User Agent HP Anda:\n\n" + userAgent);

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("User Agent", userAgent);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(MainActivity.this, "User Agent berhasil di-copy!", Toast.LENGTH_SHORT).show();
        });
    }
}
