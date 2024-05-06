package app.list.mymusic.firebase;

import com.google.firebase.firestore.FirebaseFirestore;

public class DataBase {
    public static FirebaseFirestore db;

    public DataBase(){
        db = FirebaseFirestore.getInstance();
    }
}
