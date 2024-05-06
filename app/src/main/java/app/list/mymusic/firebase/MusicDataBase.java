package app.list.mymusic.firebase;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import app.list.mymusic.R;
import app.list.mymusic.dialog.progress;
import app.list.mymusic.interfaces.DbMusicListener;
import app.list.mymusic.models.YTVideo;
import app.list.mymusic.utils.Constants;

public class MusicDataBase extends DataBase {
    private Context context;
    private DbMusicListener musicListener;
    public MusicDataBase(Context context, DbMusicListener musicListener) {
        super();
        this.context = context;
        this.musicListener = musicListener;
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

    public void LoadByCtg(String ctg, boolean progressVisible){
        if(progressVisible) progress.run(context.getString(R.string.load), context);
        db.collection(Constants.MUSIC_COLLECTION).whereEqualTo("ctgCode", ctg)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progress.diss();
                        if(task.isSuccessful()) {
                            YTVideo ytVideo;
                            ArrayList<YTVideo> list = new ArrayList<>();
                            for(QueryDocumentSnapshot snapshot : task.getResult()) {
                                ytVideo = snapshot.toObject(YTVideo.class);
                                ytVideo.setCode(snapshot.getId());
                                list.add(ytVideo);
                            }
                            musicListener.loadedMusicPlaylist(list);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println(e.getMessage());
                        musicListener.errorLoadedMusicPlayList(e.getMessage(), android.R.drawable.stat_notify_error);
                    }
                });
    }
}
