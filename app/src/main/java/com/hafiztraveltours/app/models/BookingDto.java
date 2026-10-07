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

    @SerializedName("deposit_due_date")
    public String depositDueDate;

    @SerializedName("is_deposit_paid")
    public boolean isDepositPaid;

    @SerializedName("is_merchandise_eligible")
    public boolean isMerchandiseEligible;

    @SerializedName("is_tour")
    public boolean isTour;

    @SerializedName("receipts_count")
    public int receiptsCount;

    @SerializedName("has_receipts")
    public boolean hasReceipts;

    @SerializedName("is_cancellable")
    public Boolean isCancellable;

    @SerializedName("cancellation_reason")
    public String cancellationReason;

    @SerializedName("cancelled_at")
    public String cancelledAt;

    @SerializedName("created_at")
    public String createdAt;

    public boolean isTourPackage() {
        if (isTour) return true;
        if (packageCategory != null) {
            String cat = packageCategory.toLowerCase(java.util.Locale.ROOT);
            if (cat.contains("tour") || cat.contains("pelancongan") || cat.contains("inbound") || cat.contains("outbound")) return true;
        }
        if (packageName != null) {
            String name = packageName.toLowerCase(java.util.Locale.ROOT);
            if (name.contains("tour") || name.contains("pelancongan") || name.contains("vietnam")
                    || name.contains("balkan") || name.contains("turki") || name.contains("turkey")
                    || name.contains("japan") || name.contains("korea") || name.contains("switzerland")
                    || name.contains("europe") || name.contains("china")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Required deposit (Phase 20): the ERP value wins whenever present.
     * The local 500/1000-per-pax formula is only a fallback for API responses
     * that predate the server field — never the source of truth.
     */
    public double getRequiredDepositAmount() {
        if (requiredDeposit > 0) return requiredDeposit;
        int pax = totalPax > 0 ? totalPax : 1;
        double ratePerPax = isTourPackage() ? 500.0 : 1000.0;
        return ratePerPax * pax;
    }

    public double getDepositRemainingAmount() {
        double req = getRequiredDepositAmount();
        double paid = paidAmount;
        return Math.max(0, req - paid);
    }
}
