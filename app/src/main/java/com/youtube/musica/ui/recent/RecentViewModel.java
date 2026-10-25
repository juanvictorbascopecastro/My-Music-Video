package com.youtube.musica.ui.recent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import com.youtube.musica.firebase.Category;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.utils.Constants;

/**
 * RecentViewModel
 *
 * Guarda las últimas canciones agregadas y las categorías del usuario. RecentFragment lo
 * obtiene con el alcance de MainActivity, así que los datos sobreviven al cambiar de pantalla
 * en el menú lateral y al rotar el teléfono: Firestore no se vuelve a consultar cada vez.
 *
 * En lugar de una consulta única usa listeners en tiempo real, para que la lista se actualice
 * sola cuando se agrega o elimina una canción desde cualquier pantalla. Los listeners se quitan
 * en onCleared(), cuando MainActivity se destruye.
 *
 * Con el modo invitado, cerrar sesión ya no cierra MainActivity: por eso se escucha el estado de
 * FirebaseAuth y, si cambia el usuario, se sueltan los listeners y los datos del anterior.
 */
public class RecentViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<MusicCollection>> list = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<CategoryCollection>> categories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final Music musicDb = new Music();
    private final Category ctgDb = new Category();
    private ListenerRegistration musicRegistration;
    private ListenerRegistration ctgRegistration;
    // Usuario para el que están activos los listeners
    private String listeningUid = null;

    private final FirebaseAuth.AuthStateListener authListener = auth -> {
        FirebaseUser user = auth.getCurrentUser();
        String uid = user != null ? user.getUid() : null;
        if (listeningUid != null && !listeningUid.equals(uid)) {
            stop();
            list.setValue(null);
            categories.setValue(new ArrayList<>());
            error.setValue(null);
        }
    };

    public RecentViewModel() {
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }

    public LiveData<ArrayList<MusicCollection>> getList() {
        return list;
    }

    public LiveData<ArrayList<CategoryCollection>> getCategories() {
        return categories;
    }

    public LiveData<String> getError() {
        return error;
    }

    /**
     * Activa los listeners de Firestore si todavía no lo están. Se puede llamar cada vez que
     * se crea la vista: si ya están escuchando, no hace nada y la vista usa los datos guardados.
     */
    public void start() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Invitado: no tiene canciones guardadas
            list.setValue(new ArrayList<>());
            return;
        }
        if (!user.getUid().equals(listeningUid)) {
            stop();
            listeningUid = user.getUid();
        }
        if (musicRegistration == null) {
            musicRegistration = musicDb.listenRecent(Constants.RECENT_MUSIC_LIMIT, (snapshots, e) -> {
                if (e != null) {
                    // Firestore cierra el listener cuando falla (por ejemplo, si falta el índice);
                    // se libera para reintentar la próxima vez que se abra la pantalla.
                    System.out.println(e.getMessage());
                    musicRegistration = null;
                    error.setValue(e.getMessage());
                    return;
                }
                ArrayList<MusicCollection> musics = new ArrayList<>();
                MusicCollection musicCollection;
                for (QueryDocumentSnapshot snapshot : snapshots) {
                    musicCollection = snapshot.toObject(MusicCollection.class);
                    musicCollection.setCode(snapshot.getId());
                    musics.add(musicCollection);
                }
                list.setValue(musics);
            });
        }
        if (ctgRegistration == null) {
            ctgRegistration = ctgDb.listenCtg((snapshots, e) -> {
                if (e != null) {
                    System.out.println(e.getMessage());
                    ctgRegistration = null;
                    return;
                }
                ArrayList<CategoryCollection> ctgs = new ArrayList<>();
                CategoryCollection ctg;
                for (QueryDocumentSnapshot snapshot : snapshots) {
                    ctg = snapshot.toObject(CategoryCollection.class);
                    ctg.setCode(snapshot.getId());
                    ctgs.add(ctg);
                }
                categories.setValue(ctgs);
            });
        }
    }

    // Se llama después de mostrar el error, para que no se repita al volver a la pantalla
    public void clearError() {
        error.setValue(null);
    }

    private void stop() {
        if (musicRegistration != null) {
            musicRegistration.remove();
            musicRegistration = null;
        }
        if (ctgRegistration != null) {
            ctgRegistration.remove();
            ctgRegistration = null;
        }
        listeningUid = null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        stop();
    }
}
