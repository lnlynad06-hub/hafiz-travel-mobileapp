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
import android.view.View;
import java.util.Calendar;

public class PrayerTimeScheduler {

    private static final String PREFS_NAME = "prayer_times_cache";

    // Panggil ni SETIAP KALI dapat waktu solat baru (dalam MainActivity),
    // dengan epochSeconds untuk setiap waktu (dari JAKIM API atau Adhan fallback)
    public static void scheduleAll(Context context, long fajrEpoch, long dhuhrEpoch,
                                   long asrEpoch, long maghribEpoch, long ishaEpoch) {
        if (context == null) return;

        // Simpan untuk reschedule lepas reboot atau bila feature di-enable
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putLong("fajr", fajrEpoch)
                .putLong("dhuhr", dhuhrEpoch)
                .putLong("asr", asrEpoch)
                .putLong("maghrib", maghribEpoch)
                .putLong("isha", ishaEpoch)
                .apply();

        if (!OnboardingManager.isPrayerFeatureEnabled(context) || !OnboardingManager.isAzanFeatureEnabled(context)) {
            cancelAllAlarms(context);
            return;
        }

        scheduleOne(context, "fajr", fajrEpoch, 1, false);
        scheduleOne(context, "dhuhr", dhuhrEpoch, 2, false);
        scheduleOne(context, "asr", asrEpoch, 3, false);
        scheduleOne(context, "maghrib", maghribEpoch, 4, false);
        scheduleOne(context, "isha", ishaEpoch, 5, false);

        // Schedule 5-minute pre-prayer notification reminders
        scheduleOne(context, "fajr", fajrEpoch - 300, 11, true);
        scheduleOne(context, "dhuhr", dhuhrEpoch - 300, 12, true);
        scheduleOne(context, "asr", asrEpoch - 300, 13, true);
        scheduleOne(context, "maghrib", maghribEpoch - 300, 14, true);
        scheduleOne(context, "isha", ishaEpoch - 300, 15, true);
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

        if (fajr == 0) return; // belum ada cache, tunggu app dibuka

        scheduleOne(context, "fajr", fajr, 1, false);
        scheduleOne(context, "dhuhr", dhuhr, 2, false);
        scheduleOne(context, "asr", asr, 3, false);
        scheduleOne(context, "maghrib", maghrib, 4, false);
        scheduleOne(context, "isha", isha, 5, false);

        scheduleOne(context, "fajr", fajr - 300, 11, true);
        scheduleOne(context, "dhuhr", dhuhr - 300, 12, true);
        scheduleOne(context, "asr", asr - 300, 13, true);
        scheduleOne(context, "maghrib", maghrib - 300, 14, true);
        scheduleOne(context, "isha", isha - 300, 15, true);
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

    private static void scheduleOne(Context context, String prayerName, long epochSeconds, int requestCode, boolean isReminder) {
        if (epochSeconds <= 0) {
            return;
        }

        long triggerTimeMillis = epochSeconds * 1000L;

        if (triggerTimeMillis < System.currentTimeMillis()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, PrayerAlarmReceiver.class);
        intent.putExtra("prayer_name", prayerName);
        intent.putExtra("is_reminder", isReminder);

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
            return; // Hormati pilihan 'Mungkin Nanti'
        }
        if (OnboardingManager.isBatteryPromptShown(context)) {
            return; // Sudah pernah ditanya, elakkan prompt berulang
        }
        showBatteryOptimizationDialog(context, false);
    }

    public static void showBatteryOptimizationDialog(Context context, boolean forceShow) {
        if (context == null) return;
        android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        String packageName = context.getPackageName();

        if (pm != null && pm.isIgnoringBatteryOptimizations(packageName) && !forceShow) {
            return; // Dah exempted, tidak perlu tanya lagi
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
                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + packageName));
                    context.startActivity(intent);
                } catch (Exception e) {
                    try {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception ignored) {
                    }
                }
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
        } catch (Exception ignored) {
        }
    }

    // Baca semula waktu solat yang dah di-cache, untuk refresh arc widget
    // tanpa perlu call API baru.
    public static long[] getCachedEpochs(Context context) {
        if (context == null) return null;
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