package com.youtube.musica.adapter;

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

import com.youtube.musica.R;
import com.youtube.musica.dialog.AddCtg;
import com.youtube.musica.interfaces.CtgListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.ui.ctg.CtgViewModel;

public class CtgAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<CategoryCollection> list;
    private CtgListener listener;
    private CtgViewModel ctgViewModel;

    public CtgAdapter(Context context, ArrayList<CategoryCollection> list, CtgListener listener, CtgViewModel ctgViewModel){
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
            holder.txtName = view.findViewById(R.id.txtName);
            view.setTag(holder);
        }else{
            holder = (ViewHolder) view.getTag();
        }
        final CategoryCollection ctg = list.get(i);
        holder.txtName.setText(ctg.getName());
        return view;
    }

    public class ViewHolder{
        TextView txtName;
    }

}

