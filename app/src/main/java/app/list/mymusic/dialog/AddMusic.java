package app.list.mymusic.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import app.list.mymusic.R;
import app.list.mymusic.firebase.MusicDb;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;

public class AddMusic extends AlertDialog {
        private Context context;
        AlertDialog alertDialog;
        private EditText editURL, editName, editDetails, editCode;
        private ArrayList<CtgMusic> list_ctg;
        private String url, id_video;
        private ArrayAdapter<String> adapter;
        private String[] categorias;
        private Spinner spinner;
        Fragment frm;
        private msgInfo msg;
        private MusicDb db;

        public AddMusic(Context context, ArrayList<CtgMusic> list,String url, String id_video) {
            super(context);
            this.url = url;
            this.list_ctg = list;
            this.id_video = id_video;
            this.context = context;
            Iniciar();
        }
        private YTVideo youTube;
        public AddMusic(Context context, ArrayList<CtgMusic> list, YTVideo youTube, Fragment frm) {
            super(context);
            this.youTube = youTube;
            this.list_ctg = list;
            this.context = context;
            this.frm = frm;
            Iniciar();
        }
        private void Iniciar() {
            LayoutInflater inf = ((Activity) context).getLayoutInflater();
            View view = inf.inflate(R.layout.add_video, null);
            Button btnCerrar = view.findViewById(R.id.btnCerrar);
            Button btnCerrar2 = view.findViewById(R.id.btnCerrar2);
            editDetails = view.findViewById(R.id.editDetails);
            editName = view.findViewById(R.id.editName);
            editURL = view.findViewById(R.id.editUrl);
            editCode = view.findViewById(R.id.editCode);
            spinner = view.findViewById(R.id.spinner);
            TextView txtTitle = view.findViewById(R.id.txtTitle);
            Button btnSend = view.findViewById(R.id.btnSend);

            editURL.setText(url);
            editCode.setText(id_video);
            db = new MusicDb();
            msg = new msgInfo(getContext());

            categorias = new String[list_ctg.size()];
            for (int i = 0; i < list_ctg.size(); i++){
                categorias[i] = list_ctg.get(i).getName();
            }
            adapter = new ArrayAdapter(getContext(),
                    android.R.layout.simple_list_item_1, categorias);
            spinner.setAdapter(adapter);

            if(youTube != null){
                txtTitle.setText(context.getString(R.string.edit));
                btnSend.setText(context.getString(R.string.save_changue));
                editName.setText(youTube.getName());
                editDetails.setText(youTube.getDetails());
                editURL.setText(youTube.getUrl());
                for (int i = 0; i < list_ctg.size(); i++){
                    if(list_ctg.get(i).getCode().equals(youTube.getIdvideo())){
                        spinner.setSelection(i);
                    }
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(view);
            alertDialog = builder.create();

            btnCerrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!editName.getText().toString().trim().equals("")){
                        if(youTube == null) {
                            if(id_video != null){
                                SaveData();
                            }else{
                                msg.showMsg(context.getString(R.string.no_id_video),"#FF9800",1);
                            }
                        }else UpdateData();
                    }else{
                        msg.showMsg(context.getString(R.string.enter_name),"#FF9800",1);
                    }
                }
            });
            btnCerrar2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
            editName.setOnEditorActionListener(editor);
        }
        TextView.OnEditorActionListener editor = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId){
                    case EditorInfo.IME_ACTION_NEXT:

                        break;
                    case EditorInfo.IME_ACTION_SEND:

                        break;
                    case EditorInfo.IME_ACTION_GO:

                        break;
                }
                return false;
            }
        };

        private void SaveData(){
            progress.run(context.getString(R.string.saving), context);
            HashMap<String, Object> object = new HashMap<>();
            object.put("idvideo", id_video);
            object.put("url", url);
            object.put("name", editName.getText().toString().trim());
            object.put("details", editDetails.getText().toString());
            object.put("date", new Timestamp(new Date()));
            object.put("ctgCode", list_ctg.get(spinner.getSelectedItemPosition()).getCode());
            db.addMusic(object).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                @Override
                public void onComplete(@NonNull Task<DocumentReference> task) {
                    progress.diss();
                    if(task.isSuccessful()) {
                        Toast.makeText(getContext(), context.getString(R.string.saved), Toast.LENGTH_LONG).show();
                        alertDialog.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Ocurrio un error al guardar el registro!", Toast.LENGTH_LONG).show();
                }
            });
        }
        private void UpdateData(){
            progress.run("Editando registro...", context);
            Map<String, String> datos = new HashMap<>();
            datos.put("idmusic", youTube.getCode());
            datos.put("name", editName.getText().toString().trim());
            datos.put("details", editDetails.getText().toString());
            datos.put("ctgCode", list_ctg.get(spinner.getSelectedItemPosition()).getCode());
            // JSONObject object = new JSONObject(datos); // a json
        }



}
