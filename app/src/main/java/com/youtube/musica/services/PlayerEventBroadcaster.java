package com.youtube.musica.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.youtube.musica.interfaces.PlayerListener;

public class PlayerEventBroadcaster {
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PLAYBACK_MODE = "ACTION_PLAYBACK_MODE";
    
    private static PlayerEventBroadcaster instance;
    private PlayerListener activeListener;
    private boolean isRegistered = false;

    private PlayerEventBroadcaster() {
        // Private constructor para Singleton
    }

    public static PlayerEventBroadcaster getInstance() {
        if (instance == null) {
            instance = new PlayerEventBroadcaster();
        }
        return instance;
    }

    // Registra o actualiza el listener activo. Quien llame a esto se convierte en el reproductor principal.
    public void register(Context context, PlayerListener listener) {
        if (this.activeListener != null && this.activeListener != listener) {
            // Notificar al antiguo reproductor que ha perdido el foco (debe pausarse)
            this.activeListener.onLostActiveFocus();
        }
        this.activeListener = listener;
        if (!isRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_PLAY_PAUSE);
            intentFilter.addAction(ACTION_NEXT);
            intentFilter.addAction(ACTION_PREVIOUS);
            intentFilter.addAction(ACTION_PLAYBACK_MODE);
            androidx.core.content.ContextCompat.registerReceiver(
                    context.getApplicationContext(), // Usar application context para evitar memory leaks
                    playbackReceiver,
                    intentFilter,
                    androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            );
            isRegistered = true;
        }
    }

    // Da de baja al listener actual si es que aún es el activo
    public void unregisterListener(PlayerListener listener) {
        if (this.activeListener == listener) {
            this.activeListener = null;
        }
    }

    public BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (activeListener == null) return;
            
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_PLAY_PAUSE:
                        activeListener.onPlayPauseNotification();
                        break;
                    case ACTION_NEXT:
                        activeListener.onNextNotification();
                        break;
                    case ACTION_PREVIOUS:
                        activeListener.onPreviewNotification();
                        break;
                    case ACTION_PLAYBACK_MODE:
                        activeListener.onPlaybackModeNotification();
                        break;
                }
            }
        }
    };
}
