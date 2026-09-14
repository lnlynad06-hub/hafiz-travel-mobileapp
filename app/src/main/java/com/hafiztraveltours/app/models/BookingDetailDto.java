package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BookingDetailDto extends BookingDto {
    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount_amount")
    public double discountAmount;

    @SerializedName("internal_remarks")
    public String internalRemarks;

    @SerializedName("departure")
    public DepartureInfo departure;

    @SerializedName("travellers")
    public List<TravellerInfo> travellers;

    @SerializedName("items")
    public List<ItemInfo> items;

    @SerializedName("invoices")
    public List<InvoiceInfo> invoices;

    public static class DepartureInfo {
        @SerializedName("departure_no")
        public String departureNo;

        @SerializedName("departure_date")
        public String departureDate;

        @SerializedName("return_date")
        public String returnDate;
    }

    public static class TravellerInfo {
        @SerializedName("name")
        public String name;

        @SerializedName("identification_no")
        public String identificationNo;

        @SerializedName("passport_no")
        public String passportNo;

        @SerializedName("role")
        public String role;
    }

    public static class ItemInfo {
        @SerializedName("description")
        public String description;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName("unit_price")
        public double unitPrice;

        @SerializedName("total")
        public double total;
    }

    public static class InvoiceInfo {
        @SerializedName("invoice_no")
        public String invoiceNo;

        @SerializedName("total_amount")
        public double totalAmount;

        @SerializedName("status")
        public String status;
    }
}
