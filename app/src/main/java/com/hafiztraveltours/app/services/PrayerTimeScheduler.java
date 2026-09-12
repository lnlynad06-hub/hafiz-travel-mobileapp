package com.hafiztraveltours.app.services;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import java.util.Calendar;

public class PrayerTimeScheduler {

    private static final String PREFS_NAME = "prayer_times_cache";

    // Panggil ni SETIAP KALI dapat waktu solat baru (dalam MainActivity),
    // dengan epochSeconds untuk setiap waktu (dari JAKIM API atau Adhan fallback)
    public static void scheduleAll(Context context, long fajrEpoch, long dhuhrEpoch,
                                   long asrEpoch, long maghribEpoch, long ishaEpoch) {
        // Simpan untuk reschedule lepas reboot
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("fajr", fajrEpoch)
                .putLong("dhuhr", dhuhrEpoch)
                .putLong("asr", asrEpoch)
                .putLong("maghrib", maghribEpoch)
                .putLong("isha", ishaEpoch)
                .apply();

        scheduleOne(context, "Subuh", fajrEpoch, 1);
        scheduleOne(context, "Zohor", dhuhrEpoch, 2);
        scheduleOne(context, "Asar", asrEpoch, 3);
        scheduleOne(context, "Maghrib", maghribEpoch, 4);
        scheduleOne(context, "Isyak", ishaEpoch, 5);
    }

    // Dipanggil oleh BootReceiver, guna cache tersimpan (tak perlu network call)
    public static void rescheduleFromCache(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long fajr = prefs.getLong("fajr", 0);
        long dhuhr = prefs.getLong("dhuhr", 0);
        long asr = prefs.getLong("asr", 0);
        long maghrib = prefs.getLong("maghrib", 0);
        long isha = prefs.getLong("isha", 0);

        if (fajr == 0) return; // belum ada cache, tunggu app dibuka

        scheduleOne(context, "Subuh", fajr, 1);
        scheduleOne(context, "Zohor", dhuhr, 2);
        scheduleOne(context, "Asar", asr, 3);
        scheduleOne(context, "Maghrib", maghrib, 4);
        scheduleOne(context, "Isyak", isha, 5);
    }

    private static void scheduleOne(Context context, String prayerName, long epochSeconds, int requestCode) {
        if (epochSeconds == 0) {
            android.util.Log.d("PrayerScheduler", prayerName + ": SKIP - epochSeconds = 0");
            return;
        }

        long triggerTimeMillis = epochSeconds * 1000L;

        if (triggerTimeMillis < System.currentTimeMillis()) {
            android.util.Log.d("PrayerScheduler", prayerName + ": SKIP - waktu dah lepas ("
                    + new java.util.Date(triggerTimeMillis) + " < sekarang)");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            android.util.Log.e("PrayerScheduler", prayerName + ": ERROR - AlarmManager null");
            return;
        }

        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.putExtra("prayer_name", prayerName);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean canScheduleExact = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms();
        }

        android.util.Log.d("PrayerScheduler", prayerName + ": SCHEDULING untuk "
                + new java.util.Date(triggerTimeMillis) + " | canScheduleExact=" + canScheduleExact);

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        }

        android.util.Log.d("PrayerScheduler", prayerName + ": DONE scheduling");
    }

    // Panggil ni dari MainActivity untuk minta user enable "exact alarm"
    // permission secara manual (Android 12+ sahaja).
    public static void requestExactAlarmPermissionIfNeeded(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    // Cadangan lembut (bukan wajib) untuk user matikan battery optimization
    // supaya azan/notification berfungsi konsisten di background, terutama
    // untuk phone brand yang agresif (Infinix/XOS, Xiaomi/MIUI, dsb).
    public static void requestBatteryOptimizationExemption(Context context) {
        android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();

        if (pm == null || pm.isIgnoringBatteryOptimizations(packageName)) {
            return; // dah exempted, tak perlu tanya lagi
        }

        new android.app.AlertDialog.Builder(context)
                .setTitle("Pastikan Azan Berfungsi")
                .setMessage("Untuk pastikan notifikasi & azan berbunyi tepat pada waktunya walaupun app ini ditutup, disyorkan benarkan app ini berjalan tanpa had di latar belakang.\n\nLangkah ini pilihan sahaja, boleh disetkan kemudian melalui Tetapan.")
                .setPositiveButton("Setkan Sekarang", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(android.net.Uri.parse("package:" + packageName));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        try {
                            context.startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        } catch (Exception ignored) {
                        }
                    }
                })
                .setNegativeButton("Nanti", null)
                .show();
    }

    // Baca semula waktu solat yang dah di-cache, untuk refresh arc widget
    // tanpa perlu call API baru.
    public static long[] getCachedEpochs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long fajr = prefs.getLong("fajr", 0);
        if (fajr == 0) return null; // takde cache lagi

        return new long[]{
                fajr,
                prefs.getLong("dhuhr", 0),
                prefs.getLong("asr", 0),
                prefs.getLong("maghrib", 0),
                prefs.getLong("isha", 0)
        };
    }
}