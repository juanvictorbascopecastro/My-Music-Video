package app.list.mymusic.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;

import app.list.mymusic.utils.Constants;

public class CtgDb extends Db{
    public CtgDb(){
        super();
    }
    public Task loadCtg(){
        Task query = db.collection(Constants.CTG_COLLECTION).get();
        return query;
    }

    public Task<DocumentReference> addCtg(HashMap<String, Object> object){
        Task<DocumentReference> query = db.collection(Constants.CTG_COLLECTION).add(object);
        return query;
    }
    public Task<Void> updateCtg(String code, HashMap<String, String> object){
        Task<Void> query = db.collection(Constants.CTG_COLLECTION).document(code).set(object);
        return query;
    }

    public Task<Void>  deleteCtg(String code){
        Task<Void> query = db.collection(Constants.CTG_COLLECTION).document(code).delete();
        return query;
    }
}
