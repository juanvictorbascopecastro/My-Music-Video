package com.youtube.musica.interfaces;

import java.util.ArrayList;

import com.youtube.musica.models.MusicCollection;

public interface DbMusicListener {
    default void loadedMusicPlaylist(ArrayList<MusicCollection> playList) {}
    void errorLoadedMusicPlayList (String message, int icon);
}
