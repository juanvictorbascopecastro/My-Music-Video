package app.list.mymusic;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import app.list.mymusic.models.CtgMusic;
import app.list.mymusic.models.YTVideo;


public class PlayerActivity extends AppCompatActivity {

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
    private boolean es_ctg_privada = false;

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
        //youTubePlayerView.enterFullScreen();

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
            es_ctg_privada = bundle.getBoolean("privada");
            list = (ArrayList<YTVideo>) bundle.getSerializable("list");
            list_ctg = (ArrayList<CtgMusic>) bundle.getSerializable("categorias");
            if(list != null){
                LoadReproductor();
            }else{
                btnPrevious.setVisibility(View.GONE);
                btnNext.setVisibility(View.GONE);
                youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(YouTubePlayer youTubePlayer) {
                        //super.onReady(youTubePlayer);
                        youTubePlayer.cueVideo(youTube.getIdvideo(), contador_minuto);
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
                                break;
                            case PLAYING:
                                ShowButton();
                                System.out.println("PLAYING 1");
                                break;
                            case PAUSED:
                                System.out.println("PAUSED");
                                break;
                            case BUFFERING:
                                System.out.println("BUFFERING");
                                break;
                            case VIDEO_CUED:
                                youTubePlayer.play();
                                System.out.println("VIDEO_CUED");
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
                        super.onError(youTubePlayer, error);
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

            if(list_ctg != null){
                if(list_ctg.size() > 0) btnSelect.setVisibility(View.VISIBLE);
            }
            nextButton();
            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowButton();
                    if(player != null){
                        contador++;
                        player.cueVideo(list.get(contador).getIdvideo(), 0);
                        player.play();
                        nextButton();
                    }
                }
            });
            btnPrevious.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ShowButton();
                    if(player != null){
                        contador--;
                        player.cueVideo(list.get(contador).getIdvideo(), 0);
                        player.play();
                        nextButton();
                    }
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
                if(es_ctg_privada) LoadMyMusic(list_ctg.get(position).getCode());
                else LoadMusic(list_ctg.get(position).getCode());
            }
        });
        alert.show();
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
        nextButton();
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) { //horizontal
            // youTubePlayerView.enterFullScreen(); //ponemos pantalla completa
            isFullScreem = true;
            youTubePlayerView.wrapContent();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){ //vertical
            // youTubePlayerView.exitFullScreen(); //quitamos pantalla completa
            isFullScreem = false;
            youTubePlayerView.matchParent();
        }
    }
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
        // do something with the route...
    }

    public void onStop() {
        mRouter.addCallback(mSelector, mCallback, /* flags= */ 0);
        super.onStop();
    }
    // Remove the callback when the activity is destroyed.
    public void onDestroy() {
        mRouter.removeCallback(mCallback);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(player != null) player.play();
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
                        ShowButton();
                        System.out.println("PLAYING");
                        break;
                    case PAUSED:
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

    private void nextButton(){
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

    public void LoadMusic(String idctg){
        msjProsgress(getString(R.string.load));

    }
    public void LoadMyMusic(String idctg){
        msjProsgress(getString(R.string.load));

    }

    private ProgressDialog progress;
    private void msjProsgress(String msj){
        progress = new ProgressDialog(PlayerActivity.this);
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.setMessage(msj);
        progress.show();
    }

}