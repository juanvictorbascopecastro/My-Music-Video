package com.youtube.musica;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;

import com.youtube.musica.databinding.ActivityPlayerFullscreenBinding;
import com.youtube.musica.dialog.CtgSelectionDialog;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.interfaces.DbMusicListener;
import com.youtube.musica.interfaces.PlayerListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.services.NotificationHelper;
import com.youtube.musica.services.PlayerEventBroadcaster;

public class PlayerFullscreenActivity extends AppCompatActivity implements DbMusicListener, PlayerListener {
    private ActivityPlayerFullscreenBinding binding;
    private YouTubePlayerView youTubePlayerView;

    private ArrayList<MusicCollection> list;
    private ArrayList<CategoryCollection> list_ctg;
    private int indexPlayer;
    private float minutesPlayer = 0;
    private MusicCollection currentYouTube;
    private Music musicDataBase;
    private boolean isFullScreem = false;

    ///////////
    private MediaRouter mRouter;
    private MediaRouter.Callback mCallback;
    private MediaRouteSelector mSelector;
    ////////////
    private NotificationHelper notificationHelper;
    private PlayerEventBroadcaster eventBroadcaster;
    private boolean isPlaying = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityPlayerFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        youTubePlayerView = binding.youtubePlayerView;
        getLifecycle().addObserver(youTubePlayerView);

        musicDataBase = new Music(PlayerFullscreenActivity.this, this);

        mRouter = MediaRouter.getInstance(this);
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();
        mCallback = new MyCallback();

        notificationHelper = new NotificationHelper(this);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            // currentYouTube = (YTVideo) bundle.getSerializable("music");
            minutesPlayer = bundle.getFloat("minuto");
            indexPlayer = bundle.getInt("position");
            list = (ArrayList<MusicCollection>) bundle.getSerializable("list");
            list_ctg = (ArrayList<CategoryCollection>) bundle.getSerializable("categorias");

