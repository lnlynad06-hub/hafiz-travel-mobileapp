package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class QuotationDto {
    @SerializedName("id")
    public int id;

    @SerializedName("quotation_no")
    public String quotationNo;

    @SerializedName("status")
    public String status; // "draft", "prepared", "sent", "accepted", "rejected", "expired", "converted"

    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount_amount")
    public double discountAmount;

    @SerializedName("total_amount")
    public double totalAmount;

    @SerializedName("valid_until")
    public String validUntil;

    @SerializedName("version")
    public int version = 1;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("items")
    public List<QuotationItemDto> items = new ArrayList<>();

    public static class QuotationItemDto {
        @SerializedName("id")
        public int id;

        @SerializedName("item_name")
        public String itemName;

        @SerializedName("description")
        public String description;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName("unit_price")
        public double unitPrice;

        @SerializedName("total_price")
        public double totalPrice;
    }

    public boolean isCustomerVisible() {
        return "sent".equalsIgnoreCase(status)
                || "accepted".equalsIgnoreCase(status)
                || "rejected".equalsIgnoreCase(status)
                || "converted".equalsIgnoreCase(status);
    }
}
