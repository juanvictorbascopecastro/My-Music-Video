package com.youtube.musica.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.cast.framework.CastContext;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;

/**
 * ChromecastUtil
 *
 * Clase utilitaria para encapsular y abstraer la lógica de conexión y control
 * del Chromecast. Utiliza la librería chromecast-sender de android-youtube-player.
 * Se encarga de inicializar la sesión, escuchar los eventos de conexión,
 * proveer métodos de control básicos (play, pause, cargar video) y notificar
 * al componente que la invoca (Activity o Fragment) a través de la interfaz
 * ChromecastListener.
 */
public class ChromecastUtil {

    /**
     * Interfaz para recibir notificaciones sobre el estado de la conexión Cast
     * y los eventos del reproductor de YouTube en la pantalla externa.
     */
    public interface ChromecastListener {
        void onConnected(ChromecastYouTubePlayer player);
        void onDisconnected();
        void onStateChanged(PlayerConstants.PlayerState state);
    }

    private CastContext castContext;
    private ChromecastYouTubePlayerContext chromecastYouTubePlayerContext;
    private ChromecastYouTubePlayer chromecastYouTubePlayer;
    private boolean isCasting = false;
    private ChromecastListener listener;

    /**
     * Constructor. Inicializa el CastContext y registra el listener de conexión.
     * 
     * @param context Contexto de la aplicación o actividad.
     * @param listener Callback para recibir eventos de Chromecast.
     */
    public ChromecastUtil(Context context, ChromecastListener listener) {
        this.listener = listener;
        try {
            castContext = CastContext.getSharedInstance(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (castContext != null) {
            chromecastYouTubePlayerContext = new ChromecastYouTubePlayerContext(
                    castContext.getSessionManager(),
                    new ChromecastConnectionListener() {
                        @Override
                        public void onChromecastConnecting() { }

                        @Override
                        public void onChromecastConnected(@NonNull ChromecastYouTubePlayerContext chromecastContext) {
                            isCasting = true;
                            chromecastContext.initialize(new AbstractYouTubePlayerListener() {
                                @Override
                                public void onReady(@NonNull YouTubePlayer castPlayer) {
                                    chromecastYouTubePlayer = (ChromecastYouTubePlayer) castPlayer;
                                    if (listener != null) {
                                        listener.onConnected(chromecastYouTubePlayer);
                                    }
                                }

                                @Override
                                public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                                    if (listener != null) {
                                        listener.onStateChanged(state);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onChromecastDisconnected() {
                            isCasting = false;
                            chromecastYouTubePlayer = null;
                            if (listener != null) {
                                listener.onDisconnected();
                            }
                        }
                    }
            );
        }
    }

    public ChromecastYouTubePlayerContext getChromecastYouTubePlayerContext() {
        return chromecastYouTubePlayerContext;
    }

    /**
     * Verifica si el dispositivo está actualmente casteando (conectado).
     */
    public boolean isCasting() {
        return isCasting;
    }

    /**
     * Carga un video de YouTube en el reproductor de Chromecast.
     * 
     * @param videoId El ID del video de YouTube.
     * @param seconds El segundo desde el cual iniciar la reproducción.
     */
    public void loadVideo(String videoId, float seconds) {
        if (isCasting && chromecastYouTubePlayer != null) {
            chromecastYouTubePlayer.loadVideo(videoId, seconds);
        }
    }

    /**
     * Pausa el video que se está reproduciendo en el Chromecast.
     */
    public void pause() {
        if (isCasting && chromecastYouTubePlayer != null) {
            chromecastYouTubePlayer.pause();
        }
    }

    /**
     * Reanuda la reproducción del video en el Chromecast.
     */
    public void play() {
        if (isCasting && chromecastYouTubePlayer != null) {
            chromecastYouTubePlayer.play();
        }
    }
}
