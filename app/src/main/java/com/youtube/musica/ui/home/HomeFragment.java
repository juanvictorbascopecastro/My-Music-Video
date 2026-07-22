package com.youtube.musica.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.youtube.musica.databinding.FragmentHomeBinding;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        YouTubePlayerView youTubePlayerView = binding.youtubePlayerView;
        youTubePlayerView.setEnableAutomaticInitialization(false);
        getLifecycle().addObserver(youTubePlayerView);

        // Configuramos las opciones personalizadas usando IFramePlayerOptions
        IFramePlayerOptions options = new IFramePlayerOptions.Builder(requireContext())
                .controls(0)          // Oculta los controles nativos de YouTube
                .rel(0)               // Evita mostrar videos de otros canales al final
                .ivLoadPolicy(3)      // Oculta las anotaciones (tarjetas)
                .ccLoadPolicy(1)      // Muestra subtítulos de forma predeterminada
                //.ccLangPref("es")     // Subtítulos en español
                .autoplay(1)          // Autoplay habilitado
                .mute(1)              // Silenciado para permitir autoplay en Android
                .build();

        // Inicializamos el reproductor con las opciones
        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                // Cargamos un video de ejemplo cuando el reproductor esté listo
                String videoId = "S0Q4gqBUs7c"; // Puedes cambiar este ID
                youTubePlayer.loadVideo(videoId, 0f);
            }
        }, options);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}