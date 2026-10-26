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

    @SerializedName("capacity_tier")
    public String capacityTier;

    @SerializedName("capacity_tier_label")
    public String capacityTierLabel;

    @SerializedName("is_guaranteed")
    public boolean isGuaranteed;

    @SerializedName("is_soft_hold")
    public boolean isSoftHold;

    @SerializedName("is_cancellable")
    public Boolean isCancellable;

    @SerializedName("cancellation_reason")
    public String cancellationReason;

    @SerializedName("cancelled_at")
    public String cancelledAt;

    @SerializedName("cancellation_status")
    public String cancellationStatus;

    @SerializedName("cancellation_submitted_at")
    public String cancellationSubmittedAt;

    @SerializedName("cancellation_review_reason")
    public String cancellationReviewReason;

    @SerializedName("refund_status")
    public String refundStatus;

    @SerializedName("refund_requested_amount")
    public Double refundRequestedAmount;

    @SerializedName("refund_approved_amount")
    public Double refundApprovedAmount;

    @SerializedName("room_assignment_status")
    public String roomAssignmentStatus;

    @SerializedName("room_assignment_label")
    public String roomAssignmentLabel;

    @SerializedName("created_at")
    public String createdAt;

    public boolean isRoomAssignmentConfirmed() {
        return "confirmed".equalsIgnoreCase(roomAssignmentStatus);
    }

    public String getRoomAssignmentDisplayLabel() {
        if (roomAssignmentLabel != null && !roomAssignmentLabel.trim().isEmpty()) {
            return roomAssignmentLabel;
        }
        return isRoomAssignmentConfirmed() ? "Room Assignment: Confirmed" : "Room Assignment: Pending Verification";
    }

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

    public boolean isPendingCancellation() {
        String s = status != null ? status.toLowerCase(java.util.Locale.ROOT) : "";
        String cs = cancellationStatus != null ? cancellationStatus.toLowerCase(java.util.Locale.ROOT) : "";
        return s.contains("cancel_req") || s.equals("cancellation_requested") || cs.equals("requested");
    }

    public boolean isCancelled() {
        if (isPendingCancellation()) return false;
        String s = status != null ? status.toLowerCase(java.util.Locale.ROOT) : "";
        String cs = cancellationStatus != null ? cancellationStatus.toLowerCase(java.util.Locale.ROOT) : "";
        return s.equals("cancelled") || s.equals("batal") || cs.equals("approved");
    }

    public boolean isRefundPendingOrProcessing() {
        String rs = refundStatus != null ? refundStatus.toLowerCase(java.util.Locale.ROOT) : "";
        return rs.equals("requested") || rs.equals("approved") || rs.equals("processing");
    }

    public boolean isRefundCompleted() {
        String rs = refundStatus != null ? refundStatus.toLowerCase(java.util.Locale.ROOT) : "";
        return rs.equals("processed") || rs.equals("refunded");
    }

    public boolean isCancellationRejected() {
        String cs = cancellationStatus != null ? cancellationStatus.toLowerCase(java.util.Locale.ROOT) : "";
        return cs.equals("rejected");
    }

    /** The server owns booking access; this decides whether its listed booking can show payment. */
    public boolean canMakePayment() {
        if (isPendingCancellation() || isCancelled()) {
            return false;
        }
        String bookingStatus = status != null ? status.toLowerCase(java.util.Locale.ROOT) : "";
        String payment = paymentStatus != null ? paymentStatus.toLowerCase(java.util.Locale.ROOT) : "";
        boolean unavailable = bookingStatus.contains("cancel") || bookingStatus.contains("batal")
                || bookingStatus.contains("delet") || bookingStatus.contains("hapus")
                || bookingStatus.contains("complet") || bookingStatus.contains("selesai")
                || payment.contains("cancel") || payment.contains("pending_gateway");
        return !unavailable && getOutstandingAmount() > 0;
    }

    public double getOutstandingAmount() {
        return balanceAmount > 0 ? balanceAmount : Math.max(0, totalAmount - paidAmount);
    }
}
