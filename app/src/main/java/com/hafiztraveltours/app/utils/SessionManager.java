package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.hafiztraveltours.app.network.UserDto;

/**
 * Session single source of truth (Phase 5).
 *
 * <p>Ownership:
 * <ul>
 *   <li>Auth token + user JSON live here, encrypted via SecurePrefs ("hafiz_travel_session").</li>
 *   <li>{@code ApiClient}'s in-memory token is a write-only copy, synchronized from here
 *       through {@link #syncApiToken()} — the ONLY place that calls
 *       {@code ApiClient.setAuthToken()} (save, clear, init). Getters never touch ApiClient.</li>
 *   <li>Profile updates persist user data only and can never overwrite a stored token
 *       with null/empty (see {@link #saveAuthSession}).</li>
 * </ul>
 */
public class SessionManager {

    private static final String PREF_NAME = "hafiz_travel_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_PROFILE_STATS = "profile_stats";

    private static SessionManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Gson gson;

    public SessionManager(Context context) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.prefs = SecurePrefs.wrap(this.appContext, PREF_NAME);
        this.gson = new Gson();
        syncApiToken();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * The single synchronization point for the in-memory Retrofit token.
     * Copies the stored token into ApiClient, or clears ApiClient when no token
     * is stored (e.g. after logout). Called on init, save and clear only.
     */
    private void syncApiToken() {
        String savedToken = prefs.getString(KEY_AUTH_TOKEN, "");
        if (savedToken != null && !savedToken.trim().isEmpty()) {
            ApiClient.setAuthToken(savedToken.trim());
        } else {
            ApiClient.setAuthToken(null);
        }
    }

