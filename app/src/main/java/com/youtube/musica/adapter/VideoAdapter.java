package com.youtube.musica.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.youtube.musica.R;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.models.MusicCollection;

import java.util.ArrayList;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final ArrayList<MusicCollection> videoIds;
    public MusicListener listener;
    private int currentPlayingPosition = -1;

    public VideoAdapter(ArrayList<MusicCollection> videoIds, MusicListener listener) {
        this.videoIds = videoIds;
        this.listener = listener;
    }

    public void setCurrentPlayingPosition(int position) {
        int oldPosition = currentPlayingPosition;
        currentPlayingPosition = position;
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (currentPlayingPosition != -1) {
            notifyItemChanged(currentPlayingPosition);
        }
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);
        return new VideoViewHolder(itemView, listener);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder viewHolder, int position) {
        viewHolder.bind(videoIds.get(position));
    }

    @Override
    public int getItemCount() {
        return videoIds.size();
    }

    public void removeItem(int position) {
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, videoIds.size());
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgThumbnail;
        private final TextView tvVideoTitle;
        private final ImageView imgPlayIcon;
        private final ImageView imgPlayingIndicator;
        private final View overlayView;

        public VideoViewHolder(View view, MusicListener listener) {
            super(view);
            imgThumbnail = view.findViewById(R.id.imgThumbnail);
            tvVideoTitle = view.findViewById(R.id.tvVideoTitle);
            imgPlayIcon = view.findViewById(R.id.imgPlayIcon);
            imgPlayingIndicator = view.findViewById(R.id.imgPlayingIndicator);
            overlayView = view.findViewById(R.id.overlay_view);

            overlayView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onVideoClicked(pos);
                }
            });

            overlayView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onVideoLongClicked(pos);
                }
                return true;
            });
        }

        public void bind(MusicCollection video) {
            tvVideoTitle.setText(video.getName() != null ? video.getName() : "Video");
            String thumbUrl = video.getUrl();
            if (thumbUrl != null && !thumbUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(thumbUrl)
                        .centerCrop()
                        .into(imgThumbnail);
            }
            
            if (getAdapterPosition() == currentPlayingPosition) {
                // Item en reproducción: Ocultar icono play, mostrar GIF animado
                if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.GONE);
                if (imgPlayingIndicator != null) {
                    imgPlayingIndicator.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .asGif()
                            .load(R.drawable.player)
                            .into(imgPlayingIndicator);
                }
            } else {
                // Item normal: Mostrar icono play, ocultar GIF
                if (imgPlayIcon != null) imgPlayIcon.setVisibility(View.VISIBLE);
                if (imgPlayingIndicator != null) imgPlayingIndicator.setVisibility(View.GONE);
            }
        }
    }
}
