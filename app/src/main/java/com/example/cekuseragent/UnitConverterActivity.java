package com.example.cekuseragent;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UnitConverterActivity extends AppCompatActivity {
    private Spinner spinnerCategory, spinnerFrom, spinnerTo;
    private TextInputEditText etInput;
    private TextView tvResult;
    private MaterialCardView cvResult;

    private final String[] categories = {"Panjang", "Berat", "Suhu", "Volume", "Waktu"};
    private final Map<String, String[]> unitMap = new HashMap<>();
    private final Map<String, Map<String, Double>> conversionMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_converter);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        etInput = findViewById(R.id.etInput);
        tvResult = findViewById(R.id.tvResult);
        cvResult = findViewById(R.id.cvResult);

        initUnits();

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateUnitSpinners(categories[pos]); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        Button btnConvert = findViewById(R.id.btnConvert);
        btnConvert.setOnClickListener(v -> convert());
    }

    private void initUnits() {
        unitMap.put("Panjang", new String[]{"Meter", "Kilometer", "Centimeter", "Milimeter", "Inci", "Kaki", "Mil"});
        unitMap.put("Berat", new String[]{"Kilogram", "Gram", "Miligram", "Ons", "Pon", "Ton"});
        unitMap.put("Suhu", new String[]{"Celsius", "Fahrenheit", "Kelvin"});
        unitMap.put("Volume", new String[]{"Liter", "Mililiter", "Galon", "Cangkir"});
        unitMap.put("Waktu", new String[]{"Detik", "Menit", "Jam", "Hari", "Minggu"});

        // Conversion to base unit (m, kg, L, s)
        Map<String, Double> length = new HashMap<>();
        length.put("Meter", 1.0); length.put("Kilometer", 1000.0); length.put("Centimeter", 0.01);
        length.put("Milimeter", 0.001); length.put("Inci", 0.0254); length.put("Kaki", 0.3048); length.put("Mil", 1609.344);
        conversionMap.put("Panjang", length);

        Map<String, Double> weight = new HashMap<>();
        weight.put("Kilogram", 1.0); weight.put("Gram", 0.001); weight.put("Miligram", 0.000001);
        weight.put("Ons", 0.1); weight.put("Pon", 0.453592); weight.put("Ton", 1000.0);
        conversionMap.put("Berat", weight);

        Map<String, Double> volume = new HashMap<>();
        volume.put("Liter", 1.0); volume.put("Mililiter", 0.001); volume.put("Galon", 3.78541); volume.put("Cangkir", 0.236588);
        conversionMap.put("Volume", volume);

        Map<String, Double> time = new HashMap<>();
        time.put("Detik", 1.0); time.put("Menit", 60.0); time.put("Jam", 3600.0); time.put("Hari", 86400.0); time.put("Minggu", 604800.0);
        conversionMap.put("Waktu", time);
    }

    private void updateUnitSpinners(String category) {
        String[] units = unitMap.get(category);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, units);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        if (units.length > 1) spinnerTo.setSelection(1);
    }

    private void convert() {
        String inputStr = etInput.getText().toString();
        if (inputStr.isEmpty()) { Toast.makeText(this, "Masukkan nilai", Toast.LENGTH_SHORT).show(); return; }

        double input = Double.parseDouble(inputStr);
        String category = categories[spinnerCategory.getSelectedItemPosition()];
        String from = spinnerFrom.getSelectedItem().toString();
        String to = spinnerTo.getSelectedItem().toString();

        double result;
        if (category.equals("Suhu")) {
            result = convertTemperature(input, from, to);
        } else {
            Map<String, Double> factors = conversionMap.get(category);
            double baseValue = input * factors.get(from);
            result = baseValue / factors.get(to);
        }

        tvResult.setText(String.format(Locale.US, "%,.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "") + " " + to);
        cvResult.setVisibility(View.VISIBLE);
    }

    private double convertTemperature(double val, String from, String to) {
        double celsius;
        switch (from) {
            case "Fahrenheit": celsius = (val - 32) * 5.0 / 9.0; break;
            case "Kelvin": celsius = val - 273.15; break;
            default: celsius = val;
        }
        switch (to) {
            case "Fahrenheit": return celsius * 9.0 / 5.0 + 32;
            case "Kelvin": return celsius + 273.15;
            default: return celsius;
        }
    }
}
