package app.list.mymusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.list.mymusic.R;
import app.list.mymusic.interfaces.MusicListener;
import app.list.mymusic.models.CtgMusic;

public class CtgAdapterHorizontal extends RecyclerView.Adapter<CtgAdapterHorizontal.ViewHolder>{
    private ArrayList<CtgMusic> list;
    private Context context;
    private MusicListener listener;
    private int positionActive;
    public CtgAdapterHorizontal(Context context, ArrayList<CtgMusic> list, MusicListener listener, int positionActive) {
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
        CtgMusic category = list.get(position);
        holder.txtName.setText(category.getName());
        holder.txtName.setTag(position);
        if(positionActive == position) holder.txtName.setBackgroundResource(R.drawable.style_line);
        else holder.txtName.setBackgroundResource(0);
        // final ViewHolder finalHoler = holder;
        holder.txtName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = (Integer) view.getTag();
                positionActive = index;
                notifyDataSetChanged();
                holder.txtName.setBackgroundResource(R.drawable.style_line);
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
