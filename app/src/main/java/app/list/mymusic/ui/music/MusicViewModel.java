package app.list.mymusic.ui.music;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;

public class MusicViewModel extends ViewModel {

    private MutableLiveData<ArrayList<YTVideo>> list;

    public MusicViewModel() {
        list = new MutableLiveData<>();
    }

    public LiveData<ArrayList<YTVideo>> getList() {
        return list;
    }

    public void setList(ArrayList<YTVideo> ytVideos){
        this.list.setValue(ytVideos);
    }
    public void deleteItemByCode(String code) {
        ArrayList<YTVideo> currentList = list.getValue();
        if (currentList != null) {
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).getCode().equals(code)) {
                    currentList.remove(i);
                    list.setValue(currentList);
                    break; // Salir del bucle después de encontrar y eliminar el elemento
                }
            }
        }
    }
}