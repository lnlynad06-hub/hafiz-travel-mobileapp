package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.FileProvider;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceViewerActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_BOOKING_NO = "extra_booking_no";
    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";
    public static final String EXTRA_VIEW_MODE = "extra_view_mode";
    public static final String VIEW_MODE_INVOICE = "invoice";
    public static final String VIEW_MODE_DETAILS = "details";

    private String viewMode = VIEW_MODE_INVOICE;
    private TextView tvInvoiceTopBarTitle;
    private TextView tvDocTypeTitle;
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
    private TextView tvDocumentDisclaimer;
    private TextView tvDocumentPageInfo;
    private LinearLayout invoiceContentContainer;

    // Modern Invoice Summary Card (Details Mode)
    private TextView tvCardInvoiceStatus;
    private TextView tvCardInvoiceNumber;
    private TextView tvCardInvoiceDate;
    private TextView tvCardTotalAmount;
    private TextView tvCardPaidAmount;
    private TextView tvCardBalanceAmount;
    private Button btnCardViewInvoice;
    private Button btnCardDownloadInvoice;
    private Button btnCardShareInvoice;

    // Details Mode View Components
    private LinearLayout layoutDetailsView;
    private LinearLayout layoutDetailsBottomBar;
    private LinearLayout layoutInvoiceBottomBar;

    private TextView tvDetailCategoryTag;
    private TextView tvDetailBookingStatusBadge;
    private TextView tvDetailPackageName;
    private TextView tvDetailBookingRef;
    private TextView tvDetailRoomAssignmentBadge;
    private TextView tvDetailDepartureDate;
    private TextView tvDetailReturnDate;
    private TextView tvDetailDuration;

    private TextView tvDetailRoomType;
    private LinearLayout layoutDetailHotelsContainer;

    private TextView tvDetailTravellerCount;
    private LinearLayout layoutDetailTravellersContainer;

    private TextView tvDetailTotalAmount;
    private TextView tvDetailPaidAmount;
    private TextView tvDetailBalanceAmount;
    private ProgressBar pbDetailPayment;
    private TextView tvDetailDueDate;

    private View layoutBtnViewReceipt;
    private Button btnDetailBottomViewInvoice;
    private Button btnDetailBottomShare;

    private boolean openedFromDetails = false;

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
        if (btnBack != null) btnBack.setOnClickListener(v -> handleBackNavigation());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        tvInvoiceTopBarTitle = findViewById(R.id.tvInvoiceTopBarTitle);
        tvDocTypeTitle = findViewById(R.id.tvDocTypeTitle);
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
        tvDocumentDisclaimer = findViewById(R.id.tvDocumentDisclaimer);
        tvDocumentPageInfo = findViewById(R.id.tvDocumentPageInfo);
        invoiceContentContainer = findViewById(R.id.invoiceContentContainer);

        // Modern Invoice Summary Card (Details mode)
        tvCardInvoiceStatus = findViewById(R.id.tvCardInvoiceStatus);
        tvCardInvoiceNumber = findViewById(R.id.tvCardInvoiceNumber);
        tvCardInvoiceDate = findViewById(R.id.tvCardInvoiceDate);
        tvCardTotalAmount = findViewById(R.id.tvCardTotalAmount);
        tvCardPaidAmount = findViewById(R.id.tvCardPaidAmount);
        tvCardBalanceAmount = findViewById(R.id.tvCardBalanceAmount);

        btnCardViewInvoice = findViewById(R.id.btnCardViewInvoice);
        if (btnCardViewInvoice != null) {
            btnCardViewInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                switchToInvoiceMode();
            });
        }

        btnCardDownloadInvoice = findViewById(R.id.btnCardDownloadInvoice);
        if (btnCardDownloadInvoice != null) {
            btnCardDownloadInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                downloadOfficialInvoicePdf(true);
            });
        }

        btnCardShareInvoice = findViewById(R.id.btnCardShareInvoice);
        if (btnCardShareInvoice != null) {
            btnCardShareInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                shareOfficialInvoicePdf();
            });
        }

        // Details mode views
        layoutDetailsView = findViewById(R.id.layoutDetailsView);
        layoutDetailsBottomBar = findViewById(R.id.layoutDetailsBottomBar);
        layoutInvoiceBottomBar = findViewById(R.id.layoutInvoiceBottomBar);

        tvDetailCategoryTag = findViewById(R.id.tvDetailCategoryTag);
        tvDetailBookingStatusBadge = findViewById(R.id.tvDetailBookingStatusBadge);
        tvDetailPackageName = findViewById(R.id.tvDetailPackageName);
        tvDetailBookingRef = findViewById(R.id.tvDetailBookingRef);
        tvDetailRoomAssignmentBadge = findViewById(R.id.tvDetailRoomAssignmentBadge);
        tvDetailDepartureDate = findViewById(R.id.tvDetailDepartureDate);
        tvDetailReturnDate = findViewById(R.id.tvDetailReturnDate);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);

        tvDetailRoomType = findViewById(R.id.tvDetailRoomType);
        layoutDetailHotelsContainer = findViewById(R.id.layoutDetailHotelsContainer);

        tvDetailTravellerCount = findViewById(R.id.tvDetailTravellerCount);
        layoutDetailTravellersContainer = findViewById(R.id.layoutDetailTravellersContainer);

        tvDetailTotalAmount = findViewById(R.id.tvDetailTotalAmount);
        tvDetailPaidAmount = findViewById(R.id.tvDetailPaidAmount);
        tvDetailBalanceAmount = findViewById(R.id.tvDetailBalanceAmount);
        pbDetailPayment = findViewById(R.id.pbDetailPayment);
        tvDetailDueDate = findViewById(R.id.tvDetailDueDate);

        layoutBtnViewReceipt = findViewById(R.id.layoutBtnViewReceipt);
        if (layoutBtnViewReceipt != null) {
            layoutBtnViewReceipt.setOnClickListener(v -> {
                HapticUtil.click(v);
                openReceiptViewer();
            });
        }

        btnDetailBottomViewInvoice = findViewById(R.id.btnDetailBottomViewInvoice);
        if (btnDetailBottomViewInvoice != null) {
            btnDetailBottomViewInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                switchToInvoiceMode();
            });
        }

        btnDetailBottomShare = findViewById(R.id.btnDetailBottomShare);
        if (btnDetailBottomShare != null) {
            btnDetailBottomShare.setOnClickListener(v -> {
                HapticUtil.click(v);
                shareDetailsSummary();
            });
        }

        Button btnDownload = findViewById(R.id.btnInvoiceDownloadPdf);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                HapticUtil.click(v);
                downloadOfficialInvoicePdf(true);
            });
        }

        Button btnShare = findViewById(R.id.btnInvoiceShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                HapticUtil.click(v);
                shareOfficialInvoicePdf();
            });
        }
    }

    private void handleBackNavigation() {
        if (openedFromDetails && VIEW_MODE_INVOICE.equalsIgnoreCase(viewMode)) {
            viewMode = VIEW_MODE_DETAILS;
            openedFromDetails = false;
            updateViewModeDisplay();
            ScrollView sv = findViewById(R.id.invoiceScrollView);
            if (sv != null) sv.smoothScrollTo(0, 0);
        } else {
            finish();
        }
    }

    private void switchToInvoiceMode() {
        viewMode = VIEW_MODE_INVOICE;
        openedFromDetails = true;
        updateViewModeDisplay();
        ScrollView sv = findViewById(R.id.invoiceScrollView);
        if (sv != null) sv.smoothScrollTo(0, 0);
    }

    private void openReceiptViewer() {
        int id = (bookingDetail != null && bookingDetail.id > 0) ? bookingDetail.id : bookingId;
        String bNo = (bookingDetail != null && bookingDetail.bookingNo != null) ? bookingDetail.bookingNo : bookingNo;
        Intent recIntent = new Intent(this, ReceiptViewerActivity.class);
        recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_ID, id);
        recIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, bNo);
        startActivity(recIntent);
    }

    private void updateViewModeDisplay() {
        boolean isDetails = VIEW_MODE_DETAILS.equalsIgnoreCase(viewMode);
        if (tvInvoiceTopBarTitle != null) {
            tvInvoiceTopBarTitle.setText(isDetails ? R.string.booking_title_details : R.string.invoice_title_official);
        }
        if (tvDocTypeTitle != null) {
            tvDocTypeTitle.setText(isDetails ? R.string.booking_title_details : R.string.invoice_title_official);
        }
        if (layoutDetailsView != null) {
            layoutDetailsView.setVisibility(isDetails ? View.VISIBLE : View.GONE);
        }
        if (layoutDetailsBottomBar != null) {
            layoutDetailsBottomBar.setVisibility(isDetails ? View.VISIBLE : View.GONE);
        }
        if (invoiceContentContainer != null) {
            invoiceContentContainer.setVisibility(isDetails ? View.GONE : View.VISIBLE);
        }
        if (layoutInvoiceBottomBar != null) {
            layoutInvoiceBottomBar.setVisibility(isDetails ? View.GONE : View.VISIBLE);
        }
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        viewMode = intent.getStringExtra(EXTRA_VIEW_MODE);
        if (viewMode == null || viewMode.isEmpty()) {
            viewMode = VIEW_MODE_INVOICE;
        }
        updateViewModeDisplay();

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
            boolean isDetails = VIEW_MODE_DETAILS.equalsIgnoreCase(viewMode);
            String ref = isDetails ? bookingNo : bookingNo.replace("BKG-", "INV-");
            if (tvInvoiceNumberHeader != null) tvInvoiceNumberHeader.setText(ref);
            if (tvDetailBookingRef != null) tvDetailBookingRef.setText(getString(R.string.booking_no_official_label) + ": " + bookingNo);
        }
    }

    private void populateFromBookingRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
        boolean isDetails = VIEW_MODE_DETAILS.equalsIgnoreCase(viewMode);
        String headerNo = isDetails ? displayNo : displayNo.replace("BKG-", "INV-");

        if (tvInvoiceNumberHeader != null) tvInvoiceNumberHeader.setText(headerNo);

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
            tvInvoicePackageName.setText(req.packageName != null ? req.packageName : getString(R.string.category_umrah));
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
                    addPassengerRow(i + 1, p.fullName, req.roomLabel != null ? req.roomLabel : "Standard Room");
                }
            }
        }

        // Line Items
        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            int pax = req.adultPaxCount > 0 ? req.adultPaxCount : 1;
            double uPrice = req.totalAmount / pax;
            addLineItem(req.packageName + " (" + (req.roomLabel != null ? req.roomLabel : "Standard Room") + ")",
                    BookingRequest.formatPrice(uPrice),
                    String.valueOf(pax),
                    BookingRequest.formatPrice(req.totalAmount));
        }

        if (tvInvoiceSubtotal != null) tvInvoiceSubtotal.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvInvoiceGrandTotal != null) tvInvoiceGrandTotal.setText(BookingRequest.formatPrice(req.totalAmount));

        double deposit = (req.totalAmount > 0) ? (req.totalAmount >= 2000 ? 1000.0 * req.adultPaxCount : req.totalAmount) : 1000.0 * req.adultPaxCount;
        double balance = Math.max(0, req.totalAmount - deposit);

        if (tvInvoicePaidAmount != null) tvInvoicePaidAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvInvoiceBalanceDue != null) tvInvoiceBalanceDue.setText(BookingRequest.formatPrice(balance));

        populateDetailsViewFromRequest(req, bNo);
    }

    private void fetchBookingDetailFromApi(int id) {
        ApiClient.getApiService().getBookingDetail(id).enqueue(new Callback<ApiResponse<BookingDetailDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<BookingDetailDto>> call, Response<ApiResponse<BookingDetailDto>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    bookingDetail = response.body().data;
                    populateFromBookingDetail(bookingDetail);
                    populateDetailsView(bookingDetail);
                } else {
                    showLoadError();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                showLoadError();
            }
        });
    }

    /** Server data unavailable: error toast + retry instead of silent fallback content. */
    private void showLoadError() {
        String msg = getString(R.string.doc_load_failed);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        View anchor = invoiceContentContainer != null ? invoiceContentContainer : findViewById(android.R.id.content);
        if (anchor != null) {
            try {
                com.google.android.material.snackbar.Snackbar.make(anchor, msg,
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.action_retry), v -> {
                            if (bookingId > 0) fetchBookingDetailFromApi(bookingId);
                        })
                        .show();
            } catch (Exception ignored) {}
        }
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
            if (detail.isPendingCancellation()) {
                tvInvoiceStatusBadge.setText(getString(R.string.status_pending_cancellation));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
            } else if (detail.isCancelled()) {
                if (detail.isRefundCompleted()) {
                    tvInvoiceStatusBadge.setText(getString(R.string.status_refunded));
                    tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                    tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
                } else if (detail.isRefundPendingOrProcessing()) {
                    tvInvoiceStatusBadge.setText(getString(R.string.status_refund_processing));
                    tvInvoiceStatusBadge.setTextColor(Color.parseColor("#2563EB"));
                    tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
                } else {
                    tvInvoiceStatusBadge.setText(getString(R.string.status_cancelled));
                    tvInvoiceStatusBadge.setTextColor(Color.parseColor("#DC2626"));
                    tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_inactive);
                }
            } else if (detail.balanceAmount <= 0 && detail.paidAmount > 0) {
                tvInvoiceStatusBadge.setText(getString(R.string.payment_status_fully_paid));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (detail.isDepositPaid || "deposit_paid".equalsIgnoreCase(detail.paymentStatus) || (detail.paidAmount > 0 && detail.paidAmount <= detail.requiredDeposit)) {
                tvInvoiceStatusBadge.setText(getString(R.string.status_deposit_paid));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (detail.paidAmount > 0) {
                tvInvoiceStatusBadge.setText(getString(R.string.payment_status_balance_partial));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
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
                tvInvoiceCustomerPhone.setText(getString(R.string.customer_phone_format, detail.customerInfo.mobileNumber));
            }
        } else if (detail.travellers != null && !detail.travellers.isEmpty()) {
            BookingDetailDto.TravellerInfo lead = detail.travellers.get(0);
            if (tvInvoiceCustomerName != null) tvInvoiceCustomerName.setText(lead.name + " (" + getString(R.string.lead_traveller_name_default) + ")");
            if (tvInvoiceCustomerAddress != null) tvInvoiceCustomerAddress.setText("Johor Bahru, Johor, Malaysia");
            if (tvInvoiceCustomerPhone != null) tvInvoiceCustomerPhone.setText(getString(R.string.customer_phone_format, "—"));
        }

        updateViewModeDisplay();
        boolean isDetails = VIEW_MODE_DETAILS.equalsIgnoreCase(viewMode);

        // 3. Invoice & Package Metadata
        if (detail.invoiceMeta != null) {
            if (tvInvoiceNumberHeader != null) {
                if (isDetails) {
                    String bNo = detail.bookingNo != null ? detail.bookingNo : ("BKG-" + detail.id);
                    tvInvoiceNumberHeader.setText(bNo);
                } else if (detail.invoiceMeta.invoiceNo != null) {
                    tvInvoiceNumberHeader.setText(detail.invoiceMeta.invoiceNo);
                }
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
                if (isDetails) {
                    String bNo = detail.bookingNo != null ? detail.bookingNo : ("BKG-" + detail.id);
                    tvInvoiceNumberHeader.setText(bNo);
                } else {
                    String inv = (detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).invoiceNo != null)
                            ? detail.invoices.get(0).invoiceNo : (detail.bookingNo != null ? detail.bookingNo.replace("BKG-", "INV-") : "INV-" + detail.id);
                    tvInvoiceNumberHeader.setText(inv);
                }
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
                tvInvoicePackageName.setText(detail.packageName != null ? detail.packageName : getString(R.string.category_umrah));
            }
        }

        if (tvRoomSetup != null) {
            tvRoomSetup.setText(detail.roomSetup != null ? detail.roomSetup : "Standard Room — Room Pairing 01");
        }

        // 4. Passenger Details Table (No IC/Passport exposed; displays Room Type)
        if (layoutPassengersContainer != null) {
            layoutPassengersContainer.removeAllViews();
            if (detail.travellers != null && !detail.travellers.isEmpty()) {
                for (int i = 0; i < detail.travellers.size(); i++) {
                    BookingDetailDto.TravellerInfo t = detail.travellers.get(i);
                    String room = (t.roomType != null && !t.roomType.trim().isEmpty())
                            ? t.roomType
                            : (detail.roomSetup != null ? detail.roomSetup : "Quad");
                    addPassengerRow(i + 1, t.name, room);
                }
            }
        }

        // 5. Itemized Pricing Breakdown Table
        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            if (detail.items != null && !detail.items.isEmpty()) {
                for (BookingDetailDto.ItemInfo item : detail.items) {
                    double uPrice = item.unitPrice > 0 ? item.unitPrice : (item.quantity > 0 ? (item.total / item.quantity) : item.total);
                    addLineItem(item.description != null ? item.description : detail.packageName,
                            BookingRequest.formatPrice(uPrice),
                            String.valueOf(item.quantity > 0 ? item.quantity : 1),
                            BookingRequest.formatPrice(item.total));
                }
            } else {
                int qty = detail.travellers != null && !detail.travellers.isEmpty() ? detail.travellers.size() : 1;
                double uPrice = qty > 0 ? (detail.totalAmount / qty) : detail.totalAmount;
                addLineItem(detail.packageName != null ? detail.packageName : getString(R.string.category_tourism),
                        BookingRequest.formatPrice(uPrice),
                        String.valueOf(qty),
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

        // 7. Payment History Table (Chronological verified transactions)
        if (layoutPaymentRecordsContainer != null) {
            layoutPaymentRecordsContainer.removeAllViews();
            if (detail.paymentRecords != null && !detail.paymentRecords.isEmpty()) {
                for (BookingDetailDto.PaymentRecordDto p : detail.paymentRecords) {
                    String ref = (p.refNo != null && !p.refNo.isEmpty()) ? p.refNo : (p.orNo != null ? p.orNo : "—");
                    addPaymentRecordRow(p.datePaid, ref, p.paymentType, p.amount);
                }
            } else if (detail.payments != null && !detail.payments.isEmpty()) {
                for (int i = 0; i < detail.payments.size(); i++) {
                    BookingDetailDto.PaymentInfo p = detail.payments.get(i);
                    String ref = (p.receiptNo != null && !p.receiptNo.isEmpty()) ? p.receiptNo : (p.paymentNo != null ? p.paymentNo : "TRX-" + p.id);
                    String method = p.method != null ? p.method.toUpperCase(Locale.ROOT) : "CHIP FPX";
                    addPaymentRecordRow(formatDatePretty(p.paidAt), ref, method, p.amount);
                }
            } else {
                TextView tvEmpty = new TextView(this);
                tvEmpty.setText(getString(R.string.invoice_no_payment_records));
                tvEmpty.setTextColor(Color.parseColor("#94A3B8"));
                tvEmpty.setTextSize(11.5f);
                tvEmpty.setPadding(0, 8, 0, 8);
                layoutPaymentRecordsContainer.addView(tvEmpty);
            }
        }

        // 8. Bank Details
        if (detail.bankDetails != null) {
            if (tvBankDetails != null) {
                tvBankDetails.setText(getString(R.string.invoice_bank_details_format,
                        detail.bankDetails.bankName,
                        detail.bankDetails.accountNumber,
                        detail.bankDetails.accountHolder));
            }
        }
    }

    private void addPassengerRow(int no, String name, String roomType) {
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
        tvName.setText(name != null ? name : getString(R.string.default_traveller_number, no));
        tvName.setTextColor(Color.parseColor("#18181B"));
        tvName.setTextSize(11.5f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvRoom = new TextView(this);
        tvRoom.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(110), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvRoom.setText(roomType != null ? roomType : "—");
        tvRoom.setTextColor(Color.parseColor("#64748B"));
        tvRoom.setTextSize(11.5f);
        tvRoom.setGravity(Gravity.END);

        row.addView(tvNo);
        row.addView(tvName);
        row.addView(tvRoom);

        layoutPassengersContainer.addView(row);
    }

    private void addLineItem(String desc, String unitPrice, String qty, String total) {
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

        TextView tvUnit = new TextView(this);
        tvUnit.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvUnit.setText(unitPrice);
        tvUnit.setTextColor(Color.parseColor("#64748B"));
        tvUnit.setTextSize(12f);
        tvUnit.setGravity(Gravity.END);

        TextView tvQty = new TextView(this);
        tvQty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));
        tvQty.setText(qty);
        tvQty.setTextColor(Color.parseColor("#71717A"));
        tvQty.setTextSize(12f);
        tvQty.setGravity(Gravity.CENTER);

        TextView tvTotal = new TextView(this);
        tvTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f));
        tvTotal.setText(total);
        tvTotal.setTextColor(Color.parseColor("#18181B"));
        tvTotal.setTextSize(12f);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setGravity(Gravity.END);

        row.addView(tvDesc);
        row.addView(tvUnit);
        row.addView(tvQty);
        row.addView(tvTotal);

        layoutInvoiceItems.addView(row);
    }

    private void addPaymentRecordRow(String datePaid, String refNo, String method, double amount) {
        if (layoutPaymentRecordsContainer == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvDate = new TextView(this);
        tvDate.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvDate.setText(datePaid != null ? datePaid : "—");
        tvDate.setTextColor(Color.parseColor("#18181B"));
        tvDate.setTextSize(11.5f);

        TextView tvRef = new TextView(this);
        tvRef.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f));
        tvRef.setText(refNo != null ? refNo : "—");
        tvRef.setTextColor(Color.parseColor("#18181B"));
        tvRef.setTextSize(11.5f);
        tvRef.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvMethod = new TextView(this);
        tvMethod.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f));
        tvMethod.setText(method != null ? method : "CHIP FPX");
        tvMethod.setTextColor(Color.parseColor("#64748B"));
        tvMethod.setTextSize(11.5f);

        TextView tvAmt = new TextView(this);
        tvAmt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvAmt.setText(BookingRequest.formatPrice(amount));
        tvAmt.setTextColor(Color.parseColor("#059669"));
        tvAmt.setTextSize(11.5f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAmt.setGravity(Gravity.END);

        row.addView(tvDate);
        row.addView(tvRef);
        row.addView(tvMethod);
        row.addView(tvAmt);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));

        layoutPaymentRecordsContainer.addView(row);
        layoutPaymentRecordsContainer.addView(divider);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void downloadOfficialInvoicePdf(final boolean openToView) {
        fetchOfficialInvoicePdf(openToView, false);
    }

    private void shareOfficialInvoicePdf() {
        boolean isDetails = VIEW_MODE_DETAILS.equalsIgnoreCase(viewMode);
        if (isDetails) {
            shareDetailsSummary();
            return;
        }
        fetchOfficialInvoicePdf(false, true);
    }

    private void fetchOfficialInvoicePdf(final boolean openToView, final boolean shareFile) {
        int id = (bookingDetail != null && bookingDetail.id > 0) ? bookingDetail.id : bookingId;
        if (id <= 0) {
            Toast.makeText(this, R.string.toast_invoice_booking_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String lang = ("ms".equalsIgnoreCase(Locale.getDefault().getLanguage()) || "in".equalsIgnoreCase(Locale.getDefault().getLanguage())) ? "ms" : "en";
        Toast.makeText(this, shareFile ? R.string.toast_preparing_invoice_share : R.string.toast_downloading_invoice, Toast.LENGTH_SHORT).show();

        ApiClient.getApiService().getBookingInvoicePdf(id, lang).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;

                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        try {
                            String invNum = (tvInvoiceNumberHeader != null && tvInvoiceNumberHeader.getText() != null)
                                    ? tvInvoiceNumberHeader.getText().toString().replace('/', '_').replace(' ', '_')
                                    : ("INV_" + id);
                            String fileName = "Invois_" + invNum + ".pdf";

                            File targetFile;
                            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                            if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                                targetFile = new File(downloadsDir, fileName);
                            } else {
                                targetFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                            }

                            InputStream is = response.body().byteStream();
                            FileOutputStream fos = new FileOutputStream(targetFile);
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, read);
                            }
                            fos.flush();
                            fos.close();
                            is.close();

                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;

                                Uri fileUri = FileProvider.getUriForFile(
                                        InvoiceViewerActivity.this,
                                        getPackageName() + ".provider",
                                        targetFile
                                );

                                if (shareFile) {
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("application/pdf");
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    String docTitle = getString(R.string.invoice_title_official);
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, docTitle + " - " + invNum);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, docTitle));
                                } else {
                                    Toast.makeText(InvoiceViewerActivity.this,
                                            getString(R.string.toast_invoice_saved_success, fileName),
                                            Toast.LENGTH_LONG).show();

                                    if (openToView) {
                                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                                        viewIntent.setDataAndType(fileUri, "application/pdf");
                                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        try {
                                            startActivity(Intent.createChooser(viewIntent, getString(R.string.invoice_title_official)));
                                        } catch (Exception ex) {
                                            Toast.makeText(InvoiceViewerActivity.this, fileName, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    Toast.makeText(InvoiceViewerActivity.this, "Error saving invoice: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(InvoiceViewerActivity.this, "Download failed (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(InvoiceViewerActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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

    private void shareDetailsSummary() {
        String docTitle = getString(R.string.booking_title_details);
        String num = tvDetailBookingRef != null ? tvDetailBookingRef.getText().toString() : (bookingNo != null ? bookingNo : "BKG-OFFICIAL");
        String pkg = tvDetailPackageName != null ? tvDetailPackageName.getText().toString() : getString(R.string.category_umrah);
        String dep = tvDetailDepartureDate != null ? tvDetailDepartureDate.getText().toString() : "—";
        String pax = tvDetailTravellerCount != null ? tvDetailTravellerCount.getText().toString() : "1 Pax";
        String total = tvDetailTotalAmount != null ? tvDetailTotalAmount.getText().toString() : "RM 0";
        String bal = tvDetailBalanceAmount != null ? tvDetailBalanceAmount.getText().toString() : "RM 0";

        String shareText = "🕋 *" + docTitle.toUpperCase(Locale.ROOT) + "*\n" +
                "-----------------------------------\n" +
                num + "\n" +
                getString(R.string.details_share_package) + ": " + pkg + "\n" +
                getString(R.string.details_share_dep_date) + ": " + dep + "\n" +
                getString(R.string.details_share_pilgrims) + ": " + pax + "\n" +
                getString(R.string.invoice_grand_total_label) + " " + total + "\n" +
                getString(R.string.invoice_balance_due_label) + " " + bal + "\n" +
                "-----------------------------------\n" +
                getString(R.string.invoice_footer_thanks);

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, docTitle));
    }

    private void populateDetailsView(BookingDetailDto detail) {
        if (detail == null) return;

        // 1. Hero Card
        if (tvDetailCategoryTag != null) {
            String pkg = detail.packageName != null ? detail.packageName.toLowerCase(Locale.ROOT) : "";
            if (pkg.contains("pelancongan") || pkg.contains("tour") || pkg.contains("turkey") || pkg.contains("balkan") || pkg.contains("korea")) {
                tvDetailCategoryTag.setText(getString(R.string.category_tourism));
            } else {
                tvDetailCategoryTag.setText(getString(R.string.category_umrah));
            }
        }

        if (tvDetailBookingStatusBadge != null && tvInvoiceStatusBadge != null) {
            tvDetailBookingStatusBadge.setText(tvInvoiceStatusBadge.getText());
            tvDetailBookingStatusBadge.setTextColor(tvInvoiceStatusBadge.getTextColors());
            tvDetailBookingStatusBadge.setBackground(tvInvoiceStatusBadge.getBackground());
        }

        if (tvDetailPackageName != null) {
            tvDetailPackageName.setText(detail.packageName != null ? detail.packageName : getString(R.string.category_umrah));
        }

        if (tvDetailBookingRef != null) {
            String bNo = detail.bookingNo != null ? detail.bookingNo : ("BKG-" + detail.id);
            tvDetailBookingRef.setText(getString(R.string.booking_no_prefix, bNo));
        }

        if (tvDetailRoomAssignmentBadge != null) {
            tvDetailRoomAssignmentBadge.setText(getString(R.string.room_assignment_confirmed));
            tvDetailRoomAssignmentBadge.setTextColor(Color.parseColor("#059669"));
            tvDetailRoomAssignmentBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        String dep = (detail.departure != null && detail.departure.departureDate != null) ? detail.departure.departureDate : detail.departureDate;
        if (tvDetailDepartureDate != null) {
            tvDetailDepartureDate.setText(formatDatePretty(dep));
        }

        String ret = (detail.departure != null && detail.departure.returnDate != null) ? detail.departure.returnDate : null;
        if (tvDetailReturnDate != null) {
            tvDetailReturnDate.setText(formatDatePretty(ret));
        }

        int paxCount = (detail.travellers != null && !detail.travellers.isEmpty()) ? detail.travellers.size() : 1;
        if (tvDetailDuration != null) {
            tvDetailDuration.setText(getString(R.string.details_duration_pax_format, paxCount));
        }

        // 2. Hotel & Room
        if (tvDetailRoomType != null) {
            String room = (detail.roomSetup != null && !detail.roomSetup.isEmpty()) ? detail.roomSetup : getString(R.string.details_default_room_label);
            tvDetailRoomType.setText(room);
        }

        if (layoutDetailHotelsContainer != null) {
            layoutDetailHotelsContainer.removeAllViews();
            String starSuffix = getString(R.string.hotel_5_star_suffix);
            addHotelRow("Makkah", "Hotel Anjum / Pullman ZamZam " + starSuffix);
            addHotelRow("Madinah", "Hotel Al-Aqeeq / Frontel Al-Harithia " + starSuffix);
        }

        // 3. Travellers
        if (tvDetailTravellerCount != null) {
            tvDetailTravellerCount.setText(getString(R.string.details_pax_count_format, paxCount));
        }

        if (layoutDetailTravellersContainer != null) {
            layoutDetailTravellersContainer.removeAllViews();
            if (detail.travellers != null && !detail.travellers.isEmpty()) {
                for (int i = 0; i < detail.travellers.size(); i++) {
                    BookingDetailDto.TravellerInfo t = detail.travellers.get(i);
                    addDetailTravellerCard(i + 1, t);
                }
            } else if (detail.customerInfo != null) {
                BookingDetailDto.TravellerInfo fallback = new BookingDetailDto.TravellerInfo();
                fallback.name = detail.customerInfo.fullName;
                fallback.identificationNo = detail.customerInfo.mobileNumber;
                fallback.role = getString(R.string.role_lead_pilgrim);
                fallback.isVerified = true;
                addDetailTravellerCard(1, fallback);
            }
        }

        // 4. Payment Summary
        if (tvDetailTotalAmount != null) tvDetailTotalAmount.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvDetailPaidAmount != null) tvDetailPaidAmount.setText(BookingRequest.formatPrice(detail.paidAmount));
        if (tvDetailBalanceAmount != null) tvDetailBalanceAmount.setText(BookingRequest.formatPrice(detail.balanceAmount));

        if (pbDetailPayment != null) {
            int progress = 0;
            if (detail.totalAmount > 0) {
                progress = (int) Math.min(100, Math.max(0, Math.round((detail.paidAmount / detail.totalAmount) * 100)));
            }
            pbDetailPayment.setProgress(progress);
        }

        if (tvDetailDueDate != null) {
            String due = (detail.depositDueDate != null && !detail.depositDueDate.isEmpty()) ? formatDatePretty(detail.depositDueDate) : "—";
            tvDetailDueDate.setText(getString(R.string.details_payment_due_format, due));
        }

        // 5. Populate Modern Invoice Summary Card
        if (tvCardInvoiceNumber != null) {
            String inv = (detail.invoiceMeta != null && detail.invoiceMeta.invoiceNo != null)
                    ? detail.invoiceMeta.invoiceNo
                    : ((detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).invoiceNo != null)
                        ? detail.invoices.get(0).invoiceNo
                        : (detail.bookingNo != null ? detail.bookingNo.replace("BKG-", "INV-") : "INV-" + detail.id));
            tvCardInvoiceNumber.setText(inv);
        }
        if (tvCardInvoiceDate != null) {
            String invDate = (detail.invoiceMeta != null && detail.invoiceMeta.invoiceDate != null)
                    ? detail.invoiceMeta.invoiceDate
                    : ((detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).issuedAt != null)
                        ? detail.invoices.get(0).issuedAt
                        : detail.createdAt);
            tvCardInvoiceDate.setText(formatDatePretty(invDate));
        }
        if (tvCardTotalAmount != null) tvCardTotalAmount.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvCardPaidAmount != null) tvCardPaidAmount.setText(BookingRequest.formatPrice(detail.paidAmount));
        if (tvCardBalanceAmount != null) tvCardBalanceAmount.setText(BookingRequest.formatPrice(detail.balanceAmount));

        if (tvCardInvoiceStatus != null) {
            if (detail.balanceAmount <= 0.01 && detail.totalAmount > 0) {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_fully_paid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#059669"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (detail.paidAmount > 0) {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_partially_paid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#059669"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_success);
            } else {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_unpaid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#D97706"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_accent);
            }
        }
    }

    private void populateDetailsViewFromRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        if (tvDetailCategoryTag != null) {
            String pkg = req.packageName != null ? req.packageName.toLowerCase(Locale.ROOT) : "";
            if (pkg.contains("pelancongan") || pkg.contains("tour")) {
                tvDetailCategoryTag.setText(getString(R.string.category_tourism));
            } else {
                tvDetailCategoryTag.setText(getString(R.string.category_umrah));
            }
        }

        if (tvDetailBookingStatusBadge != null) {
            tvDetailBookingStatusBadge.setText(getString(R.string.status_deposit_paid));
            tvDetailBookingStatusBadge.setTextColor(Color.parseColor("#059669"));
            tvDetailBookingStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        if (tvDetailPackageName != null) {
            tvDetailPackageName.setText(req.packageName != null ? req.packageName : getString(R.string.category_umrah));
        }

        if (tvDetailBookingRef != null) {
            String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
            tvDetailBookingRef.setText(getString(R.string.booking_no_prefix, displayNo));
        }

        if (tvDetailRoomAssignmentBadge != null) {
            tvDetailRoomAssignmentBadge.setText(getString(R.string.room_assignment_confirmed));
            tvDetailRoomAssignmentBadge.setTextColor(Color.parseColor("#059669"));
            tvDetailRoomAssignmentBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        if (tvDetailDepartureDate != null) {
            tvDetailDepartureDate.setText(req.selectedDepartureDate != null ? formatDatePretty(req.selectedDepartureDate) : "—");
        }
        if (tvDetailReturnDate != null) {
            tvDetailReturnDate.setText("—");
        }

        int paxCount = req.adultPaxCount > 0 ? req.adultPaxCount : (req.passengers != null ? req.passengers.size() : 1);
        if (tvDetailDuration != null) {
            tvDetailDuration.setText(getString(R.string.details_duration_pax_format, paxCount));
        }

        if (tvDetailRoomType != null) {
            String room = (req.roomLabel != null && !req.roomLabel.isEmpty()) ? req.roomLabel : getString(R.string.details_default_room_label);
            tvDetailRoomType.setText(room);
        }

        if (layoutDetailHotelsContainer != null) {
            layoutDetailHotelsContainer.removeAllViews();
            String starSuffix = getString(R.string.hotel_5_star_suffix);
            addHotelRow("Makkah", "Hotel Anjum / Pullman ZamZam " + starSuffix);
            addHotelRow("Madinah", "Hotel Al-Aqeeq / Frontel Al-Harithia " + starSuffix);
        }

        if (tvDetailTravellerCount != null) {
            tvDetailTravellerCount.setText(getString(R.string.details_pax_count_format, paxCount));
        }

        if (layoutDetailTravellersContainer != null) {
            layoutDetailTravellersContainer.removeAllViews();
            if (req.passengers != null && !req.passengers.isEmpty()) {
                for (int i = 0; i < req.passengers.size(); i++) {
                    BookingRequest.Passenger p = req.passengers.get(i);
                    BookingDetailDto.TravellerInfo t = new BookingDetailDto.TravellerInfo();
                    t.name = p.fullName;
                    t.identificationNo = p.icPassportNumber;
                    t.role = (i == 0) ? getString(R.string.role_lead_pilgrim) : getString(R.string.role_pilgrim);
                    t.isVerified = true;
                    addDetailTravellerCard(i + 1, t);
                }
            }
        }

        double deposit = (req.totalAmount > 0) ? (req.totalAmount >= 2000 ? 1000.0 * req.adultPaxCount : req.totalAmount) : 1000.0 * req.adultPaxCount;
        double balance = Math.max(0, req.totalAmount - deposit);

        if (tvDetailTotalAmount != null) tvDetailTotalAmount.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvDetailPaidAmount != null) tvDetailPaidAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvDetailBalanceAmount != null) tvDetailBalanceAmount.setText(BookingRequest.formatPrice(balance));

        if (pbDetailPayment != null) {
            int progress = (req.totalAmount > 0) ? (int) Math.min(100, Math.round((deposit / req.totalAmount) * 100)) : 0;
            pbDetailPayment.setProgress(progress);
        }

        // Modern Invoice Summary Card from Request
        if (tvCardInvoiceNumber != null) {
            String inv = (bNo != null && !bNo.isEmpty()) ? bNo.replace("BKG-", "INV-") : "INV-OFFICIAL";
            tvCardInvoiceNumber.setText(inv);
        }
        if (tvCardInvoiceDate != null) {
            tvCardInvoiceDate.setText(formatDatePretty(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())));
        }
        if (tvCardTotalAmount != null) tvCardTotalAmount.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvCardPaidAmount != null) tvCardPaidAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvCardBalanceAmount != null) tvCardBalanceAmount.setText(BookingRequest.formatPrice(balance));
        if (tvCardInvoiceStatus != null) {
            if (balance <= 0.01 && req.totalAmount > 0) {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_fully_paid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#059669"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (deposit > 0) {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_partially_paid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#059669"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_success);
            } else {
                tvCardInvoiceStatus.setText(getString(R.string.invoice_status_unpaid));
                tvCardInvoiceStatus.setTextColor(Color.parseColor("#D97706"));
                tvCardInvoiceStatus.setBackgroundResource(R.drawable.bg_pill_accent);
            }
        }
    }

    private void addHotelRow(String city, String name) {
        if (layoutDetailHotelsContainer == null) return;
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView dot = new ImageView(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(6), dpToPx(6));
        dotLp.setMargins(0, 0, dpToPx(8), 0);
        dot.setLayoutParams(dotLp);
        dot.setBackgroundResource(R.drawable.bg_pill_accent);

        TextView tvCity = new TextView(this);
        tvCity.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(68), LinearLayout.LayoutParams.WRAP_CONTENT));
        tvCity.setText(city);
        tvCity.setTextColor(Color.parseColor("#64748B"));
        tvCity.setTextSize(11.5f);
        tvCity.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#18181B"));
        tvName.setTextSize(12f);

        row.addView(dot);
        row.addView(tvCity);
        row.addView(tvName);
        layoutDetailHotelsContainer.addView(row);
    }

    private void addDetailTravellerCard(int no, BookingDetailDto.TravellerInfo t) {
        if (layoutDetailTravellersContainer == null || t == null) return;

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardLp);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_tip_pill);
        card.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvAvatar = new TextView(this);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38));
        tvAvatar.setLayoutParams(avLp);
        tvAvatar.setBackgroundResource(R.drawable.bg_pill_accent);
        String initial = (t.name != null && !t.name.trim().isEmpty()) ? t.name.trim().substring(0, 1).toUpperCase(Locale.ROOT) : String.valueOf(no);
        tvAvatar.setText(initial);
        tvAvatar.setTextColor(Color.parseColor("#9333EA"));
        tvAvatar.setTextSize(14f);
        tvAvatar.setTypeface(null, android.graphics.Typeface.BOLD);
        tvAvatar.setGravity(Gravity.CENTER);

        LinearLayout info = new LinearLayout(this);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        infoLp.setMargins(dpToPx(12), 0, 0, 0);
        info.setLayoutParams(infoLp);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(t.name != null && !t.name.isEmpty() ? t.name : getString(R.string.default_traveller_number, no));
        tvName.setTextColor(Color.parseColor("#18181B"));
        tvName.setTextSize(13f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        info.addView(tvName);

        String roleStr = (t.role != null && !t.role.isEmpty()) ? t.role : (no == 1 ? getString(R.string.role_lead_pilgrim) : getString(R.string.role_pilgrim));
        String idStr = (t.identificationNo != null && !t.identificationNo.isEmpty()) ? t.identificationNo : ((t.passportNo != null && !t.passportNo.isEmpty()) ? t.passportNo : "—");
        TextView tvMeta = new TextView(this);
        tvMeta.setText(roleStr + " • " + idStr);
        tvMeta.setTextColor(Color.parseColor("#64748B"));
        tvMeta.setTextSize(11f);
        tvMeta.setPadding(0, dpToPx(2), 0, 0);
        info.addView(tvMeta);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(0, dpToPx(6), 0, 0);

        TextView tvDocBadge = new TextView(this);
        boolean isDocVerified = t.isVerified || "verified".equalsIgnoreCase(t.documentStatus);
        tvDocBadge.setText(getString(isDocVerified ? R.string.doc_status_verified : R.string.doc_status_pending));
        tvDocBadge.setTextColor(Color.parseColor(isDocVerified ? "#059669" : "#D97706"));
        tvDocBadge.setTextSize(10f);
        tvDocBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDocBadge.setBackgroundResource(isDocVerified ? R.drawable.bg_pill_success : R.drawable.bg_pill_accent);
        tvDocBadge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        badgeRow.addView(tvDocBadge);

        TextView tvRoomBadge = new TextView(this);
        LinearLayout.LayoutParams roomLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        roomLp.setMargins(dpToPx(6), 0, 0, 0);
        tvRoomBadge.setLayoutParams(roomLp);
        tvRoomBadge.setText(getString(R.string.room_status_confirmed_badge));
        tvRoomBadge.setTextColor(Color.parseColor("#059669"));
        tvRoomBadge.setTextSize(10f);
        tvRoomBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRoomBadge.setBackgroundResource(R.drawable.bg_pill_success);
        tvRoomBadge.setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3));
        badgeRow.addView(tvRoomBadge);

        info.addView(badgeRow);

        card.addView(tvAvatar);
        card.addView(info);
        layoutDetailTravellersContainer.addView(card);
    }
}
