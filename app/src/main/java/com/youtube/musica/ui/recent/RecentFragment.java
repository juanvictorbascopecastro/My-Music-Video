package com.youtube.musica.ui.recent;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import com.youtube.musica.PlayerFullscreenActivity;
import com.youtube.musica.R;
import com.youtube.musica.adapter.MusicAdapter;
import com.youtube.musica.databinding.FragmentRecentBinding;
import com.youtube.musica.dialog.progress;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.utils.AuthUtils;

/**
 * RecentFragment
 *
 * Muestra las últimas canciones agregadas por el usuario, sin importar la categoría,
 * ordenadas por fecha de registro (de la más reciente a la más antigua).
 * Se reproducen igual que en MusicFragment: en línea dentro de la lista, o en
 * PlayerFullscreenActivity, que es el reproductor que sigue sonando en segundo plano.
 *
 * Los datos vienen de RecentViewModel (alcance de MainActivity), así que al volver a esta
 * pantalla se muestran al instante sin volver a consultar Firestore.
 */
public class RecentFragment extends Fragment implements MusicListener {
    private ArrayList<MusicCollection> list = new ArrayList<>();
    private RecyclerView recycler_view;
    private ProgressBar progressBar;
    private TextView txt_no_register;
    private FloatingActionButton fab, fab_open_player;
    private LinearLayout fab_container;
    private String currentState = "UNSTARTED";
    private ObjectAnimator loadingAnimator = null;

    private FragmentRecentBinding binding;
    public MusicAdapter recyclerViewAdapter;
    private RecentViewModel recentViewModel;
    private Music db;

    private int position = 0;
    private float minuto = 0;

