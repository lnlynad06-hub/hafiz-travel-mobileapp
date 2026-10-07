package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceViewerActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_BOOKING_NO = "extra_booking_no";
    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private TextView tvCompanyHeaderName;
    private TextView tvCompanyHeaderMeta;
    private TextView tvCompanyHeaderAddress;
    private TextView tvCompanyHeaderContact;
    private TextView tvInvoiceStatusBadge;
    private TextView tvInvoiceNumberHeader;
    private TextView tvInvoiceDate;
    private TextView tvTourCode;
    private TextView tvDepartureDate;
    private TextView tvPaymentDueDate;
    private TextView tvInvoiceCustomerName;
    private TextView tvInvoiceCustomerAddress;
    private TextView tvInvoiceCustomerPhone;
    private TextView tvInvoicePackageName;
    private TextView tvRoomSetup;
    private LinearLayout layoutPassengersContainer;
    private LinearLayout layoutInvoiceItems;
    private TextView tvInvoiceSubtotal;
    private TextView tvInvoiceGrandTotal;
    private TextView tvInvoicePaidAmount;
    private TextView tvInvoiceBalanceDue;
    private TextView tvInvoiceAmountInWords;
    private LinearLayout layoutPaymentRecordsContainer;
    private TextView tvBankDetails;
    private TextView tvPaymentTerms;
    private TextView tvTourRemarks;
    private TextView tvTermsConditions;
    private TextView tvDocumentDisclaimer;
    private TextView tvDocumentPageInfo;
    private LinearLayout invoiceContentContainer;

    private int bookingId = 0;
    private String bookingNo = "";
    private BookingRequest bookingRequest;
    private BookingDetailDto bookingDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_viewer);

        initViews();
        handleIntentData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnInvoiceBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvCompanyHeaderName = findViewById(R.id.tvCompanyHeaderName);
        tvCompanyHeaderMeta = findViewById(R.id.tvCompanyHeaderMeta);
        tvCompanyHeaderAddress = findViewById(R.id.tvCompanyHeaderAddress);
        tvCompanyHeaderContact = findViewById(R.id.tvCompanyHeaderContact);
        tvInvoiceStatusBadge = findViewById(R.id.tvInvoiceStatusBadge);
        tvInvoiceNumberHeader = findViewById(R.id.tvInvoiceNumberHeader);
        tvInvoiceDate = findViewById(R.id.tvInvoiceDate);
        tvTourCode = findViewById(R.id.tvTourCode);
        tvDepartureDate = findViewById(R.id.tvDepartureDate);
        tvPaymentDueDate = findViewById(R.id.tvPaymentDueDate);
        tvInvoiceCustomerName = findViewById(R.id.tvInvoiceCustomerName);
        tvInvoiceCustomerAddress = findViewById(R.id.tvInvoiceCustomerAddress);
        tvInvoiceCustomerPhone = findViewById(R.id.tvInvoiceCustomerPhone);
        tvInvoicePackageName = findViewById(R.id.tvInvoicePackageName);
        tvRoomSetup = findViewById(R.id.tvRoomSetup);
        layoutPassengersContainer = findViewById(R.id.layoutPassengersContainer);
        layoutInvoiceItems = findViewById(R.id.layoutInvoiceItems);
        tvInvoiceSubtotal = findViewById(R.id.tvInvoiceSubtotal);
        tvInvoiceGrandTotal = findViewById(R.id.tvInvoiceGrandTotal);
        tvInvoicePaidAmount = findViewById(R.id.tvInvoicePaidAmount);
        tvInvoiceBalanceDue = findViewById(R.id.tvInvoiceBalanceDue);
        tvInvoiceAmountInWords = findViewById(R.id.tvInvoiceAmountInWords);
        layoutPaymentRecordsContainer = findViewById(R.id.layoutPaymentRecordsContainer);
        tvBankDetails = findViewById(R.id.tvBankDetails);
        tvPaymentTerms = findViewById(R.id.tvPaymentTerms);
        tvTourRemarks = findViewById(R.id.tvTourRemarks);
        tvTermsConditions = findViewById(R.id.tvTermsConditions);
        tvDocumentDisclaimer = findViewById(R.id.tvDocumentDisclaimer);
        tvDocumentPageInfo = findViewById(R.id.tvDocumentPageInfo);
        invoiceContentContainer = findViewById(R.id.invoiceContentContainer);

        Button btnDownload = findViewById(R.id.btnInvoiceDownloadPdf);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                HapticUtil.click(v);
                saveOrPrintPdf();
            });
        }

        Button btnShare = findViewById(R.id.btnInvoiceShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                HapticUtil.click(v);
                shareInvoiceSummary();
            });
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        bookingId = intent.getIntExtra(EXTRA_BOOKING_ID, 0);
        bookingNo = intent.getStringExtra(EXTRA_BOOKING_NO);
        bookingRequest = (BookingRequest) intent.getSerializableExtra(EXTRA_BOOKING_REQUEST);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        if (tvInvoiceDate != null) {
            tvInvoiceDate.setText(sdf.format(new Date()));
        }

        if (bookingRequest != null) {
            populateFromBookingRequest(bookingRequest, bookingNo);
        } else if (bookingId > 0) {
            fetchBookingDetailFromApi(bookingId);
        } else if (bookingNo != null && !bookingNo.isEmpty()) {
            if (tvInvoiceNumberHeader != null) tvInvoiceNumberHeader.setText("HQ " + bookingNo.replace("BKG-", "INV-"));
        }
    }

    private void populateFromBookingRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
        String invNo = "HQ " + displayNo.replace("BKG-", "INV-");

        if (tvInvoiceNumberHeader != null) tvInvoiceNumberHeader.setText(invNo);

        if (tvInvoiceStatusBadge != null) {
            tvInvoiceStatusBadge.setText(getString(R.string.status_deposit_paid));
            tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
            tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
        }

        // Customer
        if (req.passengers != null && !req.passengers.isEmpty()) {
            BookingRequest.Passenger lead = req.passengers.get(0);
            if (tvInvoiceCustomerName != null) {
                String leadRole = "ms".equalsIgnoreCase(Locale.getDefault().getLanguage()) ? "(Ketua Jemaah)" : "(Lead Passenger)";
                tvInvoiceCustomerName.setText(lead.fullName != null && !lead.fullName.isEmpty() ? lead.fullName + " " + leadRole : getString(R.string.default_user_name));
            }
            if (tvInvoiceCustomerAddress != null) {
                tvInvoiceCustomerAddress.setText("Johor Bahru, Johor, Malaysia");
            }
            if (tvInvoiceCustomerPhone != null) {
                String phoneStr = (lead.phoneNumber != null && !lead.phoneNumber.isEmpty()) ? lead.phoneNumber : "—";
                tvInvoiceCustomerPhone.setText(phoneStr);
            }
        }

        // Package & Room
        if (tvInvoicePackageName != null) {
            tvInvoicePackageName.setText(req.packageName != null ? req.packageName : "Pakej Umrah");
        }
        if (tvRoomSetup != null) {
            String room = (req.roomLabel != null && !req.roomLabel.isEmpty()) ? req.roomLabel : "Standard Room";
            tvRoomSetup.setText(room + " — Room Pairing 01");
        }
        if (tvTourCode != null) {
            tvTourCode.setText(formatTourCode(req.selectedDepartureDate != null ? req.selectedDepartureDate : "4827"));
        }
        if (tvDepartureDate != null) {
            tvDepartureDate.setText(req.selectedDepartureDate != null ? formatDatePretty(req.selectedDepartureDate) : "—");
        }

        // Passengers
        if (layoutPassengersContainer != null) {
            layoutPassengersContainer.removeAllViews();
            if (req.passengers != null && !req.passengers.isEmpty()) {
                for (int i = 0; i < req.passengers.size(); i++) {
                    BookingRequest.Passenger p = req.passengers.get(i);
                    addPassengerRow(i + 1, p.fullName, p.icPassportNumber);
                }
            }
        }

        // Line Items
        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            addLineItem(req.packageName + " (" + (req.roomLabel != null ? req.roomLabel : "Standard Room") + ")",
                    String.valueOf(req.adultPaxCount),
                    BookingRequest.formatPrice(req.totalAmount));
        }

        if (tvInvoiceSubtotal != null) tvInvoiceSubtotal.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvInvoiceGrandTotal != null) tvInvoiceGrandTotal.setText(BookingRequest.formatPrice(req.totalAmount));

        double deposit = (req.totalAmount > 0) ? (req.totalAmount >= 2000 ? 1000.0 * req.adultPaxCount : req.totalAmount) : 1000.0 * req.adultPaxCount;
        double balance = Math.max(0, req.totalAmount - deposit);

        if (tvInvoicePaidAmount != null) tvInvoicePaidAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvInvoiceBalanceDue != null) tvInvoiceBalanceDue.setText(BookingRequest.formatPrice(balance));
    }

    private void fetchBookingDetailFromApi(int id) {
        ApiClient.getApiService().getBookingDetail(id).enqueue(new Callback<ApiResponse<BookingDetailDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingDetailDto>> call, Response<ApiResponse<BookingDetailDto>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bookingDetail = response.body().data;
                    populateFromBookingDetail(bookingDetail);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                // Keep fallback data
            }
        });
    }

    private void populateFromBookingDetail(BookingDetailDto detail) {
        if (detail == null) return;

        // 1. Company Header
        if (detail.company != null) {
            if (tvCompanyHeaderName != null && detail.company.companyName != null) {
                tvCompanyHeaderName.setText(detail.company.companyName);
            }
            if (tvCompanyHeaderMeta != null && detail.company.registrationLicence != null) {
                tvCompanyHeaderMeta.setText(detail.company.registrationLicence);
            }
            if (tvCompanyHeaderAddress != null && detail.company.address != null) {
                tvCompanyHeaderAddress.setText(detail.company.address);
            }
            if (tvCompanyHeaderContact != null) {
                String phone = detail.company.officePhone != null ? detail.company.officePhone : "+60 7-238 4567";
                String wa = detail.company.whatsappMobile != null ? detail.company.whatsappMobile : "+60 19-778 4567";
                String email = detail.company.officialEmail != null ? detail.company.officialEmail : "info@hafiztraveltours.com";
                String web = detail.company.website != null ? detail.company.website : "www.hafiztraveltours.com";
                tvCompanyHeaderContact.setText("Tel: " + phone + " • WA: " + wa + "\nEmail: " + email + " • " + web);
            }
        }

        // Status Badge
        if (tvInvoiceStatusBadge != null) {
            String rawStatus = detail.status != null ? detail.status.toLowerCase(Locale.ROOT) : "";
            if (rawStatus.contains("cancel") || rawStatus.contains("batal")) {
                tvInvoiceStatusBadge.setText(getString(R.string.status_cancelled));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#DC2626"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_inactive);
            } else if (detail.balanceAmount <= 0 && detail.paidAmount > 0) {
                tvInvoiceStatusBadge.setText(getString(R.string.payment_status_fully_paid));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (detail.paidAmount > 0) {
                tvInvoiceStatusBadge.setText(getString(R.string.payment_status_balance_partial));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
            } else {
                tvInvoiceStatusBadge.setText(getString(R.string.payment_status_deposit_pending));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
            }
        }

        // 2. Customer Info (Bill To)
        if (detail.customerInfo != null) {
            if (tvInvoiceCustomerName != null && detail.customerInfo.fullName != null) {
                tvInvoiceCustomerName.setText(detail.customerInfo.fullName + " (Ketua Jemaah)");
            }
            if (tvInvoiceCustomerAddress != null && detail.customerInfo.address != null) {
                tvInvoiceCustomerAddress.setText(detail.customerInfo.address);
            }
            if (tvInvoiceCustomerPhone != null && detail.customerInfo.mobileNumber != null) {
                tvInvoiceCustomerPhone.setText("Telefon: " + detail.customerInfo.mobileNumber);
            }
        } else if (detail.travellers != null && !detail.travellers.isEmpty()) {
            BookingDetailDto.TravellerInfo lead = detail.travellers.get(0);
            if (tvInvoiceCustomerName != null) tvInvoiceCustomerName.setText(lead.name + " (Ketua Jemaah)");
            if (tvInvoiceCustomerAddress != null) tvInvoiceCustomerAddress.setText("Johor Bahru, Johor, Malaysia");
            if (tvInvoiceCustomerPhone != null) tvInvoiceCustomerPhone.setText("Telefon: —");
        }

        // 3. Invoice & Package Metadata
        if (detail.invoiceMeta != null) {
            if (tvInvoiceNumberHeader != null && detail.invoiceMeta.invoiceNo != null) {
                tvInvoiceNumberHeader.setText(detail.invoiceMeta.invoiceNo);
            }
            if (tvInvoiceDate != null && detail.invoiceMeta.invoiceDate != null) {
                tvInvoiceDate.setText(formatDatePretty(detail.invoiceMeta.invoiceDate));
            }
            if (tvTourCode != null) {
                tvTourCode.setText(formatTourCode(detail.invoiceMeta.tourCode != null ? detail.invoiceMeta.tourCode : String.valueOf(detail.id)));
            }
            if (tvDepartureDate != null && detail.invoiceMeta.departureDate != null) {
                tvDepartureDate.setText(formatDatePretty(detail.invoiceMeta.departureDate));
            }
            if (tvPaymentDueDate != null && detail.invoiceMeta.paymentDueDate != null) {
                tvPaymentDueDate.setText(formatDatePretty(detail.invoiceMeta.paymentDueDate));
            }
            if (tvInvoicePackageName != null && detail.invoiceMeta.fullPackageTitle != null) {
                tvInvoicePackageName.setText(detail.invoiceMeta.fullPackageTitle);
            }
        } else {
            if (tvInvoiceNumberHeader != null) {
                String inv = (detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).invoiceNo != null)
                        ? detail.invoices.get(0).invoiceNo : "HQ " + (detail.bookingNo != null ? detail.bookingNo.replace("BKG-", "INV-") : "INV-" + detail.id);
                tvInvoiceNumberHeader.setText(inv);
            }
            if (tvInvoiceDate != null) {
                String dateRaw = (detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).issuedAt != null)
                        ? detail.invoices.get(0).issuedAt : detail.createdAt;
                tvInvoiceDate.setText(formatDatePretty(dateRaw));
            }
            if (tvTourCode != null) {
                String tc = (detail.departure != null && detail.departure.departureNo != null) ? detail.departure.departureNo : String.valueOf(detail.id);
                tvTourCode.setText(formatTourCode(tc));
            }
            if (tvDepartureDate != null) {
                String dep = (detail.departure != null && detail.departure.departureDate != null) ? detail.departure.departureDate : detail.departureDate;
                tvDepartureDate.setText(formatDatePretty(dep));
            }
            if (tvPaymentDueDate != null) {
                tvPaymentDueDate.setText(formatDatePretty(detail.depositDueDate));
            }
            if (tvInvoicePackageName != null) {
                tvInvoicePackageName.setText(detail.packageName != null ? detail.packageName : "Pakej Umrah");
            }
        }

        if (tvRoomSetup != null) {
            tvRoomSetup.setText(detail.roomSetup != null ? detail.roomSetup : "Standard Room — Room Pairing 01");
        }

        // 4. Passenger Details Table
        if (layoutPassengersContainer != null) {
            layoutPassengersContainer.removeAllViews();
            if (detail.travellers != null && !detail.travellers.isEmpty()) {
                for (int i = 0; i < detail.travellers.size(); i++) {
                    BookingDetailDto.TravellerInfo t = detail.travellers.get(i);
                    String idNo = t.identificationNo != null ? t.identificationNo : (t.passportNo != null ? t.passportNo : "—");
                    addPassengerRow(i + 1, t.name, idNo);
                }
            }
        }

        // 5. Itemized Billing Table
        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            if (detail.items != null && !detail.items.isEmpty()) {
                for (BookingDetailDto.ItemInfo item : detail.items) {
                    addLineItem(item.description != null ? item.description : detail.packageName,
                            String.valueOf(item.quantity),
                            BookingRequest.formatPrice(item.total));
                }
            } else {
                addLineItem(detail.packageName != null ? detail.packageName : "Pakej Pelancongan",
                        String.valueOf(detail.travellers != null ? detail.travellers.size() : 1),
                        BookingRequest.formatPrice(detail.totalAmount));
            }
        }

        // 6. Financial Summary & Amount in Words
        if (tvInvoiceSubtotal != null) tvInvoiceSubtotal.setText(BookingRequest.formatPrice(detail.subtotal > 0 ? detail.subtotal : detail.totalAmount));
        if (tvInvoiceGrandTotal != null) tvInvoiceGrandTotal.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvInvoicePaidAmount != null) tvInvoicePaidAmount.setText(BookingRequest.formatPrice(detail.paidAmount));
        if (tvInvoiceBalanceDue != null) tvInvoiceBalanceDue.setText(BookingRequest.formatPrice(detail.balanceAmount));

        if (tvInvoiceAmountInWords != null) {
            boolean isMalay = "ms".equalsIgnoreCase(Locale.getDefault().getLanguage()) || "in".equalsIgnoreCase(Locale.getDefault().getLanguage());
            String words = isMalay
                    ? (detail.amountInWordsMs != null && !detail.amountInWordsMs.isEmpty() ? detail.amountInWordsMs : detail.amountInWords)
                    : (detail.amountInWords != null && !detail.amountInWords.isEmpty() ? detail.amountInWords : detail.amountInWordsMs);
            if (words != null && !words.isEmpty()) {
                tvInvoiceAmountInWords.setText(words);
            } else {
                tvInvoiceAmountInWords.setText(BookingRequest.formatPrice(detail.totalAmount));
            }
        }

        // 7. Payment Records Table
        if (layoutPaymentRecordsContainer != null) {
            layoutPaymentRecordsContainer.removeAllViews();
            if (detail.paymentRecords != null && !detail.paymentRecords.isEmpty()) {
                for (BookingDetailDto.PaymentRecordDto p : detail.paymentRecords) {
                    addPaymentRecordRow(p.no, p.datePaid, p.refNo, p.paymentType, p.orNo, p.authorization, p.amount);
                }
            } else if (detail.payments != null && !detail.payments.isEmpty()) {
                for (int i = 0; i < detail.payments.size(); i++) {
                    BookingDetailDto.PaymentInfo p = detail.payments.get(i);
                    String ref = p.paymentNo != null ? p.paymentNo : "TRX-" + p.id;
                    String method = p.method != null ? p.method.toUpperCase(Locale.ROOT) : "ONLINE TRANSFER";
                    String orNo = p.isVerified ? (p.receiptNo != null ? p.receiptNo : "RCT-" + p.id) : "—";
                    String auth = p.isVerified ? "VERIFIED" : (p.status != null ? p.status.toUpperCase(Locale.ROOT) : "PENDING");
                    addPaymentRecordRow(i + 1, formatDatePretty(p.paidAt), ref, method, orNo, auth, p.amount);
                }
            } else {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText("Tiada rekod bayaran buat masa ini.");
                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                tvEmpty.setTextSize(11f);
                tvEmpty.setPadding(0, 8, 0, 8);
                layoutPaymentRecordsContainer.addView(tvEmpty);
            }
        }

        // 8. Bank Details & Terms
        if (detail.bankDetails != null) {
            if (tvBankDetails != null) {
                tvBankDetails.setText("Bank: " + detail.bankDetails.bankName + "\nAkaun: " + detail.bankDetails.accountNumber + "\nPenerima: " + detail.bankDetails.accountHolder);
            }
            if (tvPaymentTerms != null) {
                tvPaymentTerms.setText("Polisi Deposit: " + detail.bankDetails.depositPolicy + "\nBaki Bayaran: " + detail.bankDetails.balanceDeadline);
            }
        }

        // 9. Remarks & 10. T&C
        if (detail.termsAndConditions != null) {
            if (tvTermsConditions != null) {
                tvTermsConditions.setText("1. " + detail.termsAndConditions.itinerary + "\n2. " + detail.termsAndConditions.travelDocuments + "\n3. " + detail.termsAndConditions.cancellation);
            }
        }
    }

    private void addPassengerRow(int no, String name, String idNo) {
        if (layoutPassengersContainer == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView tvNo = new TextView(this);
        tvNo.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(32), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvNo.setText(no + ".");
        tvNo.setTextColor(Color.parseColor("#18181B"));
        tvNo.setTextSize(11.5f);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvName.setText(name != null ? name : "Jemaah " + no);
        tvName.setTextColor(Color.parseColor("#18181B"));
        tvName.setTextSize(11.5f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvId = new TextView(this);
        tvId.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvId.setText(idNo != null ? idNo : "—");
        tvId.setTextColor(Color.parseColor("#64748B"));
        tvId.setTextSize(11.5f);

        row.addView(tvNo);
        row.addView(tvName);
        row.addView(tvId);

        layoutPassengersContainer.addView(row);
    }

    private void addLineItem(String desc, String qty, String total) {
        if (layoutInvoiceItems == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 10, 0, 10);

        TextView tvDesc = new TextView(this);
        tvDesc.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f));
        tvDesc.setText(desc);
        tvDesc.setTextColor(Color.parseColor("#18181B"));
        tvDesc.setTextSize(12f);

        TextView tvQty = new TextView(this);
        tvQty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f));
        tvQty.setText(qty);
        tvQty.setTextColor(Color.parseColor("#71717A"));
        tvQty.setTextSize(12f);
        tvQty.setGravity(Gravity.CENTER);

        TextView tvTotal = new TextView(this);
        tvTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
        tvTotal.setText(total);
        tvTotal.setTextColor(Color.parseColor("#18181B"));
        tvTotal.setTextSize(12f);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setGravity(Gravity.END);

        row.addView(tvDesc);
        row.addView(tvQty);
        row.addView(tvTotal);

        layoutInvoiceItems.addView(row);
    }

    private void addPaymentRecordRow(int no, String datePaid, String refNo, String type, String orNo, String auth, double amount) {
        if (layoutPaymentRecordsContainer == null) return;

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_tip_pill);
        card.setPadding(20, 16, 20, 16);

        // Row 1: Ref No & Amount
        LinearLayout r1 = new LinearLayout(this);
        r1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        r1.setOrientation(LinearLayout.HORIZONTAL);

        TextView tvRef = new TextView(this);
        tvRef.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvRef.setText("#" + no + " • " + (refNo != null ? refNo : "TRX"));
        tvRef.setTextColor(Color.parseColor("#18181B"));
        tvRef.setTextSize(12f);
        tvRef.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(BookingRequest.formatPrice(amount));
        tvAmt.setTextColor(Color.parseColor("#059669"));
        tvAmt.setTextSize(12.5f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        r1.addView(tvRef);
        r1.addView(tvAmt);
        card.addView(r1);

        // Row 2: Date, Type, OR No
        TextView tvMeta = new TextView(this);
        tvMeta.setText("Tarikh: " + (datePaid != null ? datePaid : "—") + " • " + type + " • Resit: " + (orNo != null ? orNo : "—"));
        tvMeta.setTextColor(Color.parseColor("#64748B"));
        tvMeta.setTextSize(10.5f);
        tvMeta.setPadding(0, 4, 0, 0);
        card.addView(tvMeta);

        layoutPaymentRecordsContainer.addView(card);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void saveOrPrintPdf() {
        if (invoiceContentContainer == null) return;

        try {
            invoiceContentContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(invoiceContentContainer.getWidth() > 0 ? invoiceContentContainer.getWidth() : 1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int width = invoiceContentContainer.getMeasuredWidth() > 0 ? invoiceContentContainer.getMeasuredWidth() : 1080;
            int height = invoiceContentContainer.getMeasuredHeight() > 0 ? invoiceContentContainer.getMeasuredHeight() : 1920;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            invoiceContentContainer.draw(canvas);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas pageCanvas = page.getCanvas();
            pageCanvas.drawBitmap(bitmap, 0, 0, null);
            document.finishPage(page);

            String fileName = "Invois_" + (tvInvoiceNumberHeader != null ? tvInvoiceNumberHeader.getText().toString().replace('/', '_').replace(' ', '_') : "HafizTravel") + ".pdf";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            File pdfFile = new File(downloadsDir, fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, getString(R.string.toast_invoice_saved_success, fileName), Toast.LENGTH_LONG).show();

            try {
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(fileUri, "application/pdf");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, getString(R.string.invoice_title_official)));
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
            if (printManager != null) {
                Toast.makeText(this, getString(R.string.invoice_title_official), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shareInvoiceSummary() {
        String num = tvInvoiceNumberHeader != null ? tvInvoiceNumberHeader.getText().toString() : "HQ INV-OFFICIAL";
        String pkg = tvInvoicePackageName != null ? tvInvoicePackageName.getText().toString() : "Pakej Umrah";
        String total = tvInvoiceGrandTotal != null ? tvInvoiceGrandTotal.getText().toString() : "RM 0";
        String bal = tvInvoiceBalanceDue != null ? tvInvoiceBalanceDue.getText().toString() : "RM 0";

        String shareText = "📄 *" + getString(R.string.invoice_title_official).toUpperCase(Locale.ROOT) + "*\n" +
                "-----------------------------------\n" +
                getString(R.string.invoice_meta_no) + ": " + num + "\n" +
                "Pakej: " + pkg + "\n" +
                getString(R.string.invoice_grand_total_label) + " " + total + "\n" +
                getString(R.string.invoice_balance_due_label) + " " + bal + "\n" +
                "-----------------------------------\n" +
                getString(R.string.invoice_footer_thanks);

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.invoice_share_title)));
    }

    private String formatDatePretty(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        try {
            if (raw.length() >= 10) {
                String ymd = raw.substring(0, 10);
                SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date parsed = inFmt.parse(ymd);
                if (parsed != null) {
                    SimpleDateFormat outFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    return outFmt.format(parsed);
                }
            }
        } catch (Exception ignored) {}
        return raw;
    }

    public static String formatTourCode(String rawCode) {
        if (rawCode != null && rawCode.matches("^\\d{4}$")) {
            return rawCode;
        }
        String seed = (rawCode != null && !rawCode.trim().isEmpty()) ? rawCode.trim() : "4827";
        int hash = Math.abs(seed.hashCode());
        int fourDigit = 1000 + (hash % 9000);
        return String.format(Locale.US, "%04d", fourDigit);
    }
}
