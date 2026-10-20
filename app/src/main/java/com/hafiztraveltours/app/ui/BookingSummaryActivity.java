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
import com.hafiztraveltours.app.models.QuoteRequest;
import com.hafiztraveltours.app.models.QuoteResponse;
import java.util.ArrayList;
import java.util.List;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.DateFormats;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.LoadingButtonUtil;
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
    private LinearLayout containerRooms;

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
        containerRooms = findViewById(R.id.summaryRoomsContainer);
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

        boolean isUmrah = bookingRequest.packageDetail != null && bookingRequest.packageDetail.isUmrah;
        if (isUmrah) {
            txtRoomLabel.setText(bookingRequest.roomLabel);
        } else {
            String seasonLabel = com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonLabel(this, bookingRequest.season);
            txtRoomLabel.setText(seasonLabel);
        }
        txtPaxCount.setText(getString(R.string.summary_pax_adults_format, bookingRequest.adultPaxCount));

        double calculatedTotal = 0;
        try {
            if (bookingRequest.passengers != null) {
                for (BookingRequest.Passenger p : bookingRequest.passengers) {
                    calculatedTotal += com.hafiztraveltours.app.utils.PackagePricingCalculator.calculatePassengerPrice(
                            bookingRequest.packageDetail, bookingRequest.season, p.dateOfBirth, p.withBed, bookingRequest.unitPriceAmount);
                }
            } else {
                calculatedTotal = bookingRequest.unitPriceAmount * bookingRequest.adultPaxCount;
            }
        } catch (com.hafiztraveltours.app.utils.PackagePricingCalculator.PricingConfigurationException e) {
            android.widget.Toast.makeText(this, e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }

        txtUnitPriceLabel.setText(getString(R.string.summary_room_subtotal_format, bookingRequest.roomPriceFormatted, bookingRequest.adultPaxCount));
        txtUnitPriceAmount.setText(BookingRequest.formatPrice(calculatedTotal));

        renderBreakdown();
        double finalTotal = Math.max(0, calculatedTotal - bookingRequest.discountAmount);
        txtTotalAmount.setText(BookingRequest.formatPrice(finalTotal));

        renderRoomsBreakdown();
        renderPassengers();
        fetchServerQuote();
    }

    private void renderBreakdown() {
        if (containerBreakdown == null) return;
        containerBreakdown.removeAllViews();

        boolean isUmrah = bookingRequest.packageDetail != null && bookingRequest.packageDetail.isUmrah;
        if (!isUmrah && bookingRequest.season != null && !bookingRequest.season.isEmpty()) {
            LinearLayout sRow = new LinearLayout(this);
            sRow.setOrientation(LinearLayout.HORIZONTAL);
            sRow.setPadding(0, dp(2), 0, dp(4));

            TextView sLabel = new TextView(this);
            sLabel.setText(getString(R.string.package_season_label));
            sLabel.setTextSize(12);
            sLabel.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            sLabel.setLayoutParams(slp);

            TextView sVal = new TextView(this);
            sVal.setText(com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonLabel(this, bookingRequest.season));
            sVal.setTextSize(12);
            sVal.setTextColor(getResources().getColor(R.color.brand_magenta));
            sVal.setTypeface(null, Typeface.BOLD);

            sRow.addView(sLabel);
            sRow.addView(sVal);
            containerBreakdown.addView(sRow);
        }

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

            String displayName = (p.fullName != null && !p.fullName.trim().isEmpty())
                    ? p.fullName.trim()
                    : getString(R.string.passenger_traveller_title_format, (i + 1));
            tvName.setText(displayName);

            String docNo = (p.icPassportNumber != null && !p.icPassportNumber.isEmpty())
                    ? p.icPassportNumber : (p.passportNumber != null && !p.passportNumber.isEmpty() ? p.passportNumber : p.icNumber);
            String masked = maskSensitiveDoc(docNo);
            tvId.setText(getString(R.string.summary_id_label_format, masked));

            double paxPrice = 0;
            try {
                paxPrice = com.hafiztraveltours.app.utils.PackagePricingCalculator.calculatePassengerPrice(
                        bookingRequest.packageDetail, bookingRequest.season, p.dateOfBirth, p.withBed, bookingRequest.unitPriceAmount);
            } catch (com.hafiztraveltours.app.utils.PackagePricingCalculator.PricingConfigurationException ignored) {}

            if (p.isLead) {
                tvRole.setText(getString(R.string.summary_lead_suffix) + " • " + BookingRequest.formatPrice(paxPrice));
                tvRole.setBackgroundResource(R.drawable.bg_status_pending);
                tvRole.setTextColor(getResources().getColor(R.color.pink_dark));
            } else {
                tvRole.setText(getString(R.string.passenger_traveller_title_format, (i + 1)) + " • " + BookingRequest.formatPrice(paxPrice));
                tvRole.setBackgroundResource(R.drawable.bg_chip_minimal);
                tvRole.setTextColor(getResources().getColor(R.color.text_dark));
            }

            // Gender Chip
            if (p.gender != null && !p.gender.trim().isEmpty()) {
                String g = p.gender.trim();
                String displayGender = ("Male".equalsIgnoreCase(g) || "Lelaki".equalsIgnoreCase(g))
                        ? getString(R.string.gender_male)
                        : (("Female".equalsIgnoreCase(g) || "Perempuan".equalsIgnoreCase(g) || "Wanita".equalsIgnoreCase(g))
                                ? getString(R.string.gender_female)
                                : g);
                tvGenderChip.setText(displayGender);
                tvGenderChip.setVisibility(View.VISIBLE);
            } else {
                tvGenderChip.setVisibility(View.GONE);
            }

            // DOB Chip & Bed info
            if (p.dateOfBirth != null && !p.dateOfBirth.trim().isEmpty()) {
                String formattedDob = ProfileActivity.formatDateDisplay(this, p.dateOfBirth);
                int age = com.hafiztraveltours.app.utils.PackagePricingCalculator.calculateAge(p.dateOfBirth);
                if (p.withBed != null && age >= 2 && age <= 11) {
                    formattedDob += " (" + getString(Boolean.TRUE.equals(p.withBed) ? R.string.with_bed : R.string.without_bed) + ")";
                }
                tvDobChip.setText(formattedDob);
                tvDobChip.setVisibility(View.VISIBLE);
            } else {
                tvDobChip.setVisibility(View.GONE);
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
        LoadingButtonUtil.showLoading(btnConfirmBooking, confirmProgressBar);

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
                LoadingButtonUtil.hideLoading(btnConfirmBooking, confirmProgressBar);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingDetailDto booking = response.body().data;
                    if (booking != null && bookingRequest.promoCode != null && !bookingRequest.promoCode.trim().isEmpty()) {
                        Toast.makeText(BookingSummaryActivity.this, getString(
                                R.string.promo_confirmed_format,
                                bookingRequest.promoCode,
                                BookingRequest.formatPrice(booking.discountAmount),
                                BookingRequest.formatPrice(booking.totalAmount)), Toast.LENGTH_LONG).show();
                    }
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
                                if ((blockingBookingNo == null || blockingBookingNo.isEmpty()) && obj.has("data") && !obj.isNull("data")) {
                                    org.json.JSONObject dataObj = obj.optJSONObject("data");
                                    if (dataObj != null) {
                                        blockingBookingNo = dataObj.optString("unpaid_booking_no", "");
                                        if (blockingBookingNo.isEmpty() && dataObj.has("blocking_booking") && !dataObj.isNull("blocking_booking")) {
                                            org.json.JSONObject blk = dataObj.optJSONObject("blocking_booking");
                                            if (blk != null) {
                                                blockingBookingNo = blk.optString("booking_no", "");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    if ("UNPAID_DEPOSIT_EXISTS".equalsIgnoreCase(errorCode) || (rawError != null && rawError.contains("UNPAID_DEPOSIT_EXISTS"))) {
                        showUnpaidDepositBlockedDialog(blockingBookingNo);
                    } else if (rawError != null && rawError.contains("promo_code")) {
                        Toast.makeText(BookingSummaryActivity.this, R.string.promo_invalid, Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = com.hafiztraveltours.app.network.ApiErrors.userMessage(BookingSummaryActivity.this, response, R.string.booking_failed);
                        Toast.makeText(BookingSummaryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<BookingDetailDto>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                isSubmitting = false;
                LoadingButtonUtil.hideLoading(btnConfirmBooking, confirmProgressBar);

                String errorMsg = bookingRequest.promoCode != null && !bookingRequest.promoCode.trim().isEmpty()
                        ? getString(R.string.promo_network_error)
                        : com.hafiztraveltours.app.network.ApiErrors.userMessage(BookingSummaryActivity.this, t, R.string.booking_failed);
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

    private void renderRoomsBreakdown() {
        if (containerRooms == null) return;
        containerRooms.removeAllViews();

        if (bookingRequest.rooms == null || bookingRequest.rooms.isEmpty()) {
            View card = findViewById(R.id.cardBookedRoomsSummary);
            if (card != null) card.setVisibility(View.GONE);
            return;
        }

        View cardWrapper = findViewById(R.id.cardBookedRoomsSummary);
        if (cardWrapper != null) cardWrapper.setVisibility(View.VISIBLE);

        for (int r = 0; r < bookingRequest.rooms.size(); r++) {
            BookingRequest.RoomConfig rc = bookingRequest.rooms.get(r);

            LinearLayout rCard = new LinearLayout(this);
            rCard.setOrientation(LinearLayout.VERTICAL);
            rCard.setBackgroundResource(R.drawable.bg_luxury_form_field);
            rCard.setPadding(dp(12), dp(10), dp(12), dp(10));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            rCard.setLayoutParams(lp);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView rTitle = new TextView(this);
            String rType = rc.roomType != null ? rc.roomType.toUpperCase() : "STANDARD";
            rTitle.setText(getString(R.string.room_card_header, (r + 1), rType));
            rTitle.setTextSize(13);
            rTitle.setTypeface(null, Typeface.BOLD);
            rTitle.setTextColor(getResources().getColor(R.color.text_dark));
            LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            rTitle.setLayoutParams(tLp);
            header.addView(rTitle);

            TextView occText = new TextView(this);
            occText.setText(getString(R.string.room_occupied_count_format, rc.getOccupancy(), rc.capacity));
            occText.setTextSize(11);
            occText.setTypeface(null, Typeface.BOLD);
            occText.setTextColor(getResources().getColor(R.color.brand_magenta));
            header.addView(occText);

            rCard.addView(header);

            // Display occupants assigned to this room
            if (bookingRequest.passengers != null) {
                for (int pIdx = 0; pIdx < bookingRequest.passengers.size(); pIdx++) {
                    BookingRequest.Passenger p = bookingRequest.passengers.get(pIdx);
                    if (p.roomIndex != null && p.roomIndex == r) {
                        TextView pLine = new TextView(this);
                        String pName = p.fullName != null && !p.fullName.isEmpty() ? p.fullName : (p.isLead ? getString(R.string.lead_traveller_name_default) : getString(R.string.default_traveller_number, (pIdx + 1)));
                        String role = p.isLead ? getString(R.string.room_role_lead) : getString(R.string.room_role_traveller, (pIdx + 1));
                        String gText = "";
                        if (p.gender != null && !p.gender.isEmpty()) {
                            String g = p.gender.trim();
                            gText = ", " + (("Male".equalsIgnoreCase(g) || "Lelaki".equalsIgnoreCase(g)) ? getString(R.string.gender_male) : (("Female".equalsIgnoreCase(g) || "Perempuan".equalsIgnoreCase(g) || "Wanita".equalsIgnoreCase(g)) ? getString(R.string.gender_female) : g));
                        }
                        pLine.setText("  • " + pName + " (" + role + gText + ")");
                        pLine.setTextSize(11);
                        pLine.setTextColor(getResources().getColor(R.color.text_gray));
                        rCard.addView(pLine);
                    }
                }
            }

            containerRooms.addView(rCard);
        }
    }

    private void fetchServerQuote() {
        QuoteRequest qReq = new QuoteRequest();
        qReq.packageId = bookingRequest.packageId;
        qReq.departureId = parseDepartureId(bookingRequest.selectedDepartureId);
        qReq.pricingId = bookingRequest.selectedPricingId;
        qReq.season = bookingRequest.season;
        qReq.promoCode = bookingRequest.promoCode;
        qReq.roomType = bookingRequest.roomType;

        if (bookingRequest.rooms != null) {
            for (BookingRequest.RoomConfig rc : bookingRequest.rooms) {
                QuoteRequest.RoomQuoteItem rqi = new QuoteRequest.RoomQuoteItem(rc.roomNumber, rc.roomType, rc.assignedTravellerIndices);
                rqi.gender = rc.gender;
                qReq.rooms.add(rqi);
            }
        }

        if (bookingRequest.passengers != null) {
            for (int i = 0; i < bookingRequest.passengers.size(); i++) {
                BookingRequest.Passenger p = bookingRequest.passengers.get(i);
                QuoteRequest.TravellerQuoteItem tq = new QuoteRequest.TravellerQuoteItem();
                tq.index = i;
                tq.fullName = p.fullName;
                tq.gender = p.gender;
                tq.dateOfBirth = p.dateOfBirth;
                tq.withBed = p.withBed;
                tq.roomIndex = p.roomIndex != null ? p.roomIndex : 0;
                tq.isLead = p.isLead;
                qReq.travellers.add(tq);
            }
        }

        ApiClient.getApiService().getBookingQuote(qReq).enqueue(new retrofit2.Callback<ApiResponse<QuoteResponse>>() {
            @Override
            public void onResponse(retrofit2.Call<ApiResponse<QuoteResponse>> call,
                                   retrofit2.Response<ApiResponse<QuoteResponse>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    applyServerQuote(response.body().data);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiResponse<QuoteResponse>> call, Throwable t) {
                // Keep local calculations on failure
            }
        });
    }

    private void applyServerQuote(QuoteResponse quote) {
        if (quote == null) return;

        txtTotalAmount.setText(BookingRequest.formatPrice(quote.total));
        if (containerBreakdown != null && quote.items != null && !quote.items.isEmpty()) {
            containerBreakdown.removeAllViews();
            for (QuoteResponse.QuoteLineItem item : quote.items) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(3), 0, dp(3));

                TextView label = new TextView(this);
                String desc = item.description != null ? item.description : "";
                if (item.quantity > 1) {
                    desc += " (×" + item.quantity + ")";
                }
                label.setText(desc);
                label.setTextSize(12);
                label.setTextColor(getResources().getColor(R.color.text_dark));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                label.setLayoutParams(lp);

                TextView val = new TextView(this);
                val.setText(BookingRequest.formatPrice(item.total));
                val.setTextSize(12);
                val.setTextColor(getResources().getColor(item.total < 0 ? R.color.brand_magenta : R.color.text_dark));
                val.setTypeface(null, Typeface.BOLD);

                row.addView(label);
                row.addView(val);
                containerBreakdown.addView(row);
            }
        }
    }

    private CreateBookingRequest buildApiRequest() {
        CreateBookingRequest apiRequest = new CreateBookingRequest();
        apiRequest.packageId = bookingRequest.packageId;
        apiRequest.roomLabel = bookingRequest.roomLabel;
        apiRequest.roomType = bookingRequest.roomType;
        apiRequest.departureId = parseDepartureId(bookingRequest.selectedDepartureId);
        apiRequest.pricingId = bookingRequest.selectedPricingId;
        apiRequest.season = bookingRequest.season;

        // Populate rooms
        if (bookingRequest.rooms != null && !bookingRequest.rooms.isEmpty()) {
            for (BookingRequest.RoomConfig rc : bookingRequest.rooms) {
                CreateBookingRequest.RoomRequest rr = new CreateBookingRequest.RoomRequest();
                rr.roomNumber = rc.roomNumber;
                rr.roomType = rc.roomType;
                rr.roomCategory = rc.roomCategory != null ? rc.roomCategory : rc.roomType;
                rr.gender = rc.gender;
                rr.capacity = rc.capacity;
                rr.travellerIndices = new ArrayList<>(rc.assignedTravellerIndices);
                rr.notes = rc.notes;
                apiRequest.rooms.add(rr);
            }
        }

        int adults = 0;
        int children = 0;
        if (bookingRequest.passengers != null && !bookingRequest.passengers.isEmpty()) {
            for (int i = 0; i < bookingRequest.passengers.size(); i++) {
                BookingRequest.Passenger p = bookingRequest.passengers.get(i);
                apiRequest.travellers.add(
                        com.hafiztraveltours.app.utils.TravellerMapper.toTravellerRequest(p, i == 0));
                int age = com.hafiztraveltours.app.utils.PackagePricingCalculator.calculateAge(p.dateOfBirth, bookingRequest.selectedDepartureDate);
                if (p.dateOfBirth != null && !p.dateOfBirth.trim().isEmpty() && age < 12) {
                    children++;
                } else {
                    adults++;
                }
            }
        } else {
            CreateBookingRequest.TravellerRequest t = new CreateBookingRequest.TravellerRequest();
            t.fullName = getString(R.string.lead_traveller_name_default);
            t.isLead = true;
            t.relationship = "self";
            t.nationality = "Malaysian";
            t.roomIndex = 0;
            apiRequest.travellers.add(t);
            adults = 1;
        }

        apiRequest.adultCount = adults;
        apiRequest.childCount = children;
        apiRequest.unitPrice = bookingRequest.unitPriceAmount;
        apiRequest.discountAmount = 0.0;
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
}
