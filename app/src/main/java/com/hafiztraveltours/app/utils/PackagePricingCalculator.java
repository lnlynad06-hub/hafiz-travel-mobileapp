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

    public static class PricingConfigurationException extends IllegalStateException {
        public PricingConfigurationException(String message) {
            super(message);
        }
    }

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

        return calculatePassengerPrice(detail, season, isUmrah, baseAdult, dobIso, departureDate, withBed);
    }

    public static double calculatePassengerPrice(
            boolean isUmrah,
            double baseAdultPrice,
            String dobIso,
            String departureDateIso,
            Boolean withBed) {

        return calculatePassengerPrice(null, null, isUmrah, baseAdultPrice, dobIso, departureDateIso, withBed);
    }

    public static double calculatePassengerPrice(
            com.hafiztraveltours.app.models.PackageDetail detail,
            String season,
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

        UmrahPackage.SeasonRate seasonRate = getSeasonRate(detail, season);

        if (isUmrah) {
            // A. Below 2 years (reference date is before 2nd birthday)
            if (refCal.before(twoYears)) {
                Double configured = getChildUnder2Price(detail, seasonRate);
                if (configured == null || configured <= 0) {
                    throw new PricingConfigurationException("Harga kanak-kanak (bawah 2 tahun) belum ditetapkan dalam ERP untuk pakej ini.");
                }
                return configured;
            }
            // B. Aged 2 years 1 day through 4 years (2nd birthday up to 4th birthday)
            if (!refCal.before(twoYears) && !refCal.after(fourYears)) {
                Double discount = getChild2To4Discount(detail, seasonRate, baseAdultPrice);
                if (discount == null) {
                    throw new PricingConfigurationException("Diskaun kanak-kanak (2-4 tahun) belum ditetapkan dalam ERP untuk pakej ini.");
                }
                if (discount <= 0 || discount >= baseAdultPrice) {
                    throw new PricingConfigurationException("Amaun diskaun kanak-kanak (2-4 tahun) tidak sah dalam ERP.");
                }
                return baseAdultPrice - discount;
            }
            // C. 4 years 1 day and above -> Adult room price
            return baseAdultPrice;
        } else {
            // TOUR
            // A. Below 2 years (reference date is before 2nd birthday)
            if (refCal.before(twoYears)) {
                Double configured = getChildUnder2Price(detail, seasonRate);
                if (configured == null || configured <= 0) {
                    throw new PricingConfigurationException("Harga kanak-kanak (bawah 2 tahun) belum ditetapkan dalam ERP untuk pakej ini.");
                }
                return configured;
            }
            // B. 11 years old and below (aged 2 to 11, i.e. before 12th birthday)
            if (!refCal.before(twoYears) && refCal.before(twelveYears)) {
                boolean bed = (withBed == null || withBed);
                if (bed) {
                    Double discount = getChildWithBedDiscount(detail, seasonRate, baseAdultPrice);
                    if (discount == null) {
                        throw new PricingConfigurationException("Diskaun kanak-kanak (2-11 tahun dengan katil) belum ditetapkan dalam ERP untuk pakej ini.");
                    }
                    if (discount <= 0 || discount >= baseAdultPrice) {
                        throw new PricingConfigurationException("Amaun diskaun kanak-kanak dengan katil tidak sah dalam ERP.");
                    }
                    return baseAdultPrice - discount;
                } else {
                    Double discount = getChildNoBedDiscount(detail, seasonRate, baseAdultPrice);
                    if (discount == null) {
                        throw new PricingConfigurationException("Diskaun kanak-kanak (2-11 tahun tanpa katil) belum ditetapkan dalam ERP untuk pakej ini.");
                    }
                    if (discount <= 0 || discount >= baseAdultPrice) {
                        throw new PricingConfigurationException("Amaun diskaun kanak-kanak tanpa katil tidak sah dalam ERP.");
                    }
                    return baseAdultPrice - discount;
                }
            }
            // C. Above 11 years old (12th birthday and above) -> Adult season price
            return baseAdultPrice;
        }
    }

    private static UmrahPackage.SeasonRate getSeasonRate(com.hafiztraveltours.app.models.PackageDetail detail, String season) {
        if (detail == null || detail.seasonPricing == null || season == null) return null;
        if (SEASON_STANDARD.equalsIgnoreCase(season)) return detail.seasonPricing.standard;
        if (SEASON_LOW_PEAK.equalsIgnoreCase(season)) return detail.seasonPricing.lowPeak;
        if (SEASON_HIGH_PEAK.equalsIgnoreCase(season)) return detail.seasonPricing.highPeak;
        return null;
    }

    private static Double getChildUnder2Price(com.hafiztraveltours.app.models.PackageDetail detail, UmrahPackage.SeasonRate seasonRate) {
        if (detail != null) {
            if (detail.childUnder2Price != null && detail.childUnder2Price > 0) return detail.childUnder2Price;
            if (detail.childPricingRules != null && detail.childPricingRules.childUnder2Price != null && detail.childPricingRules.childUnder2Price > 0) return detail.childPricingRules.childUnder2Price;
            if (detail.childPricingRules != null && detail.childPricingRules.under2FixedPrice != null && detail.childPricingRules.under2FixedPrice > 0) return detail.childPricingRules.under2FixedPrice;
        }
        if (seasonRate != null && seasonRate.childUnder2 > 0) return seasonRate.childUnder2;
        return null;
    }

    private static Double getChild2To4Discount(com.hafiztraveltours.app.models.PackageDetail detail, UmrahPackage.SeasonRate seasonRate, double baseAdultPrice) {
        if (detail != null) {
            if (detail.child2To4Discount != null) return detail.child2To4Discount;
            if (detail.childPricingRules != null && detail.childPricingRules.child2To4Discount != null) return detail.childPricingRules.child2To4Discount;
            if (detail.child2To4Price != null && detail.child2To4Price > 0 && detail.child2To4Price < baseAdultPrice) {
                return baseAdultPrice - detail.child2To4Price;
            }
            if (detail.childPricingRules != null && detail.childPricingRules.child2To4Price != null && detail.childPricingRules.child2To4Price > 0 && detail.childPricingRules.child2To4Price < baseAdultPrice) {
                return baseAdultPrice - detail.childPricingRules.child2To4Price;
            }
        }
        if (seasonRate != null && seasonRate.child2To4 > 0 && seasonRate.child2To4 < baseAdultPrice) {
            return baseAdultPrice - seasonRate.child2To4;
        }
        return null;
    }

    private static Double getChildWithBedDiscount(com.hafiztraveltours.app.models.PackageDetail detail, UmrahPackage.SeasonRate seasonRate, double baseAdultPrice) {
        if (detail != null) {
            if (detail.child211WithBedDiscount != null) return detail.child211WithBedDiscount;
            if (detail.childPricingRules != null && detail.childPricingRules.child211WithBedDiscount != null) return detail.childPricingRules.child211WithBedDiscount;
            if (detail.child211WithBedPrice != null && detail.child211WithBedPrice > 0 && detail.child211WithBedPrice < baseAdultPrice) {
                return baseAdultPrice - detail.child211WithBedPrice;
            }
            if (detail.childPricingRules != null && detail.childPricingRules.child211WithBedPrice != null && detail.childPricingRules.child211WithBedPrice > 0 && detail.childPricingRules.child211WithBedPrice < baseAdultPrice) {
                return baseAdultPrice - detail.childPricingRules.child211WithBedPrice;
            }
        }
        if (seasonRate != null && seasonRate.childWithBed > 0 && seasonRate.childWithBed < baseAdultPrice) {
            return baseAdultPrice - seasonRate.childWithBed;
        }
        return null;
    }

    private static Double getChildNoBedDiscount(com.hafiztraveltours.app.models.PackageDetail detail, UmrahPackage.SeasonRate seasonRate, double baseAdultPrice) {
        if (detail != null) {
            if (detail.child211NoBedDiscount != null) return detail.child211NoBedDiscount;
            if (detail.childPricingRules != null && detail.childPricingRules.child211NoBedDiscount != null) return detail.childPricingRules.child211NoBedDiscount;
            if (detail.child211NoBedPrice != null && detail.child211NoBedPrice > 0 && detail.child211NoBedPrice < baseAdultPrice) {
                return baseAdultPrice - detail.child211NoBedPrice;
            }
            if (detail.childPricingRules != null && detail.childPricingRules.child211NoBedPrice != null && detail.childPricingRules.child211NoBedPrice > 0 && detail.childPricingRules.child211NoBedPrice < baseAdultPrice) {
                return baseAdultPrice - detail.childPricingRules.child211NoBedPrice;
            }
        }
        if (seasonRate != null && seasonRate.childWithoutBed > 0 && seasonRate.childWithoutBed < baseAdultPrice) {
            return baseAdultPrice - seasonRate.childWithoutBed;
        }
        return null;
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
