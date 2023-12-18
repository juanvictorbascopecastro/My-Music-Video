package app.list.mymusic.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;
import java.util.HashMap;

import app.list.mymusic.R;
import app.list.mymusic.firebase.CtgDb;
import app.list.mymusic.interfaces.CtgListener;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.ui.ctg.CtgViewModel;

public class AddCtg extends AlertDialog {
    private Context context;
    AlertDialog alertDialog;
    CtgMusic ctgMusic;
    private EditText editName, editDetails;
    private msgInfo msg;
    private CtgDb db;
    CtgListener listener;
    CtgViewModel ctgViewModel;
    public AddCtg(Context context, CtgMusic ctgMusic, CtgListener listener, CtgViewModel ctgViewModel) {
        super(context);
        this.ctgMusic = ctgMusic;
        this.listener = listener;
        this.context = context;
        this.ctgViewModel = ctgViewModel;
        Iniciar();
    }
    private void Iniciar() {
        try {
            LayoutInflater inf = ((Activity) context).getLayoutInflater();
            View vista = inf.inflate(R.layout.add_ctg, null);
            Button btnCerrar = vista.findViewById(R.id.btnCerrar);
            Button btnCerrar2 = vista.findViewById(R.id.btnCerrar2);
            editDetails = vista.findViewById(R.id.editDetails);
            editName = vista.findViewById(R.id.editName);
            TextView txtTitle = vista.findViewById(R.id.txtTitle);
            Button btnSend = vista.findViewById(R.id.btnSend);

            msg = new msgInfo(getContext());
            db = new CtgDb();

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(vista);
            alertDialog = builder.create();

            if(ctgMusic != null){
                txtTitle.setText(context.getString(R.string.edit_category));
                editName.setText(ctgMusic.getName());
                editDetails.setText(ctgMusic.getDescription());
            }

            btnCerrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!editName.getText().toString().trim().equals("")){ // si no esta vacio
                        if(ctgMusic == null){
                            if(!ctgViewModel.isNameExists(editName.getText().toString().trim())){ // si ya existe un registro con el mismo numero
                                SaveData();
                            }else{
                                msg.showMsg(context.getString(R.string.exits_ctg,  editName.getText().toString()),"#FF9800",1);
                            }
                        }else{
                            if(!ctgViewModel.isNameExistsExcludingCode(editName.getText().toString().trim(), ctgMusic.getName())){ // si ya existe un registro con el mismo numero
                                UpdateSata();
                            }else{
                                msg.showMsg(context.getString(R.string.exits_ctg,  editName.getText().toString()),"#FF9800",1);
                            }
                        }
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
        }catch (Exception e){}
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
        object.put("name", editName.getText().toString().trim());
        object.put("description", editDetails.getText().toString().trim());
        object.put("date", new Timestamp(new Date()));
        db.addCtg(object)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        progress.diss();
                        if(task.isSuccessful()){
                            Toast.makeText(getContext(), context.getString(R.string.saved), Toast.LENGTH_LONG).show();
                            listener.onNewRegister(task.getResult().getId());
                            alertDialog.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println(e.getMessage());
                        progress.diss();
                        Toast.makeText(context, "Ocurrio un error al guardar la categoria!", Toast.LENGTH_LONG).show();
                    }
                });


    }
    private void UpdateSata(){
        progress.run(context.getString(R.string.load), context);
        HashMap<String, String> object = new HashMap<>();
        object.put("name", editName.getText().toString().trim());
        object.put("description", editDetails.getText().toString().trim());
        db.updateCtg(ctgMusic.getCode(), object).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progress.diss();
                if(task.isSuccessful()) {
                    Toast.makeText(getContext(), context.getString(R.string.updated), Toast.LENGTH_LONG).show();
                    listener.onNewRegister(null);
                    alertDialog.dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progress.diss();
                System.out.println(e.getMessage());
                Toast.makeText(context, "Ocurrio un error al actualizar los datos!", Toast.LENGTH_LONG).show();
            }
        });
    }
}
