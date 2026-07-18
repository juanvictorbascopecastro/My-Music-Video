package com.youtube.musica.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.youtube.musica.R;
import com.youtube.musica.models.MusicCollection;
import com.youtube.musica.services.PlayerEventBroadcaster;

import java.util.ArrayList;

public class YouTubePictureInPicture {

    public static void updatePictureInPictureActions(
            Activity activity,
            ArrayList<MusicCollection> list,
            int indexPlayer,
            boolean isPlaying
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ArrayList<RemoteAction> actions = new ArrayList<>();

            // Acción Anterior
            boolean isPreviousEnabled = list != null && !list.isEmpty() && indexPlayer > 0;
            int previousIconRes = isPreviousEnabled ? R.drawable.ic_previous : R.drawable.ic_previous_disabled;
            PendingIntent previousIntent = PendingIntent.getBroadcast(
                    activity, 1,
                    new Intent(PlayerEventBroadcaster.ACTION_PREVIOUS).setPackage(activity.getPackageName()),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon iconPrev = Icon.createWithResource(activity, previousIconRes);
            RemoteAction actionPrev = new RemoteAction(iconPrev, "Anterior", "Anterior", previousIntent);
            actionPrev.setEnabled(isPreviousEnabled);
            actions.add(actionPrev);

            // Acción Play/Pause
            int playPauseIconRes = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
            PendingIntent playPauseIntent = PendingIntent.getBroadcast(
                    activity, 2,
                    new Intent(PlayerEventBroadcaster.ACTION_PLAY_PAUSE).setPackage(activity.getPackageName()),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon iconPlayPause = Icon.createWithResource(activity, playPauseIconRes);
            RemoteAction actionPlayPause = new RemoteAction(iconPlayPause, isPlaying ? "Pausa" : "Reproducir", isPlaying ? "Pausa" : "Reproducir", playPauseIntent);
            actions.add(actionPlayPause);

            // Acción Siguiente
            boolean isNextEnabled = list != null && !list.isEmpty() && indexPlayer < list.size() - 1;
            int nextIconRes = isNextEnabled ? R.drawable.ic_next : R.drawable.ic_next_disabled;
            PendingIntent nextIntent = PendingIntent.getBroadcast(
                    activity, 3,
                    new Intent(PlayerEventBroadcaster.ACTION_NEXT).setPackage(activity.getPackageName()),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            Icon iconNext = Icon.createWithResource(activity, nextIconRes);
            RemoteAction actionNext = new RemoteAction(iconNext, "Siguiente", "Siguiente", nextIntent);
            actionNext.setEnabled(isNextEnabled);
            actions.add(actionNext);

            android.app.PictureInPictureParams.Builder pipBuilder = new android.app.PictureInPictureParams.Builder();
            pipBuilder.setActions(actions);
            pipBuilder.setAspectRatio(new android.util.Rational(16, 9));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pipBuilder.setAutoEnterEnabled(true);
            }
            try {
                activity.setPictureInPictureParams(pipBuilder.build());
            } catch (IllegalStateException e) {
                // Ignore if called before activity is fully created
            }
        }
    }
}
