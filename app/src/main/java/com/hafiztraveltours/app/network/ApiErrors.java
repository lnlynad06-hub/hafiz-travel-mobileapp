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

    /**
     * Localized customer-facing message for a failed HTTP response.
     * Falls back to {@code fallbackResId} for unmapped codes.
     */
    public static String userMessage(Context context, Response<?> response, int fallbackResId) {
        int fallback = fallbackResId != 0 ? fallbackResId : R.string.err_network;
        if (context == null) return "";
        if (response == null) return context.getString(fallback);
        int code = response.code();
        logServerDetail(code, response);
        switch (code) {
            case 401:
                return context.getString(R.string.err_session_expired);
            case 403:
                return context.getString(R.string.err_forbidden);
            case 404:
                return context.getString(R.string.err_not_found);
            case 422: {
                String validation = firstValidationMessage(response);
                return validation != null ? validation : context.getString(R.string.err_validation);
            }
            case 429:
                return context.getString(R.string.err_too_many_requests);
            default:
                if (code >= 500) return context.getString(R.string.err_server);
                return context.getString(fallback);
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

    private static void logServerDetail(int code, Response<?> response) {
        try {
            String detail = extractServerMessage(response);
            Log.w(TAG, "HTTP " + code + (detail != null ? ": " + detail : " (no body)"));
        } catch (Exception e) {
            Log.w(TAG, "HTTP " + response.code() + " (unreadable body)");
        }
    }

    /** Best-effort server text for logging / 422 UX. Null when absent/unparseable. */
    private static String extractServerMessage(Response<?> response) {
        if (response == null) return null;
        try {
            if (response.body() instanceof ApiResponse) {
                String m = ((ApiResponse<?>) response.body()).message;
                if (m != null && !m.trim().isEmpty()) return truncate(m.trim());
            }
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw == null || raw.isEmpty()) return null;
                org.json.JSONObject obj = new org.json.JSONObject(raw);
                if (obj.has("message") && !obj.isNull("message")) {
                    String m = obj.optString("message", "").trim();
                    if (!m.isEmpty() && !"null".equalsIgnoreCase(m)) return truncate(m);
                }
                if (obj.has("errors") && !obj.isNull("errors")) {
                    String first = firstErrorsEntry(obj.get("errors"));
                    if (first != null) return truncate(first);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** First Laravel validation message: body.message, errorBody.message, or errors.*[0]. */
    private static String firstValidationMessage(Response<?> response) {
        return extractServerMessage(response);
    }

    private static String firstErrorsEntry(Object errors) {
        try {
            if (errors instanceof org.json.JSONObject) {
                org.json.JSONObject obj = (org.json.JSONObject) errors;
                java.util.Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    Object v = obj.get(keys.next());
                    if (v instanceof org.json.JSONArray && ((org.json.JSONArray) v).length() > 0) {
                        String s = ((org.json.JSONArray) v).optString(0, "").trim();
                        if (!s.isEmpty()) return s;
                    } else if (v instanceof String && !((String) v).trim().isEmpty()) {
                        return ((String) v).trim();
                    }
                }
            } else if (errors instanceof org.json.JSONArray && ((org.json.JSONArray) errors).length() > 0) {
                String s = ((org.json.JSONArray) errors).optString(0, "").trim();
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
