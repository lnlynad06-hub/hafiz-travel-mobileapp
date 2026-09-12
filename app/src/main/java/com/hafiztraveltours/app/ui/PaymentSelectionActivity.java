package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.LocaleHelper;

public class PaymentSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;

    private TextView txtTotalAmount;
    private TextView txtPackageSummary;
    private RadioGroup radioGroupPaymentType;
    private RadioGroup radioGroupPaymentMethod;
    private View btnConfirmAndPay;
    private ProgressBar progressBar;

    private boolean isSubmitting = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_selection);

        findViewById(R.id.paymentBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        if (bookingRequest == null) {
            finish();
            return;
        }

        txtTotalAmount = findViewById(R.id.paymentTotalAmountText);
        txtPackageSummary = findViewById(R.id.paymentPackageSummaryText);
        radioGroupPaymentType = findViewById(R.id.radioGroupPaymentType);
        radioGroupPaymentMethod = findViewById(R.id.radioGroupPaymentMethod);
        btnConfirmAndPay = findViewById(R.id.btnConfirmAndPay);
        progressBar = findViewById(R.id.paymentProgressBar);

        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);
        txtPackageSummary.setText(bookingRequest.packageName + " • " + bookingRequest.roomLabel + " • " + bookingRequest.adultPaxCount + " Pax");

        btnConfirmAndPay.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (!isSubmitting) {
                processBookingSubmission();
            }
        });
    }

    private void processBookingSubmission() {
        isSubmitting = true;
        btnConfirmAndPay.setEnabled(false);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // Simulated submission guard until backend endpoint POST /api/v1/bookings is connected
        btnConfirmAndPay.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            isSubmitting = false;
            btnConfirmAndPay.setEnabled(true);
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            Intent intent = new Intent(this, BookingSuccessActivity.class);
            intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
            startActivity(intent);
        }, 1200);
    }
}
