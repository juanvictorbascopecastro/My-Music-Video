package app.list.mymusic.ui.ctg;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import app.list.mymusic.models.CtgMusic;

public class CtgViewModel extends ViewModel {

    private MutableLiveData<ArrayList<CtgMusic>> list;

    public CtgViewModel() {
        list = new MutableLiveData<>();
    }

    public LiveData<ArrayList<CtgMusic>> getList() {
        return list;
    }

    public void setList(ArrayList<CtgMusic> ctgMusics){
        this.list.setValue(ctgMusics);
    }

    // Método para eliminar un elemento de la lista por su campo 'code'
    public void deleteItemByCode(String code) {
        ArrayList<CtgMusic> currentList = list.getValue();
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
    // Verificar si un nombre existe en la lista
    public boolean isNameExists(String name) {
        ArrayList<CtgMusic> currentList = list.getValue();
        if (currentList != null) {
            for (CtgMusic ctg : currentList) {
                if (ctg.getName().equals(name.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Verificar si un nombre existe en la lista pero excluyendo un ID específico
    public boolean isNameExistsExcludingCode(String name, String id) {
        ArrayList<CtgMusic> currentList = list.getValue();
        if (currentList != null) {
            for (CtgMusic ctg : currentList) {
                if (ctg.getName().equals(name) && !id.equals(ctg.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }
}