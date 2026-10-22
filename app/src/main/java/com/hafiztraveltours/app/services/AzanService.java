package com.hafiztraveltours.app.services;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.OnboardingManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AzanService extends Service {

    private static final String TAG = "AzanService";
    public static final String ACTION_STOP_AZAN = "com.hafiztraveltours.app.action.STOP_AZAN";
    private static final String CHANNEL_ID = "azan_channel";
    private static final int NOTIFICATION_ID = 5001;

    private static volatile AzanService sInstance;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private String activePrayerKey;
    private String activePrayerDate;

    public static void stopActivePlayback(Context context) {
        AzanService instance = sInstance;
        if (instance != null) {
            instance.stopAzan();
        } else if (context != null) {
            try {
                Intent stopIntent = new Intent(context, AzanService.class);
                stopIntent.setAction(ACTION_STOP_AZAN);
                context.startService(stopIntent);
            } catch (Exception e) {
                try {
                    context.stopService(new Intent(context, AzanService.class));
                } catch (Exception ignored) {}
            }
        }
    }

    public static boolean isPlaying() {
        AzanService instance = sInstance;
        return instance != null && instance.mediaPlayer != null && instance.mediaPlayer.isPlaying();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_AZAN.equals(intent.getAction())) {
            Log.i(TAG, "Stop Azan action intent received in onStartCommand.");
            stopAzan();
            return START_NOT_STICKY;
        }

        if (!OnboardingManager.isPrayerFeatureEnabled(this) || !OnboardingManager.isAzanFeatureEnabled(this)) {
            Log.i(TAG, "Prayer or Azan feature disabled, stopping service.");
            dismissNotificationAndStop();
            return START_NOT_STICKY;
        }

        activePrayerKey = intent != null ? intent.getStringExtra("prayer_name") : null;
        activePrayerDate = intent != null ? intent.getStringExtra("prayer_date") : null;

        // Check if user already stopped or finished this prayer playback
        if (PrayerAlarmReceiver.isPrayerPlaybackCompletedOrStopped(this, activePrayerKey, activePrayerDate)) {
            Log.w(TAG, "Azan playback already stopped/completed for " + activePrayerKey + " on " + activePrayerDate + ", dropping duplicate start.");
            dismissNotificationAndStop();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification(activePrayerKey));
        playAzan(activePrayerKey);

        return START_NOT_STICKY;
    }

    /** Maps locale-neutral keys (fajr/dhuhr/...) to the localized prayer name. */
    private String resolvePrayerName(String prayerKey) {
        if (prayerKey == null) return "";
        switch (prayerKey.toLowerCase()) {
            case "fajr":
                return getString(R.string.prayer_name_subuh);
            case "dhuhr":
                return getString(R.string.prayer_name_zohor);
            case "asr":
                return getString(R.string.prayer_name_asar);
            case "maghrib":
                return getString(R.string.prayer_name_maghrib);
            case "isha":
                return getString(R.string.prayer_name_isyak);
            default:
                return prayerKey;
        }
    }

    private void requestAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(focusChange -> {
                            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                                Log.i(TAG, "Audio focus lost permanently, stopping azan.");
                                stopAzan();
                            }
                        })
                        .build();
                audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error requesting audio focus: " + e.getMessage());
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
                audioFocusRequest = null;
            } else {
                audioManager.abandonAudioFocus(null);
            }
        } catch (Exception ignored) {}
    }

    private void playAzan(String prayerKey) {
        try {
            // Clean up any existing media player first
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                } catch (Exception ignored) {}
                try {
                    mediaPlayer.release();
                } catch (Exception ignored) {}
                mediaPlayer = null;
            }

            requestAudioFocus();

            // Subuh uses special audio (with "As-Salatu Khairun Minan-Naum"),
            // other prayers share the standard azan audio.
            int audioResource = "fajr".equalsIgnoreCase(prayerKey) ? R.raw.azan_subuh : R.raw.azan;

            mediaPlayer = MediaPlayer.create(this, audioResource);
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer for azan resource.");
                dismissNotificationAndStop();
                return;
            }

            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.i(TAG, "Azan playback completed naturally.");
                onPlaybackFinishedNaturally();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error during playback: what=" + what + ", extra=" + extra);
                dismissNotificationAndStop();
                return true;
            });

            mediaPlayer.start();
            Log.i(TAG, "Azan playback started for: " + prayerKey);
        } catch (Exception e) {
            Log.e(TAG, "Exception starting azan playback: " + e.getMessage(), e);
            dismissNotificationAndStop();
        }
    }

    private Notification buildNotification(String prayerKey) {
        // Direct Service PendingIntent for Stop Azan
        Intent stopServiceIntent = new Intent(this, AzanService.class);
        stopServiceIntent.setAction(ACTION_STOP_AZAN);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopServiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Delete intent (swipe/dismiss if permissible) also stops playback
        Intent deleteIntent = new Intent(this, StopAzanReceiver.class);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                this, 1, deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String localizedPrayerName = resolvePrayerName(prayerKey);
        String title = getString(R.string.azan_notif_title, localizedPrayerName);
        String body = getString(R.string.azan_playing);
        String stopActionText = getString(R.string.azan_stop);

        NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_close,
                stopActionText,
                stopPendingIntent
        ).build();

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notifications)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(stopAction)
                .setDeleteIntent(deletePendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, getString(R.string.azan_channel_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.azan_channel_desc));
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public synchronized void stopAzan() {
        Log.i(TAG, "stopAzan() called. Terminating active audio playback.");
        // Mark prayer as stopped to prevent replay
        if (activePrayerKey != null) {
            PrayerAlarmReceiver.markPrayerAsStopped(this, activePrayerKey, activePrayerDate);
        }
        dismissNotificationAndStop();
    }

    private synchronized void onPlaybackFinishedNaturally() {
        if (activePrayerKey != null) {
            PrayerAlarmReceiver.markPrayerAsPlayed(this, activePrayerKey, activePrayerDate);
        }
        dismissNotificationAndStop();
    }

    private void dismissNotificationAndStop() {
        abandonAudioFocus();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Exception ignored) {}

        try {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignored) {}

        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        abandonAudioFocus();
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (Exception ignored) {}
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}