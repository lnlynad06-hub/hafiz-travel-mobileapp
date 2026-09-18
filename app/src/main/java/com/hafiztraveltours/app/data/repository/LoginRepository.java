package com.hafiztraveltours.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.network.AuthResponse;
import com.hafiztraveltours.app.network.GoogleLoginRequest;
import com.hafiztraveltours.app.network.LoginRequest;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.SecurePrefs;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

/**
 * Login data access (H1/Phase 11). Owns login + Google-login + forgot-password
 * API calls, session persistence and remember-me prefs. No Views, Toasts, Dialogs,
 * Intents or Activity handling — callers track and cancel the returned Calls.
 *
 * <p>Phase-5 contract: tokens go through {@code saveAuthSession} only when
 * non-empty (else {@code saveUser}); passwords are never stored.
 */
public class LoginRepository {

    private static final String PREF_AUTH = "auth_prefs";
    private static final String KEY_REMEMBER_ME = "pref_remember_me";
    private static final String KEY_SAVED_EMAIL = "pref_saved_email";

    private final Context appContext;
    private final SessionManager session;

    public LoginRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.session = SessionManager.getInstance(this.appContext);
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    /** Fresh login Call per invocation; caller owns cancellation. No retries. */
    public Call<ApiResponse<AuthResponse>> loginCall(LoginRequest request) {
        return api().login(request);
    }

    /** Fresh Google-login Call per invocation; caller owns cancellation. No retries. */
    public Call<ApiResponse<AuthResponse>> googleLoginCall(GoogleLoginRequest request) {
        return api().googleLogin(request);
    }

    /** Fresh forgot-password Call per invocation; caller owns cancellation. No retries. */
    public Call<ApiResponse<Object>> forgotPasswordCall(String email) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        return api().forgotPassword(body);
    }

    /**
     * Persists an auth result: token + user when the backend issued a token,
     * user only otherwise (identical rule to the previous inline implementation).
     */
    public void persistAuthResult(String token, UserDto user, UserDto fallbackUser) {
        UserDto effective = user != null ? user : fallbackUser;
        if (token != null && !token.trim().isEmpty()) {
            session.saveAuthSession(token, effective);
        } else {
            session.saveUser(effective);
        }
    }

    /** Clears any session (guest entry path, same as before). */
    public void clearSession() {
        session.clearSession();
    }

    /** Remember-me state (same keys/store as before). */
    public boolean isRememberMeChecked() {
        try {
            return authPrefs().getBoolean(KEY_REMEMBER_ME, false);
        } catch (Exception e) {
            return false;
        }
    }

    /** Saved email, or "" (same as before). */
    public String savedEmail() {
        try {
            String email = authPrefs().getString(KEY_SAVED_EMAIL, "");
            return email != null ? email : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Persists remember-me + email (clears email when unchecked, same as before). */
    public void saveRememberMe(String email, boolean remember) {
        try {
            authPrefs().edit()
                    .putBoolean(KEY_REMEMBER_ME, remember)
                    .putString(KEY_SAVED_EMAIL, remember ? email : "")
                    .apply();
        } catch (Exception ignored) {}
    }

    private SharedPreferences authPrefs() {
        return SecurePrefs.wrap(appContext, PREF_AUTH);
    }
}
