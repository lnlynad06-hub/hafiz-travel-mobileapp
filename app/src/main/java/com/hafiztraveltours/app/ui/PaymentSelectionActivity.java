package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CreateBookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.util.Locale;

public class PaymentSelectionActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;

    private TextView txtTotalAmount;
    private TextView txtPackageSummary;

    // Payment Type Option Cards
    private View cardFullPayment;
    private TextView tvFullPaymentTitle;
    private TextView tvFullPaymentSub;
    private ImageView ivCheckFullPayment;

    private View cardDepositPayment;
    private TextView tvDepositPaymentTitle;
    private TextView tvDepositPaymentSub;
    private ImageView ivCheckDepositPayment;

    // Payment Channel Option Cards
    private View cardChannelFpx;
    private TextView tvChannelFpxTitle;
    private ImageView ivCheckChannelFpx;

    private View cardChannelCard;
    private TextView tvChannelCardTitle;
    private ImageView ivCheckChannelCard;

    private View cardChannelBank;
    private TextView tvChannelBankTitle;
    private ImageView ivCheckChannelBank;

    private View btnConfirmAndPay;
    private TextView txtPayButtonLabel;
    private ProgressBar progressBar;

    private String selectedPaymentType = "full"; // "full" | "deposit"
    private String selectedPaymentMethod = "fpx"; // "fpx" | "card" | "bank_transfer"

    private double depositAmount = 0.0;
    private boolean isSubmitting = false;
    private retrofit2.Call<?> bookingCall;

    @Override
    protected void onDestroy() {
        if (isSubmitting && bookingCall != null) bookingCall.cancel();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_selection);

        findViewById(R.id.paymentBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        if (bookingRequest == null || !bookingRequest.termsAgreed) {
            Toast.makeText(this, R.string.terms_required_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtTotalAmount = findViewById(R.id.paymentTotalAmountText);
        txtPackageSummary = findViewById(R.id.paymentPackageSummaryText);

        cardFullPayment = findViewById(R.id.cardFullPayment);
        tvFullPaymentTitle = findViewById(R.id.tvFullPaymentTitle);
        tvFullPaymentSub = findViewById(R.id.tvFullPaymentSub);
        ivCheckFullPayment = findViewById(R.id.ivCheckFullPayment);

        cardDepositPayment = findViewById(R.id.cardDepositPayment);
        tvDepositPaymentTitle = findViewById(R.id.tvDepositPaymentTitle);
        tvDepositPaymentSub = findViewById(R.id.tvDepositPaymentSub);
        ivCheckDepositPayment = findViewById(R.id.ivCheckDepositPayment);

        cardChannelFpx = findViewById(R.id.cardChannelFpx);
        tvChannelFpxTitle = findViewById(R.id.tvChannelFpxTitle);
        ivCheckChannelFpx = findViewById(R.id.ivCheckChannelFpx);

        cardChannelCard = findViewById(R.id.cardChannelCard);
        tvChannelCardTitle = findViewById(R.id.tvChannelCardTitle);
        ivCheckChannelCard = findViewById(R.id.ivCheckChannelCard);

        cardChannelBank = findViewById(R.id.cardChannelBank);
        tvChannelBankTitle = findViewById(R.id.tvChannelBankTitle);
        ivCheckChannelBank = findViewById(R.id.ivCheckChannelBank);

        btnConfirmAndPay = findViewById(R.id.btnConfirmAndPay);
        txtPayButtonLabel = findViewById(R.id.txtPayButtonLabel);
        progressBar = findViewById(R.id.paymentProgressBar);

        int actualPax = (bookingRequest.passengers != null && !bookingRequest.passengers.isEmpty())
                ? bookingRequest.passengers.size() : bookingRequest.adultPaxCount;
        depositAmount = Math.min(bookingRequest.totalAmount, Math.max(1000.0, 1000.0 * actualPax));

        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);
        txtPackageSummary.setText(getString(R.string.payment_summary_format, bookingRequest.packageName, bookingRequest.roomLabel, actualPax));

        if (tvFullPaymentSub != null) {
            tvFullPaymentSub.setText(getString(R.string.pay_full) + " • " + bookingRequest.totalAmountFormatted);
        }
        if (tvDepositPaymentSub != null) {
            double remainingBalance = Math.max(0, bookingRequest.totalAmount - depositAmount);
            tvDepositPaymentSub.setText(String.format(Locale.US, "RM 1,000 / jemaah • Jumlah Deposit %s (Baki %s)",
                    BookingRequest.formatPrice(depositAmount), BookingRequest.formatPrice(remainingBalance)));
        }

        // Wire up Payment Type card selection
        if (cardFullPayment != null) {
            cardFullPayment.setOnClickListener(v -> {
                HapticUtil.click(v);
                selectedPaymentType = "full";
                updatePaymentTypeUI();
            });
        }

        if (cardDepositPayment != null) {
            cardDepositPayment.setOnClickListener(v -> {
                HapticUtil.click(v);
                selectedPaymentType = "deposit";
                updatePaymentTypeUI();
            });
        }

        // Wire up Payment Channel card selection
        if (cardChannelFpx != null) {
            cardChannelFpx.setOnClickListener(v -> {
                HapticUtil.click(v);
                selectedPaymentMethod = "fpx";
                updatePaymentChannelUI();
            });
        }

        if (cardChannelCard != null) {
            cardChannelCard.setOnClickListener(v -> {
                HapticUtil.click(v);
                selectedPaymentMethod = "card";
                updatePaymentChannelUI();
            });
        }

        if (cardChannelBank != null) {
            cardChannelBank.setOnClickListener(v -> {
                HapticUtil.click(v);
                selectedPaymentMethod = "bank_transfer";
                updatePaymentChannelUI();
            });
        }

        updatePaymentTypeUI();
        updatePaymentChannelUI();

        btnConfirmAndPay.setOnClickListener(v -> {
            HapticUtil.click(v);
            if (!isSubmitting) {
                processBookingSubmission();
            }
        });
    }

    private void updatePaymentTypeUI() {
        boolean isFull = "full".equalsIgnoreCase(selectedPaymentType);

        if (cardFullPayment != null) {
            cardFullPayment.setBackgroundResource(isFull ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        }
        if (tvFullPaymentTitle != null) {
            tvFullPaymentTitle.setTextColor(ContextCompat.getColor(this, isFull ? R.color.pink_dark : R.color.text_dark));
            tvFullPaymentTitle.setTypeface(null, isFull ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivCheckFullPayment != null) {
            ivCheckFullPayment.setImageResource(isFull ? R.drawable.ic_check_circle_magenta : R.drawable.ic_circle_unselected);
        }

        if (cardDepositPayment != null) {
            cardDepositPayment.setBackgroundResource(!isFull ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        }
        if (tvDepositPaymentTitle != null) {
            tvDepositPaymentTitle.setTextColor(ContextCompat.getColor(this, !isFull ? R.color.pink_dark : R.color.text_dark));
            tvDepositPaymentTitle.setTypeface(null, !isFull ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivCheckDepositPayment != null) {
            ivCheckDepositPayment.setImageResource(!isFull ? R.drawable.ic_check_circle_magenta : R.drawable.ic_circle_unselected);
        }

        // Update CTA Button Label
        if (txtPayButtonLabel != null) {
            if (isFull) {
                txtPayButtonLabel.setText(getString(R.string.pay_confirm) + " (" + bookingRequest.totalAmountFormatted + ")");
            } else {
                txtPayButtonLabel.setText(getString(R.string.pay_deposit_cta, BookingRequest.formatPrice(depositAmount)));
            }
        }
    }

    private void updatePaymentChannelUI() {
        boolean isFpx = "fpx".equalsIgnoreCase(selectedPaymentMethod);
        boolean isCard = "card".equalsIgnoreCase(selectedPaymentMethod);
        boolean isBank = "bank_transfer".equalsIgnoreCase(selectedPaymentMethod);

        if (cardChannelFpx != null) {
            cardChannelFpx.setBackgroundResource(isFpx ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        }
        if (tvChannelFpxTitle != null) {
            tvChannelFpxTitle.setTextColor(ContextCompat.getColor(this, isFpx ? R.color.pink_dark : R.color.text_dark));
            tvChannelFpxTitle.setTypeface(null, isFpx ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivCheckChannelFpx != null) {
            ivCheckChannelFpx.setImageResource(isFpx ? R.drawable.ic_check_circle_magenta : R.drawable.ic_circle_unselected);
        }

        if (cardChannelCard != null) {
            cardChannelCard.setBackgroundResource(isCard ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        }
        if (tvChannelCardTitle != null) {
            tvChannelCardTitle.setTextColor(ContextCompat.getColor(this, isCard ? R.color.pink_dark : R.color.text_dark));
            tvChannelCardTitle.setTypeface(null, isCard ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivCheckChannelCard != null) {
            ivCheckChannelCard.setImageResource(isCard ? R.drawable.ic_check_circle_magenta : R.drawable.ic_circle_unselected);
        }

        if (cardChannelBank != null) {
            cardChannelBank.setBackgroundResource(isBank ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        }
        if (tvChannelBankTitle != null) {
            tvChannelBankTitle.setTextColor(ContextCompat.getColor(this, isBank ? R.color.pink_dark : R.color.text_dark));
            tvChannelBankTitle.setTypeface(null, isBank ? Typeface.BOLD : Typeface.NORMAL);
        }
        if (ivCheckChannelBank != null) {
            ivCheckChannelBank.setImageResource(isBank ? R.drawable.ic_check_circle_magenta : R.drawable.ic_circle_unselected);
        }
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

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingDetailDto created = response.body().data;
                    String bookingNo = (created != null && created.bookingNo != null && !created.bookingNo.isEmpty())
                            ? created.bookingNo
                            : "HTB-" + (System.currentTimeMillis() % 1000000);

                    Intent intent = new Intent(PaymentSelectionActivity.this, BookingSuccessActivity.class);
                    intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                    intent.putExtra(BookingSuccessActivity.EXTRA_BOOKING_NO, bookingNo);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(PaymentSelectionActivity.this, response, R.string.booking_failed);
                    Toast.makeText(PaymentSelectionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                btnConfirmAndPay.setEnabled(true);
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                String errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(PaymentSelectionActivity.this, t, R.string.booking_failed);
                Toast.makeText(PaymentSelectionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

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
        apiRequest.departureId = parseDepartureId(bookingRequest.selectedDepartureId);

        if (bookingRequest.passengers != null && !bookingRequest.passengers.isEmpty()) {
            for (int i = 0; i < bookingRequest.passengers.size(); i++) {
                BookingRequest.Passenger p = bookingRequest.passengers.get(i);
                apiRequest.travellers.add(
                        com.hafiztraveltours.app.utils.TravellerMapper.toTravellerRequest(p, i == 0));
            }
        } else {
            CreateBookingRequest.TravellerRequest t = new CreateBookingRequest.TravellerRequest();
            t.fullName = "Jemaah Utama";
            t.isLead = true;
            t.relationship = "self";
            t.nationality = "Malaysian";
            apiRequest.travellers.add(t);
        }

        apiRequest.adultCount = apiRequest.travellers.size();
        apiRequest.childCount = 0;
        apiRequest.unitPrice = bookingRequest.unitPriceAmount;
        apiRequest.discountAmount = bookingRequest.discountAmount;
        apiRequest.promoCode = bookingRequest.promoCode;
        apiRequest.paymentType = selectedPaymentType;
        apiRequest.paymentMethod = selectedPaymentMethod;
        apiRequest.termsAgreed = true;
        apiRequest.termsAgreedAt = (bookingRequest.termsAgreedAt != null && !bookingRequest.termsAgreedAt.isEmpty())
                ? bookingRequest.termsAgreedAt
                : com.hafiztraveltours.app.utils.DateFormats.nowIsoDateTime();
        apiRequest.termsVersion = (bookingRequest.termsVersion != null && !bookingRequest.termsVersion.isEmpty())
                ? bookingRequest.termsVersion
                : "1.0";
        return apiRequest;
    }
}
