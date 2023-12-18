package app.list.mymusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;

import app.list.mymusic.R;
import app.list.mymusic.interfaces.MusicListener;
import app.list.mymusic.models.YTVideo;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private ArrayList<YTVideo> list;
    private Lifecycle lifecycle;
    private int contador = 0;
    private ArrayList<YouTubePlayer> list_youtube;
    private MusicListener listener;
    private Context context;

    public RecyclerViewAdapter(Context context, ArrayList<YTVideo> list, Lifecycle lifecycle, MusicListener listener) {
        this.context = context;
        this.list = list;
        this.lifecycle = lifecycle;
        this.listener = listener;
        list_youtube = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_youtube, parent, false);
        return new ViewHolder(view, lifecycle, list, listener);
    }
    //ViewHolder holder;
    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int position) {
        viewHolder.cueVideo(list.get(position).getIdvideo());
        viewHolder.setIsRecyclable(false);
    }
    public void removeItem(int position, ArrayList<YTVideo> lista) {
        list = lista;
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, list.size());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private YouTubePlayerView youTubePlayerView;
        private ImageButton btnDelete;
        //private Fragment frm;
        private YouTubePlayer youTubePlayer;
        ArrayList<YTVideo> list;
        MusicListener listener;

        public ViewHolder(@NonNull View itemView, final Lifecycle lifecycle, ArrayList<YTVideo> list, MusicListener listener) {
            super(itemView);
            this.list = list;
            this.listener = listener;
            youTubePlayerView = (YouTubePlayerView) itemView.findViewById(R.id.youtube_player_view);
            btnDelete = (ImageButton) itemView.findViewById(R.id.btnDelete);
            lifecycle.addObserver(youTubePlayerView);
            btnDelete.setImageResource(R.drawable.ic_clear);
            btnDelete.setPadding(10,5,10,5);
            btnDelete.setBackgroundResource(R.drawable.btn_danger);
            // btnUpdate.setImageResource(R.drawable.ic_add);
            // btnUpdate.setPadding(10, 5, 10, 5);
            // btnUpdate.setBackgroundResource(R.drawable.btn_primary);
            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onApiChange(@NotNull YouTubePlayer youTubePlayer) {
                    super.onApiChange(youTubePlayer);
                }
                @Override
                public void onCurrentSecond(@NotNull YouTubePlayer youTubePlayer, float second) {
                    super.onCurrentSecond(youTubePlayer, second);
                    listener.onSecordPlayer(second);
                }
                @Override
                public void onError(@NotNull YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error) {
                    super.onError(youTubePlayer, error);
                }
                @Override
                public void onPlaybackQualityChange(@NotNull YouTubePlayer youTubePlayer, PlayerConstants.PlaybackQuality playbackQuality) {
                    super.onPlaybackQualityChange(youTubePlayer, playbackQuality);
                }
                @Override
                public void onPlaybackRateChange(@NotNull YouTubePlayer youTubePlayer, PlayerConstants.PlaybackRate playbackRate) {
                    super.onPlaybackRateChange(youTubePlayer, playbackRate);
                }
                @Override
                public void onReady(@NotNull YouTubePlayer initializedYouTubePlayer) {
                    super.onReady(initializedYouTubePlayer);
                    youTubePlayer = initializedYouTubePlayer;
                    youTubePlayer.cueVideo(currentVideoId, 0);
                }
                @Override
                public void onStateChange(@NotNull YouTubePlayer yt, PlayerConstants.PlayerState state) {
                    super.onStateChange(yt, state);
                    switch (state) {
                        case UNKNOWN:
                            System.out.println("UNKNOWN");
                            break;
                        case UNSTARTED:
                            System.out.println("UNSTARTED");
                            break;
                        case ENDED:
                            System.out.println("ENDED");
                            break;
                        case PLAYING:
                            setVideoChangue(currentVideoId);
                            System.out.println("PLAYING");
                            break;
                        case PAUSED:
                            System.out.println("PAUSED");
                            break;
                        case BUFFERING:
                            setVideoChangue(currentVideoId);
                            System.out.println("BUFFERING: ");
                            break;
                        case VIDEO_CUED:
                            System.out.println("VIDEO_CUED");
                            break;
                        default:
                            System.out.println("status unknown");
                            break;
                    }
                }
                @Override
                public void onVideoDuration(@NotNull YouTubePlayer youTubePlayer, float duration) {
                    super.onVideoDuration(youTubePlayer, duration);
                }
                @Override
                public void onVideoId(@NotNull YouTubePlayer youTubePlayer, @NotNull String videoId) {
                    super.onVideoId(youTubePlayer, videoId);
                    System.out.println("ID VIDEO: "+videoId);
                }
                @Override
                public void onVideoLoadedFraction(@NotNull YouTubePlayer youTubePlayer, float loadedFraction) {
                    super.onVideoLoadedFraction(youTubePlayer, loadedFraction);
                }
            });
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDeletePosition(list.get(getAdapterPosition()));
                }
            });
        }

        private String currentVideoId;

        public void cueVideo(String videoId) {
            currentVideoId = videoId;
            if(youTubePlayer == null) return;
            youTubePlayer.cueVideo(videoId, 0);
            youTubePlayer.play();
        }
        public void setVideoChangue(String id){
            for(int i = 0; i < list.size(); i++){
                if(list.get(i).getIdvideo().equals(id)) {
                    listener.onSetPosition(i);
                    break;
                }
            }
        }


    }

}
