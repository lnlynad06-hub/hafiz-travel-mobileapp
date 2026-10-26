package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreateCustomTravelRequest implements Serializable {
    @SerializedName("request_type")
    public String requestType; // "customize_package" or "custom_trip"

    @SerializedName("travel_type")
    public String travelType; // "umrah" or "tour"

    @SerializedName("package_id")
    public Integer packageId;

    @SerializedName("destination")
    public String destination;

    @SerializedName("departure_airport")
    public String departureAirport;

    @SerializedName("preferred_departure_date")
    public String preferredDepartureDate; // "YYYY-MM-DD"

    @SerializedName("preferred_return_date")
    public String preferredReturnDate; // "YYYY-MM-DD"

    @SerializedName("is_flexible_dates")
    public Boolean isFlexibleDates;

    @SerializedName("duration_days")
    public Integer durationDays;

    @SerializedName("adult_male_count")
    public int adultMaleCount;

    @SerializedName("adult_female_count")
    public int adultFemaleCount;

    @SerializedName("children_count")
    public int childrenCount;

    @SerializedName("children_details")
    public List<ChildDetail> childrenDetails = new ArrayList<>();

    @SerializedName("room_category")
    public String roomCategory; // "quint", "quad", "triple", "double", "single"

    @SerializedName("is_family_room")
    public Boolean isFamilyRoom;

    @SerializedName("room_arrangements")
    public List<RoomArrangement> roomArrangements = new ArrayList<>();

    @SerializedName("hotel_category")
    public String hotelCategory;

    @SerializedName("hotel_preferences")
    public String hotelPreferences;

    @SerializedName("flight_preferences")
    public FlightPreferences flightPreferences;

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
    public String status = "submitted";

    public static class ChildDetail implements Serializable {
        @SerializedName("dob")
        public String dob;

        @SerializedName("age")
        public int age;

        @SerializedName("gender")
        public String gender; // "male" or "female"

        public ChildDetail() {}

        public ChildDetail(String dob, int age, String gender) {
            this.dob = dob;
            this.age = age;
            this.gender = gender;
        }
    }

    public static class RoomArrangement implements Serializable {
        @SerializedName("room_type")
        public String roomType;

        @SerializedName("gender")
        public String gender; // "male", "female", "family"

        @SerializedName("capacity")
        public int capacity;

        @SerializedName("pax_count")
        public int paxCount;

        @SerializedName("description")
        public String description;

        public RoomArrangement() {}

        public RoomArrangement(String roomType, String gender, int capacity, int paxCount, String description) {
            this.roomType = roomType;
            this.gender = gender;
            this.capacity = capacity;
            this.paxCount = paxCount;
            this.description = description;
        }
    }

    public static class FlightPreferences implements Serializable {
        @SerializedName("airline")
        public String airline;

        @SerializedName("flight_class")
        public String flightClass; // "economy", "premium_economy", "business", "first"

        @SerializedName("direct_transit")
        public String directTransit; // "direct", "transit", "any"

        public FlightPreferences() {}

        public FlightPreferences(String airline, String flightClass, String directTransit) {
            this.airline = airline;
            this.flightClass = flightClass;
            this.directTransit = directTransit;
        }
    }
}
