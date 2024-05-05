package app.list.mymusic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

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

public class VideoAdapterAdapter extends RecyclerView.Adapter<VideoAdapterAdapter.YouTubePlayerViewHolder> {

    private final ArrayList<YTVideo> videoIds;
    private final Lifecycle lifecycle;
    public MusicListener listener;

    public VideoAdapterAdapter(ArrayList<YTVideo> videoIds, Lifecycle lifecycle, MusicListener listener) {
        this.videoIds = videoIds;
        this.lifecycle = lifecycle;
        this.listener = listener;
    }

    @Override
    public YouTubePlayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);

        return new YouTubePlayerViewHolder(lifecycle, itemView, listener);
    }

    @Override
    public void onBindViewHolder(YouTubePlayerViewHolder viewHolder, int position) {
        viewHolder.cueVideo(videoIds.get(position));
    }

    @Override
    public int getItemCount() {
        return videoIds.size();
    }

    public void removeItem(int position) {
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, videoIds.size());
    }
    public void setVideoChangue(String id){
        for(int i = 0; i < videoIds.size(); i++){
            if(videoIds.get(i).getIdvideo().equals(id)) {
                listener.onSetPosition(i);
                break;
            }
        }
    }
    /**
     * ViewHolder containing a YouTubePlayer. When the list is scrolled only the video id changes.
     */
    public class YouTubePlayerViewHolder extends RecyclerView.ViewHolder {
        private YouTubePlayer youTubePlayer;
        private YTVideo currentVideoId;
        private ImageButton btnDelete;

        public YouTubePlayerViewHolder(Lifecycle lifecycle, View view, MusicListener listener) {
            super(view);
            YouTubePlayerView youTubePlayerView = view.findViewById(R.id.youtube_player_view);
            lifecycle.addObserver(youTubePlayerView);

            View overlayView = view.findViewById(R.id.overlay_view);
            btnDelete = (ImageButton) itemView.findViewById(R.id.btnDelete);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition(); // Captura la posición donde se hizo clic
                    if (position != RecyclerView.NO_POSITION)
                        listener.onDeletePosition(position);
                }
            });
            overlayView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (youTubePlayer != null) {
                        youTubePlayer.play();
                    }
                }
            });

            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                    YouTubePlayerViewHolder.this.youTubePlayer = youTubePlayer;
                    if (currentVideoId != null) {
                        youTubePlayer.cueVideo(currentVideoId.getIdvideo(), 0f);
                    }
                }

                @Override
                public void onStateChange(com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
                    if (state == PlayerConstants.PlayerState.VIDEO_CUED) {
                        overlayView.setVisibility(View.VISIBLE);
                    } else {
                        overlayView.setVisibility(View.GONE);
                    }
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
                            setVideoChangue(currentVideoId.getIdvideo());
                            System.out.println("PLAYING");
                            break;
                        case PAUSED:
                            System.out.println("PAUSED");
                            break;
                        case BUFFERING:
                            setVideoChangue(currentVideoId.getIdvideo());
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
        }

        public void cueVideo(YTVideo videoId) {
            currentVideoId = videoId;
            if (youTubePlayer != null) {
                youTubePlayer.cueVideo(videoId.getIdvideo(), 0f);
            }
        }

    }
}