            binding.btnPrevious.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.GONE);


            if (list_ctg != null) {
                if (!list_ctg.isEmpty()) binding.btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();
            initYouTubePlayerView();
        }

        binding.btnScreenRotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowButton();
                isFullScreem = !isFullScreem;
                if(!isFullScreem) {
                    // youTubePlayerView.exitFullScreen();
                    isFullScreem = false;
                    youTubePlayerView.matchParent();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //vertical
                    binding.btnScreenRotation.setImageResource(R.drawable.ic_screen_rotation);
                } else {
                    // youTubePlayerView.enterFullScreen();
                    isFullScreem = true;
                    youTubePlayerView.wrapContent();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // horizontal
                    binding.btnScreenRotation.setImageResource(R.drawable.ic_celphone);
                }
            }
        });

        binding.btnSelect.setOnClickListener(view -> {
            SelectCtg();
        });
        youTubePlayerView.enableBackgroundPlayback(true);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("minuto", minutesPlayer);
                resultIntent.putExtra("position", indexPlayer);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Actualizar el intent de la actividad

        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            minutesPlayer = bundle.getFloat("minuto");
            indexPlayer = bundle.getInt("position");
            
            if (bundle.containsKey("list")) {
                list = (ArrayList<MusicCollection>) bundle.getSerializable("list");
            }
            if (bundle.containsKey("categorias")) {
                list_ctg = (ArrayList<CategoryCollection>) bundle.getSerializable("categorias");
            }

            if (list_ctg != null) {
                if (!list_ctg.isEmpty()) binding.btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();

            // Si el reproductor ya está listo, cargar el nuevo video instantáneamente
            if (ytPlayer != null && list != null && !list.isEmpty()) {
                currentYouTube = list.get(indexPlayer);
                YouTubePlayerUtils.loadOrCueVideo(
                        ytPlayer,
                        getLifecycle(),
                        currentYouTube.getIdvideo(),
                        minutesPlayer
                );
            }
        }
    }

    private void initYouTubePlayerView() {
        // iniciar picture para reproducir
        initPictureInPicture();
        // Reiniciar el broadcast en segundo plano
        eventBroadcaster = new PlayerEventBroadcaster(PlayerFullscreenActivity.this, this);
        showNotification();

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                ytPlayer = youTubePlayer;
                currentYouTube = list.get(indexPlayer);
                setPlayNextVideoButtonClickListener(youTubePlayer);

                YouTubePlayerUtils.loadOrCueVideo(
                        youTubePlayer,
                        getLifecycle(),
                        currentYouTube.getIdvideo(),
                        minutesPlayer
                );
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                playerStateToString(youTubePlayer, state);
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                NextVideo(youTubePlayer);
            }
            @Override
            public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
                super.onCurrentSecond(youTubePlayer, second);
                minutesPlayer = second;
            }

        });
    }

    private String playerStateToString(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
        switch (state) {
            case UNKNOWN:
                return "UNKNOWN";
            case UNSTARTED:
                return "UNSTARTED";
            case ENDED:
                NextVideo(youTubePlayer);
                return "ENDED";
            case PLAYING:
                isPlaying = true;
                ShowButton();
                showNotification();
                return "PLAYING";
            case PAUSED:
                isPlaying = false;
                showNotification();
                return "PAUSED";
            case BUFFERING:
                return "BUFFERING";
            case VIDEO_CUED:
                return "VIDEO_CUED";
            default:
                return "status unknown";
        }
    }

    private void setPlayNextVideoButtonClickListener(final YouTubePlayer youTubePlayer) {
        binding.btnNext.setOnClickListener(view -> {
            NextVideo(youTubePlayer);
        });

        binding.btnPrevious.setOnClickListener(view -> {
            PreviousVideo(youTubePlayer);
        });
    }

    private YouTubePlayer ytPlayer;
    private void NextVideo (YouTubePlayer youTubePlayer) {
        this.ytPlayer = youTubePlayer;
        indexPlayer += 1;
        disabledButton();
        currentYouTube = list.get(indexPlayer);
        YouTubePlayerUtils.loadOrCueVideo(
                youTubePlayer, getLifecycle(),
                currentYouTube.getIdvideo(), 0f
        );
        disabledButton();
    }

    private void PreviousVideo (YouTubePlayer youTubePlayer) {
        this.ytPlayer = youTubePlayer;
        indexPlayer -= 1;
        disabledButton();
        currentYouTube = list.get(indexPlayer);
        YouTubePlayerUtils.loadOrCueVideo(
                youTubePlayer, getLifecycle(),
                currentYouTube.getIdvideo(), 0f
        );
        disabledButton();
    }

    private void disabledButton(){
        if(list.isEmpty() || list.size() == 1){
            binding.btnNext.setVisibility(View.GONE);
            binding.btnPrevious.setVisibility(View.GONE);
        }else if(indexPlayer == list.size()-1){
            binding.btnNext.setVisibility(View.GONE);
            binding.btnPrevious.setVisibility(View.VISIBLE);
        }else if(indexPlayer == 0){
            binding.btnPrevious.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.VISIBLE);
        }else{
            binding.btnPrevious.setVisibility(View.VISIBLE);
            binding.btnNext.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onUserInteraction() {
        ShowButton();
        super.onUserInteraction();
    }
    public void onStart() {
        super.onStart();
        mRouter.addCallback(mSelector, mCallback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        MediaRouter.RouteInfo route = mRouter.updateSelectedRoute(mSelector);
    }

    public void onStop() {
        mRouter.addCallback(mSelector, mCallback, /* flags= */ 0);
        super.onStop();
    }
    public void onDestroy() {
        mRouter.removeCallback(mCallback);
        super.onDestroy();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
            unregisterReceiver(eventBroadcaster.playbackReceiver);
        }
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // if(ytPlayer != null) ytPlayer.play();
    }
    private final class MyCallback extends MediaRouter.Callback {
        // Implement callback methods as needed.
    }
    private static class ButtonHideHandler extends Handler {
        private final java.lang.ref.WeakReference<PlayerFullscreenActivity> activityRef;

        ButtonHideHandler(PlayerFullscreenActivity activity) {
            super(Looper.getMainLooper());
            this.activityRef = new java.lang.ref.WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            PlayerFullscreenActivity activity = activityRef.get();
            if (activity != null) {
                activity.binding.btnNext.setVisibility(View.GONE);
                activity.binding.btnPrevious.setVisibility(View.GONE);
                activity.binding.btnSelect.setVisibility(View.GONE);
                activity.binding.btnScreenRotation.setVisibility(View.GONE);
                activity.binding.btnPicture.setVisibility(View.GONE);
            }
        }
    }

    private Handler handler = new ButtonHideHandler(this);

    private void ShowButton(){
        binding.btnScreenRotation.setVisibility(View.VISIBLE);
        disabledButton();
        binding.btnSelect.setVisibility(View.VISIBLE);
        binding.btnPicture.setVisibility(View.VISIBLE);
        
        handler.removeCallbacksAndMessages(null);
        handler.sendMessageDelayed(new Message(), 3000);
    }

    // Lógica para manejar cambios en la configuración (orientación de la pantalla)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) { //horizontal
            //ponemos pantalla completa
            isFullScreem = true;
            youTubePlayerView.wrapContent();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){ //vertical
            //quitamos pantalla completa
            isFullScreem = false;
            youTubePlayerView.matchParent();
        }
    }
    // Lógica para ocultar la barra de navegación, osea los botones de acciones
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideNavigationBar();
        }
    }
    
    private void hideNavigationBar() {
        View decorView = this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
    @Override
    public void loadedMusicPlaylist(ArrayList<MusicCollection> playList) {
        indexPlayer = 0;
        minutesPlayer = 0;
        list = playList;
        if(!list.isEmpty()) {
            YouTubePlayerUtils.loadOrCueVideo(
                    ytPlayer, getLifecycle(),
                    list.get(indexPlayer).getIdvideo(), 0f
            );
        }
    }
    @Override
    public void errorLoadedMusicPlayList(String message, int icon) {
        Toast.makeText(PlayerFullscreenActivity.this, message, Toast.LENGTH_LONG).show();
    }
    private void SelectCtg() {
        CtgSelectionDialog.show(PlayerFullscreenActivity.this, list_ctg, new CtgSelectionDialog.OnCtgSelectedListener() {
            @Override
            public void onCtgSelected(String ctgCode) {
                musicDataBase.LoadByCtg(ctgCode, true);
            }
        });
    }


    // REPRODUCTOR EN SEGUNDO PLANO
    private void showNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, PlayerFullscreenActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        PendingIntent previousIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_PREVIOUS),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        PendingIntent playPauseIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        PendingIntent nextIntent = PendingIntent.getBroadcast(
                this,
                0,
                new Intent(PlayerEventBroadcaster.ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        boolean isNextActive, isPreviewActive;
        if(list.isEmpty() || list.size() == 1){
            isNextActive = false;
            isPreviewActive = false;
        }else if(indexPlayer == list.size()-1){
            isNextActive = false;
            isPreviewActive = true;
        }else if(indexPlayer == 0){
            isNextActive = true;
            isPreviewActive = false;
        }else{
            isNextActive = true;
            isPreviewActive = true;
        }
        notificationHelper.showNotification(list.get(indexPlayer).getName(), list.get(indexPlayer).getIdvideo(),
                contentIntent, previousIntent, playPauseIntent, nextIntent, isPlaying, isNextActive, isPreviewActive);
    }

    @Override
    public void onPlayPauseNotification() {
        if(ytPlayer != null) {
            if(isPlaying) ytPlayer.pause();
            else ytPlayer.play();
        }
    }

    @Override
    public void onNextNotification() {
        if(ytPlayer != null) NextVideo(ytPlayer);
    }

    @Override
    public void onPreviewNotification() {
        if(ytPlayer != null) PreviousVideo(ytPlayer);
    }

    // INIT PICTURE
    private void initPictureInPicture() {
        binding.btnPicture.setOnClickListener(view -> {
            enterPipMode();
        });

        // Configurar transición automática a PiP para Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.PictureInPictureParams.Builder pipBuilder = new android.app.PictureInPictureParams.Builder();
            pipBuilder.setAutoEnterEnabled(true);
            setPictureInPictureParams(pipBuilder.build());
        }
    }

    private void enterPipMode() {
        boolean supportsPIP = getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
        if (supportsPIP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.PictureInPictureParams.Builder pipBuilder = new android.app.PictureInPictureParams.Builder();
            // Aspect ratio 16:9, ideal para videos de YouTube
            pipBuilder.setAspectRatio(new android.util.Rational(16, 9));
            enterPictureInPictureMode(pipBuilder.build());
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Transición a PiP al presionar el botón Home para Android 8 a 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (isPlaying) { 
                enterPipMode();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }

        if (isInPictureInPictureMode) {
            // Ocultar por completo la interfaz (botones) en modo PiP
            binding.btnNext.setVisibility(View.GONE);
            binding.btnPrevious.setVisibility(View.GONE);
            binding.btnSelect.setVisibility(View.GONE);
            binding.btnScreenRotation.setVisibility(View.GONE);
            binding.btnPicture.setVisibility(View.GONE);
            youTubePlayerView.matchParent();
        } else {
            // Restaurar botones y estado normal al volver a la app
            ShowButton();
            if (!isFullScreem) {
                youTubePlayerView.matchParent();
            } else {
                youTubePlayerView.wrapContent();
            }
        }
    }
}