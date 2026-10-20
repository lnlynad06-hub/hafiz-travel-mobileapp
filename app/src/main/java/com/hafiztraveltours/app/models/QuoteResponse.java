package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuoteResponse implements Serializable {
    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount")
    public double discount;

    @SerializedName("total")
    public double total;

    @SerializedName("required_deposit")
    public double requiredDeposit;

    @SerializedName("base_adult_price")
    public double baseAdultPrice;

    @SerializedName("season")
    public String season;

    @SerializedName("rooms")
    public List<QuoteRoomItem> rooms = new ArrayList<>();

    @SerializedName("items")
    public List<QuoteLineItem> items = new ArrayList<>();

    public static class QuoteRoomItem implements Serializable {
        @SerializedName("room_number")
        public int roomNumber;

        @SerializedName("room_type")
        public String roomType;

        @SerializedName("room_category")
        public String roomCategory;

        @SerializedName("capacity")
        public int capacity;

        @SerializedName("occupancy")
        public int occupancy;

        @SerializedName("available_slots")
        public int availableSlots;

        @SerializedName("notes")
        public String notes;
    }

    public static class QuoteLineItem implements Serializable {
        @SerializedName("description")
        public String description;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName("unit_price")
        public double unitPrice;

        @SerializedName("total")
        public double total;

        @SerializedName("category")
        public String category;

        @SerializedName("raw_rule")
        public String rawRule;
    }
}
