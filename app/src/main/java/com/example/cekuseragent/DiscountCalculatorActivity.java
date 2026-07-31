package com.example.cekuseragent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.Locale;

public class DiscountCalculatorActivity extends AppCompatActivity {
    private TextInputEditText etPrice, etDiscount, etExtraDiscount;
    private TextView tvSaveAmount, tvFinalPrice;
    private MaterialCardView cvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount_calculator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etPrice = findViewById(R.id.etPrice);
        etDiscount = findViewById(R.id.etDiscount);
        etExtraDiscount = findViewById(R.id.etExtraDiscount);
        tvSaveAmount = findViewById(R.id.tvSaveAmount);
        tvFinalPrice = findViewById(R.id.tvFinalPrice);
        cvResult = findViewById(R.id.cvResult);

        Button btnCalculate = findViewById(R.id.btnCalculate);
        btnCalculate.setOnClickListener(v -> calculateDiscount());
    }

    private void calculateDiscount() {
        String priceStr = etPrice.getText().toString();
        String discStr = etDiscount.getText().toString();
        String extraDiscStr = etExtraDiscount.getText().toString();

        if (priceStr.isEmpty() || discStr.isEmpty()) {
            Toast.makeText(this, "Masukkan harga dan diskon utama", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            double disc = Double.parseDouble(discStr);
            double extraDisc = extraDiscStr.isEmpty() ? 0 : Double.parseDouble(extraDiscStr);

            double discountAmount1 = price * (disc / 100);
            double priceAfterDisc1 = price - discountAmount1;

            double discountAmount2 = priceAfterDisc1 * (extraDisc / 100);
            double finalPrice = priceAfterDisc1 - discountAmount2;

            double totalSaved = price - finalPrice;

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            
            tvSaveAmount.setText(format.format(totalSaved));
            tvFinalPrice.setText(format.format(finalPrice));
            cvResult.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
        }
    }
}
