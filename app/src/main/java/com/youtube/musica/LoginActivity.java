package com.youtube.musica;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

@SuppressWarnings("deprecation")
public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private MaterialButton btnGoogleSignIn;

    // Evento que escucha el resultado de login con google
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    android.util.Log.d("LoginActivity", "Inicio de sesión de Google exitoso: " + account.getEmail());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    android.util.Log.e("LoginActivity", "Fallo al iniciar sesión con Google. ResultCode de Activity: " + result.getResultCode() + ", Código de API Exception: " + e.getStatusCode(), e);
                    progressBar.setVisibility(View.GONE);
                    btnGoogleSignIn.setEnabled(true);
                    
                    String errorMsg = "Error Google Sign-In: Código " + e.getStatusCode();
                    if (e.getStatusCode() == 10) {
                        errorMsg += " (DEVELOPER_ERROR: Revisa que el SHA-1 en Firebase sea correcto y corresponda a la firma actual de la app)";
                    } else if (e.getStatusCode() == 12500) {
                        errorMsg += " (SIGN_IN_FAILED: Verifica que el email de soporte esté configurado en Firebase/Google Cloud)";
                    }
                    
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        progressBar = findViewById(R.id.progress_bar);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);

        int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        if (webClientIdRes == 0) {
            Toast.makeText(this, "Falta el Web Client ID. Actualiza google-services.json", Toast.LENGTH_LONG).show();
        } else {
            String webClientId = getString(webClientIdRes);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        btnGoogleSignIn.setOnClickListener(v -> {
            if (mGoogleSignInClient != null) {
                signIn();
            } else {
                Toast.makeText(this, "Error de configuración de Google Sign-In", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }
    }

    private void signIn() {
        progressBar.setVisibility(View.VISIBLE);
        btnGoogleSignIn.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnGoogleSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        goToMainActivity();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToMainActivity() {
        if (isTaskRoot()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
        finish();
    }
}