package com.youtube.musica.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.youtube.musica.R;
import com.youtube.musica.adapter.YoutubePlaylistAdapter;
import com.youtube.musica.models.YoutubePlaylist;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fragmento encargado de mostrar y reproducir los videos musicales del usuario.
 * Utiliza la YouTube Data API v3 para obtener la lista de videos y el módulo local 
 * (youtube-player-core) para reproducirlos esquivando anuncios.
 */
public class MyVideosFragment extends Fragment {

    // Se obtiene de local.properties y se inyecta mediante BuildConfig en build.gradle.kts
    private static final String API_KEY = com.youtube.musica.BuildConfig.YOUTUBE_API_KEY;
    private static final String TAG = "MyVideosFragment";

    private RecyclerView rvVideos;
    private ProgressBar progressBar;
    private MaterialButton btnConnectYoutube;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer currentYouTubePlayer;
    private TabLayout tabLayout;
    
    private int currentPlayingIndex = -1;
    
    private YoutubePlaylistAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_videos, container, false);

        rvVideos = view.findViewById(R.id.rv_videos);
        progressBar = view.findViewById(R.id.progress_bar);
        btnConnectYoutube = view.findViewById(R.id.btn_connect_youtube);
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);

        rvVideos.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new YoutubePlaylistAdapter(item -> {
            // Al hacer clic, reproducir y guardar el índice actual para autoplay
            currentPlayingIndex = adapter.getItems().indexOf(item);
            playVideo(item);
        });
        rvVideos.setAdapter(adapter);

        getLifecycle().addObserver(youTubePlayerView);
        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                currentYouTubePlayer = youTubePlayer;
            }

            @Override
            public void onStateChange(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerState state) {
                // Cuando el video termina, pasar automáticamente al siguiente
                if (state == PlayerConstants.PlayerState.ENDED) {
                    playNextVideo();
                }
            }
        });

        tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentPlayingIndex = -1; // Reset autoplay
                youTubePlayerView.setVisibility(View.GONE);
                if (currentYouTubePlayer != null) {
                    currentYouTubePlayer.pause();
                }
                
                int position = tab.getPosition();
                if (position == 0) {
                    checkGoogleSignIn(0); // 0 = Suscripciones
                } else if (position == 1) {
                    checkGoogleSignIn(1); // 1 = Likes
                } else if (position == 2) {
                    checkGoogleSignIn(2); // 2 = Playlists
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Carga inicial: Suscripciones
        checkGoogleSignIn(0);

        return view;
    }

    /**
     * Verifica si el usuario ya ha iniciado sesión con Google.
     * Si está logueado, inicia la obtención de los videos.
     * Si no, muestra un botón para solicitar el inicio de sesión.
     */
    private void checkGoogleSignIn(int actionType) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
        if (account != null && account.getAccount() != null) {
            btnConnectYoutube.setVisibility(View.GONE);
            if (actionType == 0) {
                // Obtiene las suscripciones del usuario y luego sus videos
                fetchSubscriptions(account);
            } else if (actionType == 1) {
                // Endpoint para obtener Likes (myRating=like)
                String urlStr = "https://www.googleapis.com/youtube/v3/videos?part=snippet&myRating=like&maxResults=25";
                fetchFromApi(urlStr, true, account);
            } else if (actionType == 2) {
                // Endpoint para obtener Listas de reproducción del usuario
                String urlStr = "https://www.googleapis.com/youtube/v3/playlists?part=snippet&mine=true&maxResults=25";
                fetchFromApi(urlStr, true, account);
            }
        } else {
            btnConnectYoutube.setVisibility(View.VISIBLE);
            btnConnectYoutube.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Por favor, inicia sesión de nuevo para dar permisos", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchSubscriptions(GoogleSignInAccount account) {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly";
                String token = GoogleAuthUtil.getToken(requireContext(), account.getAccount(), scope);

                // 1. Obtener los 5 canales a los que más recientemente se suscribió
                String subUrl = "https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&mine=true&maxResults=5";
                String subJson = makeHttpRequest(subUrl, token);

                List<String> channelIds = new ArrayList<>();
                JSONObject subObj = new JSONObject(subJson);
                JSONArray subItems = subObj.optJSONArray("items");
                if (subItems != null) {
                    for (int i = 0; i < subItems.length(); i++) {
                        JSONObject snippet = subItems.getJSONObject(i).getJSONObject("snippet");
                        String channelId = snippet.getJSONObject("resourceId").getString("channelId");
                        channelIds.add(channelId);
                    }
                }

                List<YoutubePlaylist> allVideos = new ArrayList<>();
                // 2. Obtener últimos 5 videos de cada canal
                for (String channelId : channelIds) {
                    String uploadsPlaylistId = channelId.replaceFirst("UC", "UU");
                    String vidUrl = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=" + uploadsPlaylistId + "&maxResults=5";
                    String vidJson = makeHttpRequest(vidUrl, token);
                    allVideos.addAll(parseResponse(vidJson));
                }

                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.setItems(allVideos);
                    if (allVideos.isEmpty()) {
                        Toast.makeText(getContext(), "No se encontraron videos recientes", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (UserRecoverableAuthException e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    startActivityForResult(e.getIntent(), 1001);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching Subscriptions", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al obtener suscripciones", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String makeHttpRequest(String urlStr, String token) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        int responseCode = conn.getResponseCode();
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private void fetchFromApi(String urlStr, boolean useOAuth, GoogleSignInAccount account) {
        progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (useOAuth && account != null) {
                    String scope = "oauth2:https://www.googleapis.com/auth/youtube.readonly";
                    String token = GoogleAuthUtil.getToken(requireContext(), account.getAccount(), scope);
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
                
                int responseCode = conn.getResponseCode();
                
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    List<YoutubePlaylist> parsedList = parseResponse(response.toString());
                    
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        adapter.setItems(parsedList);
                        if (parsedList.isEmpty()) {
                            Toast.makeText(getContext(), "No se encontraron resultados", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.e(TAG, "Error from API HTTP " + responseCode + ": " + response.toString());
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "API Error: " + responseCode, Toast.LENGTH_LONG).show();
                    });
                }
                
            } catch (UserRecoverableAuthException e) {
                Log.e(TAG, "UserRecoverableAuthException", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    startActivityForResult(e.getIntent(), 1001);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching YouTube data", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al obtener videos", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Convierte el JSON crudo devuelto por YouTube en una lista de objetos {@link YoutubePlaylist}.
     * 
     * @param json La respuesta en texto (formato JSON) de la API.
     * @return Una lista de modelos listos para inyectarse en el RecyclerView.
     */
    private List<YoutubePlaylist> parseResponse(String json) {
        List<YoutubePlaylist> list = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("items");
            
            // Itera sobre el array "items" del JSON
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                JSONObject snippet = item.getJSONObject("snippet");
                String videoId = "";
                
                // Dependiendo del endpoint (videos, playlistItems, search), el ID viene en un lugar distinto
                if (snippet.has("resourceId")) {
                    videoId = snippet.getJSONObject("resourceId").optString("videoId", "");
                } else if (item.optJSONObject("id") != null) {
                    videoId = item.getJSONObject("id").optString("videoId", "");
                } else {
                    videoId = item.optString("id", "");
                }
                String title = snippet.getString("title");
                String desc = snippet.getString("description");
                String thumb = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
                
                // Solo agrega elementos que sean un video válido
                if (!videoId.isEmpty()) {
                    list.add(new YoutubePlaylist(videoId, title, desc, thumb, videoId));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
        return list;
    }

    /**
     * Reproduce un video seleccionado enviando el comando al reproductor de YouTube local.
     * 
     * @param item El video a reproducir seleccionado en el RecyclerView.
     */
    private void playVideo(YoutubePlaylist item) {
        // Hace visible el reproductor (que inicialmente está oculto)
        youTubePlayerView.setVisibility(View.VISIBLE);
        if (currentYouTubePlayer != null) {
            // Le indicamos al IFrame (dentro del WebView) que cargue este ID y comience en el segundo 0
            currentYouTubePlayer.loadVideo(item.getVideoId(), 0f);
        }
    }

    /**
     * Autoplay: Salta al siguiente video de la lista
     */
    private void playNextVideo() {
        if (adapter != null && currentPlayingIndex != -1) {
            currentPlayingIndex++;
            if (currentPlayingIndex < adapter.getItems().size()) {
                YoutubePlaylist nextItem = adapter.getItems().get(currentPlayingIndex);
                playVideo(nextItem);
                
                // Mover el scroll al video que se está reproduciendo
                rvVideos.smoothScrollToPosition(currentPlayingIndex);
            }
        }
    }
}
