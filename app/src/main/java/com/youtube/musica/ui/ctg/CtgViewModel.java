package com.youtube.musica.ui.ctg;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import com.youtube.musica.models.CategoryCollection;

public class CtgViewModel extends ViewModel {

    private MutableLiveData<ArrayList<CategoryCollection>> list;

    public CtgViewModel() {
        list = new MutableLiveData<>();
    }

    public LiveData<ArrayList<CategoryCollection>> getList() {
        return list;
    }

    public void setList(ArrayList<CategoryCollection> categoryCollections){
        this.list.setValue(categoryCollections);
    }

    // Método para eliminar un elemento de la lista por su campo 'code'
    public void deleteItemByCode(String code) {
        ArrayList<CategoryCollection> currentList = list.getValue();
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
        ArrayList<CategoryCollection> currentList = list.getValue();
        if (currentList != null) {
            for (CategoryCollection ctg : currentList) {
                if (ctg.getName().equals(name.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Verificar si un nombre existe en la lista pero excluyendo un ID específico
    public boolean isNameExistsExcludingCode(String name, String id) {
        ArrayList<CategoryCollection> currentList = list.getValue();
        if (currentList != null) {
            for (CategoryCollection ctg : currentList) {
                if (ctg.getName().equals(name) && !id.equals(ctg.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }
}