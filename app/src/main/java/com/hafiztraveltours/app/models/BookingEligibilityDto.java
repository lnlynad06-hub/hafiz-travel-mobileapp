package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class BookingEligibilityDto implements Serializable {
    @SerializedName("can_book")
    public boolean canBook;

    @SerializedName("reason")
    public String reason;

    @SerializedName("message")
    public String message;

    @SerializedName("blocking_booking")
    public BlockingBooking blockingBooking;

    public static class BlockingBooking implements Serializable {
        @SerializedName("id")
        public int id;

        @SerializedName("booking_no")
        public String bookingNo;

        @SerializedName("required_deposit")
        public double requiredDeposit;

        @SerializedName("paid_amount")
        public double paidAmount;

        @SerializedName("deposit_remaining")
        public double depositRemaining;
    }
}
