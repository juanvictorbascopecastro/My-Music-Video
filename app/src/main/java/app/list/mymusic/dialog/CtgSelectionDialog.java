package app.list.mymusic.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import app.list.mymusic.R;
import app.list.mymusic.models.CtgMusic;

public class CtgSelectionDialog {
    public interface OnCtgSelectedListener {
        void onCtgSelected(String ctgCode);
    }

    public static void show(Context context, List<CtgMusic> ctgs, final OnCtgSelectedListener listener) {
        List<String> arrayList = new ArrayList<>();
        for (CtgMusic ctg : ctgs) {
            arrayList.add(ctg.getName());
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.list, null);
        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setTitle(context.getString(R.string.select_ctg));

        ListView listView = dialogView.findViewById(R.id.listView1);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.text_folder, R.id.txt, arrayList);
        listView.setAdapter(adapter);

        final AlertDialog alertDialog = alertDialogBuilder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCtgCode = ctgs.get(position).getCode();
                if (listener != null) {
                    listener.onCtgSelected(selectedCtgCode);
                }
                alertDialog.dismiss();
            }
        });

        alertDialog.setCancelable(true);
        alertDialog.show();
    }
}
