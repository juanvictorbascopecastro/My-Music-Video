package app.list.mymusic.ui.music;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import app.list.mymusic.PlayerFullscreenActivity;
import app.list.mymusic.R;
import app.list.mymusic.adapter.CtgAdapterHorizontal;
import app.list.mymusic.adapter.VideoAdapterAdapter;
import app.list.mymusic.databinding.FragmentMusicBinding;
import app.list.mymusic.dialog.progress;
import app.list.mymusic.firebase.CtgDataBase;
import app.list.mymusic.firebase.MusicDataBase;
import app.list.mymusic.interfaces.DbMusicListener;
import app.list.mymusic.interfaces.MusicListener;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;
import app.list.mymusic.ui.ctg.CtgViewModel;
import app.list.mymusic.utils.Constants;

public class MusicFragment extends Fragment implements MusicListener, DbMusicListener {
    private ArrayList<YTVideo> list;
    private RecyclerView recycler_view;
    private ProgressBar progressBar;
    private TextView txt_no_register, txtTitle;
    private FloatingActionButton fab;

    private FragmentMusicBinding binding;
    public VideoAdapterAdapter recyclerViewAdapter;
    MusicViewModel musicViewModel;
    CtgViewModel ctgViewModel;
    private MusicDataBase db;
    private CtgDataBase ctgDb;
    private CtgMusic ctgMusic;
    private RecyclerView recyclerViewCategory;


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
        recycler_view.setHasFixedSize(true);
        db = new MusicDataBase(getContext(), this);
        ctgDb = new CtgDataBase();
        LoadCtg();
        ctgViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<CtgMusic>>() {
            @Override
            public void onChanged(ArrayList<CtgMusic> ctgMusics) {
                showCategory();
            }
        });

        /* musicViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<YTVideo>>() {
            @Override
            public void onChanged(ArrayList<YTVideo> ytVideos) {
                showData();
            }
        }); */
        fab.setOnClickListener(new View.OnClickListener() {
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
                startActivityForResult(intent, Constants.SECOND_ACTIVITY_REQUEST_CODE);

            }
        });
        return root;
    }

    public void LoadCtg(){
        ArrayList<CtgMusic> listCtg = new ArrayList<>();
        progress.run(getString(R.string.load), getContext());
        ctgDb.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.getResult().isEmpty()){
                    CtgMusic ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CtgMusic.class);
                        ctg.setCode(snapshot.getId());
                        listCtg.add(ctg);
                    }
                }
                ctgViewModel.setList(listCtg);
            }
        });
    }
    int positionActive = 0;
    private void showCategory() {
        ArrayList<CtgMusic> listCat = ctgViewModel.getList().getValue();
        if(listCat.size() > 0) {
            if(ctgMusic == null) ctgMusic = listCat.get(0);
            // cargar lista de playlist
            list = new ArrayList<>();
            progressBar.setVisibility(View.VISIBLE);
            if(ctgMusic == null) return;
            db.LoadByCtg(ctgMusic.getCode(), false);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
            recyclerViewCategory.setLayoutManager(layoutManager);
            CtgAdapterHorizontal ctgAdapterHorizontal =
                    new CtgAdapterHorizontal(getActivity(), listCat, MusicFragment.this, positionActive);
            recyclerViewCategory.setAdapter(ctgAdapterHorizontal);
        } else {
            recyclerViewCategory.setVisibility(View.GONE);
        }
    }

    @Override
    public void loadedMusicPlaylist(ArrayList<YTVideo> playList) {
        musicViewModel.setList(playList);
        list = playList;
        showData();
    }

    @Override
    public void errorLoadedMusicPlayList(String message, int icon) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showData(){
        progressBar.setVisibility(View.GONE);
        recyclerViewAdapter = new VideoAdapterAdapter(list, this.getLifecycle(), MusicFragment.this);
        recycler_view.setAdapter(recyclerViewAdapter);
        recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewAdapter.notifyDataSetChanged();
        if(list.size() == 0 ) {
            txt_no_register.setVisibility(View.VISIBLE);
            fab.setVisibility(View.GONE);
        }else {
            fab.setVisibility(View.VISIBLE);
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
    public void onNewRegister(String data) {

    }

    @Override
    public void onSecordPlayer(float second) {
        this.minuto = second;
    }

    @Override
    public void onDeletePosition(int index) {
        msjConfirDelete(index);
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
        YTVideo ytVideo = list.get(index);
        progress.run("Eliminando registro...", getContext());
        db.deleteMusic(ytVideo.getCode()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progress.diss();
                if(task.isSuccessful()){
                    Toast.makeText(getContext(), "Registro eliminado correctamente", Toast.LENGTH_LONG).show();
                    musicViewModel.deleteItemByCode(ytVideo.getCode());
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
    public void onSeletedCtg(CtgMusic ctgMusic) {
        this.ctgMusic = ctgMusic;
        db.LoadByCtg(ctgMusic.getCode(), true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.SECOND_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    int minuto = data.getIntExtra("minuto", 0);
                    int position = data.getIntExtra("position", 0);
                    System.out.println("MINUTO: " + minuto);
                    System.out.println("POSICION: " + position);
                }
            } else {
                // El resultado no fue exitoso, manejar según sea necesario
            }
        }
    }
}