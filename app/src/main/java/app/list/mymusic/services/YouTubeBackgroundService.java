package app.list.mymusic.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import app.list.mymusic.MainActivity;
import app.list.mymusic.R;

public class YouTubeBackgroundService extends Service {
    private final IBinder binder = new LocalBinder();
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        YouTubeBackgroundService getService() {
            return YouTubeBackgroundService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Configurar el reproductor de YouTube
        youTubePlayerView = new YouTubePlayerView(this);
        //youTubePlayerView.getPlayerUIController().showFullscreenButton(false);
        //youTubePlayerView.getPlayerUIController().showYouTubeButton(false);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer player) {
                youTubePlayer = player;
                // Reemplaza "your_youtube_video_id" con el ID real del video de YouTube
                youTubePlayer.loadVideo("FYEyxNZCQas", 0);
                youTubePlayer.play();
            }
        });

        // Crear notificación y mostrarla en primer plano
        Notification notification = buildNotification();
        startForeground(1, notification);

        return START_STICKY;
    }

    private Notification buildNotification() {
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.drawable.play)
                .setContent(contentView)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (youTubePlayer != null) {
            // youTubePlayer.release();
        }
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }
}
