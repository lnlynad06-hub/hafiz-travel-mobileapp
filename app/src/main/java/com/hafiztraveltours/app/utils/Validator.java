package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.R;

/**
 * Shared field validation (H7). Every method returns a string resource ID for the
 * error message, or 0 when the value is valid — so EN/MS messages always come from
 * resources, never hardcoded. Business rules mirror the previously duplicated
 * per-screen checks (Login/SignUp/Profile/Passenger): same thresholds, same meanings.
 */
public final class Validator {

    private Validator() {}

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MIN_PHONE_DIGITS = 5;

    /** 0 when non-blank. */
    public static int required(String value, int emptyResId) {
        if (value == null || value.trim().isEmpty()) return emptyResId;
        return 0;
    }

    /** Email format (blank = invalid here; use {@link #required} first for blank-specific UX). */
    public static int email(String value, int invalidResId) {
        if (value == null || value.trim().isEmpty()
                || !android.util.Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()) {
            return invalidResId;
        }
        return 0;
    }

    /** Login-style password: non-blank required (server enforces length/policy). */
    public static int loginPassword(String value, int emptyResId) {
        return required(value, emptyResId);
    }

    /** Registration/change password: non-blank and at least MIN_PASSWORD_LENGTH. */
    public static int newPassword(String value, int emptyResId, int shortResId) {
        if (value == null || value.isEmpty()) return emptyResId;
        if (value.length() < MIN_PASSWORD_LENGTH) return shortResId;
        return 0;
    }

    /** Confirmation must equal the password (both raw, untrimmed). */
    public static int passwordConfirm(String password, String confirm, int mismatchResId) {
        if (password == null) password = "";
        if (confirm == null) confirm = "";
        if (!password.equals(confirm)) return mismatchResId;
        return 0;
    }

    /** Phone: at least MIN_PHONE_DIGITS digits; blank allowed only when {@code allowBlank}. */
    public static int phone(String value, boolean allowBlank, int invalidResId) {
        if (value == null || value.trim().isEmpty()) {
            return allowBlank ? 0 : invalidResId;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < MIN_PHONE_DIGITS) return invalidResId;
        return 0;
    }

    /** Username/nickname: non-blank (server owns uniqueness/format policy). */
    public static int username(String value, int emptyResId) {
        return required(value, emptyResId);
    }

    /** Name: non-blank. */
    public static int fullName(String value, int emptyResId) {
        return required(value, emptyResId);
    }

    /** Passport/IC numbers: non-blank (format policy is backend-owned). */
    public static int travelId(String value, int emptyResId) {
        return required(value, emptyResId);
    }

    /** API-format date field: blank → {@code blankResId}, malformed → {@code invalidResId}. */
    public static int apiDate(String value, int blankResId, int invalidResId) {
        if (value == null || value.trim().isEmpty()) return blankResId;
        if (!DateFormats.isValidApiDate(value.trim())) return invalidResId;
        return 0;
    }
}
