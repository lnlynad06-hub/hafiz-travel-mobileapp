package com.hafiztraveltours.app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.OnboardingManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PrayerTimeScheduler {

    private static final String TAG = "PrayerTimeScheduler";
    private static final String PREFS_NAME = "prayer_times_cache";

    public static final TimeZone TIME_ZONE_MALAYSIA = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    public static String getMalaysiaDateString(long epochSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TIME_ZONE_MALAYSIA);
        return sdf.format(new Date(epochSeconds > 0 ? epochSeconds * 1000L : System.currentTimeMillis()));
    }

    public static void scheduleAll(Context context, long fajrEpoch, long dhuhrEpoch,
                                   long asrEpoch, long maghribEpoch, long ishaEpoch) {
        String derivedDate = getMalaysiaDateString(fajrEpoch > 0 ? fajrEpoch : System.currentTimeMillis() / 1000L);
        scheduleAll(context, fajrEpoch, dhuhrEpoch, asrEpoch, maghribEpoch, ishaEpoch, null, derivedDate);
    }

    public static void scheduleAll(Context context, long fajrEpoch, long dhuhrEpoch,
                                   long asrEpoch, long maghribEpoch, long ishaEpoch,
                                   String zone, String prayerDate) {
        if (context == null) return;

        if (prayerDate == null || prayerDate.trim().isEmpty()) {
            prayerDate = getMalaysiaDateString(fajrEpoch > 0 ? fajrEpoch : System.currentTimeMillis() / 1000L);
        }

        // Simpan cache untuk reschedule lepas reboot atau bila feature di-enable
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("fajr", fajrEpoch)
                .putLong("dhuhr", dhuhrEpoch)
                .putLong("asr", asrEpoch)
                .putLong("maghrib", maghribEpoch)
                .putLong("isha", ishaEpoch)
                .putString("cache_date", prayerDate)
                .putString("cache_zone", zone != null ? zone : "")
                .putLong("cache_timestamp", System.currentTimeMillis())
                .apply();

        // Batalkan sebarang jadual lama dahulu untuk mengelakkan duplicate active alarms
        cancelAllAlarms(context);

        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            Log.i(TAG, "Prayer or Azan disabled in onboarding, cancelled all alarms.");
            return;
        }

        Log.i(TAG, "Scheduling prayer alarms: date=" + prayerDate + ", zone=" + zone +
                ", fajr=" + fajrEpoch + ", dhuhr=" + dhuhrEpoch +
                ", asr=" + asrEpoch + ", maghrib=" + maghribEpoch + ", isha=" + ishaEpoch);

        // Schedule exact Azan alarms
        scheduleOne(context, "fajr", fajrEpoch, 1, false, prayerDate, zone);
        scheduleOne(context, "dhuhr", dhuhrEpoch, 2, false, prayerDate, zone);
        scheduleOne(context, "asr", asrEpoch, 3, false, prayerDate, zone);
        scheduleOne(context, "maghrib", maghribEpoch, 4, false, prayerDate, zone);
        scheduleOne(context, "isha", ishaEpoch, 5, false, prayerDate, zone);

        // Schedule 5-minute pre-prayer notification reminders
        scheduleOne(context, "fajr", fajrEpoch - 300, 11, true, prayerDate, zone);
        scheduleOne(context, "dhuhr", dhuhrEpoch - 300, 12, true, prayerDate, zone);
        scheduleOne(context, "asr", asrEpoch - 300, 13, true, prayerDate, zone);
        scheduleOne(context, "maghrib", maghribEpoch - 300, 14, true, prayerDate, zone);
        scheduleOne(context, "isha", ishaEpoch - 300, 15, true, prayerDate, zone);
    }

    // Dipanggil oleh BootReceiver atau ProfileActivity, guna cache tersimpan (tak perlu network call)
    public static void rescheduleFromCache(Context context) {
        if (context == null) return;
        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            cancelAllAlarms(context);
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long fajr = prefs.getLong("fajr", 0);
        long dhuhr = prefs.getLong("dhuhr", 0);
        long asr = prefs.getLong("asr", 0);
        long maghrib = prefs.getLong("maghrib", 0);
        long isha = prefs.getLong("isha", 0);
        String cachedDate = prefs.getString("cache_date", null);
        String cachedZone = prefs.getString("cache_zone", "");

        if (fajr == 0) {
            Log.d(TAG, "rescheduleFromCache: No cache available.");
            return;
        }

        String todayMalaysia = getMalaysiaDateString(System.currentTimeMillis() / 1000L);
        if (cachedDate == null || !cachedDate.equals(todayMalaysia)) {
            Log.i(TAG, "rescheduleFromCache: Cached date (" + cachedDate +
                    ") is not today in Malaysia (" + todayMalaysia + "). Skipping obsolete alarm recreation.");
            cancelAllAlarms(context);
            return;
        }

        cancelAllAlarms(context);

        Log.i(TAG, "Rescheduling from cache for date=" + cachedDate + ", zone=" + cachedZone);

        scheduleOne(context, "fajr", fajr, 1, false, cachedDate, cachedZone);
        scheduleOne(context, "dhuhr", dhuhr, 2, false, cachedDate, cachedZone);
        scheduleOne(context, "asr", asr, 3, false, cachedDate, cachedZone);
        scheduleOne(context, "maghrib", maghrib, 4, false, cachedDate, cachedZone);
        scheduleOne(context, "isha", isha, 5, false, cachedDate, cachedZone);

        scheduleOne(context, "fajr", fajr - 300, 11, true, cachedDate, cachedZone);
        scheduleOne(context, "dhuhr", dhuhr - 300, 12, true, cachedDate, cachedZone);
        scheduleOne(context, "asr", asr - 300, 13, true, cachedDate, cachedZone);
        scheduleOne(context, "maghrib", maghrib - 300, 14, true, cachedDate, cachedZone);
        scheduleOne(context, "isha", isha - 300, 15, true, cachedDate, cachedZone);
    }

    public static void cancelAllAlarms(Context context) {
        if (context == null) return;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        for (int requestCode = 1; requestCode <= 15; requestCode++) {
            try {
                Intent intent = new Intent(context, PrayerAlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, requestCode, intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            } catch (Exception ignored) {}
        }
    }

    private static void scheduleOne(Context context, String prayerName, long epochSeconds,
                                    int requestCode, boolean isReminder, String prayerDate, String zone) {
        if (epochSeconds <= 0) {
            return;
        }

        long triggerTimeMillis = epochSeconds * 1000L;
        long nowMillis = System.currentTimeMillis();

        if (triggerTimeMillis <= nowMillis) {
            // Masa sudah berlalu, jangan schedule untuk mengelakkan azan berbunyi lewat / midnight
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.putExtra("prayer_name", prayerName);
        intent.putExtra("is_reminder", isReminder);
        intent.putExtra("scheduled_epoch", epochSeconds);
        intent.putExtra("prayer_date", prayerDate);
        intent.putExtra("prayer_zone", zone);

        // Batalkan intent lama dengan request code ini sekiranya wujud
        try {
            PendingIntent existing = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (existing != null) {
                alarmManager.cancel(existing);
                existing.cancel();
            }
        } catch (Exception ignored) {}

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean canScheduleExact = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms();
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        }

        Log.d(TAG, "Scheduled alarm: prayer=" + prayerName + ", reminder=" + isReminder +
                ", date=" + prayerDate + ", epoch=" + epochSeconds + ", inSec=" + ((triggerTimeMillis - nowMillis) / 1000L));
    }

    // Panggil ni dari MainActivity untuk minta user enable "exact alarm"
    // permission secara manual (Android 12+ sahaja).
    public static void requestExactAlarmPermissionIfNeeded(Context context) {
        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) return;
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

    public static boolean isBatteryOptimizationIgnored(Context context) {
        if (context == null) return true;
        android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    // Cadangan lembut untuk user benarkan background execution supaya azan/notification
    // berfungsi konsisten walaupun app ditutup.
    public static void requestBatteryOptimizationExemption(Context context) {
        if (context == null) return;
        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            return;
        }
        if (OnboardingManager.isBatteryPromptShown(context)) {
            return;
        }
        showBatteryOptimizationDialog(context, false);
    }

    public static void showBatteryOptimizationDialog(Context context, boolean forceShow) {
        if (context == null) return;
        android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();

        if (pm != null && pm.isIgnoringBatteryOptimizations(packageName) && !forceShow) {
            return;
        }

        if (context instanceof android.app.Activity && ((android.app.Activity) context).isFinishing()) {
            return;
        }

        android.app.Dialog dialog = new android.app.Dialog(context, R.style.FadeDialogTheme);
        dialog.setContentView(R.layout.dialog_keep_azan_working);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90);
            lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        View btnSetupNow = dialog.findViewById(R.id.btnBatterySetupNow);
        View btnLater = dialog.findViewById(R.id.btnBatteryLater);

        if (btnSetupNow != null) {
            btnSetupNow.setOnClickListener(v -> {
                HapticUtil.click(v);
                OnboardingManager.setBatteryPromptShown(context, true);
                dialog.dismiss();
                try {
                    context.startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception ignored) {}
            });
        }

        if (btnLater != null) {
            btnLater.setOnClickListener(v -> {
                HapticUtil.click(v);
                OnboardingManager.setBatteryPromptShown(context, true);
                dialog.dismiss();
            });
        }

        dialog.setOnCancelListener(d -> {
            OnboardingManager.setBatteryPromptShown(context, true);
        });

        try {
            dialog.show();
        } catch (Exception ignored) {}
    }

    // Baca semula waktu solat yang dah di-cache, untuk refresh arc widget
    // tanpa perlu call API baru.
    public static long[] getCachedEpochs(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long fajr = prefs.getLong("fajr", 0);
        if (fajr == 0) return null;

        return new long[]{
                fajr,
                prefs.getLong("dhuhr", 0),
                prefs.getLong("asr", 0),
                prefs.getLong("maghrib", 0),
                prefs.getLong("isha", 0)
        };
    }
}
