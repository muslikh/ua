package com.example.cekuseragent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IpInfoActivity extends AppCompatActivity {
    private TextView tvIpAddress, tvIsp, tvOrg, tvLocation, tvTimezone;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_info);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvIpAddress = findViewById(R.id.tvIpAddress);
        tvIsp = findViewById(R.id.tvIsp);
        tvOrg = findViewById(R.id.tvOrg);
        tvLocation = findViewById(R.id.tvLocation);
        tvTimezone = findViewById(R.id.tvTimezone);

        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> fetchIpInfo());

        fetchIpInfo();
    }

    private void fetchIpInfo() {
        tvIpAddress.setText("Mengecek...");
        tvIsp.setText("-");
        tvOrg.setText("-");
        tvLocation.setText("-");
        tvTimezone.setText("-");

        executorService.execute(() -> {
            try {
                URL url = new URL("http://ip-api.com/json/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.getString("status").equals("success")) {
                    String ip = json.getString("query");
                    String isp = json.getString("isp");
                    String org = json.getString("org");
                    String location = json.getString("city") + ", " + json.getString("regionName") + ", " + json.getString("country");
                    String timezone = json.getString("timezone");

                    mainHandler.post(() -> {
                        tvIpAddress.setText(ip);
                        tvIsp.setText(isp);
                        tvOrg.setText(org);
                        tvLocation.setText(location);
                        tvTimezone.setText(timezone);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    tvIpAddress.setText("Gagal mengambil data");
                    Toast.makeText(this, "Periksa koneksi internet Anda", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
