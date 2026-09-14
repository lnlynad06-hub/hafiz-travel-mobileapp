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

public class SessionManager {

    private static final String PREF_NAME = "hafiz_travel_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_PROFILE_STATS = "profile_stats";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    public SessionManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        ApiClient.setAuthToken(prefs.getString(KEY_AUTH_TOKEN, ""));
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveAuthSession(String token, UserDto user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_AUTH_TOKEN, token != null ? token : "");
        if (user != null) {
            editor.putString(KEY_USER_DATA, gson.toJson(user));
        }
        editor.apply();
        ApiClient.setAuthToken(token);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public String getAuthorizationHeader() {
        String token = getToken();
        if (token != null && !token.isEmpty()) {
            return "Bearer " + token;
        }
        return "";
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

    public String getUserEmail() {
        UserDto user = getUser();
        return user != null && user.email != null ? user.email : "";
    }

    public String getUserPhone() {
        UserDto user = getUser();
        return user != null && user.phone != null ? user.phone : "";
    }

    public String getUserId() {
        UserDto user = getUser();
        return user != null && user.id != null ? user.id : "";
    }

    public String getAuthToken() {
        return getToken();
    }

    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_USER_DATA)
                .remove(KEY_PROFILE_STATS)
                .apply();
        ApiClient.setAuthToken(null);
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
}
