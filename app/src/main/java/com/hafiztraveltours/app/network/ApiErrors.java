package com.hafiztraveltours.app.network;

import android.content.Context;
import android.util.Log;

import com.hafiztraveltours.app.R;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import retrofit2.Response;

/**
 * Central Retrofit error handling (H2). All Activities map API failures through here
 * instead of duplicating status-code branches per callback.
 *
 * <p>Policy:
 * <ul>
 *   <li>HTTP 200 + {@code status:"success"} + non-null data = success ({@link #isSuccess}).</li>
 *   <li>401/403/404/429/500+ = fixed localized messages (never raw server text).</li>
 *   <li>422 = first Laravel validation message when safely parseable, else localized fallback.</li>
 *   <li>Network failures split into timeout vs no-internet vs generic.</li>
 *   <li>Server detail is always logged (Logcat only), never shown to customers.</li>
 * </ul>
 * Global 401 sign-out/redirect is intentionally NOT done here (M4/auth redesign scope);
 * use {@link #isUnauthorized(Response)} at call sites that need it.
 */
public final class ApiErrors {

    private static final String TAG = "ApiErrors";
    private static final int MAX_SERVER_MESSAGE_CHARS = 200;

    private ApiErrors() {}

    /** True only for HTTP 2xx + body + {@code status:"success"}. Null-safe. */
    public static <T> boolean isSuccess(Response<ApiResponse<T>> response) {
        return response != null && response.isSuccessful()
                && response.body() != null && response.body().isSuccess();
    }

    /** Null-safe body data, or null when the response is not a success. */
    public static <T> T bodyData(Response<ApiResponse<T>> response) {
        if (!isSuccess(response)) return null;
        return response.body().data;
    }

    /** True for HTTP 401 (expired/invalid token). Caller decides the UX (message below). */
    public static boolean isUnauthorized(Response<?> response) {
        return response != null && response.code() == 401;
    }

