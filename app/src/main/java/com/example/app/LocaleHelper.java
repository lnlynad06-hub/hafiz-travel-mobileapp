package com.hafiztraveltours.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * Handles saving and applying the user's chosen com.hafiztraveltours.app language.
 *
 * How to use in any Activity that should respect the chosen language:
 *   @Override
 *   protected void attachBaseContext(Context newBase) {
 *       super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
 *   }
 *
 * TODO: add the attachBaseContext override above to every other Activity
 * (Login, SignUp, MainActivity, etc.) so the whole com.hafiztraveltours.app respects the chosen
 * language, not just the Welcome screen.
 */
public class LocaleHelper {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    // "en" = English (default), "ms" = Bahasa Melayu
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_MALAY = "ms";
    public static final String LANGUAGE_ARABIC = "ar";
    public static final String LANGUAGE_KOREAN = "ko";
    public static final String LANGUAGE_JAPANESE = "ja";
    public static final String LANGUAGE_CHINESE = "zh";

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH); // English is the default
    }

    public static void saveLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /** Wraps the given context so its resources (strings) use the saved language. */
    public static Context applySavedLocale(Context context) {
        return applyLocale(context, getSavedLanguage(context));
    }

    public static Context applyLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
