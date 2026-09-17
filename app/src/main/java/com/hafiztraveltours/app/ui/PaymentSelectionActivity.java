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

        CreateBookingRequest apiRequest = buildApiRequest();

        ApiClient.getApiService().createBooking(apiRequest).enqueue(new retrofit2.Callback<ApiResponse<BookingDetailDto>>() {
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
                            getString(R.string.booking_failed), Toast.LENGTH_LONG).show();
                    return;
                }

                Intent intent = new Intent(PaymentSelectionActivity.this, BookingSuccessActivity.class);
                intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_NO, created.bookingNo);
                startActivity(intent);
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                btnConfirmAndPay.setEnabled(true);
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(PaymentSelectionActivity.this,
                        getString(R.string.booking_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    private CreateBookingRequest buildApiRequest() {
        CreateBookingRequest apiRequest = new CreateBookingRequest();
        apiRequest.packageId = bookingRequest.packageId;
        apiRequest.roomLabel = bookingRequest.roomLabel;
        apiRequest.adultCount = bookingRequest.adultPaxCount;
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
                CreateBookingRequest.TravellerRequest tr = new CreateBookingRequest.TravellerRequest();
                tr.title = p.title;
                tr.fullName = p.fullName;
                tr.icNumber = p.icNumber;
                tr.passportNumber = p.passportNumber;
                tr.passportExpiryDate = p.passportExpiryDate;
                tr.issuingCountry = p.issuingCountry;
                tr.gender = p.gender;
                tr.dateOfBirth = p.dateOfBirth;
                tr.nationality = p.nationality;
                tr.clothesSize = p.clothesSize;
                tr.mahramIndex = p.mahramIndex;
                tr.relationship = p.relationship;
                tr.icPassport = p.icPassportNumber;
                tr.phone = p.phoneNumber;
                tr.email = p.email;
                tr.isLead = p.isLead || (i == 0);
                apiRequest.travellers.add(tr);
            }
        }
        return apiRequest;
    }
}
