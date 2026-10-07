package com.hafiztraveltours.app.utils;

/**
 * One-shot API operation outcome (shared by ViewModels). Carries the raw Retrofit
 * response/throwable plus a fallback string ID; the Activity resolves the message
 * with {@code ApiErrors} (which needs a Context the ViewModel must not hold).
 */
public final class ApiOpResult {

    public final boolean success;
    public final int fallbackResId;
    public final retrofit2.Response<?> errorResponse;
    public final Throwable error;

    private ApiOpResult(boolean success, int fallbackResId,
                        retrofit2.Response<?> errorResponse, Throwable error) {
        this.success = success;
        this.fallbackResId = fallbackResId;
        this.errorResponse = errorResponse;
        this.error = error;
    }

    public static ApiOpResult success() {
        return new ApiOpResult(true, 0, null, null);
    }

    public static ApiOpResult failure(retrofit2.Response<?> response, int fallbackResId) {
        return new ApiOpResult(false, fallbackResId, response, null);
    }

    public static ApiOpResult failure(Throwable error, int fallbackResId) {
        return new ApiOpResult(false, fallbackResId, null, error);
    }

    /** Resolves the customer-facing message (Activity layer only). */
    public String resolveMessage(android.content.Context context) {
        if (context == null) return "";
        if (errorResponse != null) {
            return com.hafiztraveltours.app.network.ApiErrors.userMessage(
                    context, errorResponse, fallbackResId);
        }
        return com.hafiztraveltours.app.network.ApiErrors.userMessage(
                context, error, fallbackResId);
    }
}
