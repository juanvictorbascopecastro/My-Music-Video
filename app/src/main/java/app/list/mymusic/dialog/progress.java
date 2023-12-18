package app.list.mymusic.dialog;

import android.app.ProgressDialog;
import android.content.Context;

public class progress {
    private static ProgressDialog progress;
    public static void run(String msj, Context context){
        progress = new ProgressDialog(context);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.setMessage(msj);
        progress.show();
    }
    public static void diss(){
        if(progress != null) progress.dismiss();
    }

}
