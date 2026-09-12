package com.hafiztraveltours.app;

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

    public static class NightBreakdown {
        public String city;
        public int nights;
        public NightBreakdown(String city, int nights) {
            this.city = city;
            this.nights = nights;
        }
    }

    public static class HotelInfo {
        public String type; // "mekah" | "madinah" | "flight" | "hotel"
        public String title;
        public String subtitle;
        public HotelInfo(String type, String title, String subtitle) {
            this.type = type;
            this.title = title;
            this.subtitle = subtitle;
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
        d.whatsappMessage = "Salam, saya berminat untuk mengetahui lebih lanjut mengenai pakej " + d.name + " (" + d.price + "). Boleh kongsikan jadual dan kekosongan terkini?";

        // 1. Ringkasan Penginapan (Nights Breakdown)
        if (pkg.nightsMakkah != null && pkg.nightsMakkah > 0) {
            d.nightsBreakdown.add(new NightBreakdown("Makkah", pkg.nightsMakkah));
        }
        if (pkg.nightsMadinah != null && pkg.nightsMadinah > 0) {
            d.nightsBreakdown.add(new NightBreakdown("Madinah", pkg.nightsMadinah));
        }

        // 2. Hotel & Penerbangan
        if (pkg.hotelMakkahName != null && !pkg.hotelMakkahName.trim().isEmpty()) {
            String star = (pkg.hotelMakkahRating != null && !pkg.hotelMakkahRating.trim().isEmpty())
                    ? pkg.hotelMakkahRating
                    : "5 Bintang";
            String title = "Hotel Makkah (" + star + ")";
            String subtitle = pkg.hotelMakkahName.trim() +
                    ((pkg.hotelMakkahDistance != null && !pkg.hotelMakkahDistance.trim().isEmpty())
                            ? " (" + pkg.hotelMakkahDistance.trim() + ")"
                            : "");
            d.hotels.add(new HotelInfo("mekah", title, subtitle));
        }

        if (pkg.hotelMadinahName != null && !pkg.hotelMadinahName.trim().isEmpty()) {
            String star = (pkg.hotelMadinahRating != null && !pkg.hotelMadinahRating.trim().isEmpty())
                    ? pkg.hotelMadinahRating
                    : "5 Bintang";
            String title = "Hotel Madinah (" + star + ")";
            String subtitle = pkg.hotelMadinahName.trim() +
                    ((pkg.hotelMadinahDistance != null && !pkg.hotelMadinahDistance.trim().isEmpty())
                            ? " (" + pkg.hotelMadinahDistance.trim() + ")"
                            : "");
            d.hotels.add(new HotelInfo("madinah", title, subtitle));
        }

        if (pkg.airlineName != null && !pkg.airlineName.trim().isEmpty()) {
            String flightType = (pkg.flightType != null && !pkg.flightType.trim().isEmpty())
                    ? pkg.flightType
                    : "Penerbangan";
            String title = flightType + " (" + pkg.airlineName.trim() + ")";
            String subtitle = (pkg.flightRoute != null && !pkg.flightRoute.trim().isEmpty())
                    ? pkg.flightRoute.trim()
                    : "Penerbangan pergi dan balik";
            d.hotels.add(new HotelInfo("flight", title, subtitle));
        }

        // 3. Jadual Perjalanan (Itinerary)
        if (pkg.itineraries != null && !pkg.itineraries.isEmpty()) {
            for (UmrahPackage.ItineraryItem item : pkg.itineraries) {
                ItineraryDay day = new ItineraryDay();
                day.dayNumber = item.dayNumber;
                day.dayLabel = "Hari " + item.dayNumber;
                day.tag = "Jadual Harian";
                day.title = item.title != null && !item.title.trim().isEmpty() ? item.title.trim() : ("Hari " + item.dayNumber);
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
