package com.youtube.musica.interfaces;

import com.youtube.musica.models.CategoryCollection;

public interface MusicListener {
    void onNewRegister(String data);
    void onSecordPlayer(float second);
    void onDeletePosition(int position);
    void onSeletedCtg(CategoryCollection categoryCollection);
    void onSetPosition(int position);
    void onStateChanged(String state);
    void onVideoClicked(int position);
}
