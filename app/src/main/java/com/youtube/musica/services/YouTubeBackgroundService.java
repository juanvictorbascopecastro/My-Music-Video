package com.youtube.musica.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Binder;

import androidx.annotation.Nullable;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerUtils;

public class YouTubeBackgroundService extends Service {
    private final IBinder binder = new LocalBinder();
    // MediaPlayer mediaPlayer;
    // Uri mUri;
    private YouTubePlayer youTubePlayer;

    public class LocalBinder extends Binder {
        YouTubeBackgroundService getService() {
            // Return this instance of LocalService so clients can call public methods
            return YouTubeBackgroundService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static android.app.Notification currentNotification = null;
    private android.os.PowerManager.WakeLock wakeLock;
    private android.net.wifi.WifiManager.WifiLock wifiLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentNotification != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground(1, currentNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(1, currentNotification);
            }
        }
        
        // Adquirir WakeLock para que no se duerma al apagar la pantalla
        if (wakeLock == null) {
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "MyMusic::YouTubeBackgroundWakeLock");
                wakeLock.acquire();
            }
        }
        
        // Adquirir WifiLock para evitar que Android desconecte el WiFi en modo Doze (ahorro de batería)
        if (wifiLock == null) {
            android.net.wifi.WifiManager wifiManager = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
            if (wifiManager != null) {
                wifiLock = wifiManager.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyMusic::YouTubeWifiLock");
                wifiLock.acquire();
            }
        }
        
        return START_NOT_STICKY;
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (youTubePlayer != null) {
            youTubePlayer.pause();
            youTubePlayer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
        }
    }
}
