package app.list.mymusic.ui.ctg;

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

import app.list.mymusic.R;
import app.list.mymusic.adapter.CtgAdapter;
import app.list.mymusic.databinding.FragmentCtgBinding;
import app.list.mymusic.dialog.AddCtg;
import app.list.mymusic.dialog.progress;
import app.list.mymusic.firebase.CtgDataBase;
import app.list.mymusic.interfaces.CtgListener;
import app.list.mymusic.models.CtgMusic;

public class CtgFragment extends Fragment implements CtgListener {
    private ArrayList<CtgMusic> list;
    private ProgressBar progressBar;
    private ListView listView;
    private TextView txt_no_register, text_registros;
    CtgViewModel ctgViewModel;
    private CtgAdapter ctgAdapter;
    private FragmentCtgBinding binding;
    private CtgDataBase db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);

        binding = FragmentCtgBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = binding.listView;
        txt_no_register = binding.txtNoRegister;
        text_registros = binding.txtRegistros;
        progressBar = binding.progressBar;
        db = new CtgDataBase();
        LoadData();
        ctgViewModel.getList().observe(getViewLifecycleOwner(), new Observer<ArrayList<CtgMusic>>() {
            @Override
            public void onChanged(ArrayList<CtgMusic> ctgMusics) {
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
                    CtgMusic ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CtgMusic.class);
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
    public void setList(ArrayList<CtgMusic> list) {
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