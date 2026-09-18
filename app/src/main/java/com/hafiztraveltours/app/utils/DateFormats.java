package com.hafiztraveltours.app.utils;

import java.util.Date;

/**
 * Single place for date handling (H8). The backend contract is the ISO calendar date
 * {@code yyyy-MM-dd}; parsing is strict (lenient=false) and every method is null-safe.
 * No timezone conversion is performed: API dates are calendar dates, and prayer clock
 * times are formatted in the device zone exactly as before.
 */
public final class DateFormats {

    private DateFormats() {}

    public static final String API_PATTERN = "yyyy-MM-dd";
    public static final String CLOCK_PATTERN = "h:mm a";

    private static java.text.SimpleDateFormat apiFormat() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat(API_PATTERN, java.util.Locale.US);
        sdf.setLenient(false);
        return sdf;
    }

    /** Parses an API {@code yyyy-MM-dd} date; null for null/blank/invalid. */
    public static Date parseApiDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return apiFormat().parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /** Formats a Date back to API {@code yyyy-MM-dd}; "" for null. */
    public static String formatApiDate(Date date) {
        if (date == null) return "";
        try {
            return apiFormat().format(date);
        } catch (Exception e) {
            return "";
        }
    }

    /** True when the string is a real calendar date in API format. */
    public static boolean isValidApiDate(String raw) {
        return parseApiDate(raw) != null;
    }

    /**
     * True when {@code expiryYmd} is still valid with at least {@code requiredMonths}
     * of validity remaining from today (passport rule). Invalid/blank expiry = false.
     */
    public static boolean meetsValidityMonths(String expiryYmd, int requiredMonths) {
        Date expiry = parseApiDate(expiryYmd);
        if (expiry == null) return false;
        java.util.Calendar limit = java.util.Calendar.getInstance();
        limit.add(java.util.Calendar.MONTH, Math.max(0, requiredMonths));
        // Compare at day granularity.
        java.util.Calendar exp = java.util.Calendar.getInstance();
        exp.setTime(expiry);
        exp.set(java.util.Calendar.HOUR_OF_DAY, 0);
        exp.set(java.util.Calendar.MINUTE, 0);
        exp.set(java.util.Calendar.SECOND, 0);
        exp.set(java.util.Calendar.MILLISECOND, 0);
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        if (exp.before(today)) return false;
        return !exp.before(limit);
    }

    /** Clock time (e.g. prayer times) in device zone; "--" for null. */
    public static String formatClockTime(Date date, java.util.Locale locale) {
        if (date == null) return "--";
        try {
            java.util.Locale effective = locale != null ? locale : java.util.Locale.getDefault();
            return new java.text.SimpleDateFormat(CLOCK_PATTERN, effective).format(date);
        } catch (Exception e) {
            return "--";
        }
    }

    /** Epoch seconds → clock time in device zone; "--" on failure. */
    public static String formatEpochSeconds(long epochSeconds, java.util.Locale locale) {
        try {
            return formatClockTime(new Date(epochSeconds * 1000L), locale);
        } catch (Exception e) {
            return "--";
        }
    }
}
