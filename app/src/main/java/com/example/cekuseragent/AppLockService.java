package com.example.cekuseragent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;

public class AppLockService extends Service {
    private static final String CHANNEL_ID = "AppLockServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler handler;
    private Runnable runnable;
    private AppLockManager lockManager;
    private String lastForegroundPackage = "";

    @Override
    public void onCreate() {
        super.onCreate();
        lockManager = new AppLockManager(this);
        handler = new Handler(Looper.getMainLooper());

        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        startMonitoring();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Layanan Kunci Aplikasi Pro",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Menjaga keamanan aplikasi yang terkunci");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, AppLockActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kunci Aplikasi Aktif 🛡️")
                .setContentText("Melindungi aplikasi terpilih secara aman")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void startMonitoring() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkForegroundApp();
                handler.postDelayed(this, 300); // Check every 300ms
            }
        };
        handler.post(runnable);
    }

    private void checkForegroundApp() {
        if (!lockManager.isLockEnabled()) {
            stopSelf();
            return;
        }

        String currentPackage = getForegroundPackageName();
        if (currentPackage == null || currentPackage.isEmpty() ||
                currentPackage.equals(getPackageName()) ||
                currentPackage.equals("com.android.systemui") ||
                currentPackage.contains("launcher")) {
            return;
        }

        if (!currentPackage.equals(lastForegroundPackage)) {
            if (!currentPackage.equals(AppLockManager.getLastUnlockedPackage())) {
                AppLockManager.clearTemporaryUnlockIfNot(currentPackage);
            }
            lastForegroundPackage = currentPackage;
        }

        if (lockManager.isPackageLocked(currentPackage)) {
            if (!AppLockManager.isTemporarilyUnlocked(currentPackage)) {
                Intent lockIntent = new Intent(this, AppLockScreenActivity.class);
                lockIntent.putExtra("locked_package", currentPackage);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(lockIntent);
            }
        }
    }

    private String getForegroundPackageName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return null;
            long time = System.currentTimeMillis();
            UsageEvents usageEvents = usm.queryEvents(time - 2000, time);
            UsageEvents.Event event = new UsageEvents.Event();
            String lastPackage = null;
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastPackage = event.getPackageName();
                }
            }
            return lastPackage;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
