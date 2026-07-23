package com.youtube.musica.ui.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import com.youtube.musica.PlayerFullscreenActivity;
import com.youtube.musica.R;
import com.youtube.musica.adapter.CtgAdapterHorizontal;
import com.youtube.musica.adapter.MusicAdapter;
import com.youtube.musica.databinding.FragmentMusicBinding;
import com.youtube.musica.dialog.progress;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.interfaces.DbMusicListener;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.services.NotificationHelper;
import com.youtube.musica.ui.ctg.CtgViewModel;
import com.youtube.musica.utils.Constants;

import android.widget.LinearLayout;

/**
 * MusicFragment
 *
 * Fragmento principal para mostrar la lista de música y las categorías disponibles.
 * Administra el estado de reproducción de la lista, permite seleccionar categorías
 */
public class MusicFragment extends Fragment implements MusicListener, DbMusicListener {
    private ArrayList<MusicCollection> list;
    private RecyclerView recycler_view;
    private ProgressBar progressBar;
    private TextView txt_no_register, txtTitle;
    private FloatingActionButton fab, fab_open_player;
    private LinearLayout fab_container;
    private String currentState = "UNSTARTED";
    private ObjectAnimator loadingAnimator = null;

    private FragmentMusicBinding binding;
    public MusicAdapter recyclerViewAdapter;
    MusicViewModel musicViewModel;
    CtgViewModel ctgViewModel;
    private Music db;
    private Category ctgDb;
    private CategoryCollection categoryCollection;
    private RecyclerView recyclerViewCategory;

    private final ActivityResultLauncher<Intent> playerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        float minutoExtra = data.getFloatExtra("minuto", 0f);
                        int positionExtra = data.getIntExtra("position", 0);
                        System.out.println("MINUTO: " + minutoExtra);
                        System.out.println("POSICION: " + positionExtra);
                        
                        if (minutoExtra > 0) {
                            currentState = "PLAYING";
                        } else {
                            currentState = "PAUSED";
                        }
                        updateFabIcon();
                    }
                } else {
                    // El resultado no fue exitoso, manejar según sea necesario
                    currentState = "PAUSED";
                    updateFabIcon();
                }
            });


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
       musicViewModel = new ViewModelProvider(this).get(MusicViewModel.class);
       ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);

        binding = FragmentMusicBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        recycler_view = binding.recyclerView;
        progressBar = binding.progressBar;
        txt_no_register = binding.txtNoRegister;
        recyclerViewCategory = binding.reciclerViewCategory;
        txtTitle = binding.txtTitle;
        fab = binding.fab;
        fab_open_player = binding.fabOpenPlayer;
        fab_container = binding.fabContainer;
        recycler_view.setHasFixedSize(true);
        db = new Music(getContext(), this);
        ctgDb = new Category();
        LoadCtg();
        // Observa la carga de categorías desde la base de datos
        ctgViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<CategoryCollection>>() {
            @Override
            public void onChanged(ArrayList<CategoryCollection> categoryCollections) {
                showCategory();
            }
        });

        /* musicViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<YTVideo>>() {
            @Override
            public void onChanged(ArrayList<YTVideo> ytVideos) {
                showData();
            }
        }); */
        fab_open_player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), PlayerFullscreenActivity.class);
                Bundle bundle = new Bundle();
                //bundle.putSerializable("music", youTube);
                bundle.putFloat("minuto", minuto);
                bundle.putInt("position", position);
                bundle.putSerializable("categorias", ctgViewModel.getList().getValue());
                bundle.putSerializable("list", list);
                intent.putExtras(bundle);
                playerLauncher.launch(intent);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recyclerViewAdapter != null) {
                    recyclerViewAdapter.togglePlayPause();
                }
            }
        });
        return root;
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
        } else if ("PAUSED".equals(currentState) || "ENDED".equals(currentState)) {
            fab.setEnabled(true);
            fab.setAlpha(1.0f);
            fab.setImageResource(R.drawable.ic_play);
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
     * Inicia la carga de categorías desde Firebase.
     */
    public void LoadCtg(){
        ArrayList<CategoryCollection> listCtg = new ArrayList<>();
        progress.run(getString(R.string.load), getContext());
        ctgDb.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.getResult().isEmpty()){
                    CategoryCollection ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CategoryCollection.class);
                        ctg.setCode(snapshot.getId());
                        listCtg.add(ctg);
                    }
                }
                ctgViewModel.setList(listCtg);
            }
        });
    }
    int positionActive = 0;
    /**
     * Muestra el adaptador horizontal para las categorías.
     */
    private void showCategory() {
        ArrayList<CategoryCollection> listCat = ctgViewModel.getList().getValue();
        assert listCat != null;
        if(!listCat.isEmpty()) {
            if(categoryCollection == null) categoryCollection = listCat.get(0);
            // cargar lista de playlist
            list = new ArrayList<>();
            progressBar.setVisibility(View.VISIBLE);
            if(categoryCollection == null) return;
            db.LoadByCtg(categoryCollection.getCode(), false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
            recyclerViewCategory.setLayoutManager(layoutManager);
            CtgAdapterHorizontal ctgAdapterHorizontal =
                    new CtgAdapterHorizontal(getActivity(), listCat, MusicFragment.this, positionActive);
            recyclerViewCategory.setAdapter(ctgAdapterHorizontal);
        } else {
            recyclerViewCategory.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            txt_no_register.setVisibility(View.VISIBLE);
            fab_container.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadedMusicPlaylist(ArrayList<MusicCollection> playList) {
        musicViewModel.setList(playList);
        list = playList;
        showData();
        
        // Si estamos casteando y cambiamos de categoría, iniciar automáticamente el primer video
    }

    @Override
    public void errorLoadedMusicPlayList(String message, int icon) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Configura y muestra el RecyclerView con la lista de videos musicales,
     * utilizando VideoAdapterAdapter para los reproductores en línea de YouTube.
     */
    private void showData(){
        progressBar.setVisibility(View.GONE);
        recyclerViewAdapter = new MusicAdapter(list, this.getLifecycle(), MusicFragment.this);
        recycler_view.setAdapter(recyclerViewAdapter);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));
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
        binding = null;
    }
    private int position = 0;
    private float minuto = 0;
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
        // Do nothing for now in MusicFragment
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
        if (com.youtube.musica.utils.AuthUtils.isLoggedIn()) {
            msjConfirDelete(index);
        } else {
            com.youtube.musica.utils.AuthUtils.requireLogin(getContext());
        }
    }

    private void msjConfirDelete(int index){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(Html.fromHtml("<font color='#be0d13'>"+getString(R.string.are_you_sure_you_want_delete, list.get(index).getName().toUpperCase())+"</font>"))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                       DelitingMusic(index);
                    }
                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogo, int id) {
                        dialogo.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
    private void DelitingMusic(int index){
        MusicCollection musicCollection = list.get(index);
        progress.run("Eliminando registro...", getContext());
        db.deleteMusic(musicCollection.getCode()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progress.diss();
                if(task.isSuccessful()){
                    Toast.makeText(getContext(), "Registro eliminado correctamente", Toast.LENGTH_LONG).show();
                    musicViewModel.deleteItemByCode(musicCollection.getCode());
                    recyclerViewAdapter.removeItem(index);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progress.diss();
                Toast.makeText(getContext(), "¡Se ha producido un error al eliminar la canción!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onSeletedCtg(CategoryCollection categoryCollection) {
        this.categoryCollection = categoryCollection;
        db.LoadByCtg(categoryCollection.getCode(), true);
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