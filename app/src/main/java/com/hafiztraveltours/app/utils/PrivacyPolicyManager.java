package com.hafiztraveltours.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages persistent storage and verification of the official Hafiz Travel Privacy Policy acceptance.
 * Fully decoupled from authentication and onboarding flags.
 */
public final class PrivacyPolicyManager {

    public static final String PREF_NAME = "app_privacy_policy_prefs";
    public static final String KEY_ACCEPTED_POLICY_VERSION = "accepted_policy_version";
    public static final String KEY_ACCEPTED_TIMESTAMP = "accepted_policy_timestamp";

    /**
     * Official Hafiz Travel Privacy Policy URL hosted on Huawei Sites.
     */
    public static final String OFFICIAL_POLICY_URL =
            "https://sites.google.com/view/hafiz-travel-huawei-policy/laman-utama";

    /**
     * Active policy version identifier.
     * Incrementing this version string requires renewed user consent on subsequent app launches.
     */
    public static final String CURRENT_POLICY_VERSION = "1.0";

    private PrivacyPolicyManager() {
        // Utility class
    }

    /**
     * Checks if the user has accepted the current active Privacy Policy version.
     */
    public static boolean isPrivacyPolicyAccepted(Context context) {
        if (context == null) return false;
        return isPrivacyPolicyAccepted(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Checks if the user has accepted a specific policy version.
     */
    public static boolean isPrivacyPolicyAccepted(Context context, String requiredVersion) {
        if (context == null || requiredVersion == null) return false;
        return isPrivacyPolicyAccepted(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE), requiredVersion);
    }

    /**
     * Checks if SharedPreferences contains acceptance of current policy version.
     */
    public static boolean isPrivacyPolicyAccepted(SharedPreferences prefs) {
        return isPrivacyPolicyAccepted(prefs, CURRENT_POLICY_VERSION);
    }

    /**
     * Checks if SharedPreferences contains acceptance of a specific policy version.
     */
    public static boolean isPrivacyPolicyAccepted(SharedPreferences prefs, String requiredVersion) {
        if (prefs == null || requiredVersion == null) return false;
        String acceptedVersion = prefs.getString(KEY_ACCEPTED_POLICY_VERSION, null);
        return requiredVersion.equals(acceptedVersion);
    }

    /**
     * Persistently saves or revokes acceptance of the current Privacy Policy version.
     */
    public static void setPrivacyPolicyAccepted(Context context, boolean accepted) {
        if (context == null) return;
        setPrivacyPolicyAccepted(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE), accepted);
    }

    /**
     * Persistently saves or revokes acceptance into SharedPreferences.
     */
    public static void setPrivacyPolicyAccepted(SharedPreferences prefs, boolean accepted) {
        setPrivacyPolicyAcceptedVersion(prefs, CURRENT_POLICY_VERSION, accepted);
    }

    /**
     * Persistently saves acceptance of a specific policy version into Context prefs.
     */
    public static void setPrivacyPolicyAcceptedVersion(Context context, String version, boolean accepted) {
        if (context == null) return;
        setPrivacyPolicyAcceptedVersion(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE), version, accepted);
    }

    /**
     * Persistently saves acceptance of a specific policy version into SharedPreferences.
     */
    public static void setPrivacyPolicyAcceptedVersion(SharedPreferences prefs, String version, boolean accepted) {
        if (prefs == null) return;
        SharedPreferences.Editor editor = prefs.edit();
        if (accepted && version != null) {
            editor.putString(KEY_ACCEPTED_POLICY_VERSION, version);
            editor.putLong(KEY_ACCEPTED_TIMESTAMP, System.currentTimeMillis());
        } else {
            editor.remove(KEY_ACCEPTED_POLICY_VERSION);
            editor.remove(KEY_ACCEPTED_TIMESTAMP);
        }
        editor.apply();
    }

    /**
     * Returns the currently stored accepted policy version, or null if unaccepted.
     */
    public static String getAcceptedPolicyVersion(Context context) {
        if (context == null) return null;
        return getAcceptedPolicyVersion(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Returns the currently stored accepted policy version from SharedPreferences.
     */
    public static String getAcceptedPolicyVersion(SharedPreferences prefs) {
        if (prefs == null) return null;
        return prefs.getString(KEY_ACCEPTED_POLICY_VERSION, null);
    }

    /**
     * Returns the timestamp (in epoch milliseconds) when the policy was accepted, or 0 if unaccepted.
     */
    public static long getAcceptedTimestamp(Context context) {
        if (context == null) return 0L;
        return getAcceptedTimestamp(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Returns the timestamp (in epoch milliseconds) when the policy was accepted from SharedPreferences.
     */
    public static long getAcceptedTimestamp(SharedPreferences prefs) {
        if (prefs == null) return 0L;
        return prefs.getLong(KEY_ACCEPTED_TIMESTAMP, 0L);
    }

    /**
     * Clears all stored privacy policy acceptance preferences.
     */
    public static void resetAcceptance(Context context) {
        if (context == null) return;
        resetAcceptance(context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE));
    }

    /**
     * Clears all stored privacy policy acceptance preferences from SharedPreferences.
     */
    public static void resetAcceptance(SharedPreferences prefs) {
        if (prefs == null) return;
        prefs.edit().clear().apply();
    }
}
