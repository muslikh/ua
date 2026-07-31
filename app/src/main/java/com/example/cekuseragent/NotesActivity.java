package com.example.cekuseragent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {
    private TextInputEditText etNote;
    private LinearLayout llNotes;
    private SharedPreferences prefs;
    private JSONArray notesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNote = findViewById(R.id.etNote);
        llNotes = findViewById(R.id.llNotes);
        Button btnAdd = findViewById(R.id.btnAddNote);

        prefs = getSharedPreferences("mytools_notes", MODE_PRIVATE);
        loadNotes();

        btnAdd.setOnClickListener(v -> {
            String text = etNote.getText().toString().trim();
            if (!text.isEmpty()) {
                addNote(text);
                etNote.setText("");
            } else {
                Toast.makeText(this, "Tulis catatan terlebih dahulu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotes() {
        String json = prefs.getString("notes", "[]");
        try { notesArray = new JSONArray(json); } catch (JSONException e) { notesArray = new JSONArray(); }
        renderNotes();
    }

    private void addNote(String text) {
        String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String entry = timestamp + " | " + text;
        notesArray.put(entry);
        saveNotes();
        renderNotes();
        Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show();
    }

    private void saveNotes() {
        prefs.edit().putString("notes", notesArray.toString()).apply();
    }

    private void renderNotes() {
        llNotes.removeAllViews();
        for (int i = notesArray.length() - 1; i >= 0; i--) {
            try {
                String note = notesArray.getString(i);
                MaterialCardView card = new MaterialCardView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 16);
                card.setLayoutParams(params);
                card.setCardElevation(4);
                card.setRadius(24);
                card.setContentPadding(32, 24, 32, 24);

                TextView tv = new TextView(this);
                tv.setText(note);
                tv.setTextSize(15);
                card.addView(tv);

                final int index = i;
                card.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Hapus Catatan?")
                            .setMessage(note)
                            .setPositiveButton("Hapus", (d, w) -> {
                                notesArray.remove(index);
                                saveNotes();
                                renderNotes();
                                Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Batal", null)
                            .show();
                    return true;
                });

                llNotes.addView(card);
            } catch (JSONException ignored) {}
        }

        if (notesArray.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada catatan. Mulai menulis! ✍️");
            empty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            empty.setPadding(0, 64, 0, 0);
            llNotes.addView(empty);
        }
    }
}
