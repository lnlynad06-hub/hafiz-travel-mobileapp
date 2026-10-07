package com.hafiztraveltours.app.utils;

import android.content.Context;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.PackageDetail;

/**
 * Resolves locale-neutral room label keys (PackageDetail.PriceOption.labelKey)
 * to localized display strings. Single place for EN/MS room names.
 */
public final class RoomLabels {

    private RoomLabels() {}

    public static String resolve(Context context, PackageDetail.PriceOption option) {
        if (option == null) return "";
        return resolve(context, option.labelKey, option.occupancyLabel);
    }

    public static String resolve(Context context, String labelKey, String fallbackLabel) {
        if (context == null) return fallbackLabel != null ? fallbackLabel : "";
        if (labelKey == null) return fallbackLabel != null ? fallbackLabel : "";
        switch (labelKey.trim().toLowerCase()) {
            case "quint":
                return context.getString(R.string.room_quint);
            case "quad":
                return context.getString(R.string.room_quad);
            case "triple":
                return context.getString(R.string.room_triple);
            case "double":
                return context.getString(R.string.room_double);
            case "single":
                return context.getString(R.string.room_single);
            case "from":
                return context.getString(R.string.detail_price_from);
            case "standard":
                return context.getString(R.string.room_standard);
            default:
                // Legacy Malay labels from older payloads ("Bilik Berlima (Quint)" etc.)
                return resolveLegacy(context, fallbackLabel != null ? fallbackLabel : labelKey);
        }
    }

    private static String resolveLegacy(Context context, String label) {
        if (label == null) return "";
        if ("Bilik Berlima (Quint)".equalsIgnoreCase(label)) return context.getString(R.string.room_quint);
        if ("Bilik Berempat (Quad)".equalsIgnoreCase(label)) return context.getString(R.string.room_quad);
        if ("Bilik Bertiga (Triple)".equalsIgnoreCase(label)) return context.getString(R.string.room_triple);
        if ("Bilik Berdua (Double)".equalsIgnoreCase(label)) return context.getString(R.string.room_double);
        if ("Bilik Perseorangan (Single)".equalsIgnoreCase(label)) return context.getString(R.string.room_single);
        if ("Harga Bermula Dari".equalsIgnoreCase(label)) return context.getString(R.string.detail_price_from);
        if ("Bilik Standard".equalsIgnoreCase(label)) return context.getString(R.string.room_standard);
        return label;
    }
}
