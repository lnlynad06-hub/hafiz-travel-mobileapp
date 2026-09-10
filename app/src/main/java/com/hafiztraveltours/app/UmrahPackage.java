package com.hafiztraveltours.app;

import com.google.gson.annotations.SerializedName;

public class UmrahPackage {
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

    /** "umrah" atau "tour" / "umrah_packages" atau "tour_packages" */
    public String collectionName;

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
        if (startingPrice != null && !startingPrice.trim().isEmpty()) {
            try {
                String clean = startingPrice.replaceAll("[^0-9.]", "");
                if (!clean.isEmpty()) return Double.parseDouble(clean);
            } catch (Exception ignored) {}
        }
        if (price != null && !price.trim().isEmpty()) {
            try {
                String clean = price.replaceAll("[^0-9.]", "");
                if (!clean.isEmpty()) return Double.parseDouble(clean);
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    public boolean matchesCategory(String filterCategory) {
        if (filterCategory == null || filterCategory.trim().isEmpty() || "ALL".equalsIgnoreCase(filterCategory)) {
            return true;
        }
        boolean isUmrah = ("umrah".equalsIgnoreCase(category) ||
                (collectionName != null && collectionName.contains("umrah")) ||
                (name != null && name.toLowerCase().contains("umrah")) ||
                (title != null && title.toLowerCase().contains("umrah")));

        if ("UMRAH".equalsIgnoreCase(filterCategory)) {
            return isUmrah;
        } else if ("TOUR".equalsIgnoreCase(filterCategory)) {
            return !isUmrah;
        }
        return true;
    }

    public boolean matchesDestination(String destCode) {
        if (destCode == null || destCode.trim().isEmpty() || "ALL".equalsIgnoreCase(destCode)) {
            return true;
        }
        String combined = ((name != null ? name : "") + " " +
                (title != null ? title : "") + " " +
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
}