package app.list.mymusic.interfaces;

import java.util.ArrayList;

import app.list.mymusic.models.YTVideo;

public interface DbMusicListener {
    default void loadedMusicPlaylist(ArrayList<YTVideo> playList) {}
    void errorLoadedMusicPlayList (String message, int icon);
}
