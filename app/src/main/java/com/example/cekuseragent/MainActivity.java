package com.example.cekuseragent;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvUserAgent = findViewById(R.id.tvUserAgent);
        
        // Ambil User Agent Android murni (Dalvik)
        String userAgent = System.getProperty("http.agent");
        
        tvUserAgent.setText("User Agent HP Anda:\n\n" + userAgent);
    }
}
