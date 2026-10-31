package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomizationDataDto implements Serializable {

    @SerializedName("package_inclusions")
    public PackageInclusions packageInclusions;

    @SerializedName("original_hotels")
    public OriginalHotels originalHotels;

    @SerializedName("is_full_board_eligible")
    public boolean isFullBoardEligible;

    @SerializedName("upgrades")
    public List<PackageUpgradeDto> upgrades = new ArrayList<>();

    public static class PackageInclusions implements Serializable {
        @SerializedName("full_board_meals")
        public boolean fullBoardMeals;

        @SerializedName("speed_train")
        public boolean speedTrain;

        @SerializedName("company_luggage")
        public boolean companyLuggage;

        @SerializedName("company_luggage_count")
        public int companyLuggageCount;
    }

    public static class OriginalHotels implements Serializable {
        @SerializedName("makkah")
        public HotelSummary makkah;

        @SerializedName("madinah")
        public HotelSummary madinah;
    }

    public static class HotelSummary implements Serializable {
        @SerializedName("name")
        public String name;

        @SerializedName("rating")
        public String rating;

        @SerializedName("distance")
        public String distance;

        @SerializedName("distance_tier")
        public String distanceTier;
    }
}
