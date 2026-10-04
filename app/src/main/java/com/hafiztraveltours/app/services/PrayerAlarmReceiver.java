package com.hafiztraveltours.app.services;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class PrayerAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            return;
        }

        String prayerName = intent != null ? intent.getStringExtra("prayer_name") : null;
        boolean isReminder = intent != null && intent.getBooleanExtra("is_reminder", false);

        if (isReminder) {
            String nameEn = getPrayerNameEn(prayerName);
            String nameBm = getPrayerNameBm(prayerName);
            String titleEn = "Prayer Reminder";
            String titleBm = "Peringatan Solat";
            String msgEn = nameEn + " prayer is in 5 minutes.";
            String msgBm = "Waktu solat " + nameBm + " akan masuk dalam 5 minit.";

            String notifId = "prayer_reminder_" + (prayerName != null ? prayerName.toLowerCase() : "solat") + "_" + (System.currentTimeMillis() / (1000 * 60 * 60 * 24));
            AppNotification notif = new AppNotification(
                    notifId, titleEn, titleBm, msgEn, msgBm,
                    System.currentTimeMillis(), false, "prayer_reminder");
            AppNotificationManager.postNotification(context, notif);
            return;
        }

        Intent serviceIntent = new Intent(context, AzanService.class);
        serviceIntent.putExtra("prayer_name", prayerName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    private String getPrayerNameEn(String key) {
        if (key == null) return "Prayer";
        switch (key.toLowerCase()) {
            case "fajr": return "Fajr";
            case "dhuhr": return "Dhuhr";
            case "asr": return "Asr";
            case "maghrib": return "Maghrib";
            case "isha": return "Isha";
            default: return key;
        }
    }

    private String getPrayerNameBm(String key) {
        if (key == null) return "Solat";
        switch (key.toLowerCase()) {
            case "fajr": return "Subuh";
            case "dhuhr": return "Zohor";
            case "asr": return "Asar";
            case "maghrib": return "Maghrib";
            case "isha": return "Isyak";
            default: return key;
        }
    }
}