package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuoteRequest implements Serializable {
    @SerializedName("package_id")
    public String packageId;

    @SerializedName("departure_id")
    public Integer departureId;

    @SerializedName("pricing_id")
    public Integer pricingId;

    @SerializedName("season")
    public String season;

    @SerializedName("promo_code")
    public String promoCode;

    @SerializedName("room_type")
    public String roomType;

    @SerializedName("rooms")
    public List<RoomQuoteItem> rooms = new ArrayList<>();

    @SerializedName("travellers")
    public List<TravellerQuoteItem> travellers = new ArrayList<>();

    @SerializedName("upgrades")
    public List<SelectedUpgradeItem> upgrades = new ArrayList<>();

    public static class SelectedUpgradeItem implements Serializable {
        @SerializedName("upgrade_id")
        public int upgradeId;

        @SerializedName("quantity")
        public Integer quantity = 1;

        @SerializedName("traveller_index")
        public Integer travellerIndex;

        @SerializedName("traveller_id")
        public Integer travellerId;

        public SelectedUpgradeItem() {}

        public SelectedUpgradeItem(int upgradeId, int quantity, Integer travellerIndex) {
            this.upgradeId = upgradeId;
            this.quantity = quantity;
            this.travellerIndex = travellerIndex;
        }
    }

    public static class RoomQuoteItem implements Serializable {
        @SerializedName("room_number")
        public int roomNumber;

        @SerializedName("room_type")
        public String roomType;

        @SerializedName("room_category")
        public String roomCategory;

        @SerializedName("gender")
        public String gender;

        @SerializedName("traveller_indices")
        public List<Integer> travellerIndices = new ArrayList<>();

        @SerializedName("notes")
        public String notes;

        public RoomQuoteItem() {}

        public RoomQuoteItem(int roomNumber, String roomType, List<Integer> travellerIndices) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.roomCategory = roomType;
            this.travellerIndices = travellerIndices;
        }
    }

    public static class TravellerQuoteItem implements Serializable {
        @SerializedName("index")
        public Integer index;

        @SerializedName("is_lead")
        public Boolean isLead;

        @SerializedName("full_name")
        public String fullName;

        @SerializedName("gender")
        public String gender;

        @SerializedName("date_of_birth")
        public String dateOfBirth;

        @SerializedName("with_bed")
        public Boolean withBed;

        @SerializedName("room_index")
        public Integer roomIndex;

        public TravellerQuoteItem() {}
    }
}
