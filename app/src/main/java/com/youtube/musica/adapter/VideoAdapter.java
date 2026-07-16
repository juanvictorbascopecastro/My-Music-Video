package com.youtube.musica.adapter;

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

import com.youtube.musica.R;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.models.MusicCollection;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.YouTubePlayerViewHolder> {

    private final ArrayList<MusicCollection> videoIds;
    private final Lifecycle lifecycle;
    public MusicListener listener;

    private YouTubePlayer activePlayer = null;
    private boolean isCurrentlyPlaying = false;
    private java.util.Map<Integer, YouTubePlayer> players = new java.util.HashMap<>();
    private int pendingPlayPosition = -1;

    public void playItem(int position) {
        YouTubePlayer player = players.get(position);
        if (player != null) {
            // The view might be recycled and holding another player, but let's assume it's
            // correct if we just smoothScrolled.
            // Actually it's safer to just set pendingPlayPosition and let the adapter
            // handle it via notifyItemChanged or scroll.
            pendingPlayPosition = position;
            notifyItemChanged(position); // This will trigger onBindViewHolder and call cueVideo/loadVideo
        } else {
            pendingPlayPosition = position;
        }
    }

    public void togglePlayPause() {
        if (activePlayer != null) {
            if (isCurrentlyPlaying) {
                activePlayer.pause();
            } else {
                activePlayer.play();
            }
        } else {
            YouTubePlayer first = players.get(0);
            if (first != null) {
                first.play();
            }
        }
    }

    public void pausePlayer() {
        if (activePlayer != null && isCurrentlyPlaying) {
            activePlayer.pause();
        }
    }

    public VideoAdapter(ArrayList<MusicCollection> videoIds, Lifecycle lifecycle, MusicListener listener) {
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

    public void setVideoChangue(String id) {
        for (int i = 0; i < videoIds.size(); i++) {
            if (videoIds.get(i).getIdvideo().equals(id)) {
                listener.onSetPosition(i);
                break;
            }
        }
    }

    /**
     * ViewHolder containing a YouTubePlayer. When the list is scrolled only the
     * video id changes.
     */
    public class YouTubePlayerViewHolder extends RecyclerView.ViewHolder {
        private YouTubePlayer youTubePlayer;
        private MusicCollection currentVideoId;
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
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onVideoClicked(pos);
                    }
                }
            });

            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(
                        com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer) {
                    YouTubePlayerViewHolder.this.youTubePlayer = youTubePlayer;
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        players.put(pos, youTubePlayer);
                    }
                    if (currentVideoId != null) {
                        if (pendingPlayPosition == pos) {
                            youTubePlayer.loadVideo(currentVideoId.getIdvideo(), 0f);
                            pendingPlayPosition = -1;
                        } else {
                            youTubePlayer.cueVideo(currentVideoId.getIdvideo(), 0f);
                        }
                    }
                }

                @Override
                public void onStateChange(
                        com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer youTubePlayer,
                        PlayerConstants.PlayerState state) {
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
                            listener.onStateChanged("UNSTARTED");
                            System.out.println("UNSTARTED");
                            break;
                        case ENDED:
                            isCurrentlyPlaying = false;
                            listener.onStateChanged("ENDED");
                            System.out.println("ENDED");
                            break;
                        case PLAYING:
                            activePlayer = youTubePlayer;
                            isCurrentlyPlaying = true;
                            listener.onStateChanged("PLAYING");
                            setVideoChangue(currentVideoId.getIdvideo());
                            System.out.println("PLAYING");
                            break;
                        case PAUSED:
                            isCurrentlyPlaying = false;
                            listener.onStateChanged("PAUSED");
                            System.out.println("PAUSED");
                            break;
                        case BUFFERING:
                            listener.onStateChanged("BUFFERING");
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
                public void onPlaybackQualityChange(@NotNull YouTubePlayer youTubePlayer,
                        PlayerConstants.PlaybackQuality playbackQuality) {
                    super.onPlaybackQualityChange(youTubePlayer, playbackQuality);
                }

                @Override
                public void onPlaybackRateChange(@NotNull YouTubePlayer youTubePlayer,
                        PlayerConstants.PlaybackRate playbackRate) {
                    super.onPlaybackRateChange(youTubePlayer, playbackRate);
                }

                @Override
                public void onVideoDuration(@NotNull YouTubePlayer youTubePlayer, float duration) {
                    super.onVideoDuration(youTubePlayer, duration);
                }

                @Override
                public void onVideoId(@NotNull YouTubePlayer youTubePlayer, @NotNull String videoId) {
                    super.onVideoId(youTubePlayer, videoId);
                    System.out.println("ID VIDEO: " + videoId);
                }

                @Override
                public void onVideoLoadedFraction(@NotNull YouTubePlayer youTubePlayer, float loadedFraction) {
                    super.onVideoLoadedFraction(youTubePlayer, loadedFraction);
                }
            });
        }

        public void cueVideo(MusicCollection videoId) {
            currentVideoId = videoId;
            if (youTubePlayer != null) {
                int pos = getAdapterPosition();
                if (pendingPlayPosition == pos) {
                    youTubePlayer.loadVideo(videoId.getIdvideo(), 0f);
                    pendingPlayPosition = -1;
                } else {
                    youTubePlayer.cueVideo(videoId.getIdvideo(), 0f);
                }
            }
        }

    }
}
