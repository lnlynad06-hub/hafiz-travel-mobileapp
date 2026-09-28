package com.hafiztraveltours.app.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.HapticUtil;

import java.util.List;

/**
 * Terms & Conditions viewer for Hafiz Travel & Tours.
 *
 * Displays full terms, official company information, and cancellation policies.
 */
public class TermsConditionsActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";
    public static final String TERMS_VERSION = "1.0";

    private BookingRequest bookingRequest;
    private LinearLayout containerPackageCancellation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        findViewById(R.id.termsBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        TextView txtPackageName = findViewById(R.id.termsPackageName);
        TextView txtPackageMeta = findViewById(R.id.termsPackageMeta);
        containerPackageCancellation = findViewById(R.id.containerPackageCancellation);

        if (bookingRequest != null) {
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
        }

        View btnTermsClose = findViewById(R.id.btnTermsClose);
        if (btnTermsClose != null) {
            btnTermsClose.setOnClickListener(v -> {
                HapticUtil.click(v);
                finish();
            });
        }
    }

    private void renderPackageCancellationPolicy() {
        if (containerPackageCancellation == null || bookingRequest == null || bookingRequest.packageDetail == null) return;
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
