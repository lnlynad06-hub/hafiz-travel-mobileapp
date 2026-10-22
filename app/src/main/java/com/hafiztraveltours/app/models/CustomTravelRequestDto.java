package com.hafiztraveltours.app.models;

import android.content.Context;
import com.google.gson.annotations.SerializedName;
import com.hafiztraveltours.app.R;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomTravelRequestDto implements Serializable {
    @SerializedName("id")
    public int id;

    @SerializedName("customer_id")
    public int customerId;

    @SerializedName("package_id")
    public Integer packageId;

    @SerializedName("request_no")
    public String requestNo;

    @SerializedName("request_type")
    public String requestType; // "customize_package" or "custom_trip"

    @SerializedName("travel_type")
    public String travelType; // "umrah" or "tour"

    @SerializedName("destination")
    public String destination;

    @SerializedName("departure_airport")
    public String departureAirport;

    @SerializedName("preferred_departure_date")
    public String preferredDepartureDate;

    @SerializedName("preferred_return_date")
    public String preferredReturnDate;

    @SerializedName("is_flexible_dates")
    public boolean isFlexibleDates;

    @SerializedName("duration_days")
    public Integer durationDays;

    @SerializedName("adult_male_count")
    public int adultMaleCount;

    @SerializedName("adult_female_count")
    public int adultFemaleCount;

    @SerializedName("children_count")
    public int childrenCount;

    @SerializedName("children_details")
    public List<CreateCustomTravelRequest.ChildDetail> childrenDetails = new ArrayList<>();

    @SerializedName("room_category")
    public String roomCategory;

    @SerializedName("is_family_room")
    public boolean isFamilyRoom;

    @SerializedName("room_arrangements")
    public List<CreateCustomTravelRequest.RoomArrangement> roomArrangements = new ArrayList<>();

    @SerializedName("hotel_category")
    public String hotelCategory;

    @SerializedName("hotel_preferences")
    public String hotelPreferences;

    @SerializedName("flight_preferences")
    public CreateCustomTravelRequest.FlightPreferences flightPreferences;

    @SerializedName("transportation_preferences")
    public String transportationPreferences;

    @SerializedName("itinerary_preferences")
    public String itineraryPreferences;

    @SerializedName("additional_services")
    public List<String> additionalServices = new ArrayList<>();

    @SerializedName("budget_per_pax")
    public Double budgetPerPax;

    @SerializedName("special_requirements")
    public String specialRequirements;

    @SerializedName("status")
    public String status; // "draft", "submitted", "under_review", "quoted", "revision_requested", "cancelled", "closed"

    @SerializedName("package_snapshot")
    public Map<String, Object> packageSnapshot;

    @SerializedName("package")
    public PackageMiniDto packageInfo;

    @SerializedName("latest_quotation")
    public QuotationDto latestQuotation;

    @SerializedName("quotations")
    public List<QuotationDto> quotations = new ArrayList<>();

    @SerializedName("reviewed_at")
    public String reviewedAt;

    @SerializedName("created_at")
    public String createdAt;

    public static class PackageMiniDto implements Serializable {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("slug")
        public String slug;

        @SerializedName("category")
        public String category;

        @SerializedName("destination")
        public String destination;

        @SerializedName("duration_days")
        public Integer durationDays;
    }

    public int getTotalPax() {
        return adultMaleCount + adultFemaleCount + childrenCount;
    }

    public boolean isEditable() {
        return ("submitted".equalsIgnoreCase(status) || "draft".equalsIgnoreCase(status))
                && reviewedAt == null;
    }

    public boolean isCancellable() {
        return !"cancelled".equalsIgnoreCase(status) && !"closed".equalsIgnoreCase(status);
    }

    public boolean hasCustomerVisibleQuotation() {
        return latestQuotation != null && latestQuotation.isCustomerVisible();
    }

    public String getDisplayTitle() {
        if (packageInfo != null && packageInfo.name != null && !packageInfo.name.isEmpty()) {
            return packageInfo.name;
        }
        if (packageSnapshot != null && packageSnapshot.get("name") != null) {
            return String.valueOf(packageSnapshot.get("name"));
        }
        return destination != null ? destination : "Custom Trip";
    }

    public String getStatusLocalized(Context context) {
        if (status == null) return "-";
        switch (status.toLowerCase()) {
            case "draft":
                return context.getString(R.string.custom_req_status_draft);
            case "submitted":
                return context.getString(R.string.custom_req_status_submitted);
            case "under_review":
                return context.getString(R.string.custom_req_status_under_review);
            case "quoted":
                return context.getString(R.string.custom_req_status_quoted);
            case "revision_requested":
                return context.getString(R.string.custom_req_status_revision_requested);
            case "cancelled":
                return context.getString(R.string.custom_req_status_cancelled);
            case "closed":
                return context.getString(R.string.custom_req_status_closed);
            default:
                return status;
        }
    }
}
