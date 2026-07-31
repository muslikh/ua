package com.example.cekuseragent;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Locale;

public class StopwatchActivity extends AppCompatActivity {
    private TextView tvTimer, tvLaps;
    private Button btnStartStop, btnReset, btnLap;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long startTime = 0, elapsedTime = 0, pausedTime = 0;
    private boolean isRunning = false;
    private int lapCount = 0;
    private StringBuilder lapsBuilder = new StringBuilder();

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            elapsedTime = SystemClock.elapsedRealtime() - startTime;
            updateTimerDisplay(elapsedTime);
            handler.postDelayed(this, 10);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvTimer = findViewById(R.id.tvTimer);
        tvLaps = findViewById(R.id.tvLaps);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnReset = findViewById(R.id.btnReset);
        btnLap = findViewById(R.id.btnLap);

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());

        btnLap.setOnClickListener(v -> {
            lapCount++;
            String lapText = String.format(Locale.US, "Lap %d: %s\n", lapCount, formatTime(elapsedTime));
            lapsBuilder.insert(0, lapText);
            tvLaps.setText(lapsBuilder.toString());
        });
    }

    private void startTimer() {
        startTime = SystemClock.elapsedRealtime() - pausedTime;
        handler.post(timerRunnable);
        isRunning = true;
        btnStartStop.setText("Berhenti");
        btnLap.setVisibility(View.VISIBLE);
    }

    private void pauseTimer() {
        handler.removeCallbacks(timerRunnable);
        pausedTime = elapsedTime;
        isRunning = false;
        btnStartStop.setText("Lanjut");
    }

    private void resetTimer() {
        handler.removeCallbacks(timerRunnable);
        isRunning = false;
        elapsedTime = 0;
        pausedTime = 0;
        lapCount = 0;
        lapsBuilder = new StringBuilder();
        tvTimer.setText("00:00.00");
        tvLaps.setText("");
        btnStartStop.setText("Mulai");
        btnLap.setVisibility(View.GONE);
    }

    private void updateTimerDisplay(long timeMs) {
        tvTimer.setText(formatTime(timeMs));
    }

    private String formatTime(long timeMs) {
        int minutes = (int) (timeMs / 60000);
        int seconds = (int) ((timeMs % 60000) / 1000);
        int centis = (int) ((timeMs % 1000) / 10);
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis);
    }
}
