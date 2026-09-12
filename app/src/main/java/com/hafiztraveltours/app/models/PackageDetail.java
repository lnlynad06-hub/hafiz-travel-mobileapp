package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PackageDetail {

    public String id;
    public String name;
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
    public List<HotelInfo> hotels = new ArrayList<>();
    public List<ItineraryDay> itinerary = new ArrayList<>();
    public List<ImportantNote> importantNotes = new ArrayList<>();
    public List<String> included = new ArrayList<>();
    public List<String> excluded = new ArrayList<>();
    public List<String> packingSummer = new ArrayList<>();
    public List<String> packingWinter = new ArrayList<>();
    public List<PriceOption> priceOptions = new ArrayList<>();
    public List<String> galleryImageUrls = new ArrayList<>();
    public String whatsappMessage;
    public boolean isUmrah; // set from UmrahPackage.isUmrah() during parsing

    public static class NightBreakdown {
        public String city;          // set by Activity from getString() using slot
        public int nights;
        public int slot;             // 0=Makkah/Hotel1, 1=Madinah/Hotel2, 2=Taif/Hotel3
        public String destinationHint; // raw first destination word for tour packages
        public NightBreakdown(String city, int nights) {
            this.city = city;
            this.nights = nights;
        }
    }

    public static class HotelInfo {
        public String type; // "hotel" | "flight"
        public String title;  // built lazily; overridden by Activity with getString()
        public String subtitle;
        public int slot;       // 0=Hotel1/Makkah, 1=Hotel2/Madinah, 2=Hotel3/Taif, -1=flight
        public String rawRating; // raw rating/star string from server (e.g. "5 Stars")
        public HotelInfo(String type, int slot, String rawRating, String subtitle) {
            this.type = type;
            this.slot = slot;
            this.rawRating = rawRating != null ? rawRating : "";
            this.subtitle = subtitle;
            this.title = ""; // set by Activity
        }
    }

    public static class ItineraryDay {
        public int dayNumber;
        public String dayLabel;
        public String tag;
        public String timeNote;
        public String title;
        public String routeText;
        public List<String> highlights = new ArrayList<>();
        public List<String> activities = new ArrayList<>();
        public String hotelNote = "";
        public String mealNote = "";
    }

    public static class ImportantNote {
        public String title;
        public String badge;
        public List<String> bullets = new ArrayList<>();
    }

    public static class PriceOption {
        public String price;
        public String occupancyLabel;
        public PriceOption(String price, String occupancyLabel) {
            this.price = price;
            this.occupancyLabel = occupancyLabel;
        }
    }

    public static PackageDetail fromUmrahPackage(UmrahPackage pkg) {
        PackageDetail d = new PackageDetail();
        if (pkg == null) return d;

        d.id = pkg.id;
        d.name = pkg.getDisplayName();
        d.summaryLine = pkg.summary != null && !pkg.summary.trim().isEmpty() ? pkg.summary.trim() : "";
        d.durationDays = pkg.durationDays > 0 ? pkg.durationDays : 0;
        d.nightsCount = pkg.nightsCount > 0 ? pkg.nightsCount : 0;
        d.price = pkg.price != null && !pkg.price.trim().isEmpty() ? pkg.price : "Hubungi Kami";
        d.imageUrl = pkg.imageUrl != null && !pkg.imageUrl.trim().isEmpty() ? pkg.imageUrl : "";
        d.posterImageUrl = d.imageUrl;
        d.durationFormatted = pkg.getDurationFormatted();
        // WhatsApp text is built by the Activity with localized resources.
        d.whatsappMessage = null;

        boolean isUmrah = pkg.isUmrah();
        d.isUmrah = isUmrah;

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

        if (pkg.airlineName != null && !pkg.airlineName.trim().isEmpty()) {
            // Flight type & route are raw server data — not translated here
            String flightType = (pkg.flightType != null && !pkg.flightType.trim().isEmpty())
                    ? pkg.flightType : null; // null → Activity uses getString(flight_type_fallback)
            String flightRoute = (pkg.flightRoute != null && !pkg.flightRoute.trim().isEmpty())
                    ? pkg.flightRoute.trim() : null; // null → Activity uses getString(flight_route_fallback)
            // Store flightType as rawRating slot, route as subtitle (null handled in Activity)
            String subtitle = flightRoute != null ? flightRoute : "";
            HotelInfo fi = new HotelInfo("flight", -1, flightType != null ? flightType : "", subtitle);
            fi.rawRating = flightType != null ? flightType : ""; // reuse as flightType
            d.hotels.add(fi);
        }

        // 3. Jadual Perjalanan (Itinerary)
        if (pkg.itineraries != null && !pkg.itineraries.isEmpty()) {
            for (UmrahPackage.ItineraryItem item : pkg.itineraries) {
                ItineraryDay day = new ItineraryDay();
                day.dayNumber = item.dayNumber;
                // Localized by the Activity via R.string.day_label_format /
                // R.string.itinerary_daily (dayNumber is the locale-neutral key).
                day.dayLabel = null;
                day.tag = null;
                day.title = item.title != null && !item.title.trim().isEmpty() ? item.title.trim() : null;
                day.routeText = pkg.destination != null ? pkg.destination : "";

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

        // 4. Pecahan Harga Bilik (Room Pricing Tiers)
        if (pkg.priceQuint != null && !pkg.priceQuint.trim().isEmpty() && !pkg.priceQuint.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceQuint), "Bilik Berlima (Quint)"));
        }
        if (pkg.priceQuad != null && !pkg.priceQuad.trim().isEmpty() && !pkg.priceQuad.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceQuad), "Bilik Berempat (Quad)"));
        }
        if (pkg.priceTriple != null && !pkg.priceTriple.trim().isEmpty() && !pkg.priceTriple.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceTriple), "Bilik Bertiga (Triple)"));
        }
        if (pkg.priceDouble != null && !pkg.priceDouble.trim().isEmpty() && !pkg.priceDouble.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceDouble), "Bilik Berdua (Double)"));
        }
        if (pkg.priceSingle != null && !pkg.priceSingle.trim().isEmpty() && !pkg.priceSingle.equals("0.00")) {
            d.priceOptions.add(new PriceOption(formatCurrency(pkg.priceSingle), "Bilik Perseorangan (Single)"));
        }
        if (d.priceOptions.isEmpty() && d.price != null && !d.price.isEmpty()) {
            d.priceOptions.add(new PriceOption(d.price, "Harga Bermula Dari"));
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

        // 7. Nota Penting & Syarat-Syarat
        if (pkg.importantNotes != null && !pkg.importantNotes.isEmpty()) {
            ImportantNote note = new ImportantNote();
            note.title = "Syarat & Garis Panduan Pakej";
            note.badge = "Penting";
            note.bullets.addAll(pkg.importantNotes);
            d.importantNotes.add(note);
        }

        if (pkg.requiredDocuments != null && !pkg.requiredDocuments.isEmpty()) {
            ImportantNote docNote = new ImportantNote();
            docNote.title = "Dokumen Yang Diperlukan";
            docNote.badge = "Dokumen";
            docNote.bullets.addAll(pkg.requiredDocuments);
            d.importantNotes.add(docNote);
        }

        // 8. Gallery
        if (d.imageUrl != null && !d.imageUrl.isEmpty()) {
            d.galleryImageUrls.add(d.imageUrl);
        }

        return d;
    }

    private static String formatCurrency(String raw) {
        if (raw == null) return "RM -";
        try {
            double val = Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
            DecimalFormat df = new DecimalFormat("#,##0");
            return "RM " + df.format(val);
        } catch (Exception e) {
            return "RM " + raw;
        }
    }
}
