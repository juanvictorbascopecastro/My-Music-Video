package com.youtube.musica.ui.youtube;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.youtube.musica.PlayerFullscreenActivity;
import com.youtube.musica.models.MusicCollection;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.youtube.musica.BuildConfig;
import com.youtube.musica.R;
import com.youtube.musica.adapter.YoutubePlaylistAdapter;
import com.youtube.musica.dialog.AddMusic;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.models.YoutubePlaylist;
import com.youtube.musica.ui.ctg.CtgViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YoutubeSearchFragment extends Fragment {

    private static final String API_KEY = BuildConfig.YOUTUBE_API_KEY;
    private static final String TAG = "YoutubeSearchFragment";

    private SearchView searchView;
    private RecyclerView rvYoutubeSearch;
    private ProgressBar progressBar;
    
    private YoutubePlaylistAdapter adapter;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private ArrayList<CategoryCollection> list;
    private CtgViewModel ctgViewModel;
    private Category db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_youtube_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        searchView = view.findViewById(R.id.search_view);
        rvYoutubeSearch = view.findViewById(R.id.rv_youtube_search);
        progressBar = view.findViewById(R.id.progressBar);

        ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);
        db = new Category();
        list = new ArrayList<>();
        LoadCtg();

        rvYoutubeSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new YoutubePlaylistAdapter(item -> {
            // Short click: play the video
            MusicCollection music = new MusicCollection(null, item.getVideoId(), "https://www.youtube.com/watch?v=" + item.getVideoId(), item.getTitle(), item.getDescription(), null, new java.util.Date());
            ArrayList<MusicCollection> playList = new ArrayList<>();
            playList.add(music);
            Intent intent = new Intent(getContext(), PlayerFullscreenActivity.class);
            Bundle bundle = new Bundle();
            bundle.putFloat("minuto", 0);
            bundle.putInt("position", 0);
            bundle.putSerializable("categorias", ctgViewModel.getList().getValue());
            bundle.putSerializable("list", playList);
            intent.putExtras(bundle);
            startActivity(intent);
        }, item -> {
            // Long click: open AddMusic dialog
            if (com.youtube.musica.utils.AuthUtils.isLoggedIn()) {
                String videoUrl = "https://www.youtube.com/watch?v=" + item.getVideoId();
                new AddMusic(getContext(), ctgViewModel.getList().getValue(), videoUrl, item.getVideoId(), item.getTitle());
            } else {
                com.youtube.musica.utils.AuthUtils.requireLogin(getContext());
            }
        });
        rvYoutubeSearch.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    searchVideos(query.trim());
                    searchView.clearFocus();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        
        // Búsqueda inicial por defecto
        searchVideos("Música");
    }

    private void searchVideos(String query) {
        progressBar.setVisibility(View.VISIBLE);
        
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String urlStr = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=25&videoEmbeddable=true&q=" + encodedQuery + "&type=video&key=" + API_KEY;
                
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
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
                
            } catch (Exception e) {
                Log.e(TAG, "Error fetching YouTube data", e);
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error al buscar videos", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<YoutubePlaylist> parseResponse(String json) {
        List<YoutubePlaylist> list = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("items");
            
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                JSONObject snippet = item.getJSONObject("snippet");
                
                String videoId = "";
                if (item.has("id") && item.getJSONObject("id").has("videoId")) {
                    videoId = item.getJSONObject("id").getString("videoId");
                }
                
                if (!videoId.isEmpty()) {
                    String title = snippet.getString("title");
                    String desc = snippet.getString("description");
                    String thumb = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url");
                    list.add(new YoutubePlaylist(videoId, title, desc, thumb, videoId));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
        }
        return list;
    }

    public void LoadCtg(){
        db.loadCtg().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()){
                    CategoryCollection ctg;
                    for(QueryDocumentSnapshot snapshot : task.getResult()) {
                        ctg = snapshot.toObject(CategoryCollection.class);
                        ctg.setCode(snapshot.getId());
                        list.add(ctg);
                    }
                }
                ctgViewModel.setList(list);
            }
        });
    }
}
