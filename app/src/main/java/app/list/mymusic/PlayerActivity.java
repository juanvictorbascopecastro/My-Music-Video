package app.list.mymusic;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.os.Bundle;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import app.list.mymusic.dialog.progress;
import app.list.mymusic.firebase.MusicDb;
import app.list.mymusic.interfaces.PlayerListener;
import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;
import app.list.mymusic.services.NotificationHelper;
import app.list.mymusic.services.PlayerEventBroadcaster;


public class PlayerActivity extends AppCompatActivity implements PlayerListener {

    private YTVideo youTube;
    private boolean isFullScreem = false;
    private float contador_minuto = 0;

    private YouTubePlayerView youTubePlayerView;

    private ArrayList<YTVideo> list;
    private ArrayList<CtgMusic> list_ctg;
    private int contador;
    private ImageView btnPrevious, btnNext, btnSelect, btnScreenRotation;

    private MediaRouter mRouter;
    private MediaRouter.Callback mCallback;
    private MediaRouteSelector mSelector;
    private MusicDb db;

    private NotificationHelper notificationHelper;
    private PlayerEventBroadcaster eventBroadcaster;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        youTubePlayerView = findViewById(R.id.youtube_player_view);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnSelect = findViewById(R.id.btnSelect);
        btnScreenRotation = findViewById(R.id.btnScreenRotation);

        notificationHelper = new NotificationHelper(this);
        db = new MusicDb();

        mRouter = MediaRouter.getInstance(this);
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();
        mCallback = new MyCallback();

        contador = 0;
        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            youTube = (YTVideo) bundle.getSerializable("music");
            contador_minuto = bundle.getFloat("minuto");
            contador = bundle.getInt("position");
            list = (ArrayList<YTVideo>) bundle.getSerializable("list");
            list_ctg = (ArrayList<CtgMusic>) bundle.getSerializable("categorias");
            if(list != null){
                LoadReproductor();
            }else{
                btnPrevious.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
                LoadReproductor();
            }

