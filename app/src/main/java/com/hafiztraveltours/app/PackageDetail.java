package com.hafiztraveltours.app;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Full package detail (itinerary, hotels, pricing tiers, included/excluded,
 * packing guide, gallery) - parsed from a single umrah_packages / tour_packages
 * Firestore document. See firestore_schema_package_detail.md for the field
 * layout each document should follow.
 *
 * Every getter below is defensive - a package that hasn't had a section
 * filled in yet returns an empty list/string instead of throwing, so
 * PackageDetailActivity can simply skip rendering that section.
 */
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
        NightBreakdown(String city, int nights) {
            this.city = city;
            this.nights = nights;
        }
    }

    public static class HotelInfo {
        public String type; // "mekah" | "madinah" | "flight"
        public String title;
        public String subtitle;
        HotelInfo(String type, String title, String subtitle) {
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
        public String hotelNote;
        public String mealNote;
    }

    public static class ImportantNote {
        public String title;
        public String badge;
        public List<String> bullets = new ArrayList<>();
    }

    public static class PriceOption {
        public String price;
        public String occupancyLabel;
        PriceOption(String price, String occupancyLabel) {
            this.price = price;
            this.occupancyLabel = occupancyLabel;
        }
    }

    public static PackageDetail fromDocument(DocumentSnapshot doc) {
        PackageDetail d = new PackageDetail();
        d.id = doc.getId();
        d.name = str(doc.getString("name"));
        d.summaryLine = str(doc.getString("summaryLine"));
        d.durationDays = doc.getLong("durationDays") != null ? doc.getLong("durationDays").intValue() : 0;
        d.nightsCount = doc.getLong("nightsCount") != null ? doc.getLong("nightsCount").intValue() : 0;
        d.price = str(doc.getString("price"));
        d.imageUrl = str(doc.getString("imageUrl"));
        d.posterImageUrl = str(doc.getString("posterImageUrl"));
        d.departureDatesNote = str(doc.getString("departureDatesNote"));
        d.whatsappMessage = str(doc.getString("whatsappMessage"));

        for (Object raw : safeList(doc.get("nightsBreakdown"))) {
            Map<?, ?> m = asMap(raw);
            if (m == null) continue;
            d.nightsBreakdown.add(new NightBreakdown(
                    strOf(m.get("city")), intOf(m.get("nights"))));
        }

        for (Object raw : safeList(doc.get("hotels"))) {
            Map<?, ?> m = asMap(raw);
            if (m == null) continue;
            d.hotels.add(new HotelInfo(
                    strOf(m.get("type")), strOf(m.get("title")), strOf(m.get("subtitle"))));
        }

        for (Object raw : safeList(doc.get("itinerary"))) {
            Map<?, ?> m = asMap(raw);
            if (m == null) continue;
            ItineraryDay day = new ItineraryDay();
            day.dayNumber = intOf(m.get("dayNumber"));
            day.dayLabel = strOf(m.get("dayLabel"));
            day.tag = strOf(m.get("tag"));
            day.timeNote = strOf(m.get("timeNote"));
            day.title = strOf(m.get("title"));
            day.routeText = strOf(m.get("routeText"));
            day.highlights = strList(m.get("highlights"));
            day.activities = strList(m.get("activities"));
            day.hotelNote = strOf(m.get("hotelNote"));
            day.mealNote = strOf(m.get("mealNote"));
            d.itinerary.add(day);
        }

        for (Object raw : safeList(doc.get("importantNotes"))) {
            Map<?, ?> m = asMap(raw);
            if (m == null) continue;
            ImportantNote note = new ImportantNote();
            note.title = strOf(m.get("title"));
            note.badge = strOf(m.get("badge"));
            note.bullets = strList(m.get("bullets"));
            d.importantNotes.add(note);
        }

        d.included = strList(doc.get("included"));
        d.excluded = strList(doc.get("excluded"));
        d.packingSummer = strList(doc.get("packingSummer"));
        d.packingWinter = strList(doc.get("packingWinter"));
        d.galleryImageUrls = strList(doc.get("galleryImageUrls"));

        for (Object raw : safeList(doc.get("priceOptions"))) {
            Map<?, ?> m = asMap(raw);
            if (m == null) continue;
            d.priceOptions.add(new PriceOption(strOf(m.get("price")), strOf(m.get("occupancyLabel"))));
        }

        return d;
    }

    // ---- small parsing helpers (all null-safe) ----

    private static String str(String s) { return s != null ? s : ""; }

    private static String strOf(Object o) { return o != null ? String.valueOf(o) : ""; }

    private static int intOf(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return 0;
    }

    private static List<?> safeList(Object o) {
        return (o instanceof List) ? (List<?>) o : new ArrayList<>();
    }

    private static Map<?, ?> asMap(Object o) {
        return (o instanceof Map) ? (Map<?, ?>) o : null;
    }

    private static List<String> strList(Object o) {
        List<String> result = new ArrayList<>();
        if (o instanceof List) {
            for (Object item : (List<?>) o) {
                if (item != null) result.add(String.valueOf(item));
            }
        }
        return result;
    }
}
