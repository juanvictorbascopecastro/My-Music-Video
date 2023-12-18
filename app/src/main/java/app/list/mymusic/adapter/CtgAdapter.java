package app.list.mymusic.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import app.list.mymusic.R;
import app.list.mymusic.dialog.AddCtg;
import app.list.mymusic.interfaces.CtgListener;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.ui.ctg.CtgViewModel;

public class CtgAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<CtgMusic> list;
    private CtgListener listener;
    private CtgViewModel ctgViewModel;

    public CtgAdapter(Context context, ArrayList<CtgMusic> list, CtgListener listener, CtgViewModel ctgViewModel){
        this.list = list;
        this.context = context;
        this.listener = listener;
        this.ctgViewModel = ctgViewModel;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = new ViewHolder();
        if(view == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.item_ctg, viewGroup, false);
            holder.txtName = view.findViewById(R.id.txtName); view.setTag(holder);
            holder.btnDelete = view.findViewById(R.id.btnDelete);
            holder.btnEdit = view.findViewById(R.id.btnEdit);
        }else{
            holder = (ViewHolder) view.getTag();
        }
        final CtgMusic ctg = list.get(i);
        holder.txtName.setText(ctg.getName());
        holder.btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddCtg(context, ctg, listener, ctgViewModel);
            }
        });
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msjConfirDelete(ctg);
            }
        });
        return view;
    }

    public class ViewHolder{
        TextView txtName;
        ImageButton btnEdit, btnDelete;
    }
    private void msjConfirDelete(final CtgMusic ctgMusic){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(Html.fromHtml("<font color='#be0d13'>"+context.getString(R.string.are_you_sure_you_want_delete, ctgMusic.getName())+"</font>"))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int id) {
                      listener.onDeteRegister(ctgMusic.getCode());
                    }
                }).setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogo, int id) {
                        dialogo.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

}

