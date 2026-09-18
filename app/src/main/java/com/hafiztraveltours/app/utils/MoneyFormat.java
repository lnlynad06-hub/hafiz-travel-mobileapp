package com.hafiztraveltours.app.utils;

/**
 * Single place for money handling (H6). Formatting uses "#,##0" ("RM 1,234");
 * parsing strips everything except digits and the decimal point. All null/invalid
 * inputs are safe (0.0 / "RM -" fallbacks). Monetary VALUES are never altered here.
 */
public final class MoneyFormat {

    private MoneyFormat() {}

    private static final java.text.DecimalFormat FORMATTER;

    static {
        FORMATTER = new java.text.DecimalFormat("#,##0");
        FORMATTER.setGroupingUsed(true);
    }

    /** "RM 1,234". Non-finite values render as "RM -". */
    public static String formatRM(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) return "RM -";
        try {
            return "RM " + FORMATTER.format(amount);
        } catch (Exception e) {
            return "RM -";
        }
    }

    /** Raw API string (may be null/"RM 7,990.00"/"") rendered with RM prefix; null/blank → "RM -". */
    public static String formatRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "RM -";
        double parsed = parseAmount(raw);
        if (parsed == 0.0 && !raw.matches(".*[1-9].*")) {
            // Genuinely zero/empty, or unparseable text passed through with prefix.
            try {
                Double.parseDouble(raw.trim());
                return formatRM(parsed);
            } catch (NumberFormatException e) {
                return "RM " + raw.trim();
            }
        }
        return formatRM(parsed);
    }

    /** Numeric value of a display/API price string; 0.0 for null/invalid. */
    public static double parseAmount(String raw) {
        if (raw == null) return 0.0;
        String clean = raw.replaceAll("[^0-9.]", "");
        if (clean.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /** Digits-and-decimal-only string for numeric display slots; "" for null. */
    public static String numericString(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^0-9.]", "").trim();
    }
}
