package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.DateFormats;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.util.List;

/**
 * Terms & Conditions agreement gate immediately preceding Payment / Deposit.
 *
 * Requirements:
 * - Read-through terms and conditions view before payment.
 * - Unchecked checkbox by default ("I have read and agree to the Terms & Conditions").
 * - Continue to Payment button blocked until agreement checkbox is checked.
 * - Records agreement state (timestamp, status, version) onto BookingRequest for backend audit.
 */
public class TermsConditionsActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";
    public static final String TERMS_VERSION = "1.0";

    private BookingRequest bookingRequest;
    private CheckBox checkboxAgree;
    private View btnContinueToPayment;
    private LinearLayout containerPackageCancellation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        findViewById(R.id.termsBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);
        if (bookingRequest == null) {
            finish();
            return;
        }

        TextView txtPackageName = findViewById(R.id.termsPackageName);
        TextView txtPackageMeta = findViewById(R.id.termsPackageMeta);
        checkboxAgree = findViewById(R.id.termsAgreementCheckbox);
        btnContinueToPayment = findViewById(R.id.btnContinueToPayment);
        containerPackageCancellation = findViewById(R.id.containerPackageCancellation);

        if (txtPackageName != null && bookingRequest.packageName != null) {
            txtPackageName.setText(bookingRequest.packageName);
        }

        if (txtPackageMeta != null) {
            String room = bookingRequest.roomLabel != null ? bookingRequest.roomLabel : "";
            int pax = bookingRequest.adultPaxCount;
            txtPackageMeta.setText(getString(R.string.payment_summary_format, 
                    bookingRequest.packageName != null ? bookingRequest.packageName : "", 
                    room, pax));
        }

        // Render package-specific cancellation policy if available
        renderPackageCancellationPolicy();

        // Checkbox must start unchecked by default
        checkboxAgree.setChecked(false);
        updateContinueButtonState(false);

        checkboxAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            HapticUtil.click(buttonView);
            updateContinueButtonState(isChecked);
        });

        btnContinueToPayment.setOnClickListener(v -> {
            if (!checkboxAgree.isChecked()) {
                Toast.makeText(this, R.string.terms_required_error, Toast.LENGTH_SHORT).show();
                return;
            }
            HapticUtil.click(v);
            proceedToPayment();
        });
    }

    private void updateContinueButtonState(boolean isAgreed) {
        btnContinueToPayment.setEnabled(isAgreed);
        btnContinueToPayment.setClickable(isAgreed);
        btnContinueToPayment.setFocusable(isAgreed);
        btnContinueToPayment.setAlpha(isAgreed ? 1.0f : 0.45f);
    }

    private void renderPackageCancellationPolicy() {
        if (containerPackageCancellation == null || bookingRequest.packageDetail == null) return;
        List<String> policies = bookingRequest.packageDetail.cancellationPolicy;
        if (policies == null || policies.isEmpty()) return;

        containerPackageCancellation.removeAllViews();
        for (String item : policies) {
            if (item == null || item.trim().isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(4);
            rp.bottomMargin = dp(4);
            row.setLayoutParams(rp);

            ImageView dot = new ImageView(this);
            dot.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams dip = new LinearLayout.LayoutParams(dp(14), dp(14));
            dip.rightMargin = dp(8);
            dip.topMargin = dp(2);
            dot.setLayoutParams(dip);

            TextView tv = new TextView(this);
            tv.setText(item.trim());
            tv.setTextSize(12);
            tv.setTextColor(Color.parseColor("#4B5563"));
            tv.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            tv.setLayoutParams(tp);

            row.addView(dot);
            row.addView(tv);
            containerPackageCancellation.addView(row);
        }
    }

    private void proceedToPayment() {
        // Stamp agreement details onto bookingRequest for audit
        bookingRequest.termsAgreed = true;
        bookingRequest.termsAgreedAt = DateFormats.nowIsoDateTime();
        bookingRequest.termsVersion = TERMS_VERSION;

        Intent intent = new Intent(this, PaymentSelectionActivity.class);
        intent.putExtra(PaymentSelectionActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
