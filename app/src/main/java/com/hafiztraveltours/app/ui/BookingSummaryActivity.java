package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.LocaleHelper;

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

        renderSummary();

        findViewById(R.id.btnProceedToPaymentPhase3).setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            Intent intent = new Intent(this, TermsConditionsActivity.class);
            intent.putExtra(TermsConditionsActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
            startActivity(intent);
        });
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
}
