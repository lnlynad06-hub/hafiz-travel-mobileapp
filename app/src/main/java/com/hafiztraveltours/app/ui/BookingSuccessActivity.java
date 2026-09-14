package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.LocaleHelper;

public class BookingSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";
    public static final String EXTRA_BOOKING_NO = "extra_booking_no";

    private BookingRequest bookingRequest;

    private TextView txtPackageName;
    private TextView txtRoomAndPax;
    private TextView txtTotalAmount;
    private TextView txtBookingNo;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_success);

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        txtPackageName = findViewById(R.id.successPackageName);
        txtRoomAndPax = findViewById(R.id.successRoomAndPax);
        txtTotalAmount = findViewById(R.id.successTotalAmount);
        txtBookingNo = findViewById(R.id.successBookingNo);

        if (bookingRequest != null) {
            txtPackageName.setText(bookingRequest.packageName);
            txtRoomAndPax.setText(bookingRequest.roomLabel + " • " + bookingRequest.adultPaxCount + " Pax");
            txtTotalAmount.setText(bookingRequest.totalAmountFormatted);
        }

        String bookingNo = getIntent().getStringExtra(EXTRA_BOOKING_NO);
        if (bookingNo != null && !bookingNo.isEmpty() && txtBookingNo != null) {
            txtBookingNo.setText(getString(R.string.booking_no_format, bookingNo));
            txtBookingNo.setVisibility(android.view.View.VISIBLE);
        } else if (txtBookingNo != null) {
            txtBookingNo.setVisibility(android.view.View.GONE);
        }

        findViewById(R.id.btnViewMyBookings).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(this, MyBookingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnBackToHome).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
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
