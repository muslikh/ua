package com.example.cekuseragent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Set;

public class AppBlockerVpnService extends VpnService {
    public static final String ACTION_START = "com.example.cekuseragent.START_VPN";
    public static final String ACTION_STOP = "com.example.cekuseragent.STOP_VPN";

    private static final String CHANNEL_ID = "AppBlockerVpnChannel";
    private static final int NOTIFICATION_ID = 2002;

    private ParcelFileDescriptor vpnInterface = null;
    private AppBlockerManager blockerManager;

    @Override
    public void onCreate() {
        super.onCreate();
        blockerManager = new AppBlockerManager(this);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Firewall Pemblokir Internet",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Memblokir koneksi internet untuk aplikasi yang dipilih");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification(int blockedCount) {
        Intent notificationIntent = new Intent(this, AppBlockerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Firewall Internet Aktif")
                .setContentText(blockedCount + " aplikasi diblokir akses internetnya")
                .setSmallIcon(android.R.drawable.ic_lock_power_off)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        startVpn();
        return START_STICKY;
    }

    private synchronized void startVpn() {
        closeVpnInterface();

        Set<String> blockedPackages = blockerManager.getBlockedPackages();
        if (blockedPackages.isEmpty()) {
            // No apps blocked, but keep firewall ready or stop
            startForeground(NOTIFICATION_ID, createNotification(0));
            return;
        }

        try {
            Builder builder = new Builder();
            builder.setSession("MyTools Firewall");
            // Dummy network address (blackhole)
            builder.addAddress("10.1.10.1", 32);
            builder.addRoute("0.0.0.0", 0);

            int count = 0;
            for (String pkg : blockedPackages) {
                try {
                    builder.addAllowedApplication(pkg);
                    count++;
                } catch (PackageManager.NameNotFoundException ignored) {}
            }

            if (count > 0) {
                vpnInterface = builder.establish();
            }

            startForeground(NOTIFICATION_ID, createNotification(count));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeVpnInterface() {
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            vpnInterface = null;
        }
    }

    private synchronized void stopVpn() {
        closeVpnInterface();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        closeVpnInterface();
        super.onDestroy();
    }
}
