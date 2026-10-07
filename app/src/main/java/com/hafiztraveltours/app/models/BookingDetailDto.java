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

    @SerializedName("cancellation_quote")
    public CancellationQuoteDto cancellationQuote;

    @SerializedName("payments")
    public List<PaymentInfo> payments;

    @SerializedName("company")
    public CompanyDto company;

    @SerializedName("customer_info")
    public CustomerInfoDto customerInfo;

    @SerializedName("invoice_meta")
    public InvoiceMetaDto invoiceMeta;

    @SerializedName("receipt_meta")
    public ReceiptMetaDto receiptMeta;

    @SerializedName("amount_in_words")
    public String amountInWords;

    @SerializedName("amount_in_words_ms")
    public String amountInWordsMs;

    @SerializedName("room_setup")
    public String roomSetup;

    @SerializedName("bank_details")
    public BankDetailsDto bankDetails;

    @SerializedName("terms_and_conditions")
    public TermsAndConditionsDto termsAndConditions;

    @SerializedName("payment_records")
    public List<PaymentRecordDto> paymentRecords;

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
        @SerializedName("id")
        public int id;

        @SerializedName("invoice_no")
        public String invoiceNo;

        @SerializedName("issued_at")
        public String issuedAt;

        @SerializedName("due_at")
        public String dueAt;

        @SerializedName("subtotal")
        public double subtotal;

        @SerializedName("discount_amount")
        public double discountAmount;

        @SerializedName("tax_amount")
        public double taxAmount;

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

        @SerializedName("payment_title")
        public String paymentTitle;

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

        @SerializedName("verified_at")
        public String verifiedAt;
    }

    public static class CompanyDto {
        @SerializedName("company_name")
        public String companyName;

        @SerializedName("website")
        public String website;

        @SerializedName("registration_number")
        public String registrationNumber;

        @SerializedName("motac_license_no")
        public String motacLicenseNo;

        @SerializedName("registration_licence")
        public String registrationLicence;

        @SerializedName("address")
        public String address;

        @SerializedName("office_phone")
        public String officePhone;

        @SerializedName("whatsapp_mobile")
        public String whatsappMobile;

        @SerializedName("official_email")
        public String officialEmail;
    }

    public static class CustomerInfoDto {
        @SerializedName("full_name")
        public String fullName;

        @SerializedName("address")
        public String address;

        @SerializedName("mobile_number")
        public String mobileNumber;

        @SerializedName("email")
        public String email;
    }

    public static class InvoiceMetaDto {
        @SerializedName("document_type")
        public String documentType;

        @SerializedName("invoice_no")
        public String invoiceNo;

        @SerializedName("invoice_date")
        public String invoiceDate;

        @SerializedName("sales_person")
        public String salesPerson;

        @SerializedName("departure_date")
        public String departureDate;

        @SerializedName("tour_code")
        public String tourCode;

        @SerializedName("payment_due_date")
        public String paymentDueDate;

        @SerializedName("full_package_title")
        public String fullPackageTitle;
    }

    public static class ReceiptMetaDto {
        @SerializedName("document_type")
        public String documentType;

        @SerializedName("receipt_no")
        public String receiptNo;

        @SerializedName("receipt_date")
        public String receiptDate;

        @SerializedName("received_from")
        public String receivedFrom;

        @SerializedName("payment_method")
        public String paymentMethod;

        @SerializedName("transaction_reference")
        public String transactionReference;

        @SerializedName("amount_received")
        public double amountReceived;
    }

    public static class BankDetailsDto {
        @SerializedName("bank_name")
        public String bankName;

        @SerializedName("account_number")
        public String accountNumber;

        @SerializedName("account_holder")
        public String accountHolder;

        @SerializedName("swift_code")
        public String swiftCode;

        @SerializedName("deposit_policy")
        public String depositPolicy;

        @SerializedName("balance_deadline")
        public String balanceDeadline;

        @SerializedName("refund_policy")
        public String refundPolicy;
    }

    public static class TermsAndConditionsDto {
        @SerializedName("itinerary")
        public String itinerary;

        @SerializedName("travel_documents")
        public String travelDocuments;

        @SerializedName("cancellation")
        public String cancellation;
    }

    public static class PaymentRecordDto {
        @SerializedName("no")
        public int no;

        @SerializedName("date_paid")
        public String datePaid;

        @SerializedName("ref_no")
        public String refNo;

        @SerializedName("payment_type")
        public String paymentType;

        @SerializedName("received_from")
        public String receivedFrom;

        @SerializedName("or_no")
        public String orNo;

        @SerializedName("authorization")
        public String authorization;

        @SerializedName("amount")
        public double amount;
    }
}
