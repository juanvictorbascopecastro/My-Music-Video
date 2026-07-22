package com.youtube.musica.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

import com.youtube.musica.interfaces.PlayerListener;

public class PlayerEventBroadcaster {
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PLAYBACK_MODE = "ACTION_PLAYBACK_MODE";
    PlayerListener listener;

    public PlayerEventBroadcaster(Context context, PlayerListener listener){
        this.listener = listener;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_PLAY_PAUSE);
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_NEXT);
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_PREVIOUS);
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_PLAYBACK_MODE);
        androidx.core.content.ContextCompat.registerReceiver(
                context,
                playbackReceiver,
                intentFilter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        );

    }
    public BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case ACTION_PLAY_PAUSE:
                        listener.onPlayPauseNotification();
                        break;
                    case ACTION_NEXT:
                        listener.onNextNotification();
                        break;
                    case ACTION_PREVIOUS:
                        listener.onPreviewNotification();
                        break;
                    case ACTION_PLAYBACK_MODE:
                        listener.onPlaybackModeNotification();
                        break;
                }
            }
        }
    };
}
