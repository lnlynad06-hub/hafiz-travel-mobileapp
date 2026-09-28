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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
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

    private TextView tvReceiptStatusBadge;
    private TextView tvReceiptNumber;
    private TextView tvReceiptDate;
    private TextView tvReceiptCustomerName;
    private TextView tvReceiptBookingRef;
    private TextView tvReceiptPackageName;
    private TextView tvReceiptPaymentMethod;
    private TextView tvReceiptAmount;
    private TextView tvReceiptTotalAmount;
    private TextView tvReceiptBalanceDue;
    private LinearLayout receiptContentContainer;
    private LinearLayout layoutInstallmentsContainer;
    private LinearLayout layoutInstallmentsList;

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

        tvReceiptStatusBadge = findViewById(R.id.tvReceiptStatusBadge);
        tvReceiptNumber = findViewById(R.id.tvReceiptNumber);
        tvReceiptDate = findViewById(R.id.tvReceiptDate);
        tvReceiptCustomerName = findViewById(R.id.tvReceiptCustomerName);
        tvReceiptBookingRef = findViewById(R.id.tvReceiptBookingRef);
        tvReceiptPackageName = findViewById(R.id.tvReceiptPackageName);
        tvReceiptPaymentMethod = findViewById(R.id.tvReceiptPaymentMethod);
        tvReceiptAmount = findViewById(R.id.tvReceiptAmount);
        tvReceiptTotalAmount = findViewById(R.id.tvReceiptTotalAmount);
        tvReceiptBalanceDue = findViewById(R.id.tvReceiptBalanceDue);
        receiptContentContainer = findViewById(R.id.receiptContentContainer);
        layoutInstallmentsContainer = findViewById(R.id.layoutInstallmentsContainer);
        layoutInstallmentsList = findViewById(R.id.layoutInstallmentsList);

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
                tvReceiptBookingRef.setText(getString(R.string.receipt_label_booking_ref, bookingNo));
            }
            if (tvReceiptNumber != null) {
                tvReceiptNumber.setText("REC-" + bookingNo.replace("BKG-", ""));
            }
        }
    }

    private void populateFromBookingRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
        String recNo = "REC-DEP-" + displayNo.replace("BKG-", "");

        if (tvReceiptNumber != null) tvReceiptNumber.setText(recNo);
        if (tvReceiptBookingRef != null) {
            tvReceiptBookingRef.setText(getString(R.string.receipt_label_booking_ref, displayNo));
        }

        if (tvReceiptStatusBadge != null) {
            tvReceiptStatusBadge.setText(getString(R.string.status_received));
            tvReceiptStatusBadge.setTextColor(Color.parseColor("#059669"));
            tvReceiptStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        if (req.passengers != null && !req.passengers.isEmpty()) {
            BookingRequest.Passenger lead = req.passengers.get(0);
            if (tvReceiptCustomerName != null) {
                tvReceiptCustomerName.setText(lead.fullName != null && !lead.fullName.isEmpty() ? lead.fullName : getString(R.string.default_user_name));
            }
        }

        if (tvReceiptPackageName != null) {
            tvReceiptPackageName.setText(req.packageName != null ? req.packageName : "Pakej Umrah / Pelancongan");
        }

        if (tvReceiptPaymentMethod != null) {
            tvReceiptPaymentMethod.setText(getString(R.string.receipt_label_method, "FPX / Online Banking"));
        }

        double deposit = req.totalAmount;
        double balance = 0.0;

        if (tvReceiptAmount != null) tvReceiptAmount.setText(BookingRequest.formatPrice(deposit));
        if (tvReceiptTotalAmount != null) tvReceiptTotalAmount.setText(BookingRequest.formatPrice(req.totalAmount));
        if (tvReceiptBalanceDue != null) tvReceiptBalanceDue.setText(BookingRequest.formatPrice(balance));
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

        String bNo = detail.bookingNo != null ? detail.bookingNo : "BKG-" + detail.id;
        String recNo = "REC-" + bNo.replace("BKG-", "");

        if (detail.receipts != null && !detail.receipts.isEmpty() && detail.receipts.get(0).receiptNo != null) {
            recNo = detail.receipts.get(0).receiptNo;
        }

        if (tvReceiptNumber != null) tvReceiptNumber.setText(recNo);
        if (tvReceiptBookingRef != null) {
            tvReceiptBookingRef.setText(getString(R.string.receipt_label_booking_ref, bNo));
        }

        if (tvReceiptStatusBadge != null) {
            tvReceiptStatusBadge.setText(getString(R.string.status_received));
            tvReceiptStatusBadge.setTextColor(Color.parseColor("#059669"));
            tvReceiptStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
        }

        if (detail.travellers != null && !detail.travellers.isEmpty()) {
            BookingDetailDto.TravellerInfo lead = detail.travellers.get(0);
            if (tvReceiptCustomerName != null) {
                tvReceiptCustomerName.setText(lead.name != null ? lead.name : getString(R.string.default_user_name));
            }
        }

        if (tvReceiptPackageName != null) {
            tvReceiptPackageName.setText(detail.packageName != null ? detail.packageName : "Pakej Umrah");
        }

        String pMethod = "FPX / Online Banking";
        if (detail.receipts != null && !detail.receipts.isEmpty() && detail.receipts.get(0).paymentMethod != null) {
            pMethod = detail.receipts.get(0).paymentMethod;
        } else if (detail.payments != null && !detail.payments.isEmpty() && detail.payments.get(0).method != null) {
            pMethod = detail.payments.get(0).method;
        }
        if (tvReceiptPaymentMethod != null) {
            tvReceiptPaymentMethod.setText(getString(R.string.receipt_label_method, pMethod));
        }

        double paidAmt = detail.paidAmount;
        if (tvReceiptAmount != null) tvReceiptAmount.setText(BookingRequest.formatPrice(paidAmt));
        if (tvReceiptTotalAmount != null) tvReceiptTotalAmount.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvReceiptBalanceDue != null) tvReceiptBalanceDue.setText(BookingRequest.formatPrice(detail.balanceAmount));

        // Payment History: each successful / traceable installment
        if (layoutInstallmentsContainer != null && layoutInstallmentsList != null) {
            layoutInstallmentsList.removeAllViews();
            if (detail.payments != null && !detail.payments.isEmpty()) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < detail.payments.size(); i++) {
                    BookingDetailDto.PaymentInfo pay = detail.payments.get(i);
                    int index = pay.installmentIndex > 0 ? pay.installmentIndex : (i + 1);
                    String stageLabel = pay.stageLabel != null && !pay.stageLabel.isEmpty()
                            ? pay.stageLabel
                            : (index == 1 ? getString(R.string.booking_label_stage_deposit) : getString(R.string.booking_label_stage_balance));
                    addPaymentHistoryRow(index, stageLabel, pay.paymentNo, pay.paidAt, pay.amount, pay.isVerified, pay.status, pay.receiptNo);
                }
            } else if (detail.receipts != null && !detail.receipts.isEmpty()) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < detail.receipts.size(); i++) {
                    BookingDetailDto.ReceiptInfo rec = detail.receipts.get(i);
                    String itemTitle = (i == 0) ? getString(R.string.receipt_deposit_title) : getString(R.string.receipt_installment_title, i + 1);
                    addPaymentHistoryRow(i + 1, itemTitle, rec.receiptNo, rec.receiptDate, rec.amount, true, "verified", rec.receiptNo);
                }
            } else {
                layoutInstallmentsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void addPaymentHistoryRow(int index, String stage, String paymentNo, String date, double amount, boolean isVerified, String status, String receiptNo) {
        if (layoutInstallmentsList == null) return;

        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_tip_pill);
        card.setPadding(24, 20, 24, 20);

        // Header: "Payment #1 • Deposit" (left) + "RM 200.00" (right)
        LinearLayout header = new LinearLayout(this);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        tvTitle.setText(getString(R.string.payment_history_item_title, index, stage));
        tvTitle.setTextColor(Color.parseColor("#18181B"));
        tvTitle.setTextSize(13f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(BookingRequest.formatPrice(amount));
        tvAmt.setTextColor(getResources().getColor(R.color.brand_magenta));
        tvAmt.setTextSize(14f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        header.addView(tvTitle);
        header.addView(tvAmt);
        card.addView(header);

        // Sub meta: Ref No & Date
        LinearLayout meta = new LinearLayout(this);
        meta.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setPadding(0, 8, 0, 0);

        TextView tvRef = new TextView(this);
        tvRef.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        String refText = (paymentNo != null && !paymentNo.isEmpty()) ? paymentNo : (receiptNo != null ? receiptNo : "—");
        tvRef.setText(getString(R.string.payment_history_ref_format, refText));
        tvRef.setTextColor(Color.parseColor("#71717A"));
        tvRef.setTextSize(11f);

        TextView tvDate = new TextView(this);
        tvDate.setText(date != null ? date : "—");
        tvDate.setTextColor(Color.parseColor("#71717A"));
        tvDate.setTextSize(11f);

        meta.addView(tvRef);
        meta.addView(tvDate);
        card.addView(meta);

        // Status & Receipt badge row
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        badgeRow.setPadding(0, 10, 0, 0);

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
        tvStatus.setPadding(16, 6, 16, 6);
        badgeRow.addView(tvStatus);

        if (receiptNo != null && !receiptNo.isEmpty()) {
            TextView tvRecBadge = new TextView(this);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rLp.setMarginStart(12);
            tvRecBadge.setLayoutParams(rLp);
            tvRecBadge.setText(getString(R.string.payment_history_receipt_tag, receiptNo));
            tvRecBadge.setTextColor(Color.parseColor("#4B5563"));
            tvRecBadge.setTextSize(10.5f);
            tvRecBadge.setBackgroundResource(R.drawable.bg_tip_pill);
            tvRecBadge.setPadding(14, 6, 14, 6);
            badgeRow.addView(tvRecBadge);
        }

        card.addView(badgeRow);
        layoutInstallmentsList.addView(card);
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

            String recNo = tvReceiptNumber != null ? tvReceiptNumber.getText().toString() : "HafizTravel";
            String fileName = "Resit_" + recNo.replace('/', '_') + ".pdf";
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
        String num = tvReceiptNumber != null ? tvReceiptNumber.getText().toString() : "REC-OFFICIAL";
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
