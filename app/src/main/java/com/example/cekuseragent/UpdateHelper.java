package com.example.cekuseragent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateHelper {

    private static final String TAG = "UpdateHelper";
    private static final String GITHUB_REPO = "muslikh/ua";
    private static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    private final Activity activity;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UpdateHelper(Activity activity) {
        this.activity = activity;
    }

    public void checkForUpdate() {
        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

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

                    if (isNewerVersion(currentVersion, latestVersion) && downloadUrl != null) {
                        final String finalDownloadUrl = downloadUrl;
                        final String releaseNotes = jsonObject.optString("body", "");
                        mainHandler.post(() -> showUpdateDialog(latestVersion, finalDownloadUrl, releaseNotes));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking update", e);
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

    private void showUpdateDialog(String newVersion, String downloadUrl, String releaseNotes) {
        String msg = "Versi baru (" + newVersion + ") telah tersedia.\n\nApakah Anda ingin mengunduh dan memperbarui aplikasi sekarang?";
        if (!releaseNotes.isEmpty()) {
            msg += "\n\nCatatan Rilis:\n" + (releaseNotes.length() > 200 ? releaseNotes.substring(0, 200) + "..." : releaseNotes);
        }

        new MaterialAlertDialogBuilder(activity)
                .setTitle("Pembaruan Tersedia (v" + newVersion + ")")
                .setMessage(msg)
                .setPositiveButton("Update Sekarang", (dialog, which) -> checkPermissionAndDownload(downloadUrl))
                .setNeutralButton("Unduh via Browser", (dialog, which) -> openInBrowser(downloadUrl))
                .setNegativeButton("Nanti", null)
                .show();
    }

    private void openInBrowser(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(activity, "Gagal membuka browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPermissionAndDownload(String downloadUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Izin Instalasi Diperlukan")
                        .setMessage("Untuk memperbarui aplikasi, izinkan instalasi dari sumber tidak dikenal untuk MyTools.")
                        .setPositiveButton("Buka Pengaturan", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES_SETTINGS,
                                    Uri.parse("package:" + activity.getPackageName()));
                            activity.startActivity(intent);
                        })
                        .setNegativeButton("Batal", null)
                        .show();
                return;
            }
        }
        startDirectDownload(downloadUrl);
    }

    private void startDirectDownload(String downloadUrl) {
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Mengunduh Pembaruan");
        progressDialog.setMessage("Sedang mengunduh file APK...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        executor.execute(() -> {
            File apkFile = null;
            try {
                // Gunakan direktori cache eksternal yang aman dan selalu bisa diakses FileProvider
                File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (downloadDir == null) downloadDir = activity.getCacheDir();
                apkFile = new File(downloadDir, "MyTools-Update.apk");

                if (apkFile.exists()) {
                    apkFile.delete();
                }

                // Download dengan redirect otomatis (GitHub -> S3 / CDN)
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                HttpURLConnection.setFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                conn.connect();

                // Tangani manual redirect jika diperlukan (HTTP 301 / 302 / 307 / 308)
                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    String newUrl = conn.getHeaderField("Location");
                    conn.disconnect();
                    url = new URL(newUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                    conn.connect();
                }

                int fileLength = conn.getContentLength();
                InputStream input = new BufferedInputStream(conn.getInputStream());
                OutputStream output = new FileOutputStream(apkFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    if (fileLength > 0) {
                        int progress = (int) (total * 100 / fileLength);
                        mainHandler.post(() -> progressDialog.setProgress(progress));
                    }
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();
                conn.disconnect();

                final File finalApkFile = apkFile;
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    validateAndInstallApk(finalApkFile, downloadUrl);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download error", e);
                if (apkFile != null && apkFile.exists()) {
                    apkFile.delete();
                }
                mainHandler.post(() -> {
                    progressDialog.dismiss();
                    new MaterialAlertDialogBuilder(activity)
                            .setTitle("Unduhan Gagal")
                            .setMessage("Gagal mengunduh file update secara otomatis (" + e.getMessage() + "). Buka halaman unduhan di browser?")
                            .setPositiveButton("Buka Browser", (d, w) -> openInBrowser(downloadUrl))
                            .setNegativeButton("Tutup", null)
                            .show();
                });
            }
        });
    }

    private void validateAndInstallApk(File apkFile, String downloadUrl) {
        if (!apkFile.exists() || apkFile.length() < 100000) { // Minimal 100KB untuk file APK valid
            showCorruptApkDialog(downloadUrl, "Ukuran file APK tidak lengkap (" + apkFile.length() + " bytes).");
            return;
        }

        // Validasi keutuhan APK menggunakan PackageManager bawaan Android
        PackageManager pm = activity.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
        if (info == null) {
            showCorruptApkDialog(downloadUrl, "Struktur paket APK rusak atau tidak didukung oleh perangkat ini.");
            return;
        }

        // APK valid! Luncurkan PackageInstaller
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(apkFile);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Install launch error", e);
            Toast.makeText(activity, "Gagal membuka installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
            openInBrowser(downloadUrl);
        }
    }

    private void showCorruptApkDialog(String downloadUrl, String reason) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Gagal Membuka Paket")
                .setMessage(reason + "\n\nSilakan unduh file APK terbaru langsung melalui browser.")
                .setPositiveButton("Unduh di Browser", (d, w) -> openInBrowser(downloadUrl))
                .setNegativeButton("Batal", null)
                .show();
    }
}
