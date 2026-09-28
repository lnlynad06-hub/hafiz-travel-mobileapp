package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BookingDetailDto extends BookingDto {
    @SerializedName("subtotal")
    public double subtotal;

    @SerializedName("discount_amount")
    public double discountAmount;

    @SerializedName("internal_remarks")
    public String internalRemarks;

    @SerializedName("departure")
    public DepartureInfo departure;

    @SerializedName("travellers")
    public List<TravellerInfo> travellers;

    @SerializedName("items")
    public List<ItemInfo> items;

    @SerializedName("invoices")
    public List<InvoiceInfo> invoices;

    @SerializedName("receipts")
    public List<ReceiptInfo> receipts;

    @SerializedName("payments")
    public List<PaymentInfo> payments;

    public static class DepartureInfo {
        @SerializedName("departure_no")
        public String departureNo;

        @SerializedName("departure_date")
        public String departureDate;

        @SerializedName("return_date")
        public String returnDate;
    }

    public static class TravellerInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("name")
        public String name;

        @SerializedName("identification_no")
        public String identificationNo;

        @SerializedName("passport_no")
        public String passportNo;

        @SerializedName("role")
        public String role;
    }

    public static class ItemInfo {
        @SerializedName("description")
        public String description;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName("unit_price")
        public double unitPrice;

        @SerializedName("total")
        public double total;
    }

    public static class InvoiceInfo {
        @SerializedName("invoice_no")
        public String invoiceNo;

        @SerializedName("total_amount")
        public double totalAmount;

        @SerializedName("status")
        public String status;
    }

    public static class ReceiptInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("receipt_no")
        public String receiptNo;

        @SerializedName("amount")
        public double amount;

        @SerializedName("receipt_date")
        public String receiptDate;

        @SerializedName("payment_method")
        public String paymentMethod;
    }

    public static class PaymentInfo {
        @SerializedName("id")
        public int id;

        @SerializedName("payment_no")
        public String paymentNo;

        @SerializedName("installment_index")
        public int installmentIndex;

        @SerializedName("amount")
        public double amount;

        @SerializedName("method")
        public String method;

        @SerializedName("status")
        public String status;

        @SerializedName("is_verified")
        public boolean isVerified;

        @SerializedName("stage")
        public String stage;

        @SerializedName("stage_label")
        public String stageLabel;

        @SerializedName("receipt_id")
        public Integer receiptId;

        @SerializedName("receipt_no")
        public String receiptNo;

        @SerializedName("receipt_date")
        public String receiptDate;

        @SerializedName("paid_at")
        public String paidAt;
    }
}
