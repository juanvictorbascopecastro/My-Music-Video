package com.youtube.musica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.youtube.musica.databinding.ActivityMainBinding;
import com.youtube.musica.ui.youtube.YouTubeFragment;
import com.youtube.musica.utils.AuthUtils;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private int pendingDestination = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar NewPipeExtractor
        org.schabi.newpipe.extractor.NewPipe.init(com.youtube.musica.utils.DownloaderImpl.getInstance());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_music, R.id.nav_youtube, R.id.nav_ctg, R.id.nav_youtube_search)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_music || id == R.id.nav_ctg) {
                if (!AuthUtils.isLoggedIn()) {
                    pendingDestination = id;
                    AuthUtils.requireLogin(this);
                    binding.drawerLayout.closeDrawers();
                    return false; 
                }
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                binding.drawerLayout.closeDrawers();
            }
            return handled;
        });

        if (savedInstanceState != null) {
            pendingDestination = savedInstanceState.getInt("pendingDestination", -1);
        }

        updateNavHeader(navigationView);
        
        addMenuProvider(new androidx.core.view.MenuProvider() {
            @Override
            public void onCreateMenu(@androidx.annotation.NonNull Menu menu, @androidx.annotation.NonNull android.view.MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main, menu);
            }

            @Override
            public void onPrepareMenu(@androidx.annotation.NonNull Menu menu) {
                android.view.MenuItem loginLogoutItem = menu.findItem(R.id.action_logout);
                if (loginLogoutItem != null) {
                    if (AuthUtils.isLoggedIn()) {
                        loginLogoutItem.setTitle("Cerrar Sesión");
                    } else {
                        loginLogoutItem.setTitle("Iniciar Sesión");
                    }
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean onMenuItemSelected(@androidx.annotation.NonNull android.view.MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_logout) {
                    if (AuthUtils.isLoggedIn()) {
                        FirebaseAuth.getInstance().signOut();
                        
                        int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
                        String webClientId = webClientIdRes != 0 ? getString(webClientIdRes) : "DUMMY_CLIENT_ID";
                        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(webClientId)
                                .requestEmail()
                                .build();
                        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
                        mGoogleSignInClient.signOut().addOnCompleteListener(MainActivity.this, task -> {
                            updateNavHeader(binding.navView);
                            invalidateMenu();
                        });
                        if (isTaskRoot()) {
                            startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        }
                        finish();
                    } else {
                        AuthUtils.requireLogin(MainActivity.this);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("pendingDestination", pendingDestination);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.navView != null) {
            updateNavHeader(binding.navView);
        }
        invalidateMenu();

        if (pendingDestination != -1 && AuthUtils.isLoggedIn()) {
            navController.navigate(pendingDestination);
            pendingDestination = -1;
        }
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        ImageView ivUserAvatar = headerView.findViewById(R.id.iv_user_avatar);
        TextView tvUserName = headerView.findViewById(R.id.tv_user_name);
        TextView tvUserEmail = headerView.findViewById(R.id.tv_user_email);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            tvUserName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Usuario");
            tvUserEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(ivUserAvatar);
            }
        } else {
            tvUserName.setText("Invitado");
            tvUserEmail.setText("Inicia sesión para agregar contenido");
            ivUserAvatar.setImageResource(R.mipmap.ic_launcher_round); // Default image
        }
    }



    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

}