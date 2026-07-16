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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (currentNotification != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                startForeground(1, currentNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(1, currentNotification);
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
    }
}
