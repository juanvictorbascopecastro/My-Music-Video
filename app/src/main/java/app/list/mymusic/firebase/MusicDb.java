package app.list.mymusic.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.HashMap;

import app.list.mymusic.utils.Constants;

public class MusicDb extends Db{
    public MusicDb() {
        super();
    }
    public Task loadMusic(String ctg){
        Task query = db.collection(Constants.MUSIC_COLLECTION).whereEqualTo("ctgCode", ctg)
                //.orderBy("date", Query.Direction.DESCENDING)
                .get();
        return query;
    }

    public Task<DocumentReference> addMusic(HashMap<String, Object> object){
        Task<DocumentReference> query = db.collection(Constants.MUSIC_COLLECTION).add(object);
        return query;
    }
    public Task<Void>  deleteMusic(String code){
        Task<Void> query = db.collection(Constants.MUSIC_COLLECTION).document(code).delete();
        return query;
    }
}
