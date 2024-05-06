package app.list.mymusic;

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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import app.list.mymusic.databinding.ActivityPlayerFullscreenBinding;
import app.list.mymusic.dialog.CtgSelectionDialog;
import app.list.mymusic.firebase.MusicDataBase;
import app.list.mymusic.interfaces.DbMusicListener;
import app.list.mymusic.interfaces.PlayerListener;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;
import app.list.mymusic.services.NotificationHelper;
import app.list.mymusic.services.PlayerEventBroadcaster;

public class PlayerFullscreenActivity extends AppCompatActivity implements DbMusicListener, PlayerListener {
    private ActivityPlayerFullscreenBinding binding;
    private YouTubePlayerView youTubePlayerView;

    private ArrayList<YTVideo> list;
    private ArrayList<CtgMusic> list_ctg;
    private int indexPlayer;
    private float minutesPlayer = 0;
    private YTVideo currentYouTube;
    private MusicDataBase musicDataBase;
    private boolean isFullScreem = false;

    ///////////
    private MediaRouter mRouter;
    private MediaRouter.Callback mCallback;
    private MediaRouteSelector mSelector;

    private NotificationHelper notificationHelper;
    private PlayerEventBroadcaster eventBroadcaster;
    private boolean isPlaying = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityPlayerFullscreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        musicDataBase = new MusicDataBase(PlayerFullscreenActivity.this, this);

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
            list = (ArrayList<YTVideo>) bundle.getSerializable("list");
            list_ctg = (ArrayList<CtgMusic>) bundle.getSerializable("categorias");

            binding.btnPrevious.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.GONE);


            if (list_ctg != null) {
                if (list_ctg.size() > 0) binding.btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();
            initYouTubePlayerView();
            youTubePlayerView.enableBackgroundPlayback(true);
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
    }

    private void initYouTubePlayerView() {
        youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

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
                return "PLAYING";
            case PAUSED:
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
        if(list.size() == 0 || list.size() == 1){
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
        // cancelNotification();
    }
    private final class MyCallback extends MediaRouter.Callback {
        // Implement callback methods as needed.
    }
    private boolean interrumpido_botones = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Thread thread;
    private void ShowButton(){
        binding.btnScreenRotation.setVisibility(View.VISIBLE);
        disabledButton();
        binding.btnSelect.setVisibility(View.VISIBLE);
        binding.btnPicture.setVisibility(View.VISIBLE);
        if(thread != null){
            thread.interrupt();
            interrumpido_botones = true;
        }
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if(!interrumpido_botones){
                    Bundle bundle = msg.getData();
                    binding.btnNext.setVisibility(View.GONE);
                    binding.btnPrevious.setVisibility(View.GONE);
                    binding.btnSelect.setVisibility(View.GONE);
                    binding.btnScreenRotation.setVisibility(View.GONE);
                    binding.btnPicture.setVisibility(View.GONE);
                    thread = null;
                }else{
                    interrumpido_botones = false;
                }
            }
        };
        thread = new Thread(new MiHilo());
        thread.start();
    }
    class MiHilo implements Runnable{
        @Override
        public void run() {
            Bundle bundle = new Bundle();
            bundle.putString("msj", "CALL");
            try{
                Thread.sleep(3000);
            }catch (InterruptedException e){}
            handler.sendMessage(new Message());
        }
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
        hideNavigationBar();
    }
    private void hideNavigationBar() {
        View decorView = this.getWindow().getDecorView();
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //
                                | View.SYSTEM_UI_FLAG_FULLSCREEN//
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION //
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                //| View.SYSTEM_UI_FLAG_IMMERSIVE //mas corto
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // mas largo
                        );
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(task, 1, 2);
    }
    @Override
    public void loadedMusicPlaylist(ArrayList<YTVideo> playList) {
        indexPlayer = 0;
        minutesPlayer = 0;
        list = playList;
        if(list.size() > 0) {
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
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("minuto", minutesPlayer);
        resultIntent.putExtra("position", indexPlayer);
        setResult(Activity.RESULT_OK, resultIntent); // Establecer el resultado como OK
        finish();
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
        if(list.size() == 0 || list.size() == 1){
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
        notificationHelper.showNotification(list.get(indexPlayer).getName(),
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                boolean supportsPIP = getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
                if (supportsPIP)
                    enterPictureInPictureMode();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("No se puede introducir la imagen en el modo de imagen")
                        .setMessage("Para ingresar a la imagen en modo de imagen, necesita una versión del SDK >= N.")
                        .show();
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }

        if (isInPictureInPictureMode) {
            youTubePlayerView.matchParent();
        } else {
            youTubePlayerView.wrapContent();
        }
    }
}