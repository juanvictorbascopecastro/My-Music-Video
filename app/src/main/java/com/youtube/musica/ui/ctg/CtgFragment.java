package com.youtube.musica.ui.ctg;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import com.youtube.musica.R;
import com.youtube.musica.adapter.CtgAdapter;
import com.youtube.musica.databinding.FragmentCtgBinding;
import com.youtube.musica.dialog.AddCtg;
import com.youtube.musica.dialog.progress;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.interfaces.CtgListener;
import com.youtube.musica.models.CategoryCollection;

public class CtgFragment extends Fragment implements CtgListener {
    private ArrayList<CategoryCollection> list;
    private ProgressBar progressBar;
    private ListView listView;
    private TextView txt_no_register, text_registros;
    CtgViewModel ctgViewModel;
    private CtgAdapter ctgAdapter;
    private FragmentCtgBinding binding;
    private Category db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);

        binding = FragmentCtgBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listView;
        txt_no_register = binding.txtNoRegister;
        text_registros = binding.txtRegistros;
        progressBar = binding.progressBar;
        db = new Category();
        LoadData();
        ctgViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<CategoryCollection>>() {
            @Override
            public void onChanged(ArrayList<CategoryCollection> categoryCollections) {
                showList();
            }
        });
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddCtg(getContext(), null, CtgFragment.this, ctgViewModel);
            }
        });

        return root;
    }

    public void LoadData(){
        list = new ArrayList<>();
        progress.run(getString(R.string.load), getContext());
        db.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(!task.getResult().isEmpty()){
                    CategoryCollection ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CategoryCollection.class);
                        ctg.setCode(snapshot.getId());
                        list.add(ctg);
                    }
                }
                ctgViewModel.setList(list);
                progress.diss();
            }
        });
    }
    private void showList(){
        progressBar.setVisibility(View.GONE);
        if(list.size() == 0){
            txt_no_register.setVisibility(View.VISIBLE);
            txt_no_register.setText(getString(R.string.no_register));
        }else{
            txt_no_register.setVisibility(View.GONE);
            ctgAdapter = new CtgAdapter(getContext(), list, this, ctgViewModel);
            listView.setAdapter(ctgAdapter);
        }
        text_registros.setText(getString(R.string.text_registros, list.size()));
    }
    public void setList(ArrayList<CategoryCollection> list) {
        this.list = list;
        showList();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onNewRegister(String data) {
        LoadData();
    }

    @Override
    public void onDeteRegister(String code) {
        progress.run("Eliminando registro...", getContext());
        db.deleteCtg(code).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progress.diss();
                if(task.isSuccessful()) {
                    ctgViewModel.deleteItemByCode(code);
                    Toast.makeText(getContext(), "Registro eliminado correctamente", Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getContext(), "Ocurrio un error al eliminar el registro!", Toast.LENGTH_LONG).show();
            }
        });
    }
}