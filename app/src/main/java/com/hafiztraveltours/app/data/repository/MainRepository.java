package com.hafiztraveltours.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.hafiztraveltours.app.models.UmrahPackage;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.ApiService;
import com.hafiztraveltours.app.network.HomeDataResponse;
import com.hafiztraveltours.app.utils.SessionManager;

import retrofit2.Call;

/**
 * Home/Main data access (H1/Phase 9). Owns the home Retrofit call plus session
 * reads and the notification-dot flag for this screen. No Views, Toasts, Dialogs,
 * Intents or Activity handling — callers track and cancel the returned Calls.
 *
 * <p>No string resources are resolved here (Application context may not carry the
 * per-Activity app locale); localized text stays in the Activity.
 */
public class MainRepository {

    private final Context appContext;
    private final SessionManager session;

    public MainRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.session = SessionManager.getInstance(this.appContext);
    }

    private ApiService api() {
        return ApiClient.getApiService();
    }

    /** Fresh home-data Call per invocation; caller owns cancellation. */
    public Call<ApiResponse<HomeDataResponse>> homeCall() {
        return api().getHomeData();
    }

    public boolean isLoggedIn() {
        return session.isLoggedIn();
    }

    /** Raw nickname (may be null/empty — Activity applies the localized default). */
    public String getUserNickname() {
        return session.getUserNickname();
    }

    private SharedPreferences appPrefs() {
        return appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public boolean hasUnreadNotifications() {
        try {
            return appPrefs().getBoolean("has_unread_notifications", false);
        } catch (Exception e) {
            return false;
        }
    }

    public void markNotificationsRead() {
        try {
            appPrefs().edit().putBoolean("has_unread_notifications", false).apply();
        } catch (Exception ignored) {}
    }
}
