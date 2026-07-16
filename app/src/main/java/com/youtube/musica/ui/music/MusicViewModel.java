package com.youtube.musica.ui.music;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import com.youtube.musica.models.MusicCollection;

public class MusicViewModel extends ViewModel {

    private MutableLiveData<ArrayList<MusicCollection>> list;

    public MusicViewModel() {
        list = new MutableLiveData<>();
    }

    public LiveData<ArrayList<MusicCollection>> getList() {
        return list;
    }

    public void setList(ArrayList<MusicCollection> musicCollections){
        this.list.setValue(musicCollections);
    }
    public void deleteItemByCode(String code) {
        ArrayList<MusicCollection> currentList = list.getValue();
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