package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingDetailDto;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.CreateBookingRequest;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.DateFormats;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.TravellerMapper;

public class BookingSummaryActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;

    private TextView txtPackageName;
    private TextView txtPackageDuration;
    private TextView txtRoomLabel;
    private TextView txtPaxCount;
    private LinearLayout containerPassengers;
    private TextView txtUnitPriceLabel;
    private TextView txtUnitPriceAmount;
    private TextView txtTotalAmount;

    private TextView txtDepartureDate;
    private LinearLayout containerBreakdown;

    private CheckBox summaryAgreementCheckbox;
    private View btnConfirmBooking;
    private ProgressBar confirmProgressBar;
    private TextView txtConfirmBookingLabel;

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
        setContentView(R.layout.activity_booking_summary);

        findViewById(R.id.summaryBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        if (bookingRequest == null) {
            finish();
            return;
        }

        txtPackageName = findViewById(R.id.summaryPackageName);
        txtPackageDuration = findViewById(R.id.summaryPackageDuration);
        txtDepartureDate = findViewById(R.id.summaryDepartureDate);
        txtRoomLabel = findViewById(R.id.summaryRoomLabel);
        txtPaxCount = findViewById(R.id.summaryPaxCount);
        containerPassengers = findViewById(R.id.summaryPassengersContainer);
        containerBreakdown = findViewById(R.id.summaryBreakdownContainer);
        txtUnitPriceLabel = findViewById(R.id.summaryUnitPriceLabel);
        txtUnitPriceAmount = findViewById(R.id.summaryUnitPriceAmount);
        txtTotalAmount = findViewById(R.id.summaryTotalAmount);

        summaryAgreementCheckbox = findViewById(R.id.summaryAgreementCheckbox);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        confirmProgressBar = findViewById(R.id.confirmProgressBar);
        txtConfirmBookingLabel = findViewById(R.id.txtConfirmBookingLabel);

        View btnViewTermsDetail = findViewById(R.id.btnViewTermsDetail);
        if (btnViewTermsDetail != null) {
            btnViewTermsDetail.setOnClickListener(v -> {
                HapticUtil.click(v);
                Intent intent = new Intent(this, TermsConditionsActivity.class);
                intent.putExtra(TermsConditionsActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                startActivity(intent);
            });
        }

        if (summaryAgreementCheckbox != null) {
            androidx.core.widget.CompoundButtonCompat.setButtonTintList(summaryAgreementCheckbox, null);
            summaryAgreementCheckbox.setOnCheckedChangeListener((bv, checked) -> HapticUtil.click(bv));
        }

        renderSummary();

        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                HapticUtil.click(v);
                if (summaryAgreementCheckbox != null && !summaryAgreementCheckbox.isChecked()) {
                    Toast.makeText(this, R.string.terms_required_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isSubmitting) {
                    processBookingConfirmation();
                }
            });
        }
    }

    private void renderSummary() {
        txtPackageName.setText(bookingRequest.packageName);
        txtPackageDuration.setText(bookingRequest.packageDuration);

        if (txtDepartureDate != null) {
            if (bookingRequest.selectedDepartureDate != null && !bookingRequest.selectedDepartureDate.isEmpty()) {
                txtDepartureDate.setText(getString(R.string.summary_departure_format, bookingRequest.selectedDepartureDate));
                txtDepartureDate.setVisibility(android.view.View.VISIBLE);
            } else {
                txtDepartureDate.setVisibility(android.view.View.GONE);
            }
        }

        txtRoomLabel.setText(bookingRequest.roomLabel);
        txtPaxCount.setText(getString(R.string.summary_pax_adults_format, bookingRequest.adultPaxCount));

        double roomSubtotal = bookingRequest.unitPriceAmount * bookingRequest.adultPaxCount;
        txtUnitPriceLabel.setText(getString(R.string.summary_room_subtotal_format, bookingRequest.roomPriceFormatted, bookingRequest.adultPaxCount));
        txtUnitPriceAmount.setText(BookingRequest.formatPrice(roomSubtotal));

        renderBreakdown();
        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);

        renderPassengers();
    }

    private void renderBreakdown() {
        if (containerBreakdown == null) return;
        containerBreakdown.removeAllViews();

        if (bookingRequest.discountAmount > 0) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(2), 0, dp(4));

            TextView label = new TextView(this);
            label.setText(getString(R.string.summary_promo_discount_format, bookingRequest.promoCode));
            label.setTextSize(12);
            label.setTextColor(getResources().getColor(R.color.brand_magenta));
            label.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lp);

            TextView val = new TextView(this);
            val.setText("- " + BookingRequest.formatPrice(bookingRequest.discountAmount));
            val.setTextSize(12);
            val.setTextColor(getResources().getColor(R.color.brand_magenta));
            val.setTypeface(null, Typeface.BOLD);

            row.addView(label);
            row.addView(val);
            containerBreakdown.addView(row);
        }
    }

    private void renderPassengers() {
        containerPassengers.removeAllViews();
        if (bookingRequest.passengers == null) return;

        for (int i = 0; i < bookingRequest.passengers.size(); i++) {
            BookingRequest.Passenger p = bookingRequest.passengers.get(i);

            View card = getLayoutInflater().inflate(R.layout.item_summary_passenger_card, containerPassengers, false);

            TextView tvName = card.findViewById(R.id.tvSummaryPassengerName);
            TextView tvId = card.findViewById(R.id.tvSummaryPassengerId);
            TextView tvRole = card.findViewById(R.id.tvSummaryPassengerRole);
            TextView tvGenderChip = card.findViewById(R.id.tvSummaryGenderChip);
            TextView tvDobChip = card.findViewById(R.id.tvSummaryDobChip);
            TextView tvClothesChip = card.findViewById(R.id.tvSummaryClothesChip);

            String displayName = (p.fullName != null && !p.fullName.trim().isEmpty())
                    ? p.fullName.trim()
                    : getString(R.string.passenger_traveller_title_format, (i + 1));
            tvName.setText(displayName);

            String docNo = (p.icPassportNumber != null && !p.icPassportNumber.isEmpty())
                    ? p.icPassportNumber : (p.passportNumber != null && !p.passportNumber.isEmpty() ? p.passportNumber : p.icNumber);
            String masked = maskSensitiveDoc(docNo);
            tvId.setText(getString(R.string.summary_id_label_format, masked));

            if (p.isLead) {
                tvRole.setText(getString(R.string.summary_lead_suffix));
                tvRole.setBackgroundResource(R.drawable.bg_status_pending);
                tvRole.setTextColor(getResources().getColor(R.color.pink_dark));
            } else {
                tvRole.setText(getString(R.string.passenger_traveller_title_format, (i + 1)));
                tvRole.setBackgroundResource(R.drawable.bg_chip_minimal);
                tvRole.setTextColor(getResources().getColor(R.color.text_dark));
            }

            // Gender Chip
            if (p.gender != null && !p.gender.trim().isEmpty()) {
                tvGenderChip.setText(p.gender);
                tvGenderChip.setVisibility(View.VISIBLE);
            } else {
                tvGenderChip.setVisibility(View.GONE);
            }

            // DOB Chip
            if (p.dateOfBirth != null && !p.dateOfBirth.trim().isEmpty()) {
                String formattedDob = ProfileActivity.formatDateDisplay(this, p.dateOfBirth);
                tvDobChip.setText(formattedDob);
                tvDobChip.setVisibility(View.VISIBLE);
            } else {
                tvDobChip.setVisibility(View.GONE);
            }

            // Clothes Size Chip
            if (p.clothesSize != null && !p.clothesSize.trim().isEmpty()) {
                tvClothesChip.setText(getString(R.string.clothes_size_format, p.clothesSize));
                tvClothesChip.setVisibility(View.VISIBLE);
            } else {
                tvClothesChip.setVisibility(View.GONE);
            }

            containerPassengers.addView(card);
        }
    }

    private String maskSensitiveDoc(String docNo) {
        if (docNo == null || docNo.trim().isEmpty()) return "-";
        String clean = docNo.trim();
        if (clean.length() <= 4) return "****";
        return clean.substring(0, 3) + "****" + clean.substring(clean.length() - 2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void processBookingConfirmation() {
        isSubmitting = true;
        btnConfirmBooking.setEnabled(false);
        if (confirmProgressBar != null) confirmProgressBar.setVisibility(View.VISIBLE);

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
                btnConfirmBooking.setEnabled(true);
                if (confirmProgressBar != null) confirmProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(BookingSummaryActivity.this, R.string.booking_confirmed_success, Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(BookingSummaryActivity.this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String rawError = null;
                    String errorCode = null;
                    String blockingBookingNo = null;
                    try {
                        if (response.errorBody() != null) {
                            rawError = response.errorBody().string();
                            if (rawError != null && !rawError.isEmpty()) {
                                org.json.JSONObject obj = new org.json.JSONObject(rawError);
                                errorCode = obj.optString("code", "");
                                if (obj.has("blocking_booking") && !obj.isNull("blocking_booking")) {
                                    org.json.JSONObject blk = obj.optJSONObject("blocking_booking");
                                    if (blk != null) {
                                        blockingBookingNo = blk.optString("booking_no", "");
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    if ("UNPAID_DEPOSIT_EXISTS".equalsIgnoreCase(errorCode) || (rawError != null && rawError.contains("UNPAID_DEPOSIT_EXISTS"))) {
                        showUnpaidDepositBlockedDialog(blockingBookingNo);
                    } else {
                        String errorMsg = rawError != null ? parseErrorMessage(rawError) : getString(R.string.booking_failed);
                        Toast.makeText(BookingSummaryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                btnConfirmBooking.setEnabled(true);
                if (confirmProgressBar != null) confirmProgressBar.setVisibility(View.GONE);

                String errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(BookingSummaryActivity.this, t, R.string.booking_failed);
                Toast.makeText(BookingSummaryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
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
            android.util.Log.w("BookingSummary", "Non-numeric departure id: " + clean);
            return null;
        }
    }

    private CreateBookingRequest buildApiRequest() {
        CreateBookingRequest apiRequest = new CreateBookingRequest();
        apiRequest.packageId = bookingRequest.packageId;
        apiRequest.roomLabel = bookingRequest.roomLabel;
        apiRequest.departureId = parseDepartureId(bookingRequest.selectedDepartureId);
        apiRequest.pricingId = bookingRequest.selectedPricingId;

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
        apiRequest.paymentType = "deposit";
        apiRequest.paymentMethod = "fpx";
        apiRequest.termsAgreed = true;
        apiRequest.termsAgreedAt = com.hafiztraveltours.app.utils.DateFormats.nowIsoDateTime();
        apiRequest.termsVersion = TermsConditionsActivity.TERMS_VERSION;
        return apiRequest;
    }

    private void showUnpaidDepositBlockedDialog(String bookingNo) {
        String msg = bookingNo != null && !bookingNo.isEmpty()
                ? getString(R.string.booking_blocked_deposit_msg, bookingNo)
                : getString(R.string.booking_blocked_deposit_msg, "");
        new AlertDialog.Builder(this)
                .setTitle(R.string.booking_blocked_deposit_title)
                .setMessage(msg)
                .setPositiveButton(R.string.btn_view_my_bookings, (d, w) -> {
                    Intent intent = new Intent(this, MyBookingsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String parseErrorMessage(String rawJson) {
        try {
            org.json.JSONObject obj = new org.json.JSONObject(rawJson);
            if (obj.has("message") && !obj.isNull("message")) {
                String m = obj.optString("message", "").trim();
                if (!m.isEmpty()) return m;
            }
            if (obj.has("errors") && !obj.isNull("errors")) {
                org.json.JSONObject errors = obj.optJSONObject("errors");
                if (errors != null) {
                    java.util.Iterator<String> keys = errors.keys();
                    if (keys.hasNext()) {
                        org.json.JSONArray arr = errors.optJSONArray(keys.next());
                        if (arr != null && arr.length() > 0) {
                            return arr.optString(0, "");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return getString(R.string.booking_failed);
    }
}
