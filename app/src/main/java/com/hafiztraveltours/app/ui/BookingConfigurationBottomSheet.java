package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.PackageDetail;

import java.util.List;

public class BookingConfigurationBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PACKAGE_DETAIL = "arg_package_detail";
    private static final String ARG_INITIAL_ROOM_INDEX = "arg_initial_room_index";

    private PackageDetail detail;
    private int selectedRoomIndex = 0;
    private int paxCount = 1;

    private ImageView imgPackage;
    private TextView txtPackageName;
    private TextView txtPackageDuration;
    private LinearLayout containerRoomOptions;
    private TextView txtPaxCount;
    private TextView txtTotalAmount;
    private TextView txtUnitPriceDetail;

    public static BookingConfigurationBottomSheet newInstance(PackageDetail detail, int initialRoomIndex) {
        BookingConfigurationBottomSheet sheet = new BookingConfigurationBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_PACKAGE_DETAIL, detail);
        args.putInt(ARG_INITIAL_ROOM_INDEX, initialRoomIndex);
        sheet.setArguments(args);
        return sheet;
    }

    private LinearLayout containerDeparture;
    private android.widget.EditText inputPromo;
    private TextView btnApplyPromo;

    private int selectedDepartureIndex = 0;
    private double appliedDiscount = 0.0;
    private String appliedPromoCode = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_booking_config, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            detail = (PackageDetail) getArguments().getSerializable(ARG_PACKAGE_DETAIL);
            selectedRoomIndex = getArguments().getInt(ARG_INITIAL_ROOM_INDEX, 0);
        }

        if (detail == null) {
            dismiss();
            return;
        }

        imgPackage = view.findViewById(R.id.configPackageImage);
        txtPackageName = view.findViewById(R.id.configPackageName);
        txtPackageDuration = view.findViewById(R.id.configPackageDuration);
        containerRoomOptions = view.findViewById(R.id.configRoomOptionsContainer);
        containerDeparture = view.findViewById(R.id.configDepartureContainer);
        txtPaxCount = view.findViewById(R.id.txtPaxCount);
        txtTotalAmount = view.findViewById(R.id.configTotalAmountText);
        txtUnitPriceDetail = view.findViewById(R.id.configUnitPriceDetailText);

        inputPromo = view.findViewById(R.id.inputPromoCode);
        btnApplyPromo = view.findViewById(R.id.btnApplyPromo);

        txtPackageName.setText(detail.name);
        txtPackageDuration.setText(detail.durationDays > 0 
                ? getString(R.string.duration_days_nights, detail.durationDays, detail.nightsCount) 
                : getString(R.string.package_full));

        if (detail.imageUrl != null && !detail.imageUrl.isEmpty()) {
            try {
                Glide.with(this).load(detail.imageUrl).into(imgPackage);
            } catch (Exception ignored) {}
        }

        view.findViewById(R.id.btnPaxDecrease).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (paxCount > 1) {
                paxCount--;
                updateUi();
            }
        });

        view.findViewById(R.id.btnPaxIncrease).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (paxCount < 10) {
                paxCount++;
                updateUi();
            }
        });

        if (btnApplyPromo != null) {
            btnApplyPromo.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                String code = inputPromo.getText().toString().trim().toUpperCase();
                if (!code.isEmpty()) {
                    appliedPromoCode = code;
                    appliedDiscount = 200.0; // RM 200 CRM promo discount
                    android.widget.Toast.makeText(requireContext(), "Kod promo " + code + " berjaya digunakan (-RM 200)", android.widget.Toast.LENGTH_SHORT).show();
                    updateUi();
                }
            });
        }

        view.findViewById(R.id.btnProceedToTravellers).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            proceedToTravellerDetails();
        });

        renderRoomOptions();
        renderDepartureOptions();
        updateUi();
    }

    private List<String> getDepartureList() {
        if (detail != null && detail.availableDepartureDates != null && !detail.availableDepartureDates.isEmpty()) {
            return detail.availableDepartureDates;
        }
        List<String> fallback = new java.util.ArrayList<>();
        fallback.add("15 Nov - 26 Nov 2026");
        return fallback;
    }

    private void renderDepartureOptions() {
        if (containerDeparture == null) return;
        containerDeparture.removeAllViews();

        List<String> list = getDepartureList();
        if (selectedDepartureIndex >= list.size()) {
            selectedDepartureIndex = 0;
        }

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            boolean isSelected = (i == selectedDepartureIndex);

            TextView chip = new TextView(requireContext());
            chip.setText(list.get(i));
            chip.setTextSize(11);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);

            if (isSelected) {
                chip.setTextColor(getResources().getColor(R.color.brand_magenta));
                chip.setBackgroundResource(R.drawable.bg_room_card_selected);
            } else {
                chip.setTextColor(getResources().getColor(R.color.text_dark));
                chip.setBackgroundResource(R.drawable.bg_room_card_unselected);
            }

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMarginEnd(dp(8));
            chip.setLayoutParams(p);

            chip.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                selectedDepartureIndex = index;
                renderDepartureOptions();
            });

            containerDeparture.addView(chip);
        }
    }

    private void renderRoomOptions() {
        containerRoomOptions.removeAllViews();
        if (detail.priceOptions == null || detail.priceOptions.isEmpty()) return;

        for (int i = 0; i < detail.priceOptions.size(); i++) {
            final int index = i;
            PackageDetail.PriceOption opt = detail.priceOptions.get(i);
            boolean isSelected = (i == selectedRoomIndex);

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(isSelected ? R.drawable.bg_room_card_selected : R.drawable.bg_room_card_unselected);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(8));
            card.setLayoutParams(cardParams);

            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView label = new TextView(requireContext());
            label.setText(opt.occupancyLabel);
            label.setTextSize(12);
            label.setTypeface(null, Typeface.BOLD);
            label.setTextColor(getResources().getColor(isSelected ? R.color.brand_magenta : R.color.text_dark));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            label.setLayoutParams(lp);
            headerRow.addView(label);

            if (isSelected) {
                TextView check = new TextView(requireContext());
                check.setText("✓");
                check.setTextSize(12);
                check.setTypeface(null, Typeface.BOLD);
                check.setTextColor(getResources().getColor(R.color.brand_magenta));
                headerRow.addView(check);
            }

            TextView price = new TextView(requireContext());
            price.setText(opt.price);
            price.setTextSize(15);
            price.setTypeface(null, Typeface.BOLD);
            price.setTextColor(getResources().getColor(R.color.brand_magenta));

            TextView perPax = new TextView(requireContext());
            perPax.setText(getString(R.string.detail_per_pax));
            perPax.setTextSize(10);
            perPax.setTextColor(getResources().getColor(R.color.text_gray));

            card.addView(headerRow);
            card.addView(price);
            card.addView(perPax);

            card.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                selectedRoomIndex = index;
                renderRoomOptions();
                updateUi();
            });

            containerRoomOptions.addView(card);
        }
    }

    private void updateUi() {
        txtPaxCount.setText(paxCount + " Pax");

        String roomLabel = "Bilik Standard";
        String roomPriceStr = detail.price;

        if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size()) {
            PackageDetail.PriceOption opt = detail.priceOptions.get(selectedRoomIndex);
            roomLabel = opt.occupancyLabel;
            roomPriceStr = opt.price;
        }

        double unitAmount = BookingRequest.parsePriceAmount(roomPriceStr);
        double subtotal = unitAmount * paxCount;

        double totalAmount = Math.max(0, subtotal - appliedDiscount);

        txtTotalAmount.setText(BookingRequest.formatPrice(totalAmount));
        txtUnitPriceDetail.setText(roomPriceStr + " × " + paxCount + " Pax");
    }

    private void proceedToTravellerDetails() {
        String roomLabel = "Bilik Standard";
        String roomPriceStr = detail.price;

        if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size()) {
            PackageDetail.PriceOption opt = detail.priceOptions.get(selectedRoomIndex);
            roomLabel = opt.occupancyLabel;
            roomPriceStr = opt.price;
        }

        double unitAmount = BookingRequest.parsePriceAmount(roomPriceStr);

        BookingRequest req = new BookingRequest();
        req.packageId = detail.id;
        req.packageName = detail.name;
        req.packageImageUrl = detail.imageUrl;
        req.packageDuration = detail.durationDays > 0 
                ? getString(R.string.duration_days_nights, detail.durationDays, detail.nightsCount) 
                : getString(R.string.package_full);
        req.roomLabel = roomLabel;
        req.roomPriceFormatted = roomPriceStr;
        req.unitPriceAmount = unitAmount;
        req.adultPaxCount = paxCount;
        req.packageDetail = detail;

        List<String> deps = getDepartureList();
        if (selectedDepartureIndex >= 0 && selectedDepartureIndex < deps.size()) {
            req.selectedDepartureDate = deps.get(selectedDepartureIndex);
        }

        req.promoCode = appliedPromoCode;
        req.discountAmount = appliedDiscount;

        req.recalculateTotal();

        Intent intent = new Intent(requireContext(), PassengerDetailsActivity.class);
        intent.putExtra(PassengerDetailsActivity.EXTRA_BOOKING_REQUEST, req);
        startActivity(intent);

        dismiss();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
