package com.youtube.musica.utils;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.youtube.musica.LoginActivity;

public class AuthUtils {

    /**
     * Checks if the user is currently logged in.
     * @return true if logged in, false otherwise.
     */
    public static boolean isLoggedIn() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null;
    }

    /**
     * Redirects the user to the LoginActivity if they are not logged in.
     * @param context The context used to start the intent.
     */
    public static void requireLogin(Context context) {
        requireLogin(context, null);
    }

    /**
     * Redirects the user to the LoginActivity with a custom descriptive message.
     * @param context The context used to start the intent.
     * @param message The message to display in the LoginActivity.
     */
    public static void requireLogin(Context context, String message) {
        if (context != null) {
            Intent intent = new Intent(context, LoginActivity.class);
            if (message != null) {
                intent.putExtra("login_message", message);
            }
            context.startActivity(intent);
        }
    }
}
