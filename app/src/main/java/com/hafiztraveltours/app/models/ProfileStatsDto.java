package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ProfileStatsDto {
    @SerializedName("user")
    public UserInfo user;

    @SerializedName("customer")
    public CustomerInfo customer;

    @SerializedName("stats")
    public StatsInfo stats;

    @SerializedName("loyalty")
    public LoyaltyInfo loyalty;

    public static class UserInfo {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("email")
        public String email;

        @SerializedName("phone")
        public String phone;

        @SerializedName("avatar")
        public String avatar;
    }

    public static class CustomerInfo {
        @SerializedName("customer_no")
        public String customerNo;

        @SerializedName("level")
        public String level;

        @SerializedName("member_since")
        public String memberSince;
    }

    public static class StatsInfo {
        @SerializedName("bookings_count")
        public int bookingsCount;

        @SerializedName("total_spent")
        public double totalSpent;

        @SerializedName("paid")
        public double paid;

        @SerializedName("outstanding")
        public double outstanding;

        @SerializedName("upcoming")
        public List<UpcomingBooking> upcoming;
    }

    public static class UpcomingBooking {
        @SerializedName("booking_no")
        public String bookingNo;

        @SerializedName("status")
        public String status;

        @SerializedName("package_name")
        public String packageName;

        @SerializedName("departure_date")
        public String departureDate;
    }

    public static class LoyaltyInfo {
        @SerializedName("points_balance")
        public int pointsBalance;

        @SerializedName("lifetime_points")
        public int lifetimePoints;

        @SerializedName("lifetime_spend")
        public double lifetimeSpend;

        @SerializedName("tier")
        public TierInfo tier;

        @SerializedName("next_tier")
        public TierInfo nextTier;

        @SerializedName("spend_to_next_tier")
        public double spendToNextTier;

        @SerializedName("recent_transactions")
        public List<LoyaltyTx> recentTransactions;
    }

    public static class TierInfo {
        @SerializedName("code")
        public String code;

        @SerializedName("name")
        public String name;

        @SerializedName("multiplier")
        public double multiplier;

        @SerializedName("discount_rate")
        public double discountRate;

        @SerializedName("benefits")
        public String benefits;

        @SerializedName("min_spend")
        public double minSpend;
    }

    public static class LoyaltyTx {
        @SerializedName("type")
        public String type;

        @SerializedName("points")
        public int points;

        @SerializedName("description")
        public String description;

        @SerializedName("created_at")
        public String createdAt;
    }
}
