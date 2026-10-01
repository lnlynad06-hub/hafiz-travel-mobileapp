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

public class ReceiptViewerActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_ID = "extra_booking_id";
    public static final String EXTRA_BOOKING_NO = "extra_booking_no";
    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private TextView tvCompanyHeaderName;
    private TextView tvCompanyHeaderMeta;
    private TextView tvCompanyHeaderAddress;
    private TextView tvCompanyHeaderContact;
    private TextView tvReceiptStatusBadge;
    private TextView tvReceiptNumberHeader;
    private TextView tvReceiptAmount;
    private TextView tvReceiptAmountInWords;
    private TextView tvReceiptCustomerName;
    private TextView tvReceiptCustomerDetails;
    private TextView tvReceiptDate;
    private TextView tvReceiptPaymentMethod;
    private TextView tvReceiptRefNo;
    private TextView tvReceiptBookingRef;
    private TextView tvReceiptPackageName;
    private LinearLayout layoutReceiptPassengersContainer;
    private TextView tvReceiptTotalAmount;
    private TextView tvReceiptTotalPaid;
    private TextView tvReceiptBalanceDue;
    private LinearLayout layoutInstallmentsContainer;
    private LinearLayout layoutInstallmentsList;
    private TextView tvReceiptOfficialNotes;
    private TextView tvDocumentDisclaimer;
    private TextView tvDocumentPageInfo;
    private LinearLayout receiptContentContainer;

    private int bookingId = 0;
    private String bookingNo = "";
    private BookingRequest bookingRequest;
    private BookingDetailDto bookingDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_viewer);

        initViews();
        handleIntentData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnReceiptBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvCompanyHeaderName = findViewById(R.id.tvCompanyHeaderName);
        tvCompanyHeaderMeta = findViewById(R.id.tvCompanyHeaderMeta);
        tvCompanyHeaderAddress = findViewById(R.id.tvCompanyHeaderAddress);
        tvCompanyHeaderContact = findViewById(R.id.tvCompanyHeaderContact);
        tvReceiptStatusBadge = findViewById(R.id.tvReceiptStatusBadge);
        tvReceiptNumberHeader = findViewById(R.id.tvReceiptNumberHeader);
        tvReceiptAmount = findViewById(R.id.tvReceiptAmount);
        tvReceiptAmountInWords = findViewById(R.id.tvReceiptAmountInWords);
        tvReceiptCustomerName = findViewById(R.id.tvReceiptCustomerName);
        tvReceiptCustomerDetails = findViewById(R.id.tvReceiptCustomerDetails);
        tvReceiptDate = findViewById(R.id.tvReceiptDate);
        tvReceiptPaymentMethod = findViewById(R.id.tvReceiptPaymentMethod);
        tvReceiptRefNo = findViewById(R.id.tvReceiptRefNo);
        tvReceiptBookingRef = findViewById(R.id.tvReceiptBookingRef);
        tvReceiptPackageName = findViewById(R.id.tvReceiptPackageName);
        layoutReceiptPassengersContainer = findViewById(R.id.layoutReceiptPassengersContainer);
        tvReceiptTotalAmount = findViewById(R.id.tvReceiptTotalAmount);
        tvReceiptTotalPaid = findViewById(R.id.tvReceiptTotalPaid);
        tvReceiptBalanceDue = findViewById(R.id.tvReceiptBalanceDue);
        layoutInstallmentsContainer = findViewById(R.id.layoutInstallmentsContainer);
        layoutInstallmentsList = findViewById(R.id.layoutInstallmentsList);
        tvReceiptOfficialNotes = findViewById(R.id.tvReceiptOfficialNotes);
        tvDocumentDisclaimer = findViewById(R.id.tvDocumentDisclaimer);
        tvDocumentPageInfo = findViewById(R.id.tvDocumentPageInfo);
        receiptContentContainer = findViewById(R.id.receiptContentContainer);

        Button btnDownload = findViewById(R.id.btnReceiptDownloadPdf);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> {
                HapticUtil.click(v);
                saveOrPrintReceiptPdf();
            });
        }

        Button btnShare = findViewById(R.id.btnReceiptShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                HapticUtil.click(v);
                shareReceiptSummary();
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
        if (tvReceiptDate != null) {
            tvReceiptDate.setText(sdf.format(new Date()));
        }

        if (bookingRequest != null) {
            populateFromBookingRequest(bookingRequest, bookingNo);
        } else if (bookingId > 0) {
            fetchBookingDetailFromApi(bookingId);
        } else if (bookingNo != null && !bookingNo.isEmpty()) {
            if (tvReceiptBookingRef != null) {
                tvReceiptBookingRef.setText(bookingNo);
            }
            if (tvReceiptNumberHeader != null) {
                tvReceiptNumberHeader.setText("RCT-" + bookingNo.replace("BKG-", ""));
            }
        }
    }

    private void populateFromBookingRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
        String recNo = "RCT-" + displayNo.replace("BKG-", "");

        if (tvReceiptNumberHeader != null) tvReceiptNumberHeader.setText(recNo);
        if (tvReceiptBookingRef != null) tvReceiptBookingRef.setText(displayNo);

        if (tvReceiptStatusBadge != null) {
            tvReceiptStatusBadge.setText(getString(R.string.status_received));
            tvReceiptStatusBadge.setTextColor(Color.parseColor("#059669"));
            tvReceiptStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        if (req.passengers != null && !req.passengers.isEmpty()) {
            BookingRequest.Passenger lead = req.passengers.get(0);
            if (tvReceiptCustomerName != null) {
                tvReceiptCustomerName.setText(lead.fullName != null && !lead.fullName.isEmpty() ? lead.fullName + " (Ketua Jemaah)" : getString(R.string.default_user_name));
            }
            if (tvReceiptCustomerDetails != null) {
                String phoneStr = (lead.phoneNumber != null && !lead.phoneNumber.isEmpty()) ? lead.phoneNumber : "—";
                tvReceiptCustomerDetails.setText("Telefon: " + phoneStr + " • Johor Bahru, Johor");
            }
        }

        if (tvReceiptPackageName != null) {
            tvReceiptPackageName.setText(req.packageName != null ? req.packageName : "Pakej Umrah / Pelancongan");
        }

        if (tvReceiptPaymentMethod != null) {
            tvReceiptPaymentMethod.setText("FPX / Online Banking");
        }

        if (tvReceiptRefNo != null) {
            tvReceiptRefNo.setText("TOYYIB-ONLINE-" + displayNo);
        }

        double deposit = req.totalAmount;
        double balance = 0.0;

        if (tvReceiptAmount != null) tvReceiptAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvReceiptTotalAmount != null) tvReceiptTotalAmount.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvReceiptTotalPaid != null) tvReceiptTotalPaid.setText(BookingRequest.formatPrice(deposit));
        if (tvReceiptBalanceDue != null) tvReceiptBalanceDue.setText(BookingRequest.formatPrice(balance));

        if (layoutReceiptPassengersContainer != null) {
            layoutReceiptPassengersContainer.removeAllViews();
            if (req.passengers != null && !req.passengers.isEmpty()) {
                for (int i = 0; i < req.passengers.size(); i++) {
                    BookingRequest.Passenger p = req.passengers.get(i);
                    addPassengerRow(i + 1, p.fullName, p.icPassportNumber);
                }
            }
        }
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

        if (tvReceiptStatusBadge != null) {
            tvReceiptStatusBadge.setText(getString(R.string.status_received));
            tvReceiptStatusBadge.setTextColor(Color.parseColor("#059669"));
            tvReceiptStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        // 2. Receipt Meta
        if (detail.receiptMeta != null) {
            if (tvReceiptNumberHeader != null && detail.receiptMeta.receiptNo != null) {
                tvReceiptNumberHeader.setText(detail.receiptMeta.receiptNo);
            }
            if (tvReceiptDate != null && detail.receiptMeta.receiptDate != null) {
                tvReceiptDate.setText(formatDatePretty(detail.receiptMeta.receiptDate));
            }
            if (tvReceiptPaymentMethod != null && detail.receiptMeta.paymentMethod != null) {
                tvReceiptPaymentMethod.setText(detail.receiptMeta.paymentMethod);
            }
            if (tvReceiptRefNo != null && detail.receiptMeta.transactionReference != null) {
                tvReceiptRefNo.setText(detail.receiptMeta.transactionReference);
            }
            if (tvReceiptAmount != null) {
                tvReceiptAmount.setText(BookingRequest.formatPrice(detail.receiptMeta.amountReceived > 0 ? detail.receiptMeta.amountReceived : detail.paidAmount));
            }
        } else {
            String bNo = detail.bookingNo != null ? detail.bookingNo : "BKG-" + detail.id;
            String recNo = (detail.receipts != null && !detail.receipts.isEmpty()) ? detail.receipts.get(detail.receipts.size() - 1).receiptNo : "RCT-" + bNo.replace("BKG-", "");
            if (tvReceiptNumberHeader != null) tvReceiptNumberHeader.setText(recNo);
            if (tvReceiptAmount != null) tvReceiptAmount.setText(BookingRequest.formatPrice(detail.paidAmount));
            if (tvReceiptPaymentMethod != null) tvReceiptPaymentMethod.setText("FPX / Online Banking");
            if (tvReceiptRefNo != null) tvReceiptRefNo.setText("TOYYIB-ONLINE-" + bNo);
        }

        if (tvReceiptBookingRef != null) {
            tvReceiptBookingRef.setText(detail.bookingNo != null ? detail.bookingNo : "BKG-" + detail.id);
        }

        // Amount in Words
        if (tvReceiptAmountInWords != null) {
            String words = (detail.amountInWords != null && !detail.amountInWords.isEmpty()) ? detail.amountInWords : detail.amountInWordsMs;
            if (words != null && !words.isEmpty()) {
                tvReceiptAmountInWords.setText(words);
            } else {
                tvReceiptAmountInWords.setText(BookingRequest.formatPrice(detail.paidAmount));
            }
        }

        // Customer Info
        if (detail.customerInfo != null) {
            if (tvReceiptCustomerName != null && detail.customerInfo.fullName != null) {
                tvReceiptCustomerName.setText(detail.customerInfo.fullName);
            }
            if (tvReceiptCustomerDetails != null) {
                String phone = detail.customerInfo.mobileNumber != null ? detail.customerInfo.mobileNumber : "—";
                String addr = detail.customerInfo.address != null ? detail.customerInfo.address : "Johor Bahru, Johor";
                tvReceiptCustomerDetails.setText("Telefon: " + phone + " • " + addr);
            }
        } else if (detail.travellers != null && !detail.travellers.isEmpty()) {
            BookingDetailDto.TravellerInfo lead = detail.travellers.get(0);
            if (tvReceiptCustomerName != null) tvReceiptCustomerName.setText(lead.name);
        }

        if (tvReceiptPackageName != null) {
            tvReceiptPackageName.setText(detail.packageName != null ? detail.packageName : "Pakej Umrah");
        }

        // Passenger Details
        if (layoutReceiptPassengersContainer != null) {
            layoutReceiptPassengersContainer.removeAllViews();
            if (detail.travellers != null && !detail.travellers.isEmpty()) {
                for (int i = 0; i < detail.travellers.size(); i++) {
                    BookingDetailDto.TravellerInfo t = detail.travellers.get(i);
                    String idNo = t.identificationNo != null ? t.identificationNo : (t.passportNo != null ? t.passportNo : "—");
                    addPassengerRow(i + 1, t.name, idNo);
                }
            }
        }

        // Financial Summary
        if (tvReceiptTotalAmount != null) tvReceiptTotalAmount.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvReceiptTotalPaid != null) tvReceiptTotalPaid.setText(BookingRequest.formatPrice(detail.paidAmount));
        if (tvReceiptBalanceDue != null) tvReceiptBalanceDue.setText(BookingRequest.formatPrice(detail.balanceAmount));

        // Payment History
        if (layoutInstallmentsContainer != null && layoutInstallmentsList != null) {
            layoutInstallmentsList.removeAllViews();
            if (detail.paymentRecords != null && !detail.paymentRecords.isEmpty()) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (BookingDetailDto.PaymentRecordDto p : detail.paymentRecords) {
                    addPaymentHistoryRow(p.no, "Bayaran Sah", p.refNo, p.datePaid, p.amount, true, p.authorization, p.orNo);
                }
            } else if (detail.payments != null && !detail.payments.isEmpty()) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < detail.payments.size(); i++) {
                    BookingDetailDto.PaymentInfo pay = detail.payments.get(i);
                    int index = pay.installmentIndex > 0 ? pay.installmentIndex : (i + 1);
                    String stageLabel = resolvePaymentStageLabel(pay, index, detail);
                    addPaymentHistoryRow(index, stageLabel, pay.paymentNo, formatDatePretty(pay.paidAt), pay.amount, pay.isVerified, pay.status, pay.receiptNo);
                }
            } else {
                layoutInstallmentsContainer.setVisibility(View.GONE);
            }
        }

        if (tvReceiptOfficialNotes != null) {
            tvReceiptOfficialNotes.setText("Bayaran anda telah disahkan dan direkodkan dengan selamat dalam sistem ERP Hafiz Travel & Tours Sdn Bhd.\nResit ini adalah dokumen sah komputasi.");
        }
    }

    private void addPassengerRow(int no, String name, String idNo) {
        if (layoutReceiptPassengersContainer == null) return;

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

        layoutReceiptPassengersContainer.addView(row);
    }

    private String resolvePaymentStageLabel(BookingDetailDto.PaymentInfo pay, int index, BookingDetailDto detail) {
        if (pay != null && pay.stage != null) {
            String s = pay.stage.toLowerCase(Locale.ROOT);
            if (s.contains("initial_deposit") || s.equals("deposit")) {
                return getString(R.string.payment_stage_initial_deposit);
            } else if (s.contains("final_payment")) {
                return getString(R.string.payment_stage_final_payment);
            } else if (s.contains("full_payment")) {
                return getString(R.string.payment_stage_full_payment);
            } else if (s.contains("additional_payment") || s.contains("installment") || s.contains("balance")) {
                return getString(R.string.payment_stage_additional_payment);
            } else if (s.contains("pending")) {
                return getString(R.string.payment_stage_pending);
            }
        }
        if (pay != null && pay.stageLabel != null && !pay.stageLabel.isEmpty()) {
            return pay.stageLabel;
        }
        if (index == 1) {
            return (detail != null && detail.balanceAmount <= 0)
                    ? getString(R.string.payment_stage_full_payment)
                    : getString(R.string.payment_stage_initial_deposit);
        } else if (detail != null && detail.balanceAmount <= 0) {
            return getString(R.string.payment_stage_final_payment);
        } else {
            return getString(R.string.payment_stage_additional_payment);
        }
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

    private void addPaymentHistoryRow(int index, String stage, String paymentNo, String date, double amount, boolean isVerified, String status, String receiptNo) {
        if (layoutInstallmentsList == null) return;

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_tip_pill);
        card.setPadding(20, 16, 20, 16);

        // Header: "Payment #1 • Deposit" (left) + "RM 200.00" (right)
        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvTitle.setText(getString(R.string.payment_history_item_title, index, stage));
        tvTitle.setTextColor(Color.parseColor("#18181B"));
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(BookingRequest.formatPrice(amount));
        tvAmt.setTextColor(Color.parseColor("#059669"));
        tvAmt.setTextSize(13.5f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(tvTitle);
        header.addView(tvAmt);
        card.addView(header);

        // Sub meta: Ref No & Date
        LinearLayout meta = new LinearLayout(this);
        meta.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setPadding(0, 6, 0, 0);

        TextView tvRef = new TextView(this);
        tvRef.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        String refText = (paymentNo != null && !paymentNo.isEmpty()) ? paymentNo : (receiptNo != null ? receiptNo : "—");
        tvRef.setText(getString(R.string.payment_history_ref_format, refText));
        tvRef.setTextColor(Color.parseColor("#64748B"));
        tvRef.setTextSize(11f);

        TextView tvDate = new TextView(this);
        tvDate.setText(date != null ? date : "—");
        tvDate.setTextColor(Color.parseColor("#64748B"));
        tvDate.setTextSize(11f);

        meta.addView(tvRef);
        meta.addView(tvDate);
        card.addView(meta);

        // Status & Receipt badge row
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(0, 8, 0, 0);

        TextView tvStatus = new TextView(this);
        if (isVerified) {
            tvStatus.setText(getString(R.string.status_received));
            tvStatus.setTextColor(Color.parseColor("#059669"));
            tvStatus.setBackgroundResource(R.drawable.bg_pill_success);
        } else {
            tvStatus.setText(status != null ? status.toUpperCase(Locale.ROOT) : getString(R.string.status_pending_confirmation));
            tvStatus.setTextColor(Color.parseColor("#D97706"));
            tvStatus.setBackgroundResource(R.drawable.bg_pill_accent);
        }
        tvStatus.setTextSize(10f);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setPadding(14, 4, 14, 4);
        badgeRow.addView(tvStatus);

        if (receiptNo != null && !receiptNo.isEmpty()) {
            TextView tvRecBadge = new TextView(this);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rLp.setMarginStart(10);
            tvRecBadge.setLayoutParams(rLp);
            tvRecBadge.setText(getString(R.string.payment_history_receipt_tag, receiptNo));
            tvRecBadge.setTextColor(Color.parseColor("#475569"));
            tvRecBadge.setTextSize(10f);
            tvRecBadge.setBackgroundResource(R.drawable.bg_tip_pill);
            tvRecBadge.setPadding(12, 4, 12, 4);
            badgeRow.addView(tvRecBadge);
        }

        card.addView(badgeRow);
        layoutInstallmentsList.addView(card);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void saveOrPrintReceiptPdf() {
        if (receiptContentContainer == null) return;

        try {
            receiptContentContainer.measure(
                    View.MeasureSpec.makeMeasureSpec(receiptContentContainer.getWidth() > 0 ? receiptContentContainer.getWidth() : 1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int width = receiptContentContainer.getMeasuredWidth() > 0 ? receiptContentContainer.getMeasuredWidth() : 1080;
            int height = receiptContentContainer.getMeasuredHeight() > 0 ? receiptContentContainer.getMeasuredHeight() : 1920;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            receiptContentContainer.draw(canvas);

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(width, height, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas pageCanvas = page.getCanvas();
            pageCanvas.drawBitmap(bitmap, 0, 0, null);
            document.finishPage(page);

            String recNo = tvReceiptNumberHeader != null ? tvReceiptNumberHeader.getText().toString() : "HafizTravel";
            String fileName = "Resit_" + recNo.replace('/', '_').replace(' ', '_') + ".pdf";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();
            File pdfFile = new File(downloadsDir, fileName);

            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            Toast.makeText(this, getString(R.string.toast_receipt_saved_success, fileName), Toast.LENGTH_LONG).show();

            try {
                Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(fileUri, "application/pdf");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, getString(R.string.receipt_title_official)));
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_receipt_saved_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReceiptSummary() {
        String num = tvReceiptNumberHeader != null ? tvReceiptNumberHeader.getText().toString() : "RCT-OFFICIAL";
        String pkg = tvReceiptPackageName != null ? tvReceiptPackageName.getText().toString() : "Pakej Umrah";
        String amount = tvReceiptAmount != null ? tvReceiptAmount.getText().toString() : "RM 0";
        String bal = tvReceiptBalanceDue != null ? tvReceiptBalanceDue.getText().toString() : "RM 0";

        String shareText = "🧾 *" + getString(R.string.receipt_title_official).toUpperCase(Locale.ROOT) + "*\n" +
                "-----------------------------------\n" +
                getString(R.string.receipt_meta_no) + ": " + num + "\n" +
                "Pakej: " + pkg + "\n" +
                getString(R.string.receipt_label_amount) + ": " + amount + "\n" +
                getString(R.string.invoice_balance_due_label) + " " + bal + "\n" +
                "-----------------------------------\n" +
                getString(R.string.invoice_footer_thanks);

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.receipt_title_official)));
    }
}
