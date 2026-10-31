package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class CreateBookingRequest {
    @SerializedName("package_id")
    public String packageId;

    @SerializedName("departure_id")
    public Integer departureId;

    @SerializedName("pricing_id")
    public Integer pricingId;

    @SerializedName("room_label")
    public String roomLabel;

    @SerializedName("room_type")
    public String roomType;

    @SerializedName("season")
    public String season;

    @SerializedName("adult_count")
    public int adultCount;

    @SerializedName("child_count")
    public int childCount;

    @SerializedName("unit_price")
    public double unitPrice;

    @SerializedName("discount_amount")
    public double discountAmount;

    @SerializedName("promo_code")
    public String promoCode;

    @SerializedName("payment_type")
    public String paymentType;

    @SerializedName("payment_method")
    public String paymentMethod;

    @SerializedName("terms_agreed")
    public Boolean termsAgreed;

    @SerializedName("terms_agreed_at")
    public String termsAgreedAt;

    @SerializedName("terms_version")
    public String termsVersion;

    @SerializedName("rooms")
    public List<RoomRequest> rooms = new ArrayList<>();

    @SerializedName("travellers")
    public List<TravellerRequest> travellers = new ArrayList<>();

    @SerializedName("upgrades")
    public List<SelectedUpgradeItem> upgrades = new ArrayList<>();

    @SerializedName("expected_total")
    public Double expectedTotal;

    public static class SelectedUpgradeItem {
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

    public static class RoomRequest {
        @SerializedName("room_number")
        public int roomNumber;

        @SerializedName("room_type")
        public String roomType;

        @SerializedName("room_category")
        public String roomCategory;

        @SerializedName("gender")
        public String gender;

        @SerializedName("capacity")
        public int capacity;

        @SerializedName("traveller_indices")
        public List<Integer> travellerIndices = new ArrayList<>();

        @SerializedName("notes")
        public String notes;

        public RoomRequest() {}

        public RoomRequest(int roomNumber, String roomType, List<Integer> travellerIndices) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.roomCategory = roomType;
            this.travellerIndices = travellerIndices;
        }
    }

    public static class TravellerRequest {
        @SerializedName("title")
        public String title;

        @SerializedName("full_name")
        public String fullName;

        @SerializedName("ic_number")
        public String icNumber;

        @SerializedName("passport_number")
        public String passportNumber;

        @SerializedName("passport_expiry_date")
        public String passportExpiryDate;

        @SerializedName("issuing_country")
        public String issuingCountry;

        @SerializedName("gender")
        public String gender;

        @SerializedName("date_of_birth")
        public String dateOfBirth;

        @SerializedName("nationality")
        public String nationality;

        @SerializedName("mahram_index")
        public Integer mahramIndex;

        @SerializedName("relationship")
        public String relationship;

        @SerializedName("ic_passport")
        public String icPassport;

        @SerializedName("phone")
        public String phone;

        @SerializedName("email")
        public String email;

        @SerializedName("with_bed")
        public Boolean withBed;

        @SerializedName("is_lead")
        public boolean isLead;

        @SerializedName("room_index")
        public Integer roomIndex;

        public TravellerRequest() {}
    }
}
