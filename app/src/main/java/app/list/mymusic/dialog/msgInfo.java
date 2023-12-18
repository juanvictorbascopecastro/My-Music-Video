package app.list.mymusic.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import app.list.mymusic.R;

public class msgInfo extends AlertDialog {
        private static ViewGroup viewGroup;
        private static AlertDialog alertDialog;
        private static TextView txtMesagge, txtTitle;
        private static View dialogView;
        private static Context context;

        public msgInfo(Context context) {
            super(context);
            this.context = context;
            Create();
        }

        public static void Create(){
            dialogView = LayoutInflater.from(context).inflate(R.layout.msg_info, viewGroup, false);
            txtMesagge = dialogView.findViewById(R.id.txtTexto);
            final Button btnAceptar = dialogView.findViewById(R.id.btnAceptar);
            txtTitle = dialogView.findViewById(R.id.txtTituloALert);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialogView);
            alertDialog = builder.create();
            btnAceptar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dissMsg();
                }
            });
        }
        public static void showMsg(String texto, String color, int tipo){
            txtMesagge.setText(texto);
            dialogView.setBackgroundColor(Color.parseColor(color));

            if(tipo == 0) txtTitle.setText(context.getString(R.string.info));
            else if(tipo == 1) txtTitle.setText(context.getString(R.string.warning));
            else if(tipo == 2) txtTitle.setText(context.getString(R.string.error));
            alertDialog.show();
        }

        public static void dissMsg(){
            alertDialog.dismiss();
        }

}
