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

public class LoanCalculatorActivity extends AppCompatActivity {
    private TextInputEditText etLoan, etInterest, etTenor;
    private TextView tvMonthly, tvTotalInterest, tvTotalPayment;
    private MaterialCardView cvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_calculator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etLoan = findViewById(R.id.etLoan);
        etInterest = findViewById(R.id.etInterest);
        etTenor = findViewById(R.id.etTenor);
        tvMonthly = findViewById(R.id.tvMonthly);
        tvTotalInterest = findViewById(R.id.tvTotalInterest);
        tvTotalPayment = findViewById(R.id.tvTotalPayment);
        cvResult = findViewById(R.id.cvResult);

        Button btnCalculate = findViewById(R.id.btnCalculate);
        btnCalculate.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String loanStr = etLoan.getText().toString();
        String interestStr = etInterest.getText().toString();
        String tenorStr = etTenor.getText().toString();

        if (loanStr.isEmpty() || interestStr.isEmpty() || tenorStr.isEmpty()) {
            Toast.makeText(this, "Lengkapi semua data", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double principal = Double.parseDouble(loanStr);
            double annualRate = Double.parseDouble(interestStr);
            int months = Integer.parseInt(tenorStr);

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

            if (annualRate == 0) {
                // Interest-free
                double monthly = principal / months;
                tvMonthly.setText(format.format(monthly));
                tvTotalInterest.setText(format.format(0));
                tvTotalPayment.setText(format.format(principal));
            } else {
                double monthlyRate = annualRate / 12.0 / 100.0;
                // Standard amortization formula
                double monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months))
                        / (Math.pow(1 + monthlyRate, months) - 1);
                double totalPayment = monthlyPayment * months;
                double totalInterest = totalPayment - principal;

                tvMonthly.setText(format.format(monthlyPayment));
                tvTotalInterest.setText(format.format(totalInterest));
                tvTotalPayment.setText(format.format(totalPayment));
            }

            cvResult.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
        }
    }
}
