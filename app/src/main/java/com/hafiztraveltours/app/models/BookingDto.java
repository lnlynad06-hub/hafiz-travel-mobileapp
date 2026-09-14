package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

public class BookingDto {
    @SerializedName("id")
    public int id;

    @SerializedName("booking_no")
    public String bookingNo;

    @SerializedName("status")
    public String status;

    @SerializedName("package_name")
    public String packageName;

    @SerializedName("package_category")
    public String packageCategory;

    @SerializedName("departure_date")
    public String departureDate;

    @SerializedName("total_amount")
    public double totalAmount;

    @SerializedName("paid_amount")
    public double paidAmount;

    @SerializedName("balance_amount")
    public double balanceAmount;

    @SerializedName("created_at")
    public String createdAt;
}
