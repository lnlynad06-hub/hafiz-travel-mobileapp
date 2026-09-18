package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
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
            Intent intent = new Intent(this, PaymentSelectionActivity.class);
            intent.putExtra(PaymentSelectionActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
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

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, dp(4), 0, dp(8));

            TextView nameView = new TextView(this);
            nameView.setText(getString(R.string.summary_traveller_index_format, (i + 1), p.fullName, (p.isLead ? " " + getString(R.string.summary_lead_suffix) : "")));
            nameView.setTextSize(13);
            nameView.setTypeface(null, Typeface.BOLD);
            nameView.setTextColor(getResources().getColor(R.color.text_dark));
            item.addView(nameView);

            TextView icView = new TextView(this);
            String maskedId = maskSensitiveDoc(p.icPassportNumber);
            icView.setText(getString(R.string.summary_id_label_format, maskedId));
            icView.setTextSize(12);
            icView.setTextColor(getResources().getColor(R.color.text_gray));
            item.addView(icView);

            if (p.isLead) {
                if (p.phoneNumber != null && !p.phoneNumber.isEmpty()) {
                    TextView phoneView = new TextView(this);
                    phoneView.setText(getString(R.string.summary_phone_label_format, p.phoneNumber));
                    phoneView.setTextSize(12);
                    phoneView.setTextColor(getResources().getColor(R.color.text_gray));
                    item.addView(phoneView);
                }

                if (p.email != null && !p.email.isEmpty()) {
                    TextView emailView = new TextView(this);
                    emailView.setText(getString(R.string.summary_email_label_format, p.email));
                    emailView.setTextSize(12);
                    emailView.setTextColor(getResources().getColor(R.color.text_gray));
                    item.addView(emailView);
                }
            }

            containerPassengers.addView(item);
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
