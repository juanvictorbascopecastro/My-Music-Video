package app.list.mymusic.interfaces;

import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;

public interface MusicListener {
    void onNewRegister(String data);
    void onSecordPlayer(float second);
    void onDeletePosition(YTVideo ytVideo);
    void onSeletedCtg(CtgMusic ctgMusic);
    void onSetPosition(int position);
}
