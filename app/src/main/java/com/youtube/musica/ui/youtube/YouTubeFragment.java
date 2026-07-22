package com.youtube.musica.ui.youtube;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import com.youtube.musica.R;
import com.youtube.musica.databinding.FragmentYoutubeBinding;
import com.youtube.musica.dialog.AddMusic;
import com.youtube.musica.dialog.msgInfo;
import com.youtube.musica.dialog.progress;
import com.youtube.musica.firebase.Category;
import com.youtube.musica.models.CategoryCollection;
import com.youtube.musica.ui.ctg.CtgViewModel;
import com.youtube.musica.utils.idurl.YouTubeHelperTest;

public class YouTubeFragment extends Fragment {

    private FragmentYoutubeBinding binding;
    private WebView webView;
    private ProgressBar progressBar;
    private FloatingActionButton fab;
    private ArrayList<CategoryCollection> list;
    private msgInfo msg;
    CtgViewModel ctgViewModel;
    private Category db;
    private OnBackPressedCallback backPressedCallback;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentYoutubeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.progressBar);
        webView = view.findViewById(R.id.webView);
        fab = view.findViewById(R.id.fab);

        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                backPressedCallback.setEnabled(view.canGoBack());
            }
        });
        webView.loadUrl("https://www.youtube.com");
        msg = new msgInfo(getContext());

        ctgViewModel = new ViewModelProvider(this).get(CtgViewModel.class);
        db = new Category();
        list = new ArrayList<>();
        LoadCtg();
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                    fab.setVisibility(View.VISIBLE);
                }
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (com.youtube.musica.utils.AuthUtils.isLoggedIn()) {
                    AddList();
                } else {
                    com.youtube.musica.utils.AuthUtils.requireLogin(getContext());
                }
            }
        });
    }

    public void AddList(){
        YouTubeHelperTest helperTest = new YouTubeHelperTest(webView.getUrl());
        String id_video = helperTest.getUrl();
        //System.out.println(webView.getUrl());
        //System.out.println(id_video);
        if(id_video != null){
            // SelectCtg(id_video, webView.getUrl());
            new AddMusic(getContext(), ctgViewModel.getList().getValue(), webView.getUrl(), id_video);
        }else{
            msg.showMsg(getContext().getString(R.string.no_id_video),"#FF9800",1);
        }

    }
    public void LoadCtg(){
        list = new ArrayList<>();
        progress.run(getString(R.string.load), getContext());
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
                progress.diss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}