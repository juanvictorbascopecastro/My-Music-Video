package app.list.mymusic.services;

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String videoId = intent.getStringExtra("videoId");
        if (videoId != null) {
            // Inicializar el reproductor de YouTube y cargar el video
           // YouTubePlayerUtils.loadOrCueVideo(youTubePlayer, getLifecycle(), videoId, 0);
        }
        // return START_NOT_STICKY;
        return super.onStartCommand(intent, flags, startId);
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
