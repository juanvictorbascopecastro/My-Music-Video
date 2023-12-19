package app.list.mymusic.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import app.list.mymusic.R;

public class NotificationHelper {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "channel_id";

    private Context context;
    private NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Channel Name", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @SuppressLint("MissingPermission")
    public void showNotification(String title, PendingIntent contentIntent,
                                 PendingIntent previousIntent, PendingIntent playPauseIntent,
                                 PendingIntent nextIntent, boolean isPlaying, boolean isNextActive,
                                 boolean isPreviewActive) {
        // Crear RemoteViews para el diseño personalizado
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification);

        remoteViews.setTextViewText(R.id.tvSongTitle, truncateTitle(title));
        remoteViews.setOnClickPendingIntent(R.id.btnPrevious, previousIntent);
        remoteViews.setOnClickPendingIntent(R.id.btnPlayPause, playPauseIntent);
        remoteViews.setOnClickPendingIntent(R.id.btnNext, nextIntent);

        // Configurar la imagen del botón de reproducción/pausa
        remoteViews.setImageViewResource(R.id.btnPlayPause, isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);

        // Configurar la visibilidad y estado de los botones de reproducción/avance/retroceso
        remoteViews.setImageViewResource(R.id.btnNext, isNextActive ? R.drawable.ic_next : R.drawable.ic_next_disabled);
        remoteViews.setImageViewResource(R.id.btnPrevious, isPreviewActive ? R.drawable.ic_previous : R.drawable.ic_previous_disabled);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.music)
                .setCustomContentView(remoteViews)  // Establecer el diseño personalizado
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
    private static final int MAX_TITLE_LENGTH = 30; // Elige el número máximo de caracteres a mostrar

    private String truncateTitle(String title) {
        if (title.length() > MAX_TITLE_LENGTH) {
            return title.substring(0, MAX_TITLE_LENGTH) + "..."; // Añadir "..." si se trunca
        }
        return title;
    }
}
