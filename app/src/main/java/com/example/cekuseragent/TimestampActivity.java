package com.example.cekuseragent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimestampActivity extends AppCompatActivity {
    private static final String GAS_URL = "https://script.google.com/macros/s/AKfycbyvUXTPcHdkn91I4xdgeZ4j-ouqbZv0ICD2sfokoQ5USAIc5A3slPVlkNEocR03gZp9SA/exec";
    
    private ImageView ivPreview;
    private TextInputEditText etDate, etTime, etLat, etLng, etAddress;
    private Button btnApplyStamp, btnSaveImage;
    private Bitmap originalBitmap;
    private Bitmap stampedBitmap;
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        loadImageAndExif(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timestamp);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivPreview = findViewById(R.id.ivPreview);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLat = findViewById(R.id.etLat);
        etLng = findViewById(R.id.etLng);
        etAddress = findViewById(R.id.etAddress);
        btnApplyStamp = findViewById(R.id.btnApplyStamp);
        btnSaveImage = findViewById(R.id.btnSaveImage);
        
        Button btnSelectImage = findViewById(R.id.btnSelectImage);
        Button btnSearchLocation = findViewById(R.id.btnSearchLocation);

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        setupDateAndTimePickers();

        btnSearchLocation.setOnClickListener(v -> showSearchDialog());

        btnApplyStamp.setOnClickListener(v -> {
            if (originalBitmap == null) {
                Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show();
                return;
            }
            btnApplyStamp.setText("⏳ Memproses...");
            btnApplyStamp.setEnabled(false);
            applyStampToImage();
        });

        ivPreview.setOnClickListener(v -> {
            if (stampedBitmap != null) {
                showFullscreenPreview(stampedBitmap);
            } else if (originalBitmap != null) {
                showFullscreenPreview(originalBitmap);
            }
        });

        btnSaveImage.setOnClickListener(v -> {
            if (stampedBitmap != null) {
                saveImageToGallery(stampedBitmap);
            }
        });
    }

    private void showFullscreenPreview(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullImageView = new ImageView(this);
        fullImageView.setImageBitmap(bitmap);
        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageView.setBackgroundColor(Color.BLACK);
        
        AlertDialog dialog = builder.setView(fullImageView).create();
        fullImageView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        Toast.makeText(this, "Ketuk gambar untuk menutup preview", Toast.LENGTH_SHORT).show();
    }

    private void setupDateAndTimePickers() {
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String d = year + "-" + String.format(Locale.getDefault(), "%02d", month + 1) + "-" + String.format(Locale.getDefault(), "%02d", dayOfMonth);
                etDate.setText(d);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                String t = String.format(Locale.getDefault(), "%02d", hourOfDay) + ":" + String.format(Locale.getDefault(), "%02d", minute) + ":00";
                etTime.setText(t);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
    }

    private void loadImageAndExif(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            originalBitmap = BitmapFactory.decodeStream(is);
            is.close();

            ivPreview.setImageBitmap(originalBitmap);
            ivPreview.setVisibility(View.VISIBLE);
            View tvHint = findViewById(R.id.tvPreviewHint);
            if (tvHint != null) tvHint.setVisibility(View.VISIBLE);
            
            // Set current date time as fallback
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            etDate.setText(sdfDate.format(new Date()));
            etTime.setText(sdfTime.format(new Date()));

            // Try read EXIF
            InputStream isExif = getContentResolver().openInputStream(uri);
            if (isExif != null) {
                ExifInterface exif = new ExifInterface(isExif);
                double[] latLong = exif.getLatLong();
                if (latLong != null) {
                    etLat.setText(String.valueOf(latLong[0]));
                    etLng.setText(String.valueOf(latLong[1]));
                    fetchAddress(latLong[0], latLong[1]);
                }
                
                String datetime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
                if (datetime != null && datetime.length() >= 19) {
                    // format: YYYY:MM:DD HH:MM:SS
                    String d = datetime.substring(0, 10).replace(":", "-");
                    String t = datetime.substring(11);
                    etDate.setText(d);
                    etTime.setText(t);
                }
                isExif.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAddress(double lat, double lng) {
        etAddress.setHint("Mencari alamat...");
        executorService.execute(() -> {
            try {
                URL url = new URL(GAS_URL + "?action=geocode&lat=" + lat + "&lng=" + lng);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.getString("status").equals("success")) {
                    String addr = json.getString("address");
                    mainHandler.post(() -> {
                        etAddress.setText(addr);
                        etAddress.setHint("Alamat Lengkap");
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> etAddress.setHint("Alamat Lengkap"));
            }
        });
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cari Lokasi");
        
        final EditText input = new EditText(this);
        input.setHint("Masukkan kata kunci...");
        builder.setView(input);
        
        builder.setPositiveButton("Cari", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void performSearch(String query) {
        Toast.makeText(this, "Mencari...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            try {
                URL url = new URL(GAS_URL + "?action=search&q=" + URLEncoder.encode(query, "UTF-8"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.getString("status").equals("success")) {
                    JSONArray resultsArray = json.getJSONArray("results");
                    List<String> displayTitles = new ArrayList<>();
                    List<String> fullAddresses = new ArrayList<>();
                    List<double[]> coords = new ArrayList<>();
                    
                    for (int i = 0; i < resultsArray.length(); i++) {
                        JSONObject item = resultsArray.getJSONObject(i);
                        String title = item.optString("title", "");
                        String subtitle = item.optString("subtitle", "");
                        String fullAddr = item.optString("fullAddress", item.optString("formatted_address", ""));
                        
                        double lat = item.optDouble("lat", 0.0);
                        double lng = item.optDouble("lng", 0.0);
                        if (lat == 0.0 && item.has("geometry")) {
                            JSONObject geom = item.getJSONObject("geometry").getJSONObject("location");
                            lat = geom.optDouble("lat", 0.0);
                            lng = geom.optDouble("lng", 0.0);
                        } else if (lat == 0.0 && item.has("center")) {
                            JSONObject center = item.getJSONObject("center");
                            lat = center.optDouble("lat", 0.0);
                            lng = center.optDouble("lng", 0.0);
                        }
                        
                        String displayText = title;
                        if (!subtitle.isEmpty() && !subtitle.equals(title)) {
                            displayText += "\n" + subtitle;
                        } else if (displayText.isEmpty()) {
                            displayText = fullAddr;
                        }
                        
                        displayTitles.add(displayText);
                        fullAddresses.add(fullAddr.isEmpty() ? title : fullAddr);
                        coords.add(new double[]{lat, lng});
                    }

                    mainHandler.post(() -> showSearchResultsDialog(displayTitles, fullAddresses, coords));
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(this, "Gagal mencari: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showSearchResultsDialog(List<String> displayTitles, List<String> fullAddresses, List<double[]> coords) {
        if (displayTitles.isEmpty()) {
            Toast.makeText(this, "Tidak ada hasil lokasi ditemukan", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Hasil Lokasi (" + displayTitles.size() + ")");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_2, android.R.id.text1, displayTitles);
        builder.setAdapter(adapter, (dialog, which) -> {
            double[] selected = coords.get(which);
            etLat.setText(String.valueOf(selected[0]));
            etLng.setText(String.valueOf(selected[1]));
            etAddress.setText(fullAddresses.get(which));
            Toast.makeText(this, "Lokasi terpilih: " + fullAddresses.get(which), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Tutup", null);
        builder.show();
    }

    private void applyStampToImage() {
        executorService.execute(() -> {
            try {
                Bitmap result = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(result);

                float lat = 0; float lng = 0;
                try {
                    lat = Float.parseFloat(etLat.getText().toString());
                    lng = Float.parseFloat(etLng.getText().toString());
                } catch (Exception ignored) {}

                float scale = Math.max(result.getWidth(), result.getHeight()) / 1400f;
                float margin = 40 * scale;
                float mapWidth = 280 * scale;
                float mapHeight = 320 * scale;

                // Fetch Static Map
                if (lat != 0 || lng != 0) {
                    URL url = new URL(GAS_URL + "?action=staticmap&lat=" + lat + "&lng=" + lng + "&w=280&h=320");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    
                    JSONObject json = new JSONObject(sb.toString());
                    if (json.getString("status").equals("success")) {
                        String base64Img = json.getString("image").split(",")[1];
                        byte[] decodedString = Base64.decode(base64Img, Base64.DEFAULT);
                        Bitmap mapBmp = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        if (mapBmp != null) {
                            Bitmap scaledMap = Bitmap.createScaledBitmap(mapBmp, (int)mapWidth, (int)mapHeight, true);
                            float mapX = margin;
                            float mapY = result.getHeight() - mapHeight - margin;
                            canvas.drawBitmap(scaledMap, mapX, mapY, null);
                        }
                    }
                }

                // Draw Text
                String dateStr = etDate.getText().toString();
                String timeStr = etTime.getText().toString();
                String addrStr = etAddress.getText().toString();
                
                String dateFormatted = "";
                if (!dateStr.isEmpty()) {
                    SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    try { dateFormatted = out.format(in.parse(dateStr)); } catch (Exception ignored) {}
                }
                
                String timeFormatted = timeStr.replace(":", ".");
                
                List<String> lines = new ArrayList<>();
                lines.add("Network: " + dateFormatted + " " + timeFormatted + " WIB");
                lines.add("Local: " + dateFormatted + " " + timeFormatted + " WIB");
                lines.add(toDMS(lat, true) + " " + toDMS(lng, false));
                
                if (!addrStr.isEmpty()) {
                    String[] parts = addrStr.split(",");
                    for (String p : parts) {
                        if (!p.trim().isEmpty()) lines.add(p.trim());
                    }
                }

                float fontSize = 21 * scale;
                Paint paintStroke = new Paint();
                paintStroke.setColor(Color.argb(200, 0, 0, 0));
                paintStroke.setTextSize(fontSize);
                paintStroke.setStyle(Paint.Style.STROKE);
                paintStroke.setStrokeWidth(3.5f * scale);
                paintStroke.setTextAlign(Paint.Align.RIGHT);
                paintStroke.setAntiAlias(true);

                Paint paintFill = new Paint();
                paintFill.setColor(Color.WHITE);
                paintFill.setTextSize(fontSize);
                paintFill.setStyle(Paint.Style.FILL);
                paintFill.setTextAlign(Paint.Align.RIGHT);
                paintFill.setAntiAlias(true);

                float textX = result.getWidth() - margin;
                float textY = result.getHeight() - margin;
                float lineSpacing = fontSize * 1.15f;

                for (int i = lines.size() - 1; i >= 0; i--) {
                    canvas.drawText(lines.get(i), textX, textY, paintStroke);
                    canvas.drawText(lines.get(i), textX, textY, paintFill);
                    textY -= lineSpacing;
                }

                mainHandler.post(() -> {
                    stampedBitmap = result;
                    ivPreview.setImageBitmap(result);
                    btnSaveImage.setVisibility(View.VISIBLE);
                    btnApplyStamp.setText("Terapkan Stamp ke Gambar");
                    btnApplyStamp.setEnabled(true);
                    Toast.makeText(this, "Stamp berhasil diterapkan!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    btnApplyStamp.setText("Terapkan Stamp ke Gambar");
                    btnApplyStamp.setEnabled(true);
                    Toast.makeText(this, "Terjadi kesalahan saat memproses gambar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String toDMS(float coordinate, boolean isLat) {
        if (coordinate == 0) return "-";
        float absolute = Math.abs(coordinate);
        int degrees = (int) Math.floor(absolute);
        float minutesNotTruncated = (absolute - degrees) * 60;
        int minutes = (int) Math.floor(minutesNotTruncated);
        float seconds = (minutesNotTruncated - minutes) * 60;
        
        String direction;
        if (isLat) direction = coordinate >= 0 ? "N" : "S";
        else direction = coordinate >= 0 ? "E" : "W";
        
        String sign = coordinate < 0 ? "-" : "";
        return sign + degrees + "°" + minutes + "'" + String.format(Locale.US, "%.3f", seconds).replace(".", ",") + "\"" + direction;
    }

    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "GeoStamp_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyTools");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                if (outputStream != null) {
                    outputStream.close();
                }
                Toast.makeText(this, "Gambar disimpan ke Galeri", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
