package com.hafiztraveltours.app.utils;

import java.util.regex.Pattern;

/**
 * Utility class for redacting sensitive fields (tokens, passwords, IC/passport numbers, etc.)
 * from log outputs and exception error strings across the Android application.
 */
public final class LogSanitizer {

    private static final Pattern SENSITIVE_JSON_PATTERN = Pattern.compile(
            "(?i)\"(password|password_confirmation|token|access_token|refresh_token|auth_token|ic_number|passport_number|secret|api_key|card_number|cvv)\"\\s*:\\s*\"[^\"]+\""
    );

    private LogSanitizer() {}

    /**
     * Redacts known sensitive JSON fields and Authorization tokens from raw text or JSON.
     *
     * @param input Raw input string to sanitize
     * @return Sanitized string with sensitive values masked
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Mask JSON keys matching sensitive patterns
        String sanitized = SENSITIVE_JSON_PATTERN.matcher(input)
                .replaceAll("\"$1\":\"[REDACTED]\"");

        // Mask Bearer tokens if present
        sanitized = sanitized.replaceAll("(?i)Bearer\\s+[A-Za-z0-9-_=.]+", "Bearer [REDACTED]");

        return sanitized;
    }
}
