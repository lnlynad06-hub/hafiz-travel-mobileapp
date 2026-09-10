package com.hafiztraveltours.app;

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
        public String type; // "mekah" | "madinah" | "flight"
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
        public PriceOption(String price, String occupancyLabel) {
            this.price = price;
            this.occupancyLabel = occupancyLabel;
        }
    }

    public static PackageDetail fromUmrahPackage(UmrahPackage pkg) {
        PackageDetail d = new PackageDetail();
        if (pkg == null) return d;

        d.id = pkg.id;
        d.name = pkg.name != null ? pkg.name : "";
        d.summaryLine = pkg.summary != null ? pkg.summary : "";
        d.durationDays = pkg.durationDays;
        d.nightsCount = pkg.nightsCount;
        d.price = pkg.price != null ? pkg.price : "";
        d.imageUrl = pkg.imageUrl != null ? pkg.imageUrl : "";
        d.whatsappMessage = "Salam, saya berminat dengan pakej " + d.name;

        return d;
    }
}
