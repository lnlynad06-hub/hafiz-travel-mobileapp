package com.hafiztraveltours.app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.hafiztraveltours.app.ui.LoginActivity;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application host: tracks the foreground Activity and centralizes 401 handling.
 *
 * <p>Any HTTP 401 (via the ApiClient interceptor) lands here. Login-flow screens
 * are excluded so wrong-password 401s stay local; otherwise an active session is
 * cleared and the user is sent to Login exactly once (throttled). No token
 * refresh is attempted — re-login is the contract.
 */
public class HafizApp extends Application {

    private static volatile HafizApp instance;
    private static volatile Activity foreground;
    private static final AtomicLong lastHandledAt = new AtomicLong(0);
    private static final long THROTTLE_MS = 3000;

    private static final Set<String> AUTH_SCREENS = new HashSet<>(Arrays.asList(
            "SplashActivity",
            "PrivacyPolicyActivity",
            "WelcomeActivity",
            "LoginActivity",
            "SignUpActivity",
            "ForgotPasswordActivity",
            "VerifyAccountActivity",
            "VerifyResetCodeActivity",
            "ResetPasswordActivity"
    ));

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // Fail-closed integrity: a broken KeyStore invalidates any stored session.
        SessionManager.getInstance(this).enforceCryptoIntegrity();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                foreground = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {
                if (foreground == activity) foreground = null;
            }

            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    /**
     * Central 401 entry point (called from a background OkHttp thread).
     * No-op when no session is active, when an auth screen is foreground,
     * or when handled within the throttle window.
     */
    public static void handleUnauthorized() {
        HafizApp app = instance;
        if (app == null) return;
        Activity current = foreground;
        if (current == null || current.isFinishing() || current.isDestroyed()) return;
        if (AUTH_SCREENS.contains(current.getClass().getSimpleName())) return;
        if (!SessionManager.getInstance(app).isLoggedIn()) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastHandledAt.get() < THROTTLE_MS) return;
        lastHandledAt.set(now);

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                SessionManager.getInstance(app).clearSession();
                Toast.makeText(app, app.getString(R.string.err_session_expired),
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(app, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                app.startActivity(intent);
            } catch (Exception ignored) {}
        });
    }
}