    // Al volver del reproductor completo, recuerda la canción y el segundo donde se quedó
    private final ActivityResultLauncher<Intent> playerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (binding == null) return;
                Intent data = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                    position = data.getIntExtra("position", position);
                    minuto = data.getFloatExtra("minuto", minuto);
                    if (position < list.size()) recycler_view.scrollToPosition(position);
                }
                currentState = "PAUSED";
                updateFabIcon();
            });

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Alcance de la actividad: el ViewModel (y sus datos) sobrevive aunque este fragment se destruya
        recentViewModel = new ViewModelProvider(requireActivity()).get(RecentViewModel.class);

        binding = FragmentRecentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recycler_view = binding.recyclerView;
        progressBar = binding.progressBar;
        txt_no_register = binding.txtNoRegister;
        fab = binding.fab;
        fab_open_player = binding.fabOpenPlayer;
        fab_container = binding.fabContainer;
        recycler_view.setHasFixedSize(true);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));

        db = new Music();
        recentViewModel.start();

        recentViewModel.getList().observe(getViewLifecycleOwner(), musics -> {
            if (musics == null) return; // todavía cargando, o se acaba de cambiar de sesión
            // Si llega la misma lista que ya se muestra, no se reconstruye el adaptador
            // para no cortar la canción que está sonando en línea
            if (recyclerViewAdapter != null && sameSongs(list, musics)) return;
            list = musics;
            showData();
        });
        recentViewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            recentViewModel.clearError();
        });

        fab_open_player.setOnClickListener(v -> openPlayer());
        fab.setOnClickListener(v -> {
            if (recyclerViewAdapter != null) {
                recyclerViewAdapter.togglePlayPause();
            }
        });
        return root;
    }

    /**
     * Abre el reproductor completo desde la canción y el segundo actuales.
     * Ese reproductor es el que mantiene la música en segundo plano (notificación, PiP y pantalla apagada).
     */
    private void openPlayer() {
        if (list.isEmpty()) return;
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.pausePlayer();
        }
        // El reproductor usa las categorías para su botón "Seleccionar categoría"
        ArrayList<CategoryCollection> categorias = recentViewModel.getCategories().getValue();
        Intent intent = new Intent(getContext(), PlayerFullscreenActivity.class);
        Bundle bundle = new Bundle();
        bundle.putFloat("minuto", minuto);
        bundle.putInt("position", position);
        bundle.putSerializable("categorias", categorias != null ? categorias : new ArrayList<CategoryCollection>());
        bundle.putSerializable("list", list);
        intent.putExtras(bundle);
        playerLauncher.launch(intent);
    }

    private boolean sameSongs(ArrayList<MusicCollection> current, ArrayList<MusicCollection> incoming) {
        if (current.size() != incoming.size()) return false;
        for (int i = 0; i < current.size(); i++) {
            if (!current.get(i).getCode().equals(incoming.get(i).getCode())) return false;
        }
        return true;
    }

    /**
     * Actualiza el ícono del FloatingActionButton en función del estado de reproducción local.
     * Muestra un efecto de pulsación/carga si está almacenando en búfer o sin arrancar.
     */
    private void updateFabIcon() {
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
            loadingAnimator = null;
        }

        if ("PLAYING".equals(currentState)) {
            fab.setEnabled(true);
            fab.setAlpha(1.0f);
            Glide.with(this)
                .asGif()
                .load(R.drawable.player)
                .into(fab);
        } else if ("BUFFERING".equals(currentState) || "UNSTARTED".equals(currentState)) {
            fab.setEnabled(false);
            fab.setImageResource(R.drawable.ic_play);

            // Animacion de carga (pulsacion)
            loadingAnimator = ObjectAnimator.ofFloat(fab, "alpha", 1.0f, 0.3f);
            loadingAnimator.setDuration(600);
            loadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
            loadingAnimator.setRepeatMode(ValueAnimator.REVERSE);
            loadingAnimator.start();
        } else {
            fab.setEnabled(true);
            fab.setAlpha(1.0f);
            fab.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * Muestra la lista con los reproductores en línea de YouTube (MusicAdapter, el mismo de MusicFragment).
     */
    private void showData(){
        progressBar.setVisibility(View.GONE);
        recyclerViewAdapter = new MusicAdapter(list, getViewLifecycleOwner().getLifecycle(), RecentFragment.this);
        recycler_view.setAdapter(recyclerViewAdapter);
        if (position >= list.size()) position = Math.max(0, list.size() - 1);
        if(list.isEmpty()) {
            txt_no_register.setVisibility(View.VISIBLE);
            fab_container.setVisibility(View.GONE);
        }else {
            fab_container.setVisibility(View.VISIBLE);
            recycler_view.setVisibility(View.VISIBLE);
            txt_no_register.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (loadingAnimator != null) {
            loadingAnimator.cancel();
            loadingAnimator = null;
        }
        // Los reproductores en línea se liberan con la vista; al volver se crea un adaptador nuevo
        recyclerViewAdapter = null;
        currentState = "UNSTARTED";
        binding = null;
    }

    @Override
    public void onSetPosition(int position) {
        this.position = position;
    }

    @Override
    public void onVideoClicked(int position) {
        this.position = position;
        this.minuto = 0;

        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.playItem(position);
        }
    }

    @Override
    public void onVideoLongClicked(int position) {
        // Igual que en MusicFragment: sin acción por ahora
    }

    @Override
    public void onNewRegister(String data) {

    }

    @Override
    public void onSecordPlayer(float second) {
        this.minuto = second;
    }

    @Override
    public void onDeletePosition(int index) {
        if (AuthUtils.isLoggedIn()) {
            msjConfirDelete(index);
        } else {
            AuthUtils.requireLogin(getContext());
        }
    }

    private void msjConfirDelete(int index){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(Html.fromHtml("<font color='#be0d13'>"+getString(R.string.are_you_sure_you_want_delete, list.get(index).getName().toUpperCase())+"</font>"))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                        DeletingMusic(index);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogo, int id) {
                        dialogo.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Elimina la canción en Firestore. No se quita a mano de la lista: el listener de
     * RecentViewModel recibe el cambio y entrega la lista actualizada.
     */
    private void DeletingMusic(int index){
        MusicCollection musicCollection = list.get(index);
        progress.run("Eliminando registro...", getContext());
        db.deleteMusic(musicCollection.getCode()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progress.diss();
                if(task.isSuccessful()){
                    if (index < position) position--;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Registro eliminado correctamente", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progress.diss();
                if (getContext() != null) {
                    Toast.makeText(getContext(), "¡Se ha producido un error al eliminar la canción!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onSeletedCtg(CategoryCollection categoryCollection) {
        // Esta pantalla no filtra por categoría
    }

    @Override
    public void onStateChanged(String state) {
        currentState = state;
        if ("ENDED".equals(state)) {
            if (position + 1 < list.size()) {
                position++;
                recycler_view.smoothScrollToPosition(position);
                if (recyclerViewAdapter != null) {
                    recyclerViewAdapter.playItem(position);
                }
            }
        }
        updateFabIcon();
    }
}
