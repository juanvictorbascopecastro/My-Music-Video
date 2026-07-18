package com.youtube.musica.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.youtube.musica.PlayerFullscreenActivity;
import com.youtube.musica.R;
import com.youtube.musica.adapter.CtgAdapter;
import com.youtube.musica.databinding.FragmentHomeBinding;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.interfaces.DbMusicListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.ui.ctg.CtgViewModel;
import com.youtube.musica.dialog.progress;

import java.util.ArrayList;

public class  HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private CtgViewModel ctgViewModel;
    private ArrayList<CategoryCollection> list;
    private GridView gridView;
    private ProgressBar progressBar;
    private TextView txtNoRegister;
    private Category db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Use requireActivity() to share the ViewModel with CtgFragment
        ctgViewModel = new ViewModelProvider(requireActivity()).get(CtgViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        gridView = binding.gridHome;
        progressBar = binding.progressBarHome;
        txtNoRegister = binding.txtNoRegister;
        db = new Category();

        ctgViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<CategoryCollection>>() {
            @Override
            public void onChanged(ArrayList<CategoryCollection> categoryCollections) {
                if (categoryCollections != null) {
                    list = categoryCollections;
                    showList();
                }
            }
        });

        if (ctgViewModel.getList().getValue() == null || ctgViewModel.getList().getValue().isEmpty()) {
            LoadData();
        }

        return root;
    }

    private void LoadData(){
        progressBar.setVisibility(View.VISIBLE);
        db.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                progressBar.setVisibility(View.GONE);
                if(task.isSuccessful() && !task.getResult().isEmpty()){
                    ArrayList<CategoryCollection> tempList = new ArrayList<>();
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        CategoryCollection ctg = snapshot.toObject(CategoryCollection.class);
                        ctg.setCode(snapshot.getId());
                        tempList.add(ctg);
                    }
                    ctgViewModel.setList(tempList);
                } else {
                    ctgViewModel.setList(new ArrayList<>());
                }
            }
        });
    }

    private void showList() {
        progressBar.setVisibility(View.GONE);
        if (list == null || list.isEmpty()) {
            txtNoRegister.setVisibility(View.VISIBLE);
            gridView.setAdapter(null);
        } else {
            txtNoRegister.setVisibility(View.GONE);
            CtgAdapter adapter = new CtgAdapter(getContext(), list, null, ctgViewModel);
            gridView.setAdapter(adapter);

            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CategoryCollection ctg = list.get(position);
                    playCategoryMusic(ctg);
                }
            });
        }
    }

    private void playCategoryMusic(CategoryCollection ctg) {
        Music musicDb = new Music(getContext(), new DbMusicListener() {
            @Override
            public void loadedMusicPlaylist(ArrayList<MusicCollection> playList) {
                if (playList != null && !playList.isEmpty()) {
                    Intent intent = new Intent(getContext(), PlayerFullscreenActivity.class);
                    intent.putExtra("minuto", 0f);
                    intent.putExtra("position", 0);
                    intent.putExtra("list", playList);
                    intent.putExtra("categorias", ctgViewModel.getList().getValue());
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "No hay música en esta categoría", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void errorLoadedMusicPlayList(String message, int icon) {
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
        musicDb.LoadByCtg(ctg.getCode(), true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}