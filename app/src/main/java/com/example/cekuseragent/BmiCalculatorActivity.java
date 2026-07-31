package com.example.cekuseragent;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class BmiCalculatorActivity extends AppCompatActivity {
    private TextInputEditText etWeight, etHeight;
    private TextView tvBmiScore, tvBmiCategory, tvBmiDesc;
    private MaterialCardView cvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi_calculator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        tvBmiScore = findViewById(R.id.tvBmiScore);
        tvBmiCategory = findViewById(R.id.tvBmiCategory);
        tvBmiDesc = findViewById(R.id.tvBmiDesc);
        cvResult = findViewById(R.id.cvResult);

        Button btnCalculate = findViewById(R.id.btnCalculate);
        btnCalculate.setOnClickListener(v -> calculateBmi());
    }

    private void calculateBmi() {
        String weightStr = etWeight.getText().toString();
        String heightStr = etHeight.getText().toString();

        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(this, "Masukkan berat dan tinggi badan", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            float weight = Float.parseFloat(weightStr);
            float heightCm = Float.parseFloat(heightStr);
            float heightM = heightCm / 100f;

            float bmi = weight / (heightM * heightM);
            displayBmiResult(bmi);
        } catch (Exception e) {
            Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayBmiResult(float bmi) {
        tvBmiScore.setText(String.format(java.util.Locale.US, "%.1f", bmi));
        cvResult.setVisibility(View.VISIBLE);

        String category = "";
        String desc = "";
        int color = Color.BLACK;

        if (bmi < 18.5) {
            category = "Kekurangan Berat Badan";
            desc = "Anda memiliki berat badan di bawah normal. Cobalah untuk menambah asupan kalori bergizi.";
            color = Color.parseColor("#FF9800"); // Orange
        } else if (bmi >= 18.5 && bmi < 24.9) {
            category = "Normal (Ideal)";
            desc = "Berat badan Anda berada dalam kategori ideal. Pertahankan pola makan dan gaya hidup sehat!";
            color = Color.parseColor("#4CAF50"); // Green
        } else if (bmi >= 25 && bmi < 29.9) {
            category = "Kelebihan Berat Badan";
            desc = "Anda memiliki sedikit kelebihan berat badan. Disarankan untuk mulai berolahraga secara rutin.";
            color = Color.parseColor("#FF9800"); // Orange
        } else {
            category = "Obesitas";
            desc = "Anda berada dalam kategori obesitas. Sangat disarankan untuk berkonsultasi dengan ahli gizi atau dokter.";
            color = Color.parseColor("#F44336"); // Red
        }

        tvBmiCategory.setText(category);
        tvBmiCategory.setTextColor(color);
        tvBmiDesc.setText(desc);
    }
}
