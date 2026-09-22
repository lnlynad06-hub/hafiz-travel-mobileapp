package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class CreateBookingRequest {
    @SerializedName("package_id")
    public String packageId;

    @SerializedName("departure_id")
    public Integer departureId;

    @SerializedName("room_label")
    public String roomLabel;

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

    @SerializedName("travellers")
    public List<TravellerRequest> travellers = new ArrayList<>();

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

        @SerializedName("clothes_size")
        public String clothesSize;

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

        @SerializedName("is_lead")
        public boolean isLead;

        public TravellerRequest() {}
    }
}
