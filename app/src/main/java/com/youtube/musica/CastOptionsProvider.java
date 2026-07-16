package com.youtube.musica;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import java.util.List;

public class CastOptionsProvider implements OptionsProvider {
    @NonNull
    @Override
    public CastOptions getCastOptions(@NonNull Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId("C5CBE8CA")
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(@NonNull Context context) {
        return null;
    }
}
