package com.hafiztraveltours.app.models;

import java.io.Serializable;
import java.text.DecimalFormat;
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
    public String promoCode = "";
    public double discountAmount = 0.0;
    public PackageDetail packageDetail;

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
        if (priceStr == null) return 0.0;
        String clean = priceStr.replaceAll("[^0-9.]", "");
        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static String formatPrice(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return "RM " + formatter.format(amount);
    }

    public void recalculateTotal() {
        double subtotal = (this.unitPriceAmount * this.adultPaxCount);
        this.totalAmount = Math.max(0, subtotal - this.discountAmount);
        this.totalAmountFormatted = formatPrice(this.totalAmount);
    }
}
