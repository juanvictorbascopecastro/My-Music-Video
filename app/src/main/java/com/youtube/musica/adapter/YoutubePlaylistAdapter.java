package com.youtube.musica.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.youtube.musica.R;
import com.youtube.musica.models.YoutubePlaylist;

import java.util.ArrayList;
import java.util.List;

public class YoutubePlaylistAdapter extends RecyclerView.Adapter<YoutubePlaylistAdapter.ViewHolder> {

    private List<YoutubePlaylist> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(YoutubePlaylist item);
    }

    public YoutubePlaylistAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<YoutubePlaylist> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public List<YoutubePlaylist> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_youtube_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YoutubePlaylist item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvTitle;
        private final TextView tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
        }

        public void bind(YoutubePlaylist item, OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvDescription.setText(item.getDescription());

            Glide.with(itemView.getContext())
                    .load(item.getThumbnailUrl())
                    .into(ivThumbnail);

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
