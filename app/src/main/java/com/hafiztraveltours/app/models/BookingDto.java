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

    @SerializedName("package_type")
    public String packageType;

    @SerializedName("duration_days")
    public int durationDays;

    @SerializedName("total_pax")
    public int totalPax;

    @SerializedName("total_amount")
    public double totalAmount;

    @SerializedName("paid_amount")
    public double paidAmount;

    @SerializedName("balance_amount")
    public double balanceAmount;

    @SerializedName("required_deposit")
    public double requiredDeposit;

    @SerializedName("deposit_paid")
    public double depositPaid;

    @SerializedName("deposit_remaining")
    public double depositRemaining;

    @SerializedName("balance_total")
    public double balanceTotal;

    @SerializedName("balance_paid")
    public double balancePaid;

    @SerializedName("balance_remaining")
    public double balanceRemaining;

    @SerializedName("payment_status")
    public String paymentStatus;

    @SerializedName("receipts_count")
    public int receiptsCount;

    @SerializedName("has_receipts")
    public boolean hasReceipts;

    @SerializedName("created_at")
    public String createdAt;
}
