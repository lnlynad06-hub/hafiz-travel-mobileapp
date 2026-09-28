package com.hafiztraveltours.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.utils.HapticUtil;

public class BookingSuccessActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";
    public static final String EXTRA_BOOKING_NO = "extra_booking_no";

    private BookingRequest bookingRequest;
    private String bookingNo;

    private TextView txtPackageName;
    private TextView txtRoomAndPax;
    private TextView txtTotalAmount;
    private TextView txtBookingNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_success);

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);
        bookingNo = getIntent().getStringExtra(EXTRA_BOOKING_NO);

        txtPackageName = findViewById(R.id.successPackageName);
        txtRoomAndPax = findViewById(R.id.successRoomAndPax);
        txtTotalAmount = findViewById(R.id.successTotalAmount);
        txtBookingNo = findViewById(R.id.successBookingNo);

        if (bookingRequest != null) {
            if (txtPackageName != null) {
                txtPackageName.setText(bookingRequest.packageName);
            }
            if (txtRoomAndPax != null) {
                String departure = (bookingRequest.selectedDepartureDate != null && !bookingRequest.selectedDepartureDate.trim().isEmpty())
                        ? " • " + bookingRequest.selectedDepartureDate.trim() : "";
                txtRoomAndPax.setText(getString(R.string.passenger_room_pax_format, bookingRequest.roomLabel, bookingRequest.adultPaxCount) + departure);
            }
            if (txtTotalAmount != null) {
                txtTotalAmount.setText(bookingRequest.totalAmountFormatted);
            }
        }

        if (bookingNo != null && !bookingNo.isEmpty() && txtBookingNo != null) {
            txtBookingNo.setText(bookingNo);
        }

        // Copy Booking Number Button
        View btnCopy = findViewById(R.id.btnCopyBookingNo);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                HapticUtil.click(v);
                String copyTarget = bookingNo != null ? bookingNo : (txtBookingNo != null ? txtBookingNo.getText().toString() : "");
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && !copyTarget.isEmpty()) {
                    ClipData clip = ClipData.newPlainText("Booking Number", copyTarget);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, getString(R.string.booking_success_copied_toast, copyTarget), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // View / Download Invoice Button (In-App)
        View btnInvoice = findViewById(R.id.btnDownloadInvoice);
        if (btnInvoice != null) {
            btnInvoice.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent invoiceIntent = new Intent(this, InvoiceViewerActivity.class);
                invoiceIntent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_NO, bookingNo);
                invoiceIntent.putExtra(InvoiceViewerActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                startActivity(invoiceIntent);
            });
        }

        // View / Download Receipt Button (In-App)
        View btnReceipt = findViewById(R.id.btnViewReceipt);
        if (btnReceipt != null) {
            btnReceipt.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent receiptIntent = new Intent(this, ReceiptViewerActivity.class);
                receiptIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_NO, bookingNo);
                receiptIntent.putExtra(ReceiptViewerActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                startActivity(receiptIntent);
            });
        }

        // Manage Documents / My Bookings Button
        View btnManageDocs = findViewById(R.id.btnManageDocuments);
        if (btnManageDocs != null) {
            btnManageDocs.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(this, MyBookingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // Back to Home Button
        View btnHome = findViewById(R.id.btnBackToHome);
        if (btnHome != null) {
            btnHome.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
