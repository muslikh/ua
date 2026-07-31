package com.example.cekuseragent;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AgeCalculatorActivity extends AppCompatActivity {
    private TextInputEditText etBirthDate;
    private TextView tvAge, tvAgeDetail, tvNextBirthday, tvTotalDays;
    private MaterialCardView cvResult;
    private int birthYear, birthMonth, birthDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_age_calculator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etBirthDate = findViewById(R.id.etBirthDate);
        tvAge = findViewById(R.id.tvAge);
        tvAgeDetail = findViewById(R.id.tvAgeDetail);
        tvNextBirthday = findViewById(R.id.tvNextBirthday);
        tvTotalDays = findViewById(R.id.tvTotalDays);
        cvResult = findViewById(R.id.cvResult);

        etBirthDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                birthYear = year; birthMonth = month; birthDay = day;
                etBirthDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, c.get(Calendar.YEAR) - 20, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        Button btnCalculate = findViewById(R.id.btnCalculateAge);
        btnCalculate.setOnClickListener(v -> calculateAge());
    }

    private void calculateAge() {
        if (etBirthDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Pilih tanggal lahir terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar today = Calendar.getInstance();
        Calendar birth = Calendar.getInstance();
        birth.set(birthYear, birthMonth, birthDay);

        int years = today.get(Calendar.YEAR) - birthYear;
        int months = today.get(Calendar.MONTH) - birthMonth;
        int days = today.get(Calendar.DAY_OF_MONTH) - birthDay;

        if (days < 0) {
            months--;
            Calendar temp = (Calendar) today.clone();
            temp.add(Calendar.MONTH, -1);
            days += temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years--;
            months += 12;
        }

        tvAge.setText(String.valueOf(years) + " Tahun");
        tvAgeDetail.setText(String.format(Locale.getDefault(), "%d Bulan, %d Hari", months, days));

        // Total days
        long diffMs = today.getTimeInMillis() - birth.getTimeInMillis();
        long totalDays = TimeUnit.MILLISECONDS.toDays(diffMs);
        long totalHours = TimeUnit.MILLISECONDS.toHours(diffMs);
        tvTotalDays.setText(String.format(Locale.getDefault(), "Total: %,d hari\n(%,d jam)", totalDays, totalHours));

        // Next birthday
        Calendar nextBday = Calendar.getInstance();
        nextBday.set(today.get(Calendar.YEAR), birthMonth, birthDay);
        if (nextBday.before(today) || nextBday.equals(today)) {
            nextBday.add(Calendar.YEAR, 1);
        }
        long daysToNext = TimeUnit.MILLISECONDS.toDays(nextBday.getTimeInMillis() - today.getTimeInMillis());
        tvNextBirthday.setText(String.format(Locale.getDefault(), "🎂 Ulang tahun berikutnya dalam\n%d hari", daysToNext));

        cvResult.setVisibility(View.VISIBLE);
    }
}
