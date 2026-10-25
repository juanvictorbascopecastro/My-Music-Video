package com.youtube.musica.firebase;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;

import com.youtube.musica.R;
import com.youtube.musica.dialog.progress;
import com.youtube.musica.interfaces.DbMusicListener;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.utils.Constants;

public class Music extends Firebase {
    private Context context;
    private DbMusicListener musicListener;
    public Music(Context context, DbMusicListener musicListener) {
        super();
        this.context = context;
        this.musicListener = musicListener;
    }

    // Para usos sin interfaz (ViewModels): sin diálogo de progreso ni DbMusicListener
    public Music() {
        super();
    }

    private String getIdUser() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : "";
    }

    public Task loadMusic(String ctg){
        Task query = db.collection(Constants.MUSIC_COLLECTION)
                .whereEqualTo("ctgCode", ctg)
                .whereEqualTo("idUser", getIdUser())
                //.orderBy("date", Query.Direction.DESCENDING)
                .get();
        return query;
    }

    public Task<DocumentReference> addMusic(HashMap<String, Object> object){
        object.put("idUser", getIdUser());
        Task<DocumentReference> query = db.collection(Constants.MUSIC_COLLECTION).add(object);
        return query;
    }
    public Task<Void>  deleteMusic(String code){
        Task<Void> query = db.collection(Constants.MUSIC_COLLECTION).document(code).delete();
        return query;
    }

    /**
     * Escucha en tiempo real las últimas canciones agregadas por el usuario, de todas las
     * categorías, ordenadas por fecha de registro (de la más reciente a la más antigua).
     * El listener se vuelve a llamar cada vez que se agrega, edita o elimina una canción.
     * Requiere en Firestore un índice compuesto sobre "musics": idUser (Asc) + date (Desc).
     */
    public ListenerRegistration listenRecent(int limit, EventListener<QuerySnapshot> listener){
        return db.collection(Constants.MUSIC_COLLECTION)
                .whereEqualTo("idUser", getIdUser())
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener(listener);
    }

    public void LoadByCtg(String ctg, boolean progressVisible){
        if(progressVisible) progress.run(context.getString(R.string.load), context);
        db.collection(Constants.MUSIC_COLLECTION)
                .whereEqualTo("ctgCode", ctg)
                .whereEqualTo("idUser", getIdUser())
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progress.diss();
                        if(task.isSuccessful()) {
                            MusicCollection musicCollection;
                            ArrayList<MusicCollection> list = new ArrayList<>();
                            for(QueryDocumentSnapshot snapshot : task.getResult()) {
                                musicCollection = snapshot.toObject(MusicCollection.class);
                                musicCollection.setCode(snapshot.getId());
                                list.add(musicCollection);
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
