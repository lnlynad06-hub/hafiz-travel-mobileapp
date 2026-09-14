package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.utils.LocaleHelper;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class PassengerDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private BookingRequest bookingRequest;
    private LinearLayout formContainer;
    private TextView txtPackageName;
    private TextView txtRoomAndPax;
    private TextView txtTotalAmount;

    // View references for validation
    private final List<PassengerHolder> passengerHolders = new ArrayList<>();

    private static class PassengerHolder {
        int index;
        boolean isLead;
        EditText inputName;
        TextView errorName;
        EditText inputIc;
        TextView errorIc;
        EditText inputPhone;
        TextView errorPhone;
        EditText inputEmail;
        TextView errorEmail;
        View cardView;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger_details);

        findViewById(R.id.passengerBackButton).setOnClickListener(v -> finish());

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);

        if (bookingRequest == null) {
            finish();
            return;
        }

        formContainer = findViewById(R.id.passengerFormContainer);
        txtPackageName = findViewById(R.id.passengerPackageName);
        txtRoomAndPax = findViewById(R.id.passengerRoomAndPax);
        txtTotalAmount = findViewById(R.id.passengerTotalAmount);

        txtPackageName.setText(bookingRequest.packageName);
        txtRoomAndPax.setText(bookingRequest.roomLabel + " • " + bookingRequest.adultPaxCount + " Pax");
        txtTotalAmount.setText(bookingRequest.totalAmountFormatted);

        findViewById(R.id.btnProceedToReview).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            validateAndProceed();
        });

        buildPassengerForms();
    }

    private void buildPassengerForms() {
        formContainer.removeAllViews();
        passengerHolders.clear();

        SessionManager session = SessionManager.getInstance(this);

        for (int i = 0; i < bookingRequest.adultPaxCount; i++) {
            boolean isLead = (i == 0);
            PassengerHolder holder = new PassengerHolder();
            holder.index = i + 1;
            holder.isLead = isLead;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) cp.topMargin = dp(14);
            card.setLayoutParams(cp);

            // Header
            TextView title = new TextView(this);
            title.setText(isLead ? "Ketua Jemaah (Jemaah 1)" : "Jemaah " + (i + 1));
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.brand_magenta));
            card.addView(title);

            // Full Name Field
            card.addView(createFieldLabel("Nama Penuh (seperti IC / Pasport) *"));
            holder.inputName = createEditText("Contoh: Ahmad Bin Abdullah", InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            if (isLead && session.isLoggedIn()) {
                String savedName = session.getUserName();
                if (!savedName.equals("Tetamu Jemaah")) {
                    holder.inputName.setText(savedName);
                }
            }
            holder.errorName = createErrorTextView();
            card.addView(holder.inputName);
            card.addView(holder.errorName);

            // IC / Passport Field
            card.addView(createFieldLabel("No. Kad Pengenalan / Pasport *"));
            holder.inputIc = createEditText("Contoh: 900101-01-5544 / A12345678", InputType.TYPE_CLASS_TEXT);
            holder.errorIc = createErrorTextView();
            card.addView(holder.inputIc);
            card.addView(holder.errorIc);

            if (isLead) {
                // Phone Number Field (Only for Lead)
                card.addView(createFieldLabel("No. Telefon Utama *"));
                holder.inputPhone = createEditText("Contoh: 0123456789", InputType.TYPE_CLASS_PHONE);
                if (session.isLoggedIn()) {
                    holder.inputPhone.setText(session.getUserPhone());
                }
                holder.errorPhone = createErrorTextView();
                card.addView(holder.inputPhone);
                card.addView(holder.errorPhone);

                // Email Field (Only for Lead)
                card.addView(createFieldLabel("E-mel Utama *"));
                holder.inputEmail = createEditText("Contoh: ahmad@example.com", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                if (session.isLoggedIn()) {
                    holder.inputEmail.setText(session.getUserEmail());
                }
                holder.errorEmail = createErrorTextView();
                card.addView(holder.inputEmail);
                card.addView(holder.errorEmail);
            }

            holder.cardView = card;
            passengerHolders.add(holder);
            formContainer.addView(card);
        }
    }

    private TextView createFieldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(12);
        tv.setLayoutParams(p);
        return tv;
    }

    private EditText createEditText(String hint, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(inputType);
        et.setTextSize(13);
        et.setTextColor(getResources().getColor(R.color.text_dark));
        et.setHintTextColor(getResources().getColor(R.color.text_muted));
        et.setBackgroundResource(R.drawable.bg_input_box);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(4);
        et.setLayoutParams(p);
        return et;
    }

    private TextView createErrorTextView() {
        TextView tv = new TextView(this);
        tv.setTextSize(11);
        tv.setTextColor(getResources().getColor(R.color.error_red));
        tv.setVisibility(View.GONE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(2);
        tv.setLayoutParams(p);
        return tv;
    }

    private void validateAndProceed() {
        boolean isValid = true;
        View firstErrorView = null;

        for (PassengerHolder holder : passengerHolders) {
            // Reset errors
            holder.errorName.setVisibility(View.GONE);
            holder.errorIc.setVisibility(View.GONE);
            if (holder.isLead) {
                holder.errorPhone.setVisibility(View.GONE);
                holder.errorEmail.setVisibility(View.GONE);
            }

            // Name check
            String name = holder.inputName.getText().toString().trim();
            if (name.isEmpty()) {
                holder.errorName.setText("Sila isi nama penuh mengikut IC / Pasport.");
                holder.errorName.setVisibility(View.VISIBLE);
                isValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputName;
            }

            // IC check
            String ic = holder.inputIc.getText().toString().trim();
            if (ic.isEmpty()) {
                holder.errorIc.setText("Sila isi no. IC atau Pasport.");
                holder.errorIc.setVisibility(View.VISIBLE);
                isValid = false;
                if (firstErrorView == null) firstErrorView = holder.inputIc;
            }

            if (holder.isLead) {
                // Phone check
                String phone = holder.inputPhone.getText().toString().trim();
                if (phone.isEmpty() || phone.length() < 9) {
                    holder.errorPhone.setText("Sila masukkan no. telefon yang sah.");
                    holder.errorPhone.setVisibility(View.VISIBLE);
                    isValid = false;
                    if (firstErrorView == null) firstErrorView = holder.inputPhone;
                }

                // Email check
                String email = holder.inputEmail.getText().toString().trim();
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    holder.errorEmail.setText("Sila masukkan alamat e-mel yang sah.");
                    holder.errorEmail.setVisibility(View.VISIBLE);
                    isValid = false;
                    if (firstErrorView == null) firstErrorView = holder.inputEmail;
                }
            }
        }

        if (!isValid) {
            if (firstErrorView != null) {
                firstErrorView.requestFocus();
            }
            return;
        }

        // Build list of passengers
        bookingRequest.passengers.clear();
        for (PassengerHolder holder : passengerHolders) {
            BookingRequest.Passenger p = new BookingRequest.Passenger();
            p.isLead = holder.isLead;
            p.fullName = holder.inputName.getText().toString().trim();
            p.icPassportNumber = holder.inputIc.getText().toString().trim();
            if (holder.isLead) {
                p.phoneNumber = holder.inputPhone.getText().toString().trim();
                p.email = holder.inputEmail.getText().toString().trim();
            }
            bookingRequest.passengers.add(p);
        }

        Intent intent = new Intent(this, BookingSummaryActivity.class);
        intent.putExtra(BookingSummaryActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
