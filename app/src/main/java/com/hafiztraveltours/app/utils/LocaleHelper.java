package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Enterprise-grade Locale management for Hafiz Travel & Tours.
 * Handles persistence, context wrapping, formatting helpers, and dynamic language switching.
 */
public class LocaleHelper {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_MALAY = "ms";

    public static String getSavedLanguage(Context context) {
        if (context == null) return LANGUAGE_ENGLISH;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH);
        if (!LANGUAGE_ENGLISH.equalsIgnoreCase(saved) && !LANGUAGE_MALAY.equalsIgnoreCase(saved)) {
            prefs.edit().putString(KEY_LANGUAGE, LANGUAGE_ENGLISH).apply();
            try {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(LANGUAGE_ENGLISH));
            } catch (Exception ignored) {}
            return LANGUAGE_ENGLISH;
        }
        return saved;
    }

    public static void saveLanguage(Context context, String languageCode) {
        if (context == null || languageCode == null) return;
        if (!LANGUAGE_ENGLISH.equalsIgnoreCase(languageCode) && !LANGUAGE_MALAY.equalsIgnoreCase(languageCode)) {
            languageCode = LANGUAGE_ENGLISH;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        Locale.setDefault(getLocaleForCode(languageCode));
    }

    /**
     * Smoothly switches language and restarts the given activity without black screen or freezing.
     */
    public static void changeLanguage(Activity activity, String languageCode) {
        applyAndSaveLanguage(activity, languageCode);
    }

    public static void applyAndSaveLanguage(Activity activity, String languageCode) {
        if (activity == null || languageCode == null) return;
        saveLanguage(activity, languageCode);

        Locale locale = getLocaleForCode(languageCode);
        Locale.setDefault(locale);

        try {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
            AppCompatDelegate.setApplicationLocales(appLocale);
        } catch (Exception ignored) {}

        Resources resources = activity.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        config.setLayoutDirection(locale);
        try {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        } catch (Exception ignored) {}

        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
        activity.finish();

        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.lang_switch_fade_in, R.anim.lang_switch_fade_out);
        } else {
            activity.overridePendingTransition(R.anim.lang_switch_fade_in, R.anim.lang_switch_fade_out);
        }
    }

    public static Locale getCurrentLocale(Context context) {
        String lang = getSavedLanguage(context);
        return getLocaleForCode(lang);
    }

    public static Locale getLocaleForCode(String languageCode) {
        if (languageCode == null) return Locale.ENGLISH;
        switch (languageCode.toLowerCase()) {
            case LANGUAGE_MALAY:
                return new Locale("ms", "MY");
            case LANGUAGE_ENGLISH:
            default:
                return Locale.ENGLISH;
        }
    }

    public static String getLanguageBadge(String languageCode) {
        if (languageCode == null) return "EN";
        switch (languageCode.toLowerCase()) {
            case LANGUAGE_MALAY:
                return "BM";
            case LANGUAGE_ENGLISH:
            default:
                return "EN";
        }
    }

    /** Wraps the given context so its resources use the saved language. */
    public static Context applySavedLocale(Context context) {
        if (context == null) return null;
        return applyLocale(context, getSavedLanguage(context));
    }

    public static Context applyLocale(Context context, String languageCode) {
        Locale locale = getLocaleForCode(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        config.setLayoutDirection(locale);

        try {
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        } catch (Exception ignored) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.createConfigurationContext(config);
        } else {
            return context;
        }
    }
}
