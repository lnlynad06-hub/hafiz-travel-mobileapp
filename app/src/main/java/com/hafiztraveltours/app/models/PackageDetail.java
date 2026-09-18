package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import java.util.ArrayList;
import java.util.List;

/**
 * UI/domain model for the booking flow (H4). Built ONLY by {@link #fromUmrahPackage},
 * passed between booking Activities via Intent extras. Holds locale-neutral keys and
 * backend IDs (departures, room keys); all display text is resolved by Activities
 * through string resources. Flight/route display stays on the source {@link UmrahPackage}.
 */
public class PackageDetail implements java.io.Serializable {

    public String id;
    public String name;
    public String category;
    public String summaryLine;
    public int durationDays;
    public int nightsCount;
    public String price;
    public String imageUrl;
    public String posterImageUrl;
    public String durationFormatted;
    public Integer nightsTaif;
    public String hotelTaifName;
    public String hotelTaifRating;
    public String hotelTaifDistance;

    public List<NightBreakdown> nightsBreakdown = new ArrayList<>();
    public String departureDatesNote;
    public List<String> availableDepartureDates = new ArrayList<>();
    /** Departure options carrying the real backend ID (availableDepartureDates holds display labels only). */
    public List<DepartureOption> availableDepartures = new ArrayList<>();
    public List<HotelInfo> hotels = new ArrayList<>();
    public List<ItineraryDay> itinerary = new ArrayList<>();
    public List<ImportantNote> importantNotes = new ArrayList<>();
    public List<String> requiredDocuments = new ArrayList<>();
    public List<String> cancellationPolicy = new ArrayList<>();
    public String companyWhatsapp;
    public List<String> included = new ArrayList<>();
    public List<String> excluded = new ArrayList<>();
    public List<String> packingSummer = new ArrayList<>();
    public List<String> packingWinter = new ArrayList<>();
    public List<PriceOption> priceOptions = new ArrayList<>();
    public List<String> galleryImageUrls = new ArrayList<>();
    public List<UmrahPackage> relatedPackages = new ArrayList<>();
    public String whatsappMessage;
    public String packageType = "umrah";
    public boolean requiresPassport = true;
    public boolean requiresIc = true;
    public boolean requiresMahram = false;
    public boolean requiresClothesSize = false;
    public int passportValidityMonths = 6;
    public boolean isUmrah; // set from UmrahPackage.isUmrah() during parsing

    public static class NightBreakdown implements java.io.Serializable {
        public String city;          // set by Activity from getString() using slot
        public int nights;
        public int slot;             // 0=Makkah/Hotel1, 1=Madinah/Hotel2, 2=Taif/Hotel3
        public String destinationHint; // raw first destination word for tour packages
        public NightBreakdown(String city, int nights) {
            this.city = city;
            this.nights = nights;
        }
    }

    public static class HotelInfo implements java.io.Serializable {
        public String type; // "hotel" | "flight"
        public String title;  // built lazily; overridden by Activity with getString()
        public String subtitle;
        public int slot;       // 0=Hotel1/Makkah, 1=Hotel2/Madinah, 2=Hotel3/Taif, -1=flight
        public String rawRating; // raw rating/star string from server (e.g. "5 Stars")
        public String airlineName; // airline name for flight info
        public HotelInfo(String type, int slot, String rawRating, String subtitle) {
            this.type = type;
            this.slot = slot;
            this.rawRating = rawRating != null ? rawRating : "";
            this.subtitle = subtitle;
            this.title = ""; // set by Activity
            this.airlineName = "";
        }
    }

    public static class ItineraryDay implements java.io.Serializable {
        public int dayNumber;
        public String dayLabel;
        public String dateLabel;
        public String tag;
        public String timeNote;
        public String title;
        public String routeText;
        public List<String> highlights = new ArrayList<>();
        public List<String> activities = new ArrayList<>();
        public String hotelNote = "";
        public String mealNote = "";
    }