    public void saveAuthSession(String token, UserDto user) {
        SharedPreferences.Editor editor = prefs.edit();
        boolean hasToken = token != null && !token.trim().isEmpty();
        if (hasToken) {
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_AUTH_TOKEN, token.trim());
        } else if (isLoggedIn() && getToken() != null && !getToken().trim().isEmpty()) {
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
        } else {
            editor.putBoolean(KEY_IS_LOGGED_IN, false);
        }
        if (user != null) {
            editor.putString(KEY_USER_DATA, gson.toJson(user));
            syncProfileExtras(user);
        }
        editor.apply();
        syncApiToken();
    }

    public void saveUser(UserDto user) {
        if (user != null) {
            prefs.edit().putString(KEY_USER_DATA, gson.toJson(user)).apply();
            syncProfileExtras(user);
        }
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /** Pure read — never touches ApiClient (see {@link #syncApiToken()}). */
    public String getToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN, "");
        return token != null ? token : "";
    }

    /**
     * Fail-closed token gate (Phase 20): if KeyStore crypto has actually failed,
     * the stored token cannot be trusted — wipe the session so the user
     * re-authenticates instead of running on downgraded plaintext secrets.
     *
     * @return true when a fail-closed wipe was performed.
     */
    public boolean enforceCryptoIntegrity() {
        if (SecurePrefs.cryptoFailed() && isLoggedIn()) {
            clearSession();
            return true;
        }
        return false;
    }

    public UserDto getUser() {
        String json = prefs.getString(KEY_USER_DATA, null);
        if (json != null && !json.isEmpty()) {
            try {
                return gson.fromJson(json, UserDto.class);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public String getUserName() {
        UserDto user = getUser();
        return user != null && user.name != null ? user.name : "Tetamu Jemaah";
    }

    public String getUserNickname() {
        UserDto user = getUser();
        if (user != null && user.nickname != null && !user.nickname.trim().isEmpty()) {
            return user.nickname.trim();
        }
        if (user != null && user.name != null && !user.name.trim().isEmpty()) {
            return user.name.trim().split(" ")[0];
        }
        return "Tetamu Jemaah";
    }

    public String getUserEmail() {
        UserDto user = getUser();
        return user != null && user.email != null ? user.email : "";
    }

    public String getUserPhone() {
        UserDto user = getUser();
        return user != null && user.phone != null ? user.phone : "";
    }

    public String getAuthToken() {
        return getToken();
    }

    private static final String KEY_USER_DOCUMENTS = "user_documents";

    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_USER_DATA)
                .remove(KEY_PROFILE_STATS)
                .remove(KEY_USER_DOCUMENTS)
                .apply();
        if (appContext != null) {
            try {
                SecurePrefs.wrap(appContext, "user_profile").edit().clear().apply();
            } catch (Exception ignored) {}
        }
        syncApiToken();
    }

    public void saveProfileStats(com.hafiztraveltours.app.models.ProfileStatsDto stats) {
        prefs.edit().putString(KEY_PROFILE_STATS,
                stats != null ? gson.toJson(stats) : null).apply();
    }

    public com.hafiztraveltours.app.models.ProfileStatsDto getProfileStats() {
        String json = prefs.getString(KEY_PROFILE_STATS, null);
        if (json != null && !json.isEmpty()) {
            try {
                return gson.fromJson(json, com.hafiztraveltours.app.models.ProfileStatsDto.class);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public void saveDocuments(java.util.List<com.hafiztraveltours.app.models.DocumentDto> docs) {
        if (docs != null) {
            prefs.edit().putString(KEY_USER_DOCUMENTS, gson.toJson(docs)).apply();
        } else {
            prefs.edit().remove(KEY_USER_DOCUMENTS).apply();
        }
    }

    public java.util.List<com.hafiztraveltours.app.models.DocumentDto> getDocuments() {
        String json = prefs.getString(KEY_USER_DOCUMENTS, null);
        if (json != null && !json.isEmpty()) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.List<com.hafiztraveltours.app.models.DocumentDto>>() {}.getType();
                java.util.List<com.hafiztraveltours.app.models.DocumentDto> list = gson.fromJson(json, type);
                if (list != null) return list;
            } catch (Exception ignored) {}
        }
        return new java.util.ArrayList<>();
    }

    public java.util.Map<String, String> getProfileExtras() {
        if (appContext == null) return new java.util.HashMap<>();
        SharedPreferences profilePrefs = SecurePrefs.wrap(appContext, "user_profile");
        java.util.Map<String, String> out = new java.util.HashMap<>();
        String[] keys = {
                "name", "ic_no", "passport_no", "passport_expiry", "issuing_country",
                "gender", "date_of_birth", "nationality",
                "address", "address_line_1", "address_line_2", "postcode", "city",
                "state", "country", "emergency_name", "emergency_phone",
                "mahram_name", "mahram_relationship", "has_vaccine_cert"
        };
        for (String k : keys) {
            String v = profilePrefs.getString(k, "");
            if (v != null && !v.isEmpty()) out.put(k, v);
        }
        return out;
    }

    public void syncProfileExtras(UserDto user) {
        if (user == null || appContext == null) return;
        try {
            SharedPreferences profilePrefs = SecurePrefs.wrap(appContext, "user_profile");
            SharedPreferences.Editor editor = profilePrefs.edit();
            if (user.name != null && !user.name.isEmpty()) editor.putString("name", user.name);
            if (user.nickname != null && !user.nickname.isEmpty()) editor.putString("nickname", user.nickname);
            if (user.email != null && !user.email.isEmpty()) editor.putString("email", user.email);
            if (user.phone != null && !user.phone.isEmpty()) editor.putString("phone", user.phone);
            if (user.icNumber != null && !user.icNumber.isEmpty()) editor.putString("ic_no", user.icNumber);
            if (user.passportNumber != null && !user.passportNumber.isEmpty()) editor.putString("passport_no", user.passportNumber);
            if (user.passportExpiryDate != null && !user.passportExpiryDate.isEmpty()) editor.putString("passport_expiry", user.passportExpiryDate);
            if (user.issuingCountry != null && !user.issuingCountry.isEmpty()) editor.putString("issuing_country", user.issuingCountry);
            if (user.gender != null && !user.gender.isEmpty()) editor.putString("gender", user.gender);
            if (user.dateOfBirth != null && !user.dateOfBirth.isEmpty()) editor.putString("date_of_birth", user.dateOfBirth);
            if (user.nationality != null && !user.nationality.isEmpty()) editor.putString("nationality", user.nationality);
            if (user.addressLine1 != null && !user.addressLine1.isEmpty()) editor.putString("address_line_1", user.addressLine1);
            if (user.addressLine2 != null && !user.addressLine2.isEmpty()) editor.putString("address_line_2", user.addressLine2);
            if (user.postcode != null && !user.postcode.isEmpty()) editor.putString("postcode", user.postcode);
            if (user.city != null && !user.city.isEmpty()) editor.putString("city", user.city);
            if (user.state != null && !user.state.isEmpty()) editor.putString("state", user.state);
            if (user.country != null && !user.country.isEmpty()) editor.putString("country", user.country);
            if (user.address != null && !user.address.isEmpty()) editor.putString("address", user.address);
            if (user.emergencyName != null && !user.emergencyName.isEmpty()) editor.putString("emergency_name", user.emergencyName);
            if (user.emergencyPhone != null && !user.emergencyPhone.isEmpty()) editor.putString("emergency_phone", user.emergencyPhone);
            editor.apply();
        } catch (Exception ignored) {}
    }
}
