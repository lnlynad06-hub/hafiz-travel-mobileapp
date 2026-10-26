package com.hafiztraveltours.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.hafiztraveltours.app.models.AppNotification;
import com.hafiztraveltours.app.utils.AppNotificationManager;
import com.hafiztraveltours.app.utils.OnboardingManager;

import java.util.Locale;

public class PrayerAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "PrayerAlarmReceiver";
    private static final String PREFS_TRACKER = "prayer_playback_tracker";

    // Maximum acceptable drift: 15 minutes (900 seconds).
    // An alarm delayed by Android Doze/battery-saver beyond 15 minutes must never
    // blast Azan (e.g. late night or midnight for an Isha alarm).
    public static final long MAX_ALLOWED_DRIFT_SECONDS = 15 * 60L;

    public static final int STATE_IDLE = 0;
    public static final int STATE_DISPATCHED = 1;
    public static final int STATE_COMPLETED = 2;
    public static final int STATE_STOPPED = 3;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null) return;
        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            Log.i(TAG, "Prayer or Azan feature disabled, ignoring alarm trigger.");
            return;
        }

        String prayerName = intent != null ? intent.getStringExtra("prayer_name") : null;
        boolean isReminder = intent != null && intent.getBooleanExtra("is_reminder", false);
        long scheduledEpoch = intent != null ? intent.getLongExtra("scheduled_epoch", 0) : 0;
        String prayerDate = intent != null ? intent.getStringExtra("prayer_date") : null;
        String prayerZone = intent != null ? intent.getStringExtra("prayer_zone") : null;

        long currentEpoch = System.currentTimeMillis() / 1000L;
        long driftSeconds = scheduledEpoch > 0 ? (currentEpoch - scheduledEpoch) : 0;

        if (prayerDate == null || prayerDate.isEmpty()) {
            prayerDate = PrayerTimeScheduler.getMalaysiaDateString(scheduledEpoch > 0 ? scheduledEpoch : currentEpoch);
        }

        // 1. Freshness / Stale Alarm Guard:
        if (scheduledEpoch > 0) {
            if (isStaleTrigger(scheduledEpoch, currentEpoch)) {
                Log.w(TAG, "STALE_ALARM_DROPPED: prayer=" + prayerName +
                        ", date=" + prayerDate +
                        ", zone=" + prayerZone +
                        ", timezone=Asia/Kuala_Lumpur" +
                        ", scheduledEpoch=" + scheduledEpoch +
                        ", actualEpoch=" + currentEpoch +
                        ", driftSeconds=" + driftSeconds + "s (> " + MAX_ALLOWED_DRIFT_SECONDS + "s limit). Dropping stale playback.");
                return;
            }
            if (isAnomalouslyEarlyTrigger(scheduledEpoch, currentEpoch)) {
                Log.w(TAG, "EARLY_ALARM_DROPPED: prayer=" + prayerName +
                        ", driftSeconds=" + driftSeconds + "s. Dropping anomalous trigger.");
                return;
            }
        }

        // 2. Duplicate Playback Guard:
        if (!isReminder) {
            if (isPrayerPlaybackCompletedOrStopped(context, prayerName, prayerDate)) {
                Log.w(TAG, "DUPLICATE_PLAYBACK_PREVENTED: prayer=" + prayerName +
                        " on date=" + prayerDate + " was already played or stopped. Dropping duplicate alarm.");
                return;
            }
            markPrayerAsDispatched(context, prayerName, prayerDate);
        }

        // 3. Diagnostic Logging:
        Log.i(TAG, "PRAYER_ALARM_DISPATCH: prayer=" + prayerName +
                ", date=" + prayerDate +
                ", zone=" + prayerZone +
                ", timezone=Asia/Kuala_Lumpur" +
                ", scheduledEpoch=" + scheduledEpoch +
                ", triggerEpoch=" + currentEpoch +
                ", driftSeconds=" + driftSeconds +
                ", isReminder=" + isReminder);

        // 4. Notification / Playback Execution:
        if (isReminder) {
            String nameEn = getPrayerNameEn(prayerName);
            String nameBm = getPrayerNameBm(prayerName);
            String titleEn = "Prayer Reminder";
            String titleBm = "Peringatan Solat";
            String msgEn = nameEn + " prayer is in 5 minutes.";
            String msgBm = "Waktu solat " + nameBm + " akan masuk dalam 5 minit.";

            String notifId = "prayer_reminder_" + (prayerName != null ? prayerName.toLowerCase(Locale.ROOT) : "solat") + "_" + prayerDate;
            AppNotification notif = new AppNotification(
                    notifId, titleEn, titleBm, msgEn, msgBm,
                    System.currentTimeMillis(), false, "prayer_reminder");
            AppNotificationManager.postNotification(context, notif);
            return;
        }

        Intent serviceIntent = new Intent(context, AzanService.class);
        serviceIntent.putExtra("prayer_name", prayerName);
        serviceIntent.putExtra("prayer_date", prayerDate);
        serviceIntent.putExtra("prayer_zone", prayerZone);
        serviceIntent.putExtra("scheduled_epoch", scheduledEpoch);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    public static boolean isPrayerPlaybackCompletedOrStopped(Context context, String prayerName, String prayerDate) {
        if (context == null) return false;
        return isPrayerPlaybackCompletedOrStopped(context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE), prayerName, prayerDate);
    }

    public static boolean isPrayerPlaybackCompletedOrStopped(SharedPreferences prefs, String prayerName, String prayerDate) {
        if (prefs == null || prayerName == null || prayerDate == null) return false;
        String key = buildTrackerKey(prayerName, prayerDate);
        int state = prefs.getInt(key, STATE_IDLE);
        return state == STATE_COMPLETED || state == STATE_STOPPED;
    }

    public static void markPrayerAsDispatched(Context context, String prayerName, String prayerDate) {
        if (context != null) {
            markPrayerAsDispatched(context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE), prayerName, prayerDate);
        }
    }

    public static void markPrayerAsDispatched(SharedPreferences prefs, String prayerName, String prayerDate) {
        setPrayerState(prefs, prayerName, prayerDate, STATE_DISPATCHED);
    }

    public static void markPrayerAsPlayed(Context context, String prayerName, String prayerDate) {
        if (context != null) {
            markPrayerAsPlayed(context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE), prayerName, prayerDate);
        }
    }

    public static void markPrayerAsPlayed(SharedPreferences prefs, String prayerName, String prayerDate) {
        setPrayerState(prefs, prayerName, prayerDate, STATE_COMPLETED);
    }

    public static void markPrayerAsStopped(Context context, String prayerName, String prayerDate) {
        if (context != null) {
            markPrayerAsStopped(context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE), prayerName, prayerDate);
        }
    }

    public static void markPrayerAsStopped(SharedPreferences prefs, String prayerName, String prayerDate) {
        setPrayerState(prefs, prayerName, prayerDate, STATE_STOPPED);
    }

    public static void clearTracker(Context context) {
        if (context != null) {
            clearTracker(context.getSharedPreferences(PREFS_TRACKER, Context.MODE_PRIVATE));
        }
    }

    public static void clearTracker(SharedPreferences prefs) {
        if (prefs == null) return;
        prefs.edit().clear().apply();
    }

    static boolean isStaleTrigger(long scheduledEpoch, long currentEpoch) {
        return scheduledEpoch > 0 && currentEpoch - scheduledEpoch > MAX_ALLOWED_DRIFT_SECONDS;
    }

    static boolean isAnomalouslyEarlyTrigger(long scheduledEpoch, long currentEpoch) {
        return scheduledEpoch > 0 && currentEpoch - scheduledEpoch < -300;
    }

    private static void setPrayerState(SharedPreferences prefs, String prayerName, String prayerDate, int state) {
        if (prefs == null || prayerName == null || prayerDate == null) return;
        String key = buildTrackerKey(prayerName, prayerDate);
        prefs.edit()
                .putInt(key, state)
                .putLong(key + "_time", System.currentTimeMillis())
                .apply();
    }

    private static String buildTrackerKey(String prayerName, String prayerDate) {
        return prayerName.trim().toLowerCase(Locale.ROOT) + "_" + prayerDate.trim();
    }

    private String getPrayerNameEn(String key) {
        if (key == null) return "Prayer";
        switch (key.toLowerCase(Locale.ROOT)) {
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
        switch (key.toLowerCase(Locale.ROOT)) {
            case "fajr": return "Subuh";
            case "dhuhr": return "Zohor";
            case "asr": return "Asar";
            case "maghrib": return "Maghrib";
            case "isha": return "Isyak";
            default: return key;
        }
    }
}
