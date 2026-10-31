package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class PackageUpgradeDto implements Serializable {

    public static final String CATEGORY_HOTEL_MAKKAH = "hotel_makkah";
    public static final String CATEGORY_HOTEL_MADINAH = "hotel_madinah";
    public static final String CATEGORY_MEALS = "meals";
    public static final String CATEGORY_LUGGAGE = "luggage";
    public static final String CATEGORY_TRANSPORT = "transportation";
    public static final String CATEGORY_SPEED_TRAIN = "speed_train";
    public static final String CATEGORY_ACCESSORIES = "accessories";
    public static final String CATEGORY_OTHER = "other";

    public static final String BASIS_PER_PAX = "per_pax";
    public static final String BASIS_PER_UNIT = "per_unit";

    public static final String TIER_NEAR = "near";
    public static final String TIER_MODERATE = "moderate";
    public static final String TIER_FAR = "far";

    @SerializedName("id")
    public int id;

    @SerializedName("category")
    public String category;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("hotel_star_rating")
    public String hotelStarRating;

    @SerializedName("hotel_distance_text")
    public String hotelDistanceText;

    @SerializedName("distance_tier")
    public String distanceTier;

    @SerializedName("service_scope")
    public String serviceScope;

    @SerializedName("pricing_basis")
    public String pricingBasis = BASIS_PER_PAX;

    @SerializedName("default_price")
    public double defaultPrice;

    @SerializedName("effective_price")
    public double effectivePrice;

    @SerializedName("price_formatted")
    public String priceFormatted;

    @SerializedName("max_qty_per_pax")
    public Integer maxQtyPerPax;

    @SerializedName("is_available")
    public boolean isAvailable = true;

    public PackageUpgradeDto() {}

    public boolean isPerPax() {
        return BASIS_PER_PAX.equalsIgnoreCase(pricingBasis);
    }

    public boolean isPerUnit() {
        return BASIS_PER_UNIT.equalsIgnoreCase(pricingBasis);
    }

    public double getPrice() {
        return effectivePrice > 0 ? effectivePrice : defaultPrice;
    }
}
