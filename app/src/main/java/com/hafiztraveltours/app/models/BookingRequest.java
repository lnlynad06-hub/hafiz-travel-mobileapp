package com.hafiztraveltours.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BookingRequest implements Serializable {
    public String packageId;
    public String packageName;
    public String packageImageUrl;
    public String packageDuration;
    public String roomLabel;
    public String roomType;
    public String roomPriceFormatted;
    public double unitPriceAmount;
    public int adultPaxCount = 1;
    public int adultMaleCount = 1;
    public int adultFemaleCount = 0;
    public int kidsPaxCount = 0;
    public double totalAmount;
    public String totalAmountFormatted;

    public static class ChildConfig implements Serializable {
        public int childIndex;
        public String dateOfBirth = "";
        public Boolean withBed = null; // null if unselected or not applicable
        public String gender = "Male"; // "Male" or "Female"
    }

    public List<ChildConfig> childConfigs = new ArrayList<>();
    public boolean isFamilyRoomEnabled = false;

    public static class RoomConfig implements Serializable {
        public int roomNumber = 1;
        public String roomType = "double";
        public String roomCategory = "double";
        public String gender = "male"; // "male", "female", or "family"
        public int capacity = 2;
        public List<Integer> assignedTravellerIndices = new ArrayList<>();
        public String notes = "";

        public RoomConfig() {}

        public RoomConfig(int roomNumber, String roomType, int capacity) {
            this(roomNumber, roomType, "male", capacity);
        }

        public RoomConfig(int roomNumber, String roomType, String gender, int capacity) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.gender = gender != null ? gender.toLowerCase().trim() : "male";
            this.roomCategory = roomType;
            this.capacity = capacity;
        }

        public boolean isMaleRoom() {
            return "male".equalsIgnoreCase(gender);
        }

        public boolean isFemaleRoom() {
            return "female".equalsIgnoreCase(gender);
        }

        public boolean isFamilyRoom() {
            return "family".equalsIgnoreCase(gender);
        }

        public int getOccupancy() {
            return assignedTravellerIndices != null ? assignedTravellerIndices.size() : 0;
        }

        public boolean isOverCapacity() {
            return getOccupancy() > capacity;
        }

        public int getAvailableSlots() {
            return Math.max(0, capacity - getOccupancy());
        }
    }

    public List<RoomConfig> rooms = new ArrayList<>();

    public String selectedDepartureDate = "";
    /** Raw backend departure ID (`departures[].id`) for the selected departure; sent as `departure_id`. */
    public String selectedDepartureId = "";
    public Integer selectedPricingId = null;
    public String promoCode = "";
    public double discountAmount = 0.0;
    public String packageCategory = "umrah";
    public String season = "standard";
    public PackageDetail packageDetail;

    public boolean termsAgreed = false;
    public String termsAgreedAt = "";
    public String termsVersion = "1.0";

    public boolean isUmrahPackage() {
        if (packageDetail != null) {
            return packageDetail.isUmrah;
        }
        if (packageCategory != null && !packageCategory.isEmpty()) {
            return "umrah".equalsIgnoreCase(packageCategory);
        }
        return true;
    }

    public boolean isTourPackage() {
        return !isUmrahPackage();
    }

    public String getBaseRoomType() {
        if (roomType == null || roomType.trim().isEmpty()) {
            return isUmrahPackage() ? "quint" : "double";
        }
        String rt = roomType.toLowerCase().trim();
        if (rt.contains("quint") || rt.contains("family_5")) return "quint";
        if (rt.contains("quad") || rt.contains("family_4")) return "quad";
        if (rt.contains("triple")) return "triple";
        if (rt.contains("double")) return "double";
        if (rt.contains("single")) return "single";
        return rt;
    }

    public int getBaseRoomCapacity() {
        String base = getBaseRoomType();
        switch (base) {
            case "quint": return 5;
            case "quad": return 4;
            case "triple": return 3;
            case "double": return 2;
            case "single": return 1;
            default: return 2;
        }
    }

    public List<Passenger> passengers = new ArrayList<>();

    public static class Passenger implements Serializable {
        public boolean isLead;
        public String title = "Mr";
        public String fullName = "";
        public String icNumber = "";
        public String passportNumber = "";
        public String passportExpiryDate = "";
        public String issuingCountry = "Malaysia";
        public String gender = "";
        public String dateOfBirth = "";
        public String nationality = "Malaysian";
        public Integer mahramIndex = null;
        public String relationship = "";
        public String icPassportNumber = "";
        public String phoneNumber = "";
        public String email = "";
        public String address = "";
        public String passportDocumentPath = "";
        public String icDocumentPath = "";
        public Boolean withBed = true;
        public Integer roomIndex = null;
        public double passengerCalculatedPrice = 0.0;
        public boolean isComplete = false;
    }

    public static double parsePriceAmount(String priceStr) {
        return com.hafiztraveltours.app.utils.MoneyFormat.parseAmount(priceStr);
    }

    public static String formatPrice(double amount) {
        return com.hafiztraveltours.app.utils.MoneyFormat.formatRM(amount);
    }

    public void recalculateTotal() {
        double subtotal = (this.unitPriceAmount * (this.adultPaxCount + this.kidsPaxCount));
        this.totalAmount = Math.max(0, subtotal - this.discountAmount);
        this.totalAmountFormatted = formatPrice(this.totalAmount);
    }
}
