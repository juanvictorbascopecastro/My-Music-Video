package com.youtube.musica.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.youtube.musica.R;

public class NotificationHelper {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "channel_id";

    private Context context;
    private NotificationManagerCompat notificationManager;
    private MediaSessionCompat mediaSession;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannel();
        mediaSession = new MediaSessionCompat(context, "PlayerMediaSession");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Background Player", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void showNotification(String title, String videoId, PendingIntent contentIntent,
                                 PendingIntent previousIntent, PendingIntent playPauseIntent,
                                 PendingIntent nextIntent, boolean isPlaying, boolean isNextActive,
                                 boolean isPreviewActive) {

        String imageUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        buildAndShowNotification(title, resource, contentIntent, previousIntent, playPauseIntent, nextIntent, isPlaying, isNextActive, isPreviewActive);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Si falla la descarga, mostramos la notificación sin la imagen grande
                        buildAndShowNotification(title, null, contentIntent, previousIntent, playPauseIntent, nextIntent, isPlaying, isNextActive, isPreviewActive);
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void buildAndShowNotification(String title, Bitmap largeIcon, PendingIntent contentIntent,
                                          PendingIntent previousIntent, PendingIntent playPauseIntent,
                                          PendingIntent nextIntent, boolean isPlaying, boolean isNextActive,
                                          boolean isPreviewActive) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_youtube)
                .setContentTitle(truncateTitle(title))
                .setContentText("Reproduciendo")
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setOngoing(true);

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon);
        }

        // Action Previous
        int prevIcon = isPreviewActive ? R.drawable.ic_previous : R.drawable.ic_previous_disabled;
        builder.addAction(new NotificationCompat.Action(prevIcon, "Previous", previousIntent));

        // Action Play/Pause
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseTitle = isPlaying ? "Pause" : "Play";
        builder.addAction(new NotificationCompat.Action(playPauseIcon, playPauseTitle, playPauseIntent));

        // Action Next
        int nextIcon = isNextActive ? R.drawable.ic_next : R.drawable.ic_next_disabled;
        builder.addAction(new NotificationCompat.Action(nextIcon, "Next", nextIntent));

        // Apply MediaStyle
        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(mediaSession.getSessionToken()));

        Notification notification = builder.build();

        YouTubeBackgroundService.currentNotification = notification;
        Intent serviceIntent = new Intent(context, YouTubeBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
        context.stopService(new Intent(context, YouTubeBackgroundService.class));
        if (mediaSession != null) {
            mediaSession.release();
        }
    }

    private static final int MAX_TITLE_LENGTH = 40;

    private String truncateTitle(String title) {
        if (title.length() > MAX_TITLE_LENGTH) {
            return title.substring(0, MAX_TITLE_LENGTH) + "...";
        }
        return title;
    }
}
