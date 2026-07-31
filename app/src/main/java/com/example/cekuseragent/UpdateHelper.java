package com.example.cekuseragent;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.content.FileProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateHelper {

    private static final String GITHUB_REPO = "muslikh/ua";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private final Activity activity;

    public UpdateHelper(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdate() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonObject = new JSONObject(response.toString());
                    String tagName = jsonObject.getString("tag_name");
                    String latestVersion = tagName.replace("v", "");
                    String currentVersion = BuildConfig.VERSION_NAME.replace("v", "");
                    
                    JSONArray assets = jsonObject.optJSONArray("assets");
                    String downloadUrl = null;
                    if (assets != null && assets.length() > 0) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject asset = assets.getJSONObject(i);
                            if (asset.getString("name").endsWith(".apk")) {
                                downloadUrl = asset.getString("browser_download_url");
                                break;
                            }
                        }
                    }
                    
                    // Fallback jika tidak ada aset .apk tapi ada url zip/tarball, tapi kita hanya mau apk.
                    if (downloadUrl == null && jsonObject.has("html_url")) {
                         // Biarkan null jika tidak ada apk
                    }

                    if (isNewerVersion(currentVersion, latestVersion) && downloadUrl != null) {
                        final String finalDownloadUrl = downloadUrl;
                        handler.post(() -> showUpdateDialog(latestVersion, finalDownloadUrl));
                    }
                }
            } catch (Exception e) {
                Log.e("UpdateHelper", "Error checking update", e);
            }
        });
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");
            int length = Math.max(currentParts.length, latestParts.length);
            for (int i = 0; i < length; i++) {
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latest = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                if (latest > current) return true;
                if (latest < current) return false;
            }
        } catch (Exception e) {
            return latestVersion.compareTo(currentVersion) > 0;
        }
        return false;
    }

    private void showUpdateDialog(String newVersion, String downloadUrl) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Update Tersedia")
                .setMessage("Versi baru (" + newVersion + ") telah rilis. Apakah Anda ingin mengunduh dan memperbarui aplikasi sekarang?")
                .setPositiveButton("Update", (dialog, which) -> downloadAndInstall(downloadUrl))
                .setNegativeButton("Nanti", null)
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstall(String url) {
        String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/CekUserAgent-Update.apk";
        File file = new File(destination);
        if (file.exists()) {
            file.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Mengunduh Update Cek User Agent");
        request.setDescription("Sedang mengunduh file APK...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.fromFile(file));

        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) return;
        
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    installApk(file);
                    activity.unregisterReceiver(this);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(getUriFromFile(file), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(intent);
    }

    private Uri getUriFromFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }
}
