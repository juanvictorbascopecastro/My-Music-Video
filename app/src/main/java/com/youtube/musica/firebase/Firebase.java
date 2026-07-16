package com.youtube.musica.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class Firebase {
    public static FirebaseFirestore db;

    public Firebase(){
        db = FirebaseFirestore.getInstance();
    }
}
