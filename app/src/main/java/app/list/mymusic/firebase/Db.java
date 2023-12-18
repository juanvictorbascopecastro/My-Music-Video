package app.list.mymusic.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class Db {
    public static FirebaseFirestore db;

    public Db(){
        db = FirebaseFirestore.getInstance();
    }
}
