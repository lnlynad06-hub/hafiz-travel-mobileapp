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

        double deposit = (req.totalAmount > 0) ? (req.totalAmount >= 2000 ? 1000.0 * req.adultPaxCount : req.totalAmount) : 1000.0 * req.adultPaxCount;
        double balance = Math.max(0, req.totalAmount - deposit);

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

        double paidAmt = detail.paidAmount > 0 ? detail.paidAmount : (detail.totalAmount >= 2000 ? 1000.0 : detail.totalAmount);
        if (tvReceiptAmount != null) tvReceiptAmount.setText(BookingRequest.formatPrice(paidAmt));
        if (tvReceiptTotalAmount != null) tvReceiptTotalAmount.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvReceiptBalanceDue != null) tvReceiptBalanceDue.setText(BookingRequest.formatPrice(detail.balanceAmount));

        // If multiple receipts or payments exist, show them
        if (layoutInstallmentsContainer != null && layoutInstallmentsList != null) {
            layoutInstallmentsList.removeAllViews();
            if (detail.receipts != null && detail.receipts.size() > 1) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < detail.receipts.size(); i++) {
                    BookingDetailDto.ReceiptInfo rec = detail.receipts.get(i);
                    String itemTitle = (i == 0) ? getString(R.string.receipt_deposit_title) : getString(R.string.receipt_installment_title, i);
                    addInstallmentRow(itemTitle, rec.receiptNo, rec.receiptDate, rec.amount);
                }
            } else if (detail.payments != null && detail.payments.size() > 1) {
                layoutInstallmentsContainer.setVisibility(View.VISIBLE);
                for (int i = 0; i < detail.payments.size(); i++) {
                    BookingDetailDto.PaymentInfo pay = detail.payments.get(i);
                    String itemTitle = (i == 0) ? getString(R.string.receipt_deposit_title) : getString(R.string.receipt_installment_title, i);
                    addInstallmentRow(itemTitle, pay.paymentNo, pay.paidAt, pay.amount);
                }
            } else {
                layoutInstallmentsContainer.setVisibility(View.GONE);
            }
        }
    }

    private void addInstallmentRow(String title, String no, String date, double amount) {
        if (layoutInstallmentsList == null) return;

        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(this);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        left.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title + " (" + (no != null ? no : "—") + ")");
        tvTitle.setTextColor(Color.parseColor("#18181B"));
        tvTitle.setTextSize(12f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText(date != null ? date : "—");
        tvDate.setTextColor(Color.parseColor("#71717A"));
        tvDate.setTextSize(11f);

        left.addView(tvTitle);
        left.addView(tvDate);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(BookingRequest.formatPrice(amount));
        tvAmt.setTextColor(getResources().getColor(R.color.brand_magenta));
        tvAmt.setTextSize(13f);
        tvAmt.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(left);
        row.addView(tvAmt);

        layoutInstallmentsList.addView(row);
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
