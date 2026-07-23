package com.youtube.musica.interfaces;

public interface PlayerListener {
    void onPlayPauseNotification();
    void onNextNotification();
    void onPreviewNotification();
    void onPlaybackModeNotification();
    void onLostActiveFocus();
}
