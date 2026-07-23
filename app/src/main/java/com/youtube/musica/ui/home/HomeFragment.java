package com.youtube.musica.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.youtube.musica.adapter.VideoAdapter;
import com.youtube.musica.databinding.FragmentHomeBinding;
import com.youtube.musica.interfaces.MusicListener;
import com.youtube.musica.interfaces.PlayerListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.MainActivity;
import com.youtube.musica.services.NotificationHelper;
import com.youtube.musica.services.PlayerEventBroadcaster;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.youtube.musica.dialog.AddMusic;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.ui.ctg.CtgViewModel;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements MusicListener, PlayerListener {

    private FragmentHomeBinding binding;
    private VideoAdapter adapter;
    private ArrayList<MusicCollection> musicList = new ArrayList<>();
    private ExecutorService executorService;
    private Handler mainHandler;

    private YouTubePlayer globalPlayer;
    private int currentPlayingPosition = -1;
    
    private NotificationHelper notificationHelper;
    // eventBroadcaster is managed by Singleton
    private boolean isPlaying = false;
    private boolean isUserIntentionallyPaused = false;
    private boolean isAppVisible = false;
    private boolean isLoadingMore = false;
    private float currentMinutesPlayer = 0f;
    
    private CtgViewModel ctgViewModel;
    private Category dbCategory;
    private ArrayList<CategoryCollection> ctgList;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        isAppVisible = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isAppVisible = false;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        notificationHelper = new NotificationHelper(requireContext());

        setupRecyclerView();
        setupSearchView();
        setupGlobalPlayer();

        ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);
        dbCategory = new Category();
        ctgList = new ArrayList<>();
        LoadCtg();

        // Cargar canciones "Trending" por defecto
        loadTrending();
    }

    private void setupGlobalPlayer() {
        getViewLifecycleOwner().getLifecycle().addObserver(binding.globalYoutubePlayerView);
        binding.globalYoutubePlayerView.setEnableAutomaticInitialization(false);
        binding.globalYoutubePlayerView.enableBackgroundPlayback(true);
        PlayerEventBroadcaster.getInstance().register(requireContext(), this);
        
        IFramePlayerOptions options = new IFramePlayerOptions.Builder(requireContext())
                .controls(1)
                .rel(0)
                .ivLoadPolicy(3)
                .ccLoadPolicy(0)
                .fullscreen(1)
                .build();

        binding.globalYoutubePlayerView.addFullscreenListener(new com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener() {
            @Override
            public void onEnterFullscreen(@NonNull View fullscreenView, @NonNull kotlin.jvm.functions.Function0<kotlin.Unit> exitFullscreen) {
                exitFullscreen.invoke(); // Evitar el fullscreen interno
                
                if (globalPlayer != null) {
                    isUserIntentionallyPaused = true;
                    globalPlayer.pause();
                }

                if (currentPlayingPosition != -1 && !musicList.isEmpty()) {
                    android.content.Intent intent = new android.content.Intent(requireContext(), com.youtube.musica.PlayerFullscreenActivity.class);
                    intent.putExtra("position", currentPlayingPosition);
                    intent.putExtra("minuto", currentMinutesPlayer);
                    intent.putExtra("list", musicList);
                    if (ctgViewModel != null && ctgViewModel.getList().getValue() != null) {
                        intent.putExtra("categorias", ctgViewModel.getList().getValue());
                    } else {
                        intent.putExtra("categorias", new ArrayList<CategoryCollection>());
                    }
                    startActivityForResult(intent, 100);
                }
            }

            @Override
            public void onExitFullscreen() { }
        });

        binding.globalYoutubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                globalPlayer = youTubePlayer;
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    playNextVideo();
                } else if (state == PlayerConstants.PlayerState.PLAYING) {
                    PlayerEventBroadcaster.getInstance().register(requireContext(), HomeFragment.this);
                    isPlaying = true;
                    isUserIntentionallyPaused = false;
                    binding.recyclerViewHome.setVisibility(View.GONE);
                    if (currentPlayingPosition != -1 && !musicList.isEmpty()) {
                        isUserIntentionallyPaused = false;
                        showNotification();
                    }
                } else if (state == PlayerConstants.PlayerState.PAUSED) {
                    android.os.PowerManager pm = (android.os.PowerManager) requireContext().getSystemService(android.content.Context.POWER_SERVICE);
                    if (pm != null && !pm.isInteractive()) {
                        if (!isUserIntentionallyPaused) {
                            if (!isAppVisible) {
                                youTubePlayer.play();
                                return;
                            }
                        }
                    }
                    isPlaying = false;
                    isUserIntentionallyPaused = true;
                    showNotification();
                }
            }

            @Override
            public void onCurrentSecond(@NonNull YouTubePlayer youTubePlayer, float second) {
                currentMinutesPlayer = second;
            }
        }, options);
    }

    private void showNotification() {
        if (currentPlayingPosition != -1) {
            notificationHelper.showNotification(
                    MainActivity.class, 
                    musicList, 
                    currentPlayingPosition, 
                    isPlaying, 
                    0, 
                    false // showModeButton = false
            );
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            currentPlayingPosition = data.getIntExtra("position", currentPlayingPosition);
            currentMinutesPlayer = data.getFloatExtra("minuto", currentMinutesPlayer);
            
            if (currentPlayingPosition != -1 && !musicList.isEmpty() && currentPlayingPosition < musicList.size()) {
                MusicCollection selectedMusic = musicList.get(currentPlayingPosition);
                binding.miniPlayerContainer.setVisibility(View.VISIBLE);
                if (globalPlayer != null) {
                    isUserIntentionallyPaused = false;
                    globalPlayer.loadVideo(selectedMusic.getIdvideo(), currentMinutesPlayer);
                }
            }
        }
    }

    private void playNextVideo() {
        if (currentPlayingPosition != -1 && currentPlayingPosition < musicList.size() - 1) {
            currentPlayingPosition++;
            MusicCollection nextMusic = musicList.get(currentPlayingPosition);
            if (globalPlayer != null) {
                globalPlayer.loadVideo(nextMusic.getIdvideo(), 0f);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new VideoAdapter(musicList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        binding.recyclerViewHome.setLayoutManager(layoutManager);
        binding.recyclerViewHome.setAdapter(adapter);

        binding.recyclerViewHome.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && !isLoadingMore) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Cargar más cuando falten 5 elementos para llegar al final
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreRecommendations();
                    }
                }
            }
        });
    }

    private void loadMoreRecommendations() {
        if (musicList.isEmpty()) return;
        MusicCollection lastMusic = musicList.get(musicList.size() - 1);
        loadRecommendationsForVideo(lastMusic);
    }

    private void setupSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    performSearch(query.trim());
                    binding.searchView.clearFocus();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void loadTrending() {
        binding.progressBarHome.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                String defaultQuery = "Mejores canciones populares";
                SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.getSearchQHFactory().fromQuery(defaultQuery));
                List<StreamInfoItem> items = new ArrayList<>();
                
                for (org.schabi.newpipe.extractor.InfoItem item : searchInfo.getRelatedItems()) {
                    if (item instanceof StreamInfoItem) {
                        items.add((StreamInfoItem) item);
                    }
                }
                
                updateListOnMainThread(items);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorOnMainThread("Error cargando sugerencias: " + e.getMessage());
            }
        });
    }

    public void LoadCtg() {
        dbCategory.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()){
                    CategoryCollection ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CategoryCollection.class);
                        ctg.setCode(snapshot.getId());
                        ctgList.add(ctg);
                    }
                }
                ctgViewModel.setList(ctgList);
            }
        });
    }

    private void performSearch(String query) {
        binding.progressBarHome.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.getSearchQHFactory().fromQuery(query));
                List<StreamInfoItem> items = new ArrayList<>();
                
                for (org.schabi.newpipe.extractor.InfoItem item : searchInfo.getRelatedItems()) {
                    if (item instanceof StreamInfoItem) {
                        items.add((StreamInfoItem) item);
                    }
                }
                
                updateListOnMainThread(items);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorOnMainThread("Error realizando la búsqueda: " + e.getMessage());
            }
        });
    }

    private void updateListOnMainThread(List<StreamInfoItem> streamItems) {
        mainHandler.post(() -> {
            binding.progressBarHome.setVisibility(View.GONE);
            musicList.clear();
            
            for (StreamInfoItem item : streamItems) {
                MusicCollection music = new MusicCollection();
                music.setIdvideo(item.getUrl().replace("https://www.youtube.com/watch?v=", "").split("&")[0]);
                music.setName(item.getName());
                
                String thumbUrl = item.getThumbnailUrl();
                music.setUrl(thumbUrl);
                
                musicList.add(music);
            }
            
            adapter.notifyDataSetChanged();
            if (!musicList.isEmpty()) {
                binding.recyclerViewHome.postDelayed(() -> binding.recyclerViewHome.smoothScrollToPosition(0), 100);
            }
        });
    }

    private void showErrorOnMainThread(String message) {
        mainHandler.post(() -> {
            binding.progressBarHome.setVisibility(View.GONE);
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVideoClicked(int position) {
        currentPlayingPosition = position;
        MusicCollection selectedMusic = musicList.get(position);
        
        binding.miniPlayerContainer.setVisibility(View.VISIBLE);
        
        if (globalPlayer != null) {
            globalPlayer.loadVideo(selectedMusic.getIdvideo(), 0f);
        }
        
        // Cargar recomendaciones inteligentes basadas en este video
        loadRecommendationsForVideo(selectedMusic);
    }

    private void loadRecommendationsForVideo(MusicCollection sourceMusic) {
        if (isLoadingMore) return;
        isLoadingMore = true;
        binding.progressBarHome.setVisibility(View.VISIBLE);
        executorService.execute(() -> {
            try {
                String videoUrl = "https://www.youtube.com/watch?v=" + sourceMusic.getIdvideo();
                List<StreamInfoItem> recommendedItems = new ArrayList<>();
                
                try {
                    StreamInfo streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl);
                    for (org.schabi.newpipe.extractor.InfoItem item : streamInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            recommendedItems.add((StreamInfoItem) item);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String fallbackQuery = sourceMusic.getName();
                    SearchInfo searchInfo = SearchInfo.getInfo(ServiceList.YouTube, ServiceList.YouTube.getSearchQHFactory().fromQuery(fallbackQuery));
                    for (org.schabi.newpipe.extractor.InfoItem item : searchInfo.getRelatedItems()) {
                        if (item instanceof StreamInfoItem) {
                            StreamInfoItem streamItem = (StreamInfoItem) item;
                            if (!streamItem.getUrl().contains(sourceMusic.getIdvideo())) {
                                recommendedItems.add(streamItem);
                            }
                        }
                    }
                }
                
                appendRecommendationsOnMainThread(recommendedItems);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorOnMainThread("Error cargando recomendaciones: " + e.getMessage());
                mainHandler.post(() -> isLoadingMore = false);
            }
        });
    }

    private void appendRecommendationsOnMainThread(List<StreamInfoItem> streamItems) {
        mainHandler.post(() -> {
            binding.progressBarHome.setVisibility(View.GONE);
            int initialSize = musicList.size();
            
            for (StreamInfoItem item : streamItems) {
                String id = item.getUrl().replace("https://www.youtube.com/watch?v=", "").split("&")[0];
                
                // Evitar duplicados en la lista actual
                boolean exists = false;
                for (MusicCollection m : musicList) {
                    if (m.getIdvideo().equals(id)) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    MusicCollection music = new MusicCollection();
                    music.setIdvideo(id);
                    music.setName(item.getName());
                    music.setUrl(item.getThumbnailUrl());
                    musicList.add(music);
                }
            }
            
            int newItemsCount = musicList.size() - initialSize;
            if (newItemsCount > 0) {
                // Notificar inserción al final para mantener el scroll intacto
                adapter.notifyItemRangeInserted(initialSize, newItemsCount);
            }
            
            isLoadingMore = false;
            showNotification(); // Actualizar notificación
        });
    }

    @Override
    public void onVideoLongClicked(int position) {
        if (com.youtube.musica.utils.AuthUtils.isLoggedIn()) {
            MusicCollection item = musicList.get(position);
            String videoUrl = "https://www.youtube.com/watch?v=" + item.getIdvideo();
            new AddMusic(getContext(), ctgViewModel.getList().getValue(), videoUrl, item.getIdvideo(), item.getName());
        } else {
            com.youtube.musica.utils.AuthUtils.requireLogin(getContext());
        }
    }

    @Override
    public void onSetPosition(int position) { }

    @Override
    public void onNewRegister(String data) { }

    @Override
    public void onSecordPlayer(float second) { }

    @Override
    public void onDeletePosition(int position) { }

    @Override
    public void onSeletedCtg(CategoryCollection categoryCollection) { }

    @Override
    public void onStateChanged(String state) { }

    @Override
    public void onPlayPauseNotification() {
        if (globalPlayer != null) {
            if (isPlaying) {
                isUserIntentionallyPaused = true;
                globalPlayer.pause();
            } else {
                isUserIntentionallyPaused = false;
                globalPlayer.play();
            }
        }
    }

    @Override
    public void onNextNotification() {
        playNextVideo();
    }

    @Override
    public void onPreviewNotification() {
        if (currentPlayingPosition > 0) {
            currentPlayingPosition--;
            MusicCollection prevMusic = musicList.get(currentPlayingPosition);
            if (globalPlayer != null) {
                globalPlayer.loadVideo(prevMusic.getIdvideo(), 0f);
            }
        }
    }

    @Override
    public void onPlaybackModeNotification() {
        // Ignorado
    }

    @Override
    public void onLostActiveFocus() {
        if (globalPlayer != null) {
            globalPlayer.pause();
        }
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (executorService != null) {
            executorService.shutdown();
        }
        PlayerEventBroadcaster.getInstance().unregisterListener(this);
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }
    }
}