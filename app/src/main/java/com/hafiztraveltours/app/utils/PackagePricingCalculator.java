package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.models.UmrahPackage;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Client-side calculation helper for UX/presentation purposes.
 * Authoritative pricing is always computed and validated on the backend.
 */
public final class PackagePricingCalculator {

    public static final String SEASON_STANDARD = "standard";
    public static final String SEASON_LOW_PEAK = "low_peak";
    public static final String SEASON_HIGH_PEAK = "high_peak";

    private PackagePricingCalculator() {}

    public static double getSeasonPrice(com.hafiztraveltours.app.models.PackageDetail detail, String season) {
        if (detail == null) return 0.0;
        if (detail.seasonPrices != null && season != null) {
            String priceStr = detail.seasonPrices.get(season);
            if (priceStr != null && !priceStr.trim().isEmpty()) {
                double parsed = com.hafiztraveltours.app.models.BookingRequest.parsePriceAmount(priceStr);
                if (parsed > 0) return parsed;
            }
        }
        if (detail.rawPackage != null) {
            return getSeasonAdultPrice(detail.rawPackage, season, com.hafiztraveltours.app.models.BookingRequest.parsePriceAmount(detail.price));
        }
        return com.hafiztraveltours.app.models.BookingRequest.parsePriceAmount(detail.price);
    }

    public static String getSeasonLabel(android.content.Context context, String season) {
        if (context == null || season == null) return "Standard";
        if (SEASON_LOW_PEAK.equalsIgnoreCase(season)) {
            return context.getString(com.hafiztraveltours.app.R.string.season_low_peak);
        } else if (SEASON_HIGH_PEAK.equalsIgnoreCase(season)) {
            return context.getString(com.hafiztraveltours.app.R.string.season_high_peak);
        } else {
            return context.getString(com.hafiztraveltours.app.R.string.season_standard);
        }
    }

    public static int calculateAge(String dobIso) {
        if (dobIso == null || dobIso.trim().isEmpty()) return 0;
        Calendar dob = parseIsoDate(dobIso);
        if (dob == null) return 0;
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return Math.max(0, age);
    }

    public static double calculatePassengerPrice(
            com.hafiztraveltours.app.models.PackageDetail detail,
            String season,
            String dobIso,
            Boolean withBed,
            double fallbackAdultPrice) {

        boolean isUmrah = detail != null && detail.isUmrah;
        double baseAdult = fallbackAdultPrice > 0 ? fallbackAdultPrice : getSeasonPrice(detail, season);
        String departureDate = (detail != null && detail.availableDepartures != null && !detail.availableDepartures.isEmpty())
                ? detail.availableDepartures.get(0).departureDate : null;

        return calculatePassengerPrice(isUmrah, baseAdult, dobIso, departureDate, withBed);
    }

    public static double getSeasonAdultPrice(UmrahPackage pkg, String season, double fallbackPrice) {
        if (pkg == null) return fallbackPrice;
        if (pkg.seasonPrices != null && season != null) {
            String raw = pkg.seasonPrices.get(season);
            if (raw != null && !raw.trim().isEmpty()) {
                double parsed = MoneyFormat.parseAmount(raw);
                if (parsed > 0) return parsed;
            }
        }
        if (pkg.seasonPricing != null && season != null) {
            UmrahPackage.SeasonRate rate = null;
            if (SEASON_STANDARD.equalsIgnoreCase(season)) rate = pkg.seasonPricing.standard;
            else if (SEASON_LOW_PEAK.equalsIgnoreCase(season)) rate = pkg.seasonPricing.lowPeak;
            else if (SEASON_HIGH_PEAK.equalsIgnoreCase(season)) rate = pkg.seasonPricing.highPeak;

            if (rate != null && rate.adultPrice > 0) {
                return rate.adultPrice;
            }
        }
        return fallbackPrice > 0 ? fallbackPrice : pkg.getNumericPrice();
    }

    public static double calculatePassengerPrice(
            boolean isUmrah,
            double baseAdultPrice,
            String dobIso,
            String departureDateIso,
            Boolean withBed) {

        if (dobIso == null || dobIso.trim().isEmpty()) {
            return baseAdultPrice;
        }

        Calendar dobCal = parseIsoDate(dobIso);
        if (dobCal == null) {
            return baseAdultPrice;
        }

        Calendar refCal = parseIsoDate(departureDateIso);
        if (refCal == null) {
            refCal = Calendar.getInstance();
        }

        // Exact age boundaries
        Calendar twoYears = (Calendar) dobCal.clone();
        twoYears.add(Calendar.YEAR, 2);

        Calendar fourYears = (Calendar) dobCal.clone();
        fourYears.add(Calendar.YEAR, 4);

        Calendar twelveYears = (Calendar) dobCal.clone();
        twelveYears.add(Calendar.YEAR, 12);

        if (isUmrah) {
            // A. Below 2 years -> Fixed RM2,000
            if (refCal.before(twoYears)) {
                return 2000.0;
            }
            // B. 2 years 1 day through 4 years -> Basic - RM300
            if (!refCal.before(twoYears) && !refCal.after(fourYears)) {
                return Math.max(0, baseAdultPrice - 300.0);
            }
            // C. 4 years 1 day and above -> Adult price
            return baseAdultPrice;
        } else {
            // TOUR
            // A. Below 2 years -> Fixed RM500
            if (refCal.before(twoYears)) {
                return 500.0;
            }
            // B. 2 to 11 years (before 12th birthday)
            if (!refCal.before(twoYears) && refCal.before(twelveYears)) {
                boolean bed = (withBed == null || withBed);
                return Math.max(0, baseAdultPrice - (bed ? 100.0 : 200.0));
            }
            // C. 12 years and above -> Adult price
            return baseAdultPrice;
        }
    }

    public static String getPassengerCategory(boolean isUmrah, String dobIso, String departureDateIso) {
        if (dobIso == null || dobIso.trim().isEmpty()) {
            return "adult";
        }
        Calendar dobCal = parseIsoDate(dobIso);
        if (dobCal == null) return "adult";

        Calendar refCal = parseIsoDate(departureDateIso);
        if (refCal == null) refCal = Calendar.getInstance();

        Calendar twoYears = (Calendar) dobCal.clone();
        twoYears.add(Calendar.YEAR, 2);

        if (refCal.before(twoYears)) {
            return "infant";
        }

        if (isUmrah) {
            Calendar fourYears = (Calendar) dobCal.clone();
            fourYears.add(Calendar.YEAR, 4);
            if (!refCal.after(fourYears)) {
                return "child";
            }
            return "adult";
        } else {
            Calendar twelveYears = (Calendar) dobCal.clone();
            twelveYears.add(Calendar.YEAR, 12);
            if (refCal.before(twelveYears)) {
                return "child";
            }
            return "adult";
        }
    }

    private static Calendar parseIsoDate(String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = sdf.parse(iso.trim());
            if (d != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                return c;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
