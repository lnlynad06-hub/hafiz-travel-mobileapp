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

    private TextView tvInvoiceStatusBadge;
    private TextView tvInvoiceNumber;
    private TextView tvInvoiceDate;
    private TextView tvInvoiceCustomerName;
    private TextView tvInvoiceCustomerDetails;
    private TextView tvInvoicePackageName;
    private TextView tvInvoiceTripMeta;
    private LinearLayout layoutInvoiceItems;
    private TextView tvInvoiceSubtotal;
    private TextView tvInvoiceGrandTotal;
    private TextView tvInvoicePaidAmount;
    private TextView tvInvoiceBalanceDue;
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

        tvInvoiceStatusBadge = findViewById(R.id.tvInvoiceStatusBadge);
        tvInvoiceNumber = findViewById(R.id.tvInvoiceNumber);
        tvInvoiceDate = findViewById(R.id.tvInvoiceDate);
        tvInvoiceCustomerName = findViewById(R.id.tvInvoiceCustomerName);
        tvInvoiceCustomerDetails = findViewById(R.id.tvInvoiceCustomerDetails);
        tvInvoicePackageName = findViewById(R.id.tvInvoicePackageName);
        tvInvoiceTripMeta = findViewById(R.id.tvInvoiceTripMeta);
        layoutInvoiceItems = findViewById(R.id.layoutInvoiceItems);
        tvInvoiceSubtotal = findViewById(R.id.tvInvoiceSubtotal);
        tvInvoiceGrandTotal = findViewById(R.id.tvInvoiceGrandTotal);
        tvInvoicePaidAmount = findViewById(R.id.tvInvoicePaidAmount);
        tvInvoiceBalanceDue = findViewById(R.id.tvInvoiceBalanceDue);
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
            if (tvInvoiceNumber != null) tvInvoiceNumber.setText(bookingNo);
        }
    }

    private void populateFromBookingRequest(BookingRequest req, String bNo) {
        if (req == null) return;

        String displayNo = (bNo != null && !bNo.isEmpty()) ? bNo : "BKG-OFFICIAL";
        if (tvInvoiceNumber != null) tvInvoiceNumber.setText(displayNo);

        if (tvInvoiceStatusBadge != null) {
            tvInvoiceStatusBadge.setText(getString(R.string.status_deposit_paid));
            tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
            tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
        }

        // Customer
        if (req.passengers != null && !req.passengers.isEmpty()) {
            BookingRequest.Passenger lead = req.passengers.get(0);
            if (tvInvoiceCustomerName != null) {
                tvInvoiceCustomerName.setText(lead.fullName != null && !lead.fullName.isEmpty() ? lead.fullName : getString(R.string.default_user_name));
            }
            if (tvInvoiceCustomerDetails != null) {
                String idStr = (lead.icPassportNumber != null && !lead.icPassportNumber.isEmpty()) ? lead.icPassportNumber : "—";
                String phoneStr = (lead.phoneNumber != null && !lead.phoneNumber.isEmpty()) ? lead.phoneNumber : "—";
                tvInvoiceCustomerDetails.setText(getString(R.string.summary_id_label_format, idStr) + " • " + getString(R.string.summary_phone_label_format, phoneStr));
            }
        }

        // Package & Trip Meta
        if (tvInvoicePackageName != null) {
            tvInvoicePackageName.setText(req.packageName != null ? req.packageName : "Pakej Umrah / Pelancongan");
        }
        if (tvInvoiceTripMeta != null) {
            String dep = (req.selectedDepartureDate != null && !req.selectedDepartureDate.isEmpty())
                    ? req.selectedDepartureDate : "—";
            String room = (req.roomLabel != null && !req.roomLabel.isEmpty()) ? req.roomLabel : "Standard";
            tvInvoiceTripMeta.setText(getString(R.string.summary_departure_format, dep) + " • " + room + " • " + getString(R.string.pax_count_format, req.adultPaxCount));
        }

        // Line Items
        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            addLineItem(req.packageName + " (" + req.roomLabel + ")",
                    getString(R.string.pax_count_format, req.adultPaxCount),
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

        if (tvInvoiceNumber != null) {
            String inv = (detail.invoices != null && !detail.invoices.isEmpty() && detail.invoices.get(0).invoiceNo != null)
                    ? detail.invoices.get(0).invoiceNo : detail.bookingNo;
            tvInvoiceNumber.setText(inv != null ? inv : "BKG-" + detail.id);
        }

        if (tvInvoiceStatusBadge != null) {
            String rawStatus = detail.status != null ? detail.status.toLowerCase(Locale.ROOT) : "";
            if (rawStatus.contains("confirm") || rawStatus.contains("active") || rawStatus.contains("sah")) {
                tvInvoiceStatusBadge.setText(getString(R.string.status_confirmed));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#059669"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_success);
            } else if (rawStatus.contains("cancel") || rawStatus.contains("batal")) {
                tvInvoiceStatusBadge.setText(getString(R.string.status_cancelled));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#DC2626"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_inactive);
            } else {
                tvInvoiceStatusBadge.setText(getString(R.string.status_pending_confirmation));
                tvInvoiceStatusBadge.setTextColor(Color.parseColor("#D97706"));
                tvInvoiceStatusBadge.setBackgroundResource(R.drawable.bg_pill_accent);
            }
        }

        if (tvInvoicePackageName != null) {
            tvInvoicePackageName.setText(detail.packageName != null ? detail.packageName : "Pakej Umrah");
        }

        if (detail.travellers != null && !detail.travellers.isEmpty()) {
            BookingDetailDto.TravellerInfo lead = detail.travellers.get(0);
            if (tvInvoiceCustomerName != null) tvInvoiceCustomerName.setText(lead.name != null ? lead.name : getString(R.string.default_user_name));
            if (tvInvoiceCustomerDetails != null) {
                String idNo = lead.identificationNo != null ? lead.identificationNo : (lead.passportNo != null ? lead.passportNo : "—");
                tvInvoiceCustomerDetails.setText(getString(R.string.summary_id_label_format, idNo));
            }
        }

        if (tvInvoiceTripMeta != null) {
            String depDate = (detail.departure != null && detail.departure.departureDate != null)
                    ? detail.departure.departureDate : (detail.departureDate != null ? detail.departureDate : "—");
            int pax = (detail.travellers != null && !detail.travellers.isEmpty()) ? detail.travellers.size() : 1;
            tvInvoiceTripMeta.setText(getString(R.string.summary_departure_format, depDate) + " • " + getString(R.string.pax_count_format, pax));
        }

        if (layoutInvoiceItems != null) {
            layoutInvoiceItems.removeAllViews();
            if (detail.items != null && !detail.items.isEmpty()) {
                for (BookingDetailDto.ItemInfo item : detail.items) {
                    addLineItem(item.description != null ? item.description : detail.packageName,
                            getString(R.string.pax_count_format, item.quantity),
                            BookingRequest.formatPrice(item.total));
                }
            } else {
                addLineItem(detail.packageName != null ? detail.packageName : "Pakej Pelancongan",
                        getString(R.string.pax_count_format, 1),
                        BookingRequest.formatPrice(detail.totalAmount));
            }
        }

        if (tvInvoiceSubtotal != null) tvInvoiceSubtotal.setText(BookingRequest.formatPrice(detail.subtotal > 0 ? detail.subtotal : detail.totalAmount));
        if (tvInvoiceGrandTotal != null) tvInvoiceGrandTotal.setText(BookingRequest.formatPrice(detail.totalAmount));
        if (tvInvoicePaidAmount != null) tvInvoicePaidAmount.setText(BookingRequest.formatPrice(detail.paidAmount));
        if (tvInvoiceBalanceDue != null) tvInvoiceBalanceDue.setText(BookingRequest.formatPrice(detail.balanceAmount));
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
        tvQty.setGravity(android.view.Gravity.CENTER);

        TextView tvTotal = new TextView(this);
        tvTotal.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
        tvTotal.setText(total);
        tvTotal.setTextColor(Color.parseColor("#18181B"));
        tvTotal.setTextSize(12f);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setGravity(android.view.Gravity.END);

        row.addView(tvDesc);
        row.addView(tvQty);
        row.addView(tvTotal);

        layoutInvoiceItems.addView(row);
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

            String fileName = "Invois_" + (tvInvoiceNumber != null ? tvInvoiceNumber.getText().toString().replace('/', '_') : "HafizTravel") + ".pdf";
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
        String num = tvInvoiceNumber != null ? tvInvoiceNumber.getText().toString() : "BKG-OFFICIAL";
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
}
