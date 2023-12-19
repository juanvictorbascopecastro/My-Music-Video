package app.list.mymusic.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

import app.list.mymusic.interfaces.PlayerListener;

public class PlayerEventBroadcaster {
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    PlayerListener listener;

    public PlayerEventBroadcaster(Context context, PlayerListener listener){
        this.listener = listener;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_PLAY_PAUSE);
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_NEXT);
        intentFilter.addAction(PlayerEventBroadcaster.ACTION_PREVIOUS);
        context.registerReceiver(playbackReceiver, intentFilter);

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
                }
            }
        }
    };
}
