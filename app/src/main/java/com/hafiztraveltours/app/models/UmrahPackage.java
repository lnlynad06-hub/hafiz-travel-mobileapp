package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import com.google.gson.annotations.SerializedName;

/**
 * API response DTO for packages (H4). Mirrors the Laravel JSON 1:1 — field names and
 * types are contractual, do not rename. Also carries list helpers (filtering, price
 * parsing). Convert to {@link PackageDetail} via {@code PackageDetail.fromUmrahPackage()}
 * before showing booking UI; raw flight/route fields are read directly by
 * PackageDetailActivity from the DTO.
 */
public class UmrahPackage implements java.io.Serializable {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("title")
    public String title;

    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) return name;
        if (title != null && !title.trim().isEmpty()) return title;
        return "Pakej Pelancongan";
    }

    @SerializedName("duration_days")
    public int durationDays;

    @SerializedName("nights_count")
    public int nightsCount;

    @SerializedName("price_formatted")
    public String price;

    @SerializedName("starting_price")
    public String startingPrice;

    @SerializedName("url")
    public String url;

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("category")
    public String category;

    @SerializedName("destination")
    public String destination;

    @SerializedName("summary")
    public String summary;

    @SerializedName("is_featured")
    public boolean isFeatured;

    @SerializedName("hotel_distance")
    public String hotelDistance;

    @SerializedName("makkah_hotel_distance")
    public String makkahHotelDistance;

    @SerializedName("nights_makkah")
    public Integer nightsMakkah;

    @SerializedName("nights_madinah")
    public Integer nightsMadinah;

    @SerializedName("nights_taif")
    public Integer nightsTaif;

    @SerializedName("airline_name")
    public String airlineName;

    @SerializedName("flight_type")
    public String flightType;

    @SerializedName("flight_route")
    public String flightRoute;

    @SerializedName("hotel_makkah_name")
    public String hotelMakkahName;

    @SerializedName("hotel_makkah_rating")
    public String hotelMakkahRating;

    @SerializedName("hotel_makkah_distance")
    public String hotelMakkahDistance;

    @SerializedName("hotel_madinah_name")
    public String hotelMadinahName;

    @SerializedName("hotel_madinah_rating")
    public String hotelMadinahRating;

    @SerializedName("hotel_madinah_distance")
    public String hotelMadinahDistance;

    @SerializedName("hotel_taif_name")
    public String hotelTaifName;

    @SerializedName("hotel_taif_rating")
    public String hotelTaifRating;

    @SerializedName("hotel_taif_distance")
    public String hotelTaifDistance;

    @SerializedName("price_quint")
    public String priceQuint;

    @SerializedName("price_quad")
    public String priceQuad;

    @SerializedName("duration_formatted")
    public String durationFormatted;

    @SerializedName("price_triple")
    public String priceTriple;

    @SerializedName("price_double")
    public String priceDouble;

    @SerializedName("price_single")
    public String priceSingle;

    @SerializedName("inclusions")
    public java.util.List<String> inclusions;

    @SerializedName("exclusions")
    public java.util.List<String> exclusions;

    @SerializedName("package_type")
    public String packageType;

    @SerializedName("tour_category_type")
    public String tourCategoryType;

    @SerializedName("is_umrah")
    public Boolean isUmrahFlag;

    @SerializedName("tour_hotels")
    public java.util.List<TourHotelItem> tourHotels;

    @SerializedName("season_prices")
    public java.util.Map<String, String> seasonPrices;

    @SerializedName("season_pricing")
    public SeasonPricingData seasonPricing;

    @SerializedName("child_under_2_price")
    public Double childUnder2Price;

    @SerializedName("child_2_11_with_bed_discount")
    public Double child211WithBedDiscount;

    @SerializedName("child_2_11_no_bed_discount")
    public Double child211NoBedDiscount;

    @SerializedName("child_2_to_4_discount")
    public Double child2To4Discount;

    @SerializedName("child_2_11_with_bed_price")
    public Double child211WithBedPrice;

    @SerializedName("child_2_11_no_bed_price")
    public Double child211NoBedPrice;

    @SerializedName("child_2_to_4_price")
    public Double child2To4Price;

    @SerializedName("child_pricing_rules")
    public ChildPricingRules childPricingRules;

    public static class TourHotelItem implements java.io.Serializable {
        @SerializedName("id")
        public String id;

        @SerializedName("hotel_name")
        public String hotelName;

        @SerializedName("nights")
        public Integer nights;

        @SerializedName("rating")
        public String rating;

        @SerializedName("sort_order")
        public Integer sortOrder;
    }

    public static class SeasonPricingData implements java.io.Serializable {
        @SerializedName("standard")
        public SeasonRate standard;

        @SerializedName("low_peak")
        public SeasonRate lowPeak;

        @SerializedName("high_peak")
        public SeasonRate highPeak;
    }

    public static class SeasonRate implements java.io.Serializable {
        @SerializedName("adult_price")
        public double adultPrice;

        @SerializedName("child_with_bed")
        public double childWithBed;

        @SerializedName("child_without_bed")
        public double childWithoutBed;

        @SerializedName("child_under_2")
        public double childUnder2;

        @SerializedName("child_2_to_4")
        public double child2To4;
    }

    public static class ChildPricingRules implements java.io.Serializable {
        @SerializedName("type")
        public String type;

        @SerializedName("child_under_2_price")
        public Double childUnder2Price;

        @SerializedName("child_2_11_with_bed_discount")
        public Double child211WithBedDiscount;

        @SerializedName("child_2_11_no_bed_discount")
        public Double child211NoBedDiscount;

        @SerializedName("child_2_to_4_discount")
        public Double child2To4Discount;

        @SerializedName("child_2_11_with_bed_price")
        public Double child211WithBedPrice;

        @SerializedName("child_2_11_no_bed_price")
        public Double child211NoBedPrice;

        @SerializedName("child_2_to_4_price")
        public Double child2To4Price;

        @SerializedName("under_2_fixed_price")
        public Double under2FixedPrice;

        @SerializedName("under_2_label")
        public String under2Label;

        @SerializedName("age_2_to_11_bed_label")
        public String age2To11BedLabel;

        @SerializedName("age_2_to_11_no_bed_label")
        public String age2To11NoBedLabel;

        @SerializedName("age_2_to_4_label")
        public String age2To4Label;

        @SerializedName("above_11_label")
        public String above11Label;

        @SerializedName("above_4_label")
        public String above4Label;
    }

    @SerializedName("requires_passport")
    public Boolean requiresPassport;

    @SerializedName("requires_ic")
    public Boolean requiresIc;

    @SerializedName("requires_mahram")
    public Boolean requiresMahram;

    @SerializedName("requires_clothes_size")
    public Boolean requiresClothesSize;

    @SerializedName("passport_validity_months")
    public Integer passportValidityMonths;

    @SerializedName("required_documents")
    public java.util.List<String> requiredDocuments;

    @SerializedName("packing_guide")
    public java.util.List<String> packingGuide;

    @SerializedName("important_notes")
    public java.util.List<String> importantNotes;

    @SerializedName("cancellation_policy")
    public java.util.List<String> cancellationPolicy;

    @SerializedName("company_whatsapp")
    public String companyWhatsapp;

    @SerializedName("images")
    public java.util.List<ImageItem> images;

    public static class ImageItem implements java.io.Serializable {
        // Backend gallery image id (integer PK). String receives both JSON numbers
        // and numeric strings without crashing (Gson coerces numbers to String).
        @SerializedName("id")
        public String id;

        @SerializedName("url")
        public String url;

        @SerializedName("is_primary")
        public boolean isPrimary;
    }

    @SerializedName("departures")
    public java.util.List<DepartureItem> departures;

    public static class DepartureItem implements java.io.Serializable {
        // Backend departures PK is an integer, but kept as String on receive:
        // Gson coerces JSON numbers into String safely, while Integer would crash
        // on numeric strings. Parsed to Integer at booking creation time.
        @SerializedName("id")
        public String id;

        @SerializedName("departure_date")
        public String departureDate;

        @SerializedName("return_date")
        public String returnDate;

        @SerializedName("capacity")
        public Integer capacity;

        @SerializedName("total_seats")
        public Integer totalSeats;

        @SerializedName("seats_booked")
        public Integer seatsBooked;

        @SerializedName("seats_available")
        public Integer seatsAvailable;

        @SerializedName("is_full")
        public Boolean isFull;

        @SerializedName("season")
        public String season;

        @SerializedName("price")
        public String price;

        @SerializedName("price_formatted")
        public String priceFormatted;

        @SerializedName("pricing")
        public java.util.List<PricingTier> pricing;

        public static class PricingTier implements java.io.Serializable {
            @SerializedName("id")
            public Integer id;
            @SerializedName("label")
            public String label;
            @SerializedName("amount")
            public String amount;
            @SerializedName("currency")
            public String currency;
        }
    }

    @SerializedName("itineraries")
    public java.util.List<ItineraryItem> itineraries;

    @SerializedName("related_packages")
    public java.util.List<UmrahPackage> relatedPackages;

    public static class ItineraryItem implements java.io.Serializable {
        @SerializedName("day_number")
        public int dayNumber;

        @SerializedName("date_label")
        public String dateLabel;

        @SerializedName("title")
        public String title;

        @SerializedName("description")
        public String description;

        @SerializedName("accommodation")
        public String accommodation;

        @SerializedName("meals")
        public String meals;
    }

    /** "umrah" atau "tour" / "umrah_packages" atau "tour_packages" */
    public String collectionName;

    public boolean isUmrah() {
        if (isUmrahFlag != null) {
            return isUmrahFlag;
        }
        if (tourCategoryType != null && !tourCategoryType.trim().isEmpty()) {
            String tct = tourCategoryType.trim().toLowerCase();
            if ("umrah".equals(tct)) return true;
            if ("outbound".equals(tct) || "domestic".equals(tct)) return false;
        }
        if (packageType != null && !packageType.trim().isEmpty()) {
            String pt = packageType.trim().toLowerCase();
            if (pt.contains("umrah") || pt.contains("haji") || pt.contains("hajj") || pt.contains("ziarah")) return true;
            if (pt.contains("outbound") || pt.contains("international") || pt.contains("domestic") || pt.contains("tour")) return false;
        }
        if (collectionName != null) {
            String col = collectionName.toLowerCase();
            if (col.contains("umrah")) return true;
            if (col.contains("tour")) return false;
        }
        if (nightsMakkah != null && nightsMakkah > 0) return true;
        if (hotelMakkahName != null && !hotelMakkahName.trim().isEmpty()) return true;

        return false;
    }

    /**
     * Raw hotel distance as sent by the API (e.g. "50m"), or null when absent.
     * Callers append the localized suffix via R.string.hotel_distance_to,
     * unless the value already mentions a mosque.
     */
    public String getRawHotelDistance() {
        String[] candidates = {hotelMakkahDistance, makkahHotelDistance, hotelDistance};
        for (String raw : candidates) {
            if (raw != null && !raw.trim().isEmpty()) {
                return raw.trim();
            }
        }
        return null;
    }

    /**
     * @deprecated Use {@link #getRawHotelDistance()} plus
     * R.string.hotel_distance_to at the call site instead.
     */
    @Deprecated
    public String getHotelDistanceDisplay() {
        return getRawHotelDistance();
    }

    public String getDurationFormatted() {
        if (durationFormatted != null && !durationFormatted.trim().isEmpty()) {
            return durationFormatted;
        }
        return null;
    }

    public UmrahPackage() {}

    public UmrahPackage(String id, String name, int durationDays, int nightsCount,
                        String price, String url, String imageUrl) {
        this.id = id;
        this.name = name;
        this.durationDays = durationDays;
        this.nightsCount = nightsCount;
        this.price = price;
        this.url = url;
        this.imageUrl = imageUrl;
    }

    public double getNumericPrice() {
        // Preserved semantics: startingPrice wins when present (even "0.00"), else price.
        if (startingPrice != null && !startingPrice.trim().isEmpty()) {
            return com.hafiztraveltours.app.utils.MoneyFormat.parseAmount(startingPrice);
        }
        return com.hafiztraveltours.app.utils.MoneyFormat.parseAmount(price);
    }

    public boolean matchesCategory(String filterCategory) {
        if (filterCategory == null || filterCategory.trim().isEmpty() || "ALL".equalsIgnoreCase(filterCategory)) {
            return true;
        }
        boolean isUmrahPkg = isUmrah();

        if ("UMRAH".equalsIgnoreCase(filterCategory)) {
            return isUmrahPkg;
        } else if ("TOUR".equalsIgnoreCase(filterCategory)) {
            return !isUmrahPkg;
        }

        if (category != null && category.toLowerCase().contains(filterCategory.toLowerCase())) {
            return true;
        }
        return false;
    }

    public boolean matchesDestination(String destCode) {
        if (destCode == null || destCode.trim().isEmpty() || "ALL".equalsIgnoreCase(destCode)) {
            return true;
        }
        String combined = ((name != null ? name : "") + " " +
                (title != null ? title : "") + " " +
                (category != null ? category : "") + " " +
                (destination != null ? destination : "") + " " +
                (summary != null ? summary : "")).toLowerCase();

        switch (destCode.toUpperCase()) {
            case "TURKEY":
                return combined.contains("turki") || combined.contains("turkey") ||
                        combined.contains("istanbul") || combined.contains("cappadocia");
            case "KOREA":
                return combined.contains("korea") || combined.contains("seoul") ||
                        combined.contains("jeju") || combined.contains("nami");
            case "JAPAN":
                return combined.contains("jepun") || combined.contains("japan") ||
                        combined.contains("tokyo") || combined.contains("osaka");
            case "SAUDI":
                return combined.contains("saudi") || combined.contains("makkah") ||
                        combined.contains("madinah") || combined.contains("umrah") ||
                        combined.contains("taif") || combined.contains("syawal");
            case "EUROPE":
                return combined.contains("eropah") || combined.contains("europe") ||
                        combined.contains("balkan") || combined.contains("switzerland") ||
                        combined.contains("bosnia") || combined.contains("london");
            case "VIETNAM":
                return combined.contains("vietnam") || combined.contains("danang") ||
                        combined.contains("hanoi") || combined.contains("ho chi minh");
            case "CHINA":
                return combined.contains("china") || combined.contains("beijing") ||
                        combined.contains("shanghai") || combined.contains("guangzhou");
            default:
                return combined.contains(destCode.toLowerCase());
        }
    }

    public boolean matchesPriceRange(float min, float max) {
        double p = getNumericPrice();
        if (p <= 0.0) return true; // Include packages without explicit price in results
        return p >= min && p <= max;
    }

    public boolean matchesCriteria(FilterCriteria criteria) {
        if (criteria == null || criteria.isDefault()) return true;
        return matchesCategory(criteria.category) &&
                matchesDestination(criteria.destination) &&
                matchesPriceRange(criteria.minPrice, criteria.maxPrice);
    }
    /**
     * Returns accommodation labels based on package type.
     * For Umrah packages: "Penginapan Makkah", "Penginapan Madinah", optional "Penginapan Taif".
     * For Tour packages: "Penginapan Utama", "Penginapan Kedua", optional "Penginapan Tambahan".
     */
    public java.util.List<String> getAccommodationLabels() {
        java.util.List<String> labels = new java.util.ArrayList<>();
        if (isUmrah()) {
            if (hotelMakkahName != null && !hotelMakkahName.trim().isEmpty()) {
                labels.add("Penginapan Makkah");
            }
            if (hotelMadinahName != null && !hotelMadinahName.trim().isEmpty()) {
                labels.add("Penginapan Madinah");
            }
            if (nightsTaif != null && nightsTaif > 0) {
                labels.add("Penginapan Taif");
            }
        } else {
            if (hotelMakkahName != null && !hotelMakkahName.trim().isEmpty()) {
                labels.add("Penginapan Utama");
            }
            if (hotelMadinahName != null && !hotelMadinahName.trim().isEmpty()) {
                labels.add("Penginapan Kedua");
            }
            if (hotelTaifName != null && !hotelTaifName.trim().isEmpty()) {
                labels.add("Penginapan Tambahan");
            }
        }
        return labels;
    }
}