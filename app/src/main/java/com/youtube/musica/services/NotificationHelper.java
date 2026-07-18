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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.youtube.musica.R;
import com.youtube.musica.models.MusicCollection;
import java.util.ArrayList;

/**
 * Clase encargada de gestionar y mostrar las notificaciones del reproductor de música.
 * También configura la MediaSession, la cual permite controlar la reproducción desde
 * la pantalla de bloqueo, relojes inteligentes o auriculares Bluetooth.
 */
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
        // MediaSessionCompat sirve para integrarse con los controles del sistema (pantalla de bloqueo, auriculares, etc.)
        mediaSession = new MediaSessionCompat(context, "PlayerMediaSession");
        
        // Configuramos qué pasa cuando el sistema (fuera de la app) solicita cambiar la reproducción
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                context.sendBroadcast(new Intent(PlayerEventBroadcaster.ACTION_PLAY_PAUSE).setPackage(context.getPackageName()));
            }

            @Override
            public void onPause() {
                context.sendBroadcast(new Intent(PlayerEventBroadcaster.ACTION_PLAY_PAUSE).setPackage(context.getPackageName()));
            }

            @Override
            public void onSkipToNext() {
                context.sendBroadcast(new Intent(PlayerEventBroadcaster.ACTION_NEXT).setPackage(context.getPackageName()));
            }

            @Override
            public void onSkipToPrevious() {
                context.sendBroadcast(new Intent(PlayerEventBroadcaster.ACTION_PREVIOUS).setPackage(context.getPackageName()));
            }
        });
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

    /**
     * Prepara y muestra una notificación persistente para el control de reproducción en segundo plano.
     * Esta función centraliza la creación de los PendingIntents necesarios para las acciones
     * del reproductor (Play/Pausa, Siguiente, Anterior) y calcula si dichos botones deben
     * estar habilitados basándose en el estado de la lista de reproducción.
     * 
     * @param targetActivityClass La clase de la actividad a la que se dirigirá al pulsar la notificación (ej. PlayerFullscreenActivity.class).
     * @param list Lista actual de videos/canciones en reproducción.
     * @param indexPlayer Índice actual del video que se está reproduciendo en la lista.
     * @param isPlaying Estado actual de reproducción: true si está reproduciendo, false si está pausado.
     */
    @SuppressLint("MissingPermission")
    public void showNotification(Class<?> targetActivityClass, ArrayList<MusicCollection> list, int indexPlayer, boolean isPlaying) {
        if (list == null || list.isEmpty() || indexPlayer < 0 || indexPlayer >= list.size()) {
            return;
        }

        // Creamos los PendingIntent que se ejecutarán cuando el usuario pulse los botones de la notificación
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, targetActivityClass),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent previousIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_PREVIOUS).setPackage(context.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent playPauseIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_PLAY_PAUSE).setPackage(context.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent nextIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_NEXT).setPackage(context.getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Lógica para determinar si habilitar los botones anterior/siguiente según la posición en la lista
        boolean isNextActive, isPreviewActive;
        if(list.size() == 1){
            isNextActive = false;
            isPreviewActive = false;
        }else if(indexPlayer == list.size()-1){
            isNextActive = false;
            isPreviewActive = true;
        }else if(indexPlayer == 0){
            isNextActive = true;
            isPreviewActive = false;
        }else{
            isNextActive = true;
            isPreviewActive = true;
        }

        String title = list.get(indexPlayer).getName();
        String videoId = list.get(indexPlayer).getIdvideo();
        String imageUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

        // Carga asíncrona de la miniatura de YouTube usando Glide
        Glide.with(context.getApplicationContext())
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

    /**
     * Construye y lanza la notificación final después de haber obtenido la imagen (o haber fallado).
     * También actualiza la MediaSession para sincronizar los estados en el sistema Android.
     */
    @SuppressLint("MissingPermission")
    private void buildAndShowNotification(String title, Bitmap largeIcon, PendingIntent contentIntent,
                                          PendingIntent previousIntent, PendingIntent playPauseIntent,
                                          PendingIntent nextIntent, boolean isPlaying, boolean isNextActive,
                                          boolean isPreviewActive) {

        // Configuración visual básica de la notificación
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
        PendingIntent finalPrevIntent = isPreviewActive ? previousIntent : null;
        builder.addAction(new NotificationCompat.Action(prevIcon, "Previous", finalPrevIntent));

        // Action Play/Pause
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseTitle = isPlaying ? "Pause" : "Play";
        builder.addAction(new NotificationCompat.Action(playPauseIcon, playPauseTitle, playPauseIntent));

        // Action Next
        int nextIcon = isNextActive ? R.drawable.ic_next : R.drawable.ic_next_disabled;
        PendingIntent finalNextIntent = isNextActive ? nextIntent : null;
        builder.addAction(new NotificationCompat.Action(nextIcon, "Next", finalNextIntent));

        // Aplicar MediaSession y MediaMetadata para la pantalla de bloqueo
        if (mediaSession != null) {
            // Actualizar Metadatos (título y portada para pantalla de bloqueo)
            MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "My Music Video");
            if (largeIcon != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon);
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, largeIcon);
            }
            mediaSession.setMetadata(metadataBuilder.build());

            // Actualizar estado de reproducción y acciones disponibles
            long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE;
            
            if (isNextActive) actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
            if (isPreviewActive) actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

            int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

            PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build();

            mediaSession.setPlaybackState(playbackState);
            mediaSession.setActive(true);
        }

        // Apply MediaStyle para que la notificación se vea como un reproductor nativo
        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(0, 1, 2) // Muestra los primeros 3 botones en la vista compacta
                .setMediaSession(mediaSession.getSessionToken()));

        Notification notification = builder.build();

        // Lanzamos un servicio en primer plano (Foreground Service) asociado a esta notificación.
        // Esto previene que el sistema Android mate nuestra app cuando está en segundo plano.
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
