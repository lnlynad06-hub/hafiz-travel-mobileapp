package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages first-launch onboarding, prayer/azan, notifications, and battery optimization state.
 * Fully decoupled from authentication states and persistent across app lifecycles.
 */
public final class OnboardingManager {

    private static final String PREF_NAME = "app_onboarding_prefs";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_PRAYER_FEATURE_ENABLED = "prayer_feature_enabled";
    private static final String KEY_QIBLA_FEATURE_ENABLED = "qibla_feature_enabled";
    private static final String KEY_AZAN_FEATURE_ENABLED = "azan_feature_enabled";
    private static final String KEY_NOTIFICATION_FEATURE_ENABLED = "notification_feature_enabled";
    private static final String KEY_BATTERY_PROMPT_SHOWN = "battery_prompt_shown";

    private OnboardingManager() {
        // Utility class
    }

    public static boolean isOnboardingCompleted(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
                .apply();
    }

    public static boolean isPrayerFeatureEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PRAYER_FEATURE_ENABLED, false);
    }

    public static boolean isPrayerTimesEnabled(Context context) {
        return isPrayerFeatureEnabled(context);
    }

    public static void setPrayerFeatureEnabled(Context context, boolean enabled) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PRAYER_FEATURE_ENABLED, enabled)
                .apply();
    }

    public static boolean isQiblaFeatureEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_QIBLA_FEATURE_ENABLED, false);
    }

    public static boolean isQiblaEnabled(Context context) {
        return isQiblaFeatureEnabled(context);
    }

    public static void setQiblaFeatureEnabled(Context context, boolean enabled) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_QIBLA_FEATURE_ENABLED, enabled)
                .apply();
    }

    public static boolean isAzanFeatureEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AZAN_FEATURE_ENABLED, false);
    }

    public static boolean isAzanEnabled(Context context) {
        return isAzanFeatureEnabled(context);
    }

    public static void setAzanFeatureEnabled(Context context, boolean enabled) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AZAN_FEATURE_ENABLED, enabled)
                .apply();
    }

    public static boolean isNotificationFeatureEnabled(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NOTIFICATION_FEATURE_ENABLED, false);
    }

    public static boolean isNotificationsEnabled(Context context) {
        return isNotificationFeatureEnabled(context);
    }

    public static void setNotificationFeatureEnabled(Context context, boolean enabled) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTIFICATION_FEATURE_ENABLED, enabled)
                .apply();
    }

    public static boolean isBatteryPromptShown(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BATTERY_PROMPT_SHOWN, false);
    }

    public static void setBatteryPromptShown(Context context, boolean shown) {
        if (context == null) return;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BATTERY_PROMPT_SHOWN, shown)
                .apply();
    }
}
