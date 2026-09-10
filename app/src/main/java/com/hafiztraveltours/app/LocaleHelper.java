package com.hafiztraveltours.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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
    public static final String LANGUAGE_ARABIC = "ar";
    public static final String LANGUAGE_KOREAN = "ko";
    public static final String LANGUAGE_JAPANESE = "ja";
    public static final String LANGUAGE_CHINESE = "zh";

    public static String getSavedLanguage(Context context) {
        if (context == null) return LANGUAGE_ENGLISH;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH);
    }

    public static void saveLanguage(Context context, String languageCode) {
        if (context == null || languageCode == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        try {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
            AppCompatDelegate.setApplicationLocales(appLocale);
        } catch (Exception ignored) {}
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
            case LANGUAGE_ARABIC:
                return new Locale("ar", "SA");
            case LANGUAGE_KOREAN:
                return Locale.KOREA;
            case LANGUAGE_JAPANESE:
                return Locale.JAPAN;
            case LANGUAGE_CHINESE:
                return Locale.SIMPLIFIED_CHINESE;
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
            case LANGUAGE_ARABIC:
                return "AR";
            case LANGUAGE_KOREAN:
                return "KO";
            case LANGUAGE_JAPANESE:
                return "JA";
            case LANGUAGE_CHINESE:
                return "ZH";
            case LANGUAGE_ENGLISH:
            default:
                return "EN";
        }
    }

    public static String getLanguageName(String languageCode) {
        if (languageCode == null) return "English";
        switch (languageCode.toLowerCase()) {
            case LANGUAGE_MALAY:
                return "Bahasa Melayu";
            case LANGUAGE_ARABIC:
                return "العربية";
            case LANGUAGE_KOREAN:
                return "한국어";
            case LANGUAGE_JAPANESE:
                return "日本語";
            case LANGUAGE_CHINESE:
                return "中文";
            case LANGUAGE_ENGLISH:
            default:
                return "English";
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

        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }
}
