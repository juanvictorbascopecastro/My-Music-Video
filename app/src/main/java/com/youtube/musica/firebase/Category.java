package com.youtube.musica.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;

import com.youtube.musica.utils.Constants;

public class Category extends Firebase {
    public Category(){
        super();
    }
    
    private String getIdUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "";
    }

    public Task<com.google.firebase.firestore.QuerySnapshot> loadCtg(){
        Task<com.google.firebase.firestore.QuerySnapshot> query = db.collection(Constants.CATEGORIES_COLLECTION)
                .whereEqualTo("idUser", getIdUser())
                .get();
        return query;
    }

    public Task<DocumentReference> addCtg(HashMap<String, Object> object){
        object.put("idUser", getIdUser());
        Task<DocumentReference> query = db.collection(Constants.CATEGORIES_COLLECTION).add(object);
        return query;
    }
    
    public Task<Void> updateCtg(String code, HashMap<String, String> object){
        object.put("idUser", getIdUser());
        Task<Void> query = db.collection(Constants.CATEGORIES_COLLECTION).document(code).set(object);
        return query;
    }

    public Task<Void>  deleteCtg(String code){
        Task<Void> query = db.collection(Constants.CATEGORIES_COLLECTION).document(code).delete();
        return query;
    }
}
