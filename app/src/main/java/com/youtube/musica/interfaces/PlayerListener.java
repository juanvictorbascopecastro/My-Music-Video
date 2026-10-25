package com.youtube.musica.interfaces;

public interface PlayerListener {
    void onPlayNotification();
    void onPauseNotification();
    void onNextNotification();
    void onPreviewNotification();
    void onPlaybackModeNotification();
    void onLostActiveFocus();
}
