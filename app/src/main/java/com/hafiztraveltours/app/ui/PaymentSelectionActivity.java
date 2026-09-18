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
import com.hafiztraveltours.app.models.CreateBookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.utils.LocaleHelper;

public class PaymentSelectionActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;

    private TextView txtTotalAmount;
    private TextView txtPackageSummary;
    private RadioGroup radioGroupPaymentType;
    private RadioGroup radioGroupPaymentMethod;
    private View btnConfirmAndPay;
    private ProgressBar progressBar;

    private boolean isSubmitting = false;
    private retrofit2.Call<?> bookingCall;

    @Override
    protected void onDestroy() {
        // If the user backs out mid-flight, stop waiting on the request.
        // (Server-side idempotency is a backend item; this only guards the client.)
        if (isSubmitting && bookingCall != null) bookingCall.cancel();
        super.onDestroy();
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
        txtPackageSummary.setText(getString(R.string.payment_summary_format, bookingRequest.packageName, bookingRequest.roomLabel, bookingRequest.adultPaxCount));

        btnConfirmAndPay.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            if (!isSubmitting) {
                processBookingSubmission();
            }
        });
    }

    private void processBookingSubmission() {
        isSubmitting = true;
        btnConfirmAndPay.setEnabled(false);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        CreateBookingRequest apiRequest = buildApiRequest();

        if (bookingCall != null) bookingCall.cancel();
        retrofit2.Call<ApiResponse<BookingDetailDto>> createCall =
                ApiClient.getApiService().createBooking(apiRequest);
        bookingCall = createCall;
        createCall.enqueue(new retrofit2.Callback<ApiResponse<BookingDetailDto>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<BookingDetailDto>> call,
                                   retrofit2.Response<ApiResponse<BookingDetailDto>> response) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                btnConfirmAndPay.setEnabled(true);
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                BookingDetailDto created = (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) ? response.body().data : null;
                if (created == null) {
                    Toast.makeText(PaymentSelectionActivity.this,
                            com.hafiztraveltours.app.network.ApiErrors.userMessage(PaymentSelectionActivity.this, response, R.string.booking_failed), Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(PaymentSelectionActivity.this, BookingSuccessActivity.class);
                intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_NO, created.bookingNo);
                startActivity(intent);
                // Leave the stack: back from Success goes Home, so a stale Payment
                // screen can never resubmit the same booking.
                finish();
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                btnConfirmAndPay.setEnabled(true);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(PaymentSelectionActivity.this,
                        com.hafiztraveltours.app.network.ApiErrors.userMessage(PaymentSelectionActivity.this, t, R.string.booking_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Parses the raw `departures[].id` carried through the booking flow.
     * Returns null when missing/non-numeric so Gson omits `departure_id`
     * instead of sending a wrong value (previous behaviour: always null).
     */
    private static Integer parseDepartureId(String rawId) {
        if (rawId == null) return null;
        String clean = rawId.trim();
        if (clean.isEmpty()) return null;
        try {
            return Integer.valueOf(clean);
        } catch (NumberFormatException e) {
            android.util.Log.w("PaymentSelection", "Non-numeric departure id: " + clean);
            return null;
        }
    }

    private CreateBookingRequest buildApiRequest() {
        CreateBookingRequest apiRequest = new CreateBookingRequest();
        apiRequest.packageId = bookingRequest.packageId;
        apiRequest.roomLabel = bookingRequest.roomLabel;
        // C1: send the selected departure's real backend ID (never the display label).
        apiRequest.departureId = parseDepartureId(bookingRequest.selectedDepartureId);
        int actualTravellers = (bookingRequest.passengers != null && !bookingRequest.passengers.isEmpty())
                ? bookingRequest.passengers.size() : bookingRequest.adultPaxCount;
        apiRequest.adultCount = actualTravellers;
        apiRequest.childCount = 0;
        apiRequest.unitPrice = bookingRequest.unitPriceAmount;
        apiRequest.discountAmount = bookingRequest.discountAmount;
        apiRequest.promoCode = bookingRequest.promoCode;
        apiRequest.paymentType = radioGroupPaymentType.getCheckedRadioButtonId() == R.id.radioDepositPayment
                ? "deposit" : "full";
        int methodId = radioGroupPaymentMethod.getCheckedRadioButtonId();
        if (methodId == R.id.radioCardPayment) {
            apiRequest.paymentMethod = "card";
        } else if (methodId == R.id.radioBankTransfer) {
            apiRequest.paymentMethod = "bank_transfer";
        } else {
            apiRequest.paymentMethod = "fpx";
        }
        if (bookingRequest.passengers != null) {
            for (int i = 0; i < bookingRequest.passengers.size(); i++) {
                BookingRequest.Passenger p = bookingRequest.passengers.get(i);
                apiRequest.travellers.add(
                        com.hafiztraveltours.app.utils.TravellerMapper.toTravellerRequest(p, i == 0));
            }
        }
        return apiRequest;
    }
}