    private static final java.util.Map<Response<?>, String> cachedErrorBodies =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * Reads and caches the raw errorBody string once to prevent stream-closed exceptions.
     */
    public static String getRawErrorBody(Response<?> response) {
        if (response == null) return null;
        String cached = cachedErrorBodies.get(response);
        if (cached != null) return cached;
        if (response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                if (raw != null) {
                    cachedErrorBodies.put(response, raw);
                    return raw;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** True if server specifically indicated unverified account in 403 error body. */
    public static boolean isAccountUnverified(Response<?> response) {
        if (response == null || response.code() != 403) return false;
        String raw = getRawErrorBody(response);
        if (raw == null || raw.isEmpty()) return false;
        try {
            com.google.gson.JsonElement el = new com.google.gson.JsonParser().parse(raw);
            if (el.isJsonObject()) {
                com.google.gson.JsonObject obj = el.getAsJsonObject();
                if (obj.has("code") && !obj.get("code").isJsonNull()) {
                    return "ACCOUNT_UNVERIFIED".equalsIgnoreCase(obj.get("code").getAsString());
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Localized customer-facing message for a failed HTTP response.
     * Falls back to {@code fallbackResId} for unmapped codes.
     */
    public static String userMessage(Context context, Response<?> response, int fallbackResId) {
        int fallback = fallbackResId != 0 ? fallbackResId : R.string.err_network;
        if (context == null) return "";
        if (response == null) return context.getString(fallback);
        int code = response.code();
        String serverDetail = extractServerMessage(response);
        Log.w(TAG, "HTTP " + code + (serverDetail != null ? ": " + com.hafiztraveltours.app.utils.LogSanitizer.sanitize(serverDetail) : ""));
        switch (code) {
            case 401:
                String reqPath = "";
                try {
                    if (response.raw() != null && response.raw().request() != null
                            && response.raw().request().url() != null) {
                        reqPath = response.raw().request().url().encodedPath();
                    }
                } catch (Exception ignored) {}
                boolean isLoginRequest = reqPath.contains("/auth/login")
                        || fallbackResId == R.string.login_failed_default;
                if (isLoginRequest) {
                    return (serverDetail != null && !serverDetail.isEmpty())
                            ? serverDetail
                            : context.getString(fallback != 0 ? fallback : R.string.login_failed_default);
                }
                return context.getString(R.string.err_session_expired);
            case 403:
                return (serverDetail != null && !serverDetail.isEmpty())
                        ? serverDetail
                        : context.getString(R.string.err_forbidden);
            case 404:
                if (serverDetail != null && !serverDetail.isEmpty()
                        && !serverDetail.startsWith("The route ")
                        && !serverDetail.toLowerCase(java.util.Locale.ROOT).contains("could not be found")) {
                    return serverDetail;
                }
                return context.getString(fallback != 0 ? fallback : R.string.err_not_found);
            case 422:
                return (serverDetail != null && !serverDetail.isEmpty())
                        ? serverDetail
                        : context.getString(R.string.err_validation);
            case 429:
                return context.getString(R.string.err_too_many_requests);
            default:
                if (code >= 500) return context.getString(R.string.err_server);
                return (serverDetail != null && !serverDetail.isEmpty())
                        ? serverDetail
                        : context.getString(fallback);
        }
    }

    /** Localized customer-facing message for transport failures (no HTTP response). */
    public static String userMessage(Context context, Throwable t, int fallbackResId) {
        int fallback = fallbackResId != 0 ? fallbackResId : R.string.err_network;
        if (context == null) return "";
        if (t == null) return context.getString(fallback);
        Log.w(TAG, "Network failure: " + t);
        if (t instanceof SocketTimeoutException || t instanceof TimeoutException
                || (t.getMessage() != null && t.getMessage().toLowerCase().contains("timeout"))) {
            return context.getString(R.string.err_timeout);
        }
        if (t instanceof UnknownHostException || t instanceof ConnectException
                || t instanceof NoRouteToHostException
                || (t.getMessage() != null
                    && (t.getMessage().contains("Unable to resolve host")
                        || t.getMessage().contains("Failed to connect")))) {
            return context.getString(R.string.err_no_internet);
        }
        return context.getString(fallback);
    }

    // ---------- internals ----------

    /** Best-effort server text for logging / 422 UX. Null when absent/unparseable. */
    public static String extractServerMessage(Response<?> response) {
        if (response == null) return null;
        try {
            if (response.body() instanceof ApiResponse) {
                String m = ((ApiResponse<?>) response.body()).message;
                if (m != null && !m.trim().isEmpty()) return truncate(m.trim());
            }
            String raw = getRawErrorBody(response);
            if (raw != null && !raw.isEmpty()) {
                com.google.gson.JsonElement el = new com.google.gson.JsonParser().parse(raw);
                if (el.isJsonObject()) {
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    if (obj.has("message") && !obj.get("message").isJsonNull()) {
                        String m = obj.get("message").getAsString().trim();
                        if (!m.isEmpty() && !"null".equalsIgnoreCase(m)) return truncate(m);
                    }
                    if (obj.has("errors") && !obj.get("errors").isJsonNull()) {
                        String first = firstErrorsEntry(obj.get("errors"));
                        if (first != null) return truncate(first);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String firstErrorsEntry(com.google.gson.JsonElement errors) {
        try {
            if (errors.isJsonObject()) {
                com.google.gson.JsonObject obj = errors.getAsJsonObject();
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
                    com.google.gson.JsonElement v = entry.getValue();
                    if (v.isJsonArray() && v.getAsJsonArray().size() > 0) {
                        String s = v.getAsJsonArray().get(0).getAsString().trim();
                        if (!s.isEmpty()) return s;
                    } else if (v.isJsonPrimitive()) {
                        String s = v.getAsString().trim();
                        if (!s.isEmpty()) return s;
                    }
                }
            } else if (errors.isJsonArray() && errors.getAsJsonArray().size() > 0) {
                String s = errors.getAsJsonArray().get(0).getAsString().trim();
                if (!s.isEmpty()) return s;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String truncate(String s) {
        if (s != null && s.length() > MAX_SERVER_MESSAGE_CHARS) {
            return s.substring(0, MAX_SERVER_MESSAGE_CHARS).trim();
        }
        return s;
    }
}
