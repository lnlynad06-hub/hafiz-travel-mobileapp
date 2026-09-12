package com.hafiztraveltours.app.services;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.app.*;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class AzanService extends Service {

    private static final String CHANNEL_ID = "azan_channel";
    private static final int NOTIFICATION_ID = 5001;
    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String prayerName = intent != null ? intent.getStringExtra("prayer_name") : "Solat";

        startForeground(NOTIFICATION_ID, buildNotification(prayerName));
        playAzan(prayerName);

        return START_NOT_STICKY;
    }

    private void playAzan(String prayerName) {
        try {
            // Subuh guna audio khas (ada tambahan "As-Salatu Khairun Minan-Naum"),
            // waktu solat lain kongsi audio azan biasa.
            int audioResource = "Subuh".equals(prayerName) ? R.raw.azan_subuh : R.raw.azan;

            mediaPlayer = MediaPlayer.create(this, audioResource);
            if (mediaPlayer == null) {
                stopSelf();
                return;
            }
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setOnCompletionListener(mp -> stopSelf());
            mediaPlayer.start();
        } catch (Exception e) {
            stopSelf();
        }
    }

    private Notification buildNotification(String prayerName) {
        Intent stopIntent = new Intent(this, StopAzanReceiver.class);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Waktu " + prayerName)
                .setContentText("Azan sedang berkumandang")
                .setSmallIcon(R.drawable.ic_notifications)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(0, "Berhenti", stopPendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Notifikasi Azan", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifikasi untuk waktu solat & azan");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public void stopAzan() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
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