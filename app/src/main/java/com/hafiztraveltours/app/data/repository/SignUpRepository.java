package com.hafiztraveltours.app.data.repository;

import android.content.Context;

import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.network.AuthResponse;
import com.hafiztraveltours.app.network.GoogleLoginRequest;
import com.hafiztraveltours.app.network.RegisterRequest;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.SessionManager;

import retrofit2.Call;

/**
 * Registration data access (H1/Phase 10). Owns registration + Google-login API
 * calls and the resulting session persistence. No Views, Toasts, Dialogs, Intents
 * or Activity handling — callers track and cancel the returned Calls.
 *
 * <p>Phase-5 contract: tokens go through {@code saveAuthSession} only when
 * non-empty (else {@code saveUser}); passwords are never stored. Registration
 * authenticates the user exactly like login (pre-existing behavior, preserved).
 */
public class SignUpRepository {

    private final Context appContext;
    private final SessionManager session;

    public SignUpRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.session = SessionManager.getInstance(this.appContext);
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    /** Fresh registration Call per invocation; caller owns cancellation. No retries. */
    public Call<ApiResponse<AuthResponse>> registerCall(RegisterRequest request) {
        return api().register(request);
    }

    /** Fresh Google-login Call per invocation; caller owns cancellation. No retries. */
    public Call<ApiResponse<AuthResponse>> googleLoginCall(GoogleLoginRequest request) {
        return api().googleLogin(request);
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
}
