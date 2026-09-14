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

    @SerializedName("travellers")
    public List<TravellerRequest> travellers = new ArrayList<>();

    public static class TravellerRequest {
        @SerializedName("full_name")
        public String fullName;

        @SerializedName("ic_passport")
        public String icPassport;

        @SerializedName("phone")
        public String phone;

        @SerializedName("email")
        public String email;

        @SerializedName("is_lead")
        public boolean isLead;

        public TravellerRequest(String fullName, String icPassport, String phone, String email, boolean isLead) {
            this.fullName = fullName;
            this.icPassport = icPassport;
            this.phone = phone;
            this.email = email;
            this.isLead = isLead;
        }
    }
}
