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
        this.prefs = SecurePrefs.wrap(context.getApplicationContext(), PREF_NAME);
        this.gson = new Gson();
        String savedToken = prefs.getString(KEY_AUTH_TOKEN, "");
        if (savedToken != null && !savedToken.trim().isEmpty()) {
            ApiClient.setAuthToken(savedToken.trim());
        }
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        } else {
            String savedToken = instance.prefs.getString(KEY_AUTH_TOKEN, "");
            if (savedToken != null && !savedToken.trim().isEmpty()) {
                ApiClient.setAuthToken(savedToken.trim());
            }
        }
        return instance;
    }

    public void saveAuthSession(String token, UserDto user) {
        SharedPreferences.Editor editor = prefs.edit();
        boolean hasToken = token != null && !token.trim().isEmpty();
        if (hasToken) {
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_AUTH_TOKEN, token.trim());
            ApiClient.setAuthToken(token.trim());
        } else if (isLoggedIn() && getToken() != null && !getToken().trim().isEmpty()) {
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
        } else {
            editor.putBoolean(KEY_IS_LOGGED_IN, false);
        }
        if (user != null) {
            editor.putString(KEY_USER_DATA, gson.toJson(user));
        }
        editor.apply();
    }

    public void saveUser(UserDto user) {
        if (user != null) {
            prefs.edit().putString(KEY_USER_DATA, gson.toJson(user)).apply();
        }
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        String token = prefs.getString(KEY_AUTH_TOKEN, "");
        if (token != null && !token.trim().isEmpty()) {
            ApiClient.setAuthToken(token.trim());
        }
        return token;
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
