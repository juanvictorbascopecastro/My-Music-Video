package com.youtube.musica;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;
import java.util.Stack;

import com.youtube.musica.databinding.ActivityPlayerFullscreenBinding;
import com.youtube.musica.dialog.CtgSelectionDialog;
import com.youtube.musica.firebase.Music;
import com.youtube.musica.interfaces.DbMusicListener;
import com.youtube.musica.interfaces.PlayerListener;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.services.NotificationHelper;
import com.youtube.musica.services.PlayerEventBroadcaster;
import com.youtube.musica.utils.ChromecastUtil;
import com.youtube.musica.utils.YouTubePictureInPicture;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayer;
import com.google.android.gms.cast.framework.CastButtonFactory;
import androidx.mediarouter.app.MediaRouteButton;

/**
 * PlayerFullscreenActivity
 * 
 * Actividad encargada de reproducir videos de YouTube en pantalla completa (o
 * modo apaisado).
 * Además, maneja la reproducción en segundo plano a través de un servicio de
 * notificaciones,
 * transiciones al modo Picture-in-Picture (PiP), y la reproducción continua de
 * una lista de reproducción.
 * Ya no gestiona la lógica de Chromecast, la cual fue movida a MusicFragment.
 */
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

    // Historial de reproducción para el Modo Aleatorio. Permite al usuario regresar
    // a
    // canciones anteriores que fueron escogidas al azar al presionar "Previo".
    private Stack<Integer> shuffleHistory = new Stack<>();

    private NotificationHelper notificationHelper;
    private PlayerEventBroadcaster eventBroadcaster;
    private boolean isPlaying = false;

    // =========================================================================
    // MODOS DE REPRODUCCIÓN (Playback Modes)
    // =========================================================================
    // MODE_SEQUENTIAL: Reproduce de forma lineal. Se detiene al final o inicio.
    // MODE_SHUFFLE: Escoge videos al azar. Usa 'shuffleHistory' para recordar el
    // historial.
    // MODE_REPEAT_ONE: Repite el mismo video indefinidamente (no altera el
    // indexPlayer).
    private static final int MODE_SEQUENTIAL = 0;
    private static final int MODE_SHUFFLE = 1;
    private static final int MODE_REPEAT_ONE = 2;
    // Estado actual del modo de reproducción
    private int currentPlaybackMode = MODE_SEQUENTIAL;

    private ChromecastUtil chromecastUtil;
    private MediaRouteButton mediaRouteButton;
    private android.widget.FrameLayout castButtonContainer;

    private boolean isAppVisible = false;
    private long lastPipExitTime = 0;
    private boolean isUserIntentionallyPaused = false;

    @Override
    protected void onStart() {
        super.onStart();
        isAppVisible = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAppVisible = false;
    }

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

        notificationHelper = new NotificationHelper(this);

        // Recuperar el último modo de reproducción guardado (Persistencia).
        // Si no existe, se utiliza el MODE_SEQUENTIAL por defecto.
        SharedPreferences prefs = getSharedPreferences("MusicAppPrefs", Context.MODE_PRIVATE);
        currentPlaybackMode = prefs.getInt("playback_mode", MODE_SEQUENTIAL);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            // currentYouTube = (YTVideo) bundle.getSerializable("music");
            minutesPlayer = bundle.getFloat("minuto");
            indexPlayer = bundle.getInt("position");
            list = (ArrayList<MusicCollection>) bundle.getSerializable("list");
            list_ctg = (ArrayList<CategoryCollection>) bundle.getSerializable("categorias");
            shuffleHistory.clear();

            binding.btnPrevious.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.GONE);

            if (list_ctg != null) {
                if (!list_ctg.isEmpty())
                    binding.btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();
            initYouTubePlayerView();
        }

        binding.btnScreenRotation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowButton();
                isFullScreem = !isFullScreem;
                if (!isFullScreem) {
                    // youTubePlayerView.exitFullScreen();
                    isFullScreem = false;
                    youTubePlayerView.matchParent();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // vertical
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

        // Listener para cambiar los Modos de Reproducción (Secuencial -> Aleatorio ->
        // Repetir Uno)
        binding.btnPlaybackMode.setOnClickListener(v -> {
            ShowButton();
            // Alternar ciclicamente entre los 3 modos disponibles (0, 1, 2)
            currentPlaybackMode = (currentPlaybackMode + 1) % 3;

            // Guardar el modo seleccionado en SharedPreferences para la próxima vez
            getSharedPreferences("MusicAppPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("playback_mode", currentPlaybackMode)
                    .apply();

            updatePlaybackModeIcon();
            disabledButton(); // Re-evaluar la visibilidad de los botones Next/Previous según el modo
        });
        updatePlaybackModeIcon(); // Setear el icono correcto al crear la actividad

        youTubePlayerView.enableBackgroundPlayback(true);

        mediaRouteButton = findViewById(R.id.media_route_button);
        castButtonContainer = findViewById(R.id.cast_button_container);
        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton);

        chromecastUtil = new ChromecastUtil(this, new ChromecastUtil.ChromecastListener() {
            @Override
            public void onConnected(ChromecastYouTubePlayer player) {
                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                shape.setColor(getResources().getColor(R.color.primary));
                castButtonContainer.setBackground(shape);

                if (ytPlayer != null) {
                    ytPlayer.pause();
                }
                if (list != null && !list.isEmpty()) {
                    chromecastUtil.loadVideo(currentYouTube.getIdvideo(), minutesPlayer);
                }
            }

            @Override
            public void onDisconnected() {
                castButtonContainer.setBackground(null);
            }

            @Override
            public void onStateChanged(PlayerConstants.PlayerState state) {
                if (state == PlayerConstants.PlayerState.ENDED) {
                    if (ytPlayer != null) {
                        NextVideo(ytPlayer);
                    }
                }
            }
        });

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
                shuffleHistory.clear();
            }
            if (bundle.containsKey("categorias")) {
                list_ctg = (ArrayList<CategoryCollection>) bundle.getSerializable("categorias");
            }

            if (list_ctg != null) {
                if (!list_ctg.isEmpty())
                    binding.btnSelect.setVisibility(View.VISIBLE);
            }
            disabledButton();

            // Si el reproductor ya está listo, cargar el nuevo video instantáneamente
            if (ytPlayer != null && list != null && !list.isEmpty()) {
                currentYouTube = list.get(indexPlayer);
                ytPlayer.loadVideo(currentYouTube.getIdvideo(), minutesPlayer);
            }
        }
    }

    /**
     * Inicializa el reproductor de YouTube, el broadcast de eventos en segundo
     * plano
     * y las notificaciones para controlar el reproductor fuera de la app.
     */
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

                youTubePlayer.loadVideo(currentYouTube.getIdvideo(), minutesPlayer);
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer,
                    @NonNull PlayerConstants.PlayerState state) {
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
                bufferingHandler.removeCallbacks(bufferingRunnable);
                isPlaying = true;
                isUserIntentionallyPaused = false;
                ShowButton();
                showNotification();
                updatePictureInPictureActions();
                return "PLAYING";
            case PAUSED:
                android.os.PowerManager pm = (android.os.PowerManager) getSystemService(
                        android.content.Context.POWER_SERVICE);
                if (pm != null && !pm.isInteractive()) {
                    if (!isUserIntentionallyPaused) {
                        boolean inPip = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            inPip = isInPictureInPictureMode();
                        }
                        boolean wasRecentlyInPip = (System.currentTimeMillis() - lastPipExitTime) < 2000;
                        if (inPip || !isAppVisible || wasRecentlyInPip) {
                            youTubePlayer.play();
                            return "PLAYING";
                        }
                    }
                }
                isPlaying = false;
                isUserIntentionallyPaused = true;
                bufferingHandler.removeCallbacks(bufferingRunnable);
                showNotification();
                updatePictureInPictureActions();
                return "PAUSED";
            case BUFFERING:
                bufferingHandler.removeCallbacks(bufferingRunnable);
                bufferingHandler.postDelayed(bufferingRunnable, 4000);
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
    private Handler bufferingHandler = new Handler(Looper.getMainLooper());
    private Runnable bufferingRunnable = () -> {
        if (ytPlayer != null && !isUserIntentionallyPaused) {
            ytPlayer.play();
        }
    };

    /**
     * Reproduce el siguiente video en la lista. Si llega al final, deshabilita el
     * botón de siguiente.
     * Modificado para soportar Modos de Reproducción.
     */
    private void NextVideo(YouTubePlayer youTubePlayer) {
        if (list == null || list.isEmpty())
            return;

        // Evaluar cuál será el siguiente índice en base al modo actual
        if (currentPlaybackMode == MODE_SEQUENTIAL) {
            // Comportamiento normal: sumar 1. Si estamos al final, no hacer nada.
            if (indexPlayer >= list.size() - 1)
                return;
            indexPlayer += 1;
        } else if (currentPlaybackMode == MODE_SHUFFLE) {
            // Guardar el índice actual en el historial ANTES de cambiarlo a uno aleatorio.
            // Esto permite que el botón "Anterior" funcione correctamente en modo
            // aleatorio.
            shuffleHistory.push(indexPlayer);
            indexPlayer = new java.util.Random().nextInt(list.size());
        } else if (currentPlaybackMode == MODE_REPEAT_ONE) {
            // Mantener el mismo indexPlayer para que el video se repita desde el inicio
            // (0f)
        }

        this.ytPlayer = youTubePlayer;
        disabledButton();
        currentYouTube = list.get(indexPlayer);

        if (chromecastUtil != null && chromecastUtil.isCasting()) {
            chromecastUtil.loadVideo(currentYouTube.getIdvideo(), 0f);
        } else {
            youTubePlayer.loadVideo(currentYouTube.getIdvideo(), 0f);
        }
        disabledButton();
    }

    /**
     * Reproduce el video anterior en la lista. Si llega al principio, deshabilita
     * el botón de anterior.
     * Modificado para soportar Modos de Reproducción y el Historial de Aleatorio.
     */
    private void PreviousVideo(YouTubePlayer youTubePlayer) {
        if (list == null || list.isEmpty())
            return;

        // Evaluar cuál será el índice anterior en base al modo actual
        if (currentPlaybackMode == MODE_SEQUENTIAL) {
            // Comportamiento normal: restar 1. Si estamos al inicio, no hacer nada.
            if (indexPlayer <= 0)
                return;
            indexPlayer -= 1;
        } else if (currentPlaybackMode == MODE_SHUFFLE) {
            // En modo aleatorio, primero verificamos si venimos de otra canción (historial)
            if (!shuffleHistory.isEmpty()) {
                // Recuperar la última canción aleatoria escuchada
                indexPlayer = shuffleHistory.pop();
            } else {
                // Si no hay historial (ej. recién abrimos la app), elegir una al azar
                indexPlayer = new java.util.Random().nextInt(list.size());
            }
        } else if (currentPlaybackMode == MODE_REPEAT_ONE) {
            // Mantener el mismo indexPlayer y recargar el video actual
        }

        this.ytPlayer = youTubePlayer;
        disabledButton();
        currentYouTube = list.get(indexPlayer);

        if (chromecastUtil != null && chromecastUtil.isCasting()) {
            chromecastUtil.loadVideo(currentYouTube.getIdvideo(), 0f);
        } else {
            youTubePlayer.loadVideo(currentYouTube.getIdvideo(), 0f);
        }
        disabledButton();
    }

    /**
     * Actualiza el icono (drawable) del botón en la UI dependiendo del modo de
     * reproducción actual
     */
    private void updatePlaybackModeIcon() {
        switch (currentPlaybackMode) {
            case MODE_SEQUENTIAL:
                binding.btnPlaybackMode.setImageResource(R.drawable.ic_repeat);
                break;
            case MODE_SHUFFLE:
                binding.btnPlaybackMode.setImageResource(R.drawable.ic_shuffle);
                break;
            case MODE_REPEAT_ONE:
                binding.btnPlaybackMode.setImageResource(R.drawable.ic_repeat_one);
                break;
        }
    }

    /**
     * Oculta los botones Anterior/Siguiente si estamos en los extremos de la lista
     * (Modo Secuencial).
     * En los modos Aleatorio y Repetir, los botones siempre deben estar visibles.
     */
    private void disabledButton() {
        if (isInPictureInPictureMode()) {
            updatePictureInPictureActions();
            return;
        }

        if (list == null || list.isEmpty() || list.size() == 1) {
            binding.btnNext.setVisibility(View.GONE);
            binding.btnPrevious.setVisibility(View.GONE);
        } else if (currentPlaybackMode == MODE_SEQUENTIAL) {
            if (indexPlayer == list.size() - 1) {
                binding.btnNext.setVisibility(View.GONE);
                binding.btnPrevious.setVisibility(View.VISIBLE);
            } else if (indexPlayer == 0) {
                binding.btnPrevious.setVisibility(View.GONE);
                binding.btnNext.setVisibility(View.VISIBLE);
            } else {
                binding.btnPrevious.setVisibility(View.VISIBLE);
                binding.btnNext.setVisibility(View.VISIBLE);
            }
        } else {
            binding.btnPrevious.setVisibility(View.VISIBLE);
            binding.btnNext.setVisibility(View.VISIBLE);
        }
        updatePictureInPictureActions();
    }

    @Override
    public void onUserInteraction() {
        ShowButton();
        super.onUserInteraction();
    }

    public void onDestroy() {
        super.onDestroy();
        bufferingHandler.removeCallbacks(bufferingRunnable);
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
                activity.binding.btnPlaybackMode.setVisibility(View.GONE);
                activity.binding.btnPicture.setVisibility(View.GONE);
                if (activity.castButtonContainer != null) {
                    activity.castButtonContainer.setVisibility(View.GONE);
                }
            }
        }
    }

    private Handler handler = new ButtonHideHandler(this);

    private void ShowButton() {
        if (isInPictureInPictureMode()) {
            return;
        }
        
        binding.btnScreenRotation.setVisibility(View.VISIBLE);
        binding.btnPlaybackMode.setVisibility(View.VISIBLE);
        disabledButton();
        binding.btnSelect.setVisibility(View.VISIBLE);
        binding.btnPicture.setVisibility(View.VISIBLE);
        if (castButtonContainer != null) {
            castButtonContainer.setVisibility(View.VISIBLE);
        }

        handler.removeCallbacksAndMessages(null);
        handler.sendMessageDelayed(new Message(), 3000);
    }

    // Lógica para manejar cambios en la configuración (orientación de la pantalla)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) { // horizontal
            // ponemos pantalla completa
            isFullScreem = true;
            youTubePlayerView.wrapContent();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) { // vertical
            // quitamos pantalla completa
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

    //
    private void hideNavigationBar() {
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void loadedMusicPlaylist(ArrayList<MusicCollection> playList) {
        indexPlayer = 0;
        minutesPlayer = 0;
        list = playList;
        shuffleHistory.clear();
        if (!list.isEmpty()) {
            if (ytPlayer != null) {
                ytPlayer.loadVideo(list.get(indexPlayer).getIdvideo(), 0f);
            }
        }
    }

    @Override
    public void errorLoadedMusicPlayList(String message, int icon) {
        Toast.makeText(PlayerFullscreenActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void SelectCtg() {
        CtgSelectionDialog.show(PlayerFullscreenActivity.this, list_ctg,
                new CtgSelectionDialog.OnCtgSelectedListener() {
                    @Override
                    public void onCtgSelected(String ctgCode) {
                        musicDataBase.LoadByCtg(ctgCode, true);
                    }
                });
    }

    // =========================================================================
    // REPRODUCTOR EN SEGUNDO PLANO Y NOTIFICACIONES
    // =========================================================================

    /**
     * Crea y muestra la notificación persistente que permite al usuario
     * controlar la reproducción (Pausa, Play, Siguiente, Anterior) cuando minimiza
     * la app.
     */
    private void showNotification() {
        notificationHelper.showNotification(
                PlayerFullscreenActivity.class,
                list,
                indexPlayer,
                isPlaying,
                currentPlaybackMode
        );
    }

    @Override
    public void onPlayPauseNotification() {
        if (chromecastUtil != null && chromecastUtil.isCasting()) {
            if (isPlaying) {
                chromecastUtil.pause();
                isPlaying = false;
            } else {
                chromecastUtil.play();
                isPlaying = true;
            }
            showNotification();
        } else if (ytPlayer != null) {
            if (isPlaying) {
                isUserIntentionallyPaused = true;
                ytPlayer.pause();
            } else {
                isUserIntentionallyPaused = false;
                ytPlayer.play();
            }
        }
    }

    @Override
    public void onNextNotification() {
        if (ytPlayer != null)
            NextVideo(ytPlayer);
    }

    @Override
    public void onPreviewNotification() {
        if (ytPlayer != null)
            PreviousVideo(ytPlayer);
    }

    @Override
    public void onPlaybackModeNotification() {
        // Alternar ciclicamente entre los 3 modos disponibles (0, 1, 2)
        currentPlaybackMode = (currentPlaybackMode + 1) % 3;

        // Guardar el modo seleccionado en SharedPreferences para la próxima vez
        getSharedPreferences("MusicAppPrefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("playback_mode", currentPlaybackMode)
                .apply();

        updatePlaybackModeIcon();
        disabledButton(); // Re-evaluar la visibilidad de los botones Next/Previous según el modo
        showNotification(); // Actualizar la notificación con el nuevo ícono
    }

    // =========================================================================
    // PICTURE IN PICTURE (PiP)
    // =========================================================================

    /**
     * Configura el comportamiento del modo PiP (Picture in Picture),
     * habilitando la transición automática para dispositivos con Android 12+.
     */
    private void updatePictureInPictureActions() {
        YouTubePictureInPicture.updatePictureInPictureActions(this, list, indexPlayer, isPlaying);
    }

    private void initPictureInPicture() {
        binding.btnPicture.setOnClickListener(view -> {
            enterPipMode();
        });

        updatePictureInPictureActions();
    }

    // Permite entrar al modo PiP (Picture in Picture) si el dispositivo lo soporta
    // y la versión de Android es O o superior.
    private void enterPipMode() {
        boolean supportsPIP = getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
        if (supportsPIP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updatePictureInPictureActions();
            android.app.PictureInPictureParams.Builder pipBuilder = new android.app.PictureInPictureParams.Builder();
            // Aspect ratio 16:9, ideal para videos de YouTube
            pipBuilder.setAspectRatio(new android.util.Rational(16, 9));
            enterPictureInPictureMode(pipBuilder.build());
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        
        // Evitar la transición a PiP si la pantalla se está apagando (bloqueo de pantalla).
        // Entrar a PiP mientras se bloquea la pantalla causa una pequeña pausa o tartamudeo en el audio.
        android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            return;
        }

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
            binding.btnPlaybackMode.setVisibility(View.GONE);
            binding.btnPicture.setVisibility(View.GONE);
            if (castButtonContainer != null)
                castButtonContainer.setVisibility(View.GONE);
            youTubePlayerView.matchParent();
        } else {
            lastPipExitTime = System.currentTimeMillis();
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