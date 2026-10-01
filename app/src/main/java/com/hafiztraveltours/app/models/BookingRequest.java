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
    public String roomPriceFormatted;
    public double unitPriceAmount;
    public int adultPaxCount = 1;
    public double totalAmount;
    public String totalAmountFormatted;

    public String selectedDepartureDate = "";
    /** Raw backend departure ID (`departures[].id`) for the selected departure; sent as `departure_id`. */
    public String selectedDepartureId = "";
    public Integer selectedPricingId = null;
    public String promoCode = "";
    public double discountAmount = 0.0;
    public String packageCategory = "umrah";
    public PackageDetail packageDetail;

    public boolean termsAgreed = false;
    public String termsAgreedAt = "";
    public String termsVersion = "1.0";

    public boolean isUmrahPackage() {
        if (packageCategory != null && !packageCategory.isEmpty()) {
            return "umrah".equalsIgnoreCase(packageCategory);
        }
        if (packageName != null) {
            String lower = packageName.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("tour") || lower.contains("pelancongan") || lower.contains("switzerland") 
                    || lower.contains("swiss") || lower.contains("turkey") || lower.contains("turki") 
                    || lower.contains("japan") || lower.contains("korea") || lower.contains("balkan") 
                    || lower.contains("vietnam") || lower.contains("china")) {
                return false;
            }
        }
        return true;
    }

    public boolean isTourPackage() {
        return !isUmrahPackage();
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
        public String clothesSize = "";
        public Integer mahramIndex = null;
        public String relationship = "";
        public String icPassportNumber = "";
        public String phoneNumber = "";
        public String email = "";
        public String address = "";
        public String passportDocumentPath = "";
        public String icDocumentPath = "";
        public boolean isComplete = false;
    }

    public static double parsePriceAmount(String priceStr) {
        return com.hafiztraveltours.app.utils.MoneyFormat.parseAmount(priceStr);
    }

    public static String formatPrice(double amount) {
        return com.hafiztraveltours.app.utils.MoneyFormat.formatRM(amount);
    }

    public void recalculateTotal() {
        double subtotal = (this.unitPriceAmount * this.adultPaxCount);
        this.totalAmount = Math.max(0, subtotal - this.discountAmount);
        this.totalAmountFormatted = formatPrice(this.totalAmount);
    }
}
