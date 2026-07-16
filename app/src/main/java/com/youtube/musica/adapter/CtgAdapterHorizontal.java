package com.youtube.musica.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import androidx.core.content.ContextCompat;

import com.youtube.musica.R;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.models.CategoryCollection;

public class CtgAdapterHorizontal extends RecyclerView.Adapter<CtgAdapterHorizontal.ViewHolder>{
    private ArrayList<CategoryCollection> list;
    private Context context;
    private MusicListener listener;
    private int positionActive;
    public CtgAdapterHorizontal(Context context, ArrayList<CategoryCollection> list, MusicListener listener, int positionActive) {
        this.list = list;
        this.context = context;
        this.listener = listener;
        this.positionActive = positionActive;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ctg_horizontal, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryCollection category = list.get(position);
        holder.txtName.setText(category.getName());
        holder.txtName.setTag(position);
        if (positionActive == position) {
            holder.txtName.setBackgroundResource(R.drawable.style_line);
            holder.txtName.setTextColor(ContextCompat.getColor(context, R.color.primary));
        } else {
            holder.txtName.setBackgroundResource(0);
            holder.txtName.setTextColor(ContextCompat.getColor(context, R.color.fondo));
        }
        // final ViewHolder finalHoler = holder;
        holder.txtName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = (Integer) view.getTag();
                positionActive = index;
                notifyDataSetChanged();
                listener.onSeletedCtg(list.get(index));
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
        }
    }
}