            if(list_ctg != null){
                if(list_ctg.size() > 0) btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();
            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextPlayer();
                }
            });
            btnPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    previewPlayer();
                }
            });
            btnSelect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowButton();
                    SelectCtg();
                }
            });
            btnScreenRotation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowButton();
                    isFullScreem = !isFullScreem;
                    if(!isFullScreem) {
                        // youTubePlayerView.exitFullScreen();
                        isFullScreem = false;
                        youTubePlayerView.matchParent();
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //vertical
                        btnScreenRotation.setImageResource(R.drawable.ic_screen_rotation);
                    } else {
                        // youTubePlayerView.enterFullScreen();
                        isFullScreem = true;
                        youTubePlayerView.wrapContent();
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // horizontal
                        btnScreenRotation.setImageResource(R.drawable.ic_celphone);
                    }
                }
            });
        }
        youTubePlayerView.enableBackgroundPlayback(true);
    }
    private void showNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, PlayerActivity.class),
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
        }else if(contador == list.size()-1){
            isNextActive = false;
            isPreviewActive = true;
        }else if(contador == 0){
            isNextActive = true;
            isPreviewActive = false;
        }else{
            isNextActive = true;
            isPreviewActive = true;
        }
        notificationHelper.showNotification(list.get(contador).getName(),
                contentIntent, previousIntent, playPauseIntent, nextIntent, isPlaying, isNextActive, isPreviewActive);
    }

    private void cancelNotification() {
        notificationHelper.cancelNotification();
    }
    @Override
    public void onUserInteraction() {
        ShowButton();
        super.onUserInteraction();
    }

    private boolean interrumpido_botones = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Thread thread;
    private void ShowButton(){
        btnScreenRotation.setVisibility(View.VISIBLE);
        disabledButton();
        btnSelect.setVisibility(View.VISIBLE);
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
                    btnNext.setVisibility(View.GONE);
                    btnPrevious.setVisibility(View.GONE);
                    btnSelect.setVisibility(View.GONE);
                    btnScreenRotation.setVisibility(View.GONE);
                    thread = null;
                }else{
                    interrumpido_botones = false;
                }
            }
        };
        thread = new Thread(new MiHilo());
        thread.start();
    }

    @Override
    public void onPlayPauseNotification() {
        if(player != null) {
            if(isPlaying) player.pause();
            else player.play();
        }
    }

    @Override
    public void onNextNotification() {
        nextPlayer();
    }

    @Override
    public void onPreviewNotification() {
        previewPlayer();
    }
    public void nextPlayer(){
        ShowButton();
        if(player != null){
            contador++;
            player.cueVideo(list.get(contador).getIdvideo(), 0);
            player.play();
            disabledButton();
        }
        // showNotification();
    }
    public void previewPlayer(){
        ShowButton();
        if(player != null){
            contador--;
            player.cueVideo(list.get(contador).getIdvideo(), 0);
            player.play();
            disabledButton();
        }
        // showNotification();
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
    // Lógica para ocultar la barra de navegación
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
                PlayerActivity.this.runOnUiThread(new Runnable() {
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
    // Add the callback on start to tell the media router what kinds of routes
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

    @Override
    protected void onResume() {
        super.onResume();
        // if(player != null) player.play();
    }

    @Override
    public void onBackPressed() { // al llamar a este evento enviamos en que parte del video se quedo
        Intent intent = new Intent();
        intent.putExtra("minuto", contador_minuto);
        intent.putExtra("position", contador);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.sample_media_router_menu, menu);
        return true;
    }

    private final class MyCallback extends MediaRouter.Callback {
        // Implement callback methods as needed.
    }

    private YouTubePlayer player;
    private void LoadReproductor(){
        eventBroadcaster = new PlayerEventBroadcaster(PlayerActivity.this, this);
        showNotification();
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(YouTubePlayer youTubePlayer) {
                //super.onReady(youTubePlayer);
                youTubePlayer.cueVideo(list.get(contador).getIdvideo(), contador_minuto);
                //youTubePlayer.play();
                player = youTubePlayer;
            }

            @Override
            public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
                //super.onStateChange(youTubePlayer, state);
                switch (state) {
                    case UNKNOWN:
                        System.out.println("UNKNOWN");
                        break;
                    case UNSTARTED:
                        System.out.println("UNSTARTED");
                        break;
                    case ENDED:
                        System.out.println("ENDED");
                        ChangueMusic(youTubePlayer);
                        break;
                    case PLAYING:
                        isPlaying = true;
                        ShowButton();
                        showNotification();
                        System.out.println("PLAYING");
                        break;
                    case PAUSED:
                        isPlaying = false;
                        showNotification();
                        System.out.println("PAUSED");
                        break;
                    case BUFFERING:
                        System.out.println("BUFFERING");
                        break;
                    case VIDEO_CUED:
                        System.out.println("VIDEO_CUED");
                        youTubePlayer.play();
                        break;
                    default:
                        System.out.println("status unknown");
                        break;
                }
            }

            @Override
            public void onApiChange(YouTubePlayer youTubePlayer) {
                super.onApiChange(youTubePlayer);
            }

            @Override
            public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
                super.onCurrentSecond(youTubePlayer, second);
                contador_minuto = second;
            }

            @Override
            public void onError(YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error) {
                //super.onError(youTubePlayer, error);
                ChangueMusic(youTubePlayer);
            }

            @Override
            public void onPlaybackQualityChange(YouTubePlayer youTubePlayer, PlayerConstants.PlaybackQuality playbackQuality) {
                super.onPlaybackQualityChange(youTubePlayer, playbackQuality);
            }

            @Override
            public void onPlaybackRateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlaybackRate playbackRate) {
                super.onPlaybackRateChange(youTubePlayer, playbackRate);
            }

            @Override
            public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
                super.onVideoDuration(youTubePlayer, duration);
            }

            @Override
            public void onVideoId(YouTubePlayer youTubePlayer, String videoId) {
                super.onVideoId(youTubePlayer, videoId);
            }

            @Override
            public void onVideoLoadedFraction(YouTubePlayer youTubePlayer, float loadedFraction) {
                super.onVideoLoadedFraction(youTubePlayer, loadedFraction);
            }
        });
    }

    private void ChangueMusic(YouTubePlayer youTubePlayer){
        contador++;
        if(contador >= list.size()){
            contador = 0;
        }
        youTubePlayer.cueVideo(list.get(contador).getIdvideo(), 0);
        //youTubePlayer.play();
    }
    private void SelectCtg(){
        String [] ctgs = new String[list_ctg.size()];
        for(int i = 0; i < list_ctg.size(); i++){
            ctgs[i] = list_ctg.get(i).getName();
        }
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(PlayerActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View convertView = (View) inflater.inflate(R.layout.list, null);
        alertDialog.setView(convertView);
        alertDialog.setTitle(getString(R.string.select_ctg));
        ListView lv = (ListView) convertView.findViewById(R.id.listView1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(PlayerActivity.this, R.layout.text_folder, R.id.txt, ctgs);
        lv.setAdapter(adapter);
        alertDialog.setCancelable(true);
        AlertDialog alert = alertDialog.create();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                alert.dismiss();
                LoadMusic(list_ctg.get(position).getCode());
            }
        });
        alert.show();
    }
    private void disabledButton(){
        if(list.size() == 0 || list.size() == 1){
            btnNext.setVisibility(View.GONE);
            btnPrevious.setVisibility(View.GONE);
        }else if(contador == list.size()-1){
            btnNext.setVisibility(View.GONE);
            btnPrevious.setVisibility(View.VISIBLE);
        }else if(contador == 0){
            btnPrevious.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
        }else{
            btnPrevious.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.VISIBLE);
        }
    }

    public void LoadMusic(String ctgCode){
        progress.run(getString(R.string.load), PlayerActivity.this);
        list = new ArrayList<>();
        db.loadMusic(ctgCode).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                progress.diss();
                if(task.isSuccessful()) {
                    YTVideo ytVideo;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ytVideo = snapshot.toObject(YTVideo.class);
                        ytVideo.setCode(snapshot.getId());
                        list.add(ytVideo);
                    }
                    contador = 0;
                    contador_minuto = 0;
                    player.cueVideo(list.get(contador).getIdvideo(), contador_minuto);
                    player.play();
                }
                // musicViewModel.setList(list);
            }
        });
    }
}