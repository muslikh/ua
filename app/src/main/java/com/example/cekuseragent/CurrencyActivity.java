package com.example.cekuseragent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyActivity extends AppCompatActivity {
    private TextInputEditText etAmount;
    private Spinner spinnerFrom, spinnerTo;
    private TextView tvResult, tvRate;
    private MaterialCardView cvResult;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final String[] currencies = {"IDR", "USD", "EUR", "GBP", "JPY", "SGD", "MYR", "AUD", "CNY", "KRW", "SAR", "THB"};
    private final String[] currencyNames = {
            "IDR - Rupiah", "USD - Dolar AS", "EUR - Euro", "GBP - Poundsterling",
            "JPY - Yen Jepang", "SGD - Dolar Singapura", "MYR - Ringgit Malaysia",
            "AUD - Dolar Australia", "CNY - Yuan Tiongkok", "KRW - Won Korea",
            "SAR - Riyal Saudi", "THB - Baht Thailand"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etAmount = findViewById(R.id.etAmount);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        tvResult = findViewById(R.id.tvResult);
        tvRate = findViewById(R.id.tvRate);
        cvResult = findViewById(R.id.cvResult);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencyNames);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerTo.setSelection(1);

        Button btnConvert = findViewById(R.id.btnConvert);
        btnConvert.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Masukkan jumlah", Toast.LENGTH_SHORT).show();
                return;
            }
            btnConvert.setText("Mengonversi...");
            btnConvert.setEnabled(false);
            convertCurrency(Double.parseDouble(amountStr), btnConvert);
        });
    }

    private void convertCurrency(double amount, Button btn) {
        String from = currencies[spinnerFrom.getSelectedItemPosition()];
        String to = currencies[spinnerTo.getSelectedItemPosition()];

        executor.execute(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/" + from);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONObject rates = json.getJSONObject("rates");
                double rate = rates.getDouble(to);
                double result = amount * rate;

                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                nf.setMaximumFractionDigits(2);

                mainHandler.post(() -> {
                    tvResult.setText(nf.format(result) + " " + to);
                    tvRate.setText("1 " + from + " = " + nf.format(rate) + " " + to);
                    cvResult.setVisibility(View.VISIBLE);
                    btn.setText("Konversi");
                    btn.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Gagal mengambil data. Periksa internet Anda.", Toast.LENGTH_SHORT).show();
                    btn.setText("Konversi");
                    btn.setEnabled(true);
                });
            }
        });
    }
}