    public static class ImportantNote implements java.io.Serializable {
        public String title;
        public String badge;
        public List<String> bullets = new ArrayList<>();
    }

    public static class PriceOption implements java.io.Serializable {
        public String price;
        /** Locale-neutral room key ("quint"|"quad"|"triple"|"double"|"single"|"from"|"standard"). */
        public String labelKey;
        /** Legacy display label (fallback only; UI resolves labelKey via RoomLabels). */
        public String occupancyLabel;
        public PriceOption(String price, String labelKey, String occupancyLabel) {
            this.price = price;
            this.labelKey = labelKey;
            this.occupancyLabel = occupancyLabel;
        }
    }

    /** A selectable departure: backend ID + raw dates; the UI builds the localized label. */
    public static class DepartureOption implements java.io.Serializable {
        /** Raw `departures[].id` from the API (numeric string). May be null for legacy fallbacks. */
        public String id;
        /** Raw API dates (may be null). */
        public String departureDate;
        public String returnDate;
        /** Pre-built display label (legacy fallback). */
        public String label;
        public DepartureOption(String id, String departureDate, String returnDate, String label) {
            this.id = id;
            this.departureDate = departureDate;
            this.returnDate = returnDate;
            this.label = label != null ? label : "";
        }
    }

    public static PackageDetail fromUmrahPackage(UmrahPackage pkg) {
        PackageDetail d = new PackageDetail();
        if (pkg == null) return d;

        d.id = pkg.id;
        d.name = pkg.getDisplayName();
        d.category = pkg.category != null && !pkg.category.trim().isEmpty() ? pkg.category.trim() : null;
        d.summaryLine = pkg.summary != null && !pkg.summary.trim().isEmpty() ? pkg.summary.trim() : "";
        d.durationDays = pkg.durationDays > 0 ? pkg.durationDays : 0;
        d.nightsCount = pkg.nightsCount > 0 ? pkg.nightsCount : 0;
        d.price = pkg.price != null && !pkg.price.trim().isEmpty() ? pkg.price : "";
        d.imageUrl = pkg.imageUrl != null && !pkg.imageUrl.trim().isEmpty() ? pkg.imageUrl : "";
        d.posterImageUrl = d.imageUrl;
        d.durationFormatted = pkg.getDurationFormatted();
        // WhatsApp text is built by the Activity with localized resources.
        d.whatsappMessage = null;

        boolean isUmrah = pkg.isUmrah();
        d.isUmrah = isUmrah;

        // Package-driven requirements & type (H4): copy backend values when present,
        // otherwise keep the model defaults. Previously these were never copied, so
        // PassengerDetails always saw the defaults regardless of the API.
        if (pkg.packageType != null && !pkg.packageType.trim().isEmpty()) {
            d.packageType = pkg.packageType.trim();
        }
        if (pkg.requiresPassport != null) d.requiresPassport = pkg.requiresPassport;
        if (pkg.requiresIc != null) d.requiresIc = pkg.requiresIc;
        if (pkg.requiresMahram != null) d.requiresMahram = pkg.requiresMahram;
        if (pkg.requiresClothesSize != null) d.requiresClothesSize = pkg.requiresClothesSize;
        if (pkg.passportValidityMonths != null && pkg.passportValidityMonths > 0) {
            d.passportValidityMonths = pkg.passportValidityMonths;
        }

        // 1. Nights Breakdown — city labels use slot index, translated by Activity
        // slot 0=Makkah/Hotel1, 1=Madinah/Hotel2, 2=Taif/Hotel3
        if (pkg.nightsMakkah != null && pkg.nightsMakkah > 0) {
            NightBreakdown nb = new NightBreakdown(null, pkg.nightsMakkah);
            nb.slot = 0;
            nb.destinationHint = (pkg.destination != null && !pkg.destination.isEmpty())
                    ? pkg.destination.split("[,&/-]")[0].trim() : "";
            d.nightsBreakdown.add(nb);
        }
        if (pkg.nightsTaif != null && pkg.nightsTaif > 0) {
            NightBreakdown nb = new NightBreakdown(null, pkg.nightsTaif);
            nb.slot = 2;
            d.nightsBreakdown.add(nb);
        }
        if (pkg.nightsMadinah != null && pkg.nightsMadinah > 0) {
            NightBreakdown nb = new NightBreakdown(null, pkg.nightsMadinah);
            nb.slot = 1;
            d.nightsBreakdown.add(nb);
        }

        // 2. Hotel info — title built by Activity using getString() for locale support
        if (pkg.hotelMakkahName != null && !pkg.hotelMakkahName.trim().isEmpty()) {
            String rating = (pkg.hotelMakkahRating != null && !pkg.hotelMakkahRating.trim().isEmpty())
                    ? pkg.hotelMakkahRating : "";
            String subtitle = pkg.hotelMakkahName.trim() +
                    ((pkg.hotelMakkahDistance != null && !pkg.hotelMakkahDistance.trim().isEmpty())
                            ? " (" + pkg.hotelMakkahDistance.trim() + ")" : "");
            d.hotels.add(new HotelInfo("hotel", 0, rating, subtitle));
        }

        if (pkg.hotelMadinahName != null && !pkg.hotelMadinahName.trim().isEmpty()) {
            String rating = (pkg.hotelMadinahRating != null && !pkg.hotelMadinahRating.trim().isEmpty())
                    ? pkg.hotelMadinahRating : "";
            String subtitle = pkg.hotelMadinahName.trim() +
                    ((pkg.hotelMadinahDistance != null && !pkg.hotelMadinahDistance.trim().isEmpty())
                            ? " (" + pkg.hotelMadinahDistance.trim() + ")" : "");
            d.hotels.add(new HotelInfo("hotel", 1, rating, subtitle));
        }

        if (pkg.hotelTaifName != null && !pkg.hotelTaifName.trim().isEmpty()) {
            String rating = (pkg.hotelTaifRating != null && !pkg.hotelTaifRating.trim().isEmpty())
                    ? pkg.hotelTaifRating : "";
            String subtitle = pkg.hotelTaifName.trim() +
                    ((pkg.hotelTaifDistance != null && !pkg.hotelTaifDistance.trim().isEmpty())
                            ? " (" + pkg.hotelTaifDistance.trim() + ")" : "");
            d.hotels.add(new HotelInfo("hotel", 2, rating, subtitle));
        }

        // 3. Jadual Perjalanan (Itinerary)
        if (pkg.itineraries != null && !pkg.itineraries.isEmpty()) {
            for (UmrahPackage.ItineraryItem item : pkg.itineraries) {
                ItineraryDay day = new ItineraryDay();
                day.dayNumber = item.dayNumber;
                day.dateLabel = item.dateLabel != null && !item.dateLabel.trim().isEmpty() ? item.dateLabel.trim() : null;
                // Localized by the Activity via R.string.day_label_format /
                // R.string.itinerary_daily (dayNumber is the locale-neutral key).
                day.dayLabel = null;
                day.tag = null;
                day.title = item.title != null && !item.title.trim().isEmpty() ? item.title.trim() : null;
                day.routeText = "";

                if (item.description != null && !item.description.trim().isEmpty()) {
                    String[] lines = item.description.split("\r?\n");
                    for (String line : lines) {
                        String clean = line.trim();
                        if (!clean.isEmpty()) {
                            day.activities.add(clean.replaceFirst("^[•\\-*\\d.]+\\s*", ""));
                        }
                    }
                }
                day.hotelNote = item.accommodation != null ? item.accommodation : "";
                day.mealNote = item.meals != null ? item.meals : "";
                d.itinerary.add(day);
            }
        }

        // 4. Pecahan Harga Bilik (Room Pricing Tiers) — store locale-neutral keys;
        // the UI resolves them via RoomLabels (EN/MS). No hardcoded language here.
        if (pkg.priceQuint != null && !pkg.priceQuint.trim().isEmpty() && !pkg.priceQuint.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceQuint), "quint", "quint"));
        }
        if (pkg.priceQuad != null && !pkg.priceQuad.trim().isEmpty() && !pkg.priceQuad.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceQuad), "quad", "quad"));
        }
        if (pkg.priceTriple != null && !pkg.priceTriple.trim().isEmpty() && !pkg.priceTriple.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceTriple), "triple", "triple"));
        }
        if (pkg.priceDouble != null && !pkg.priceDouble.trim().isEmpty() && !pkg.priceDouble.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceDouble), "double", "double"));
        }
        if (pkg.priceSingle != null && !pkg.priceSingle.trim().isEmpty() && !pkg.priceSingle.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceSingle), "single", "single"));
        }
        if (d.priceOptions.isEmpty() && d.price != null && !d.price.isEmpty()) {
            d.priceOptions.add(new PriceOption(d.price, "from", "from"));
        }

        // 5. Termasuk (Inclusions) & Tidak Termasuk (Exclusions)
        if (pkg.inclusions != null && !pkg.inclusions.isEmpty()) {
            d.included.addAll(pkg.inclusions);
        }
        if (pkg.exclusions != null && !pkg.exclusions.isEmpty()) {
            d.excluded.addAll(pkg.exclusions);
        }

        // 6. Panduan & Senarai Keperluan (Packing Guide)
        if (pkg.packingGuide != null && !pkg.packingGuide.isEmpty()) {
            d.packingSummer.addAll(pkg.packingGuide);
        }

        d.companyWhatsapp = pkg.companyWhatsapp;

        // 7. Nota Penting & Syarat-Syarat
        if (pkg.importantNotes != null && !pkg.importantNotes.isEmpty()) {
            ImportantNote note = new ImportantNote();
            // Locale-neutral keys only (never displayed directly; UI uses resources).
            note.title = "terms_guidelines";
            note.badge = "important";
            note.bullets.addAll(pkg.importantNotes);
            d.importantNotes.add(note);
        }

        if (pkg.requiredDocuments != null && !pkg.requiredDocuments.isEmpty()) {
            d.requiredDocuments.addAll(pkg.requiredDocuments);
        }

        if (pkg.cancellationPolicy != null && !pkg.cancellationPolicy.isEmpty()) {
            d.cancellationPolicy.addAll(pkg.cancellationPolicy);
        }

        // 8. Departures — keep the backend ID alongside the display label (C1).
        if (pkg.departures != null && !pkg.departures.isEmpty()) {
            for (UmrahPackage.DepartureItem item : pkg.departures) {
                if (item.departureDate != null && !item.departureDate.isEmpty()) {
                    String label = item.departureDate;
                    if (item.returnDate != null && !item.returnDate.isEmpty()) {
                        label += " hingga " + item.returnDate;
                    }
                    d.availableDepartureDates.add(label);
                    d.availableDepartures.add(new DepartureOption(item.id, item.departureDate, item.returnDate, label));
                }
            }
        }

        // 9. Gallery
        if (d.imageUrl != null && !d.imageUrl.isEmpty()) {
            d.galleryImageUrls.add(d.imageUrl);
        }
        if (pkg.images != null && !pkg.images.isEmpty()) {
            for (UmrahPackage.ImageItem img : pkg.images) {
                if (img.url != null && !img.url.trim().isEmpty() && !d.galleryImageUrls.contains(img.url.trim())) {
                    d.galleryImageUrls.add(img.url.trim());
                }
            }
        }

        if (pkg.relatedPackages != null && !pkg.relatedPackages.isEmpty()) {
            d.relatedPackages.addAll(pkg.relatedPackages);
        }

        return d;
    }

    private static String formatCurrency(String raw) {
        return com.hafiztraveltours.app.utils.MoneyFormat.formatRaw(raw);
    }
}
