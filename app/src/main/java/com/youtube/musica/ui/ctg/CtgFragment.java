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
        ctgViewModel = new ViewModelProvider(requireActivity()).get(CtgViewModel.class);

        binding = FragmentCtgBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listView;
        txt_no_register = binding.txtNoRegister;
        text_registros = binding.txtRegistros;
        progressBar = binding.progressBar;
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

            listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    CategoryCollection ctg = list.get(position);
                    showBottomSheet(ctg);
                }
            });

            listView.setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    CategoryCollection ctg = list.get(position);
                    checkAndDelete(ctg);
                    return true;
                }
            });
        }
        text_registros.setText(getString(R.string.text_registros, list.size()));
    }

    private void showBottomSheet(final CategoryCollection ctg) {
        final com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_ctg_actions, null);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.btnEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                new AddCtg(getContext(), ctg, CtgFragment.this, ctgViewModel);
            }
        });

        view.findViewById(R.id.btnDelete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                checkAndDelete(ctg);
            }
        });

        bottomSheetDialog.show();
    }

    private void checkAndDelete(final CategoryCollection categoryCollection) {
        progress.run("Verificando...", getContext());
        com.youtube.musica.firebase.Music musicDb = new com.youtube.musica.firebase.Music(getContext(), null);
        musicDb.loadMusic(categoryCollection.getCode()).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                progress.diss();
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                        builder.setTitle("Acción denegada")
                               .setMessage("No se puede eliminar esta categoría porque tiene música asignada.")
                               .setPositiveButton("Aceptar", null)
                               .show();
                    } else {
                        msjConfirDelete(categoryCollection);
                    }
                } else {
                    Toast.makeText(getContext(), "Error al verificar la categoría", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void msjConfirDelete(final CategoryCollection categoryCollection){
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setMessage(android.text.Html.fromHtml("<font color='#be0d13'>"+getString(R.string.are_you_sure_you_want_delete, categoryCollection.getName())+"</font>"))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.accept), new android.content.DialogInterface.OnClickListener() {
                    public void onClick(final android.content.DialogInterface dialog, int id) {
                        onDeteRegister(categoryCollection.getCode());
                    }
                }).setNegativeButton(getString(R.string.cancel), new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialogo, int id) {
                        dialogo.cancel();
                    }
                });
        android.app.AlertDialog alert = builder.create();
        alert.show();
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