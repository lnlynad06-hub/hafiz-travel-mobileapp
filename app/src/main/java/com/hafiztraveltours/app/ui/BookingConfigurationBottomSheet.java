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

import android.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.DocumentDto;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.network.UserDto;
import com.hafiztraveltours.app.utils.BookingEligibility;
import com.hafiztraveltours.app.utils.SessionManager;

import java.util.List;

public class BookingConfigurationBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PACKAGE_DETAIL = "arg_package_detail";
    private static final String ARG_INITIAL_ROOM_INDEX = "arg_initial_room_index";

    private PackageDetail detail;
    private int selectedRoomIndex = 0;
    private int paxCount = 1;
    private int kidsCount = 0;
    private String selectedSeason = com.hafiztraveltours.app.utils.PackagePricingCalculator.SEASON_STANDARD;

    private ImageView imgPackage;
    private TextView txtPackageName;
    private TextView txtPackageDuration;
    private LinearLayout containerSeason;
    private LinearLayout containerRoomOptions;
    private TextView txtPaxCount;
    private TextView txtKidsCount;
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
        containerSeason = view.findViewById(R.id.configSeasonContainer);
        containerRoomOptions = view.findViewById(R.id.configRoomOptionsContainer);
        containerDeparture = view.findViewById(R.id.configDepartureContainer);
        txtPaxCount = view.findViewById(R.id.txtPaxCount);
        txtKidsCount = view.findViewById(R.id.txtKidsCount);
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
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            if (paxCount > 1) {
                paxCount--;
                updateUi();
            }
        });

        view.findViewById(R.id.btnPaxIncrease).setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            if (paxCount < 10) {
                paxCount++;
                updateUi();
            }
        });

        View btnKidsDecrease = view.findViewById(R.id.btnKidsDecrease);
        if (btnKidsDecrease != null) {
            btnKidsDecrease.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                if (kidsCount > 0) {
                    kidsCount--;
                    updateUi();
                }
            });
        }

        View btnKidsIncrease = view.findViewById(R.id.btnKidsIncrease);
        if (btnKidsIncrease != null) {
            btnKidsIncrease.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                if (kidsCount < 10) {
                    kidsCount++;
                    updateUi();
                }
            });
        }

        if (btnApplyPromo != null) {
            btnApplyPromo.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                String code = inputPromo.getText().toString().trim().toUpperCase();
                if (!code.isEmpty()) {
                    appliedPromoCode = code;
                    appliedDiscount = 200.0; // RM 200 CRM promo discount
                    android.widget.Toast.makeText(requireContext(), getString(R.string.promo_applied_format, code), android.widget.Toast.LENGTH_SHORT).show();
                    updateUi();
                }
            });
        }

        view.findViewById(R.id.btnProceedToTravellers).setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            proceedToTravellerDetails();
        });

        View seasonTitle = view.findViewById(R.id.configSeasonTitle);
        View seasonScrollView = view.findViewById(R.id.configSeasonScrollView);
        View roomTitle = view.findViewById(R.id.configRoomTitle);
        View roomScrollView = view.findViewById(R.id.configRoomScrollView);

        if (seasonTitle != null) seasonTitle.setVisibility(View.GONE);
        if (seasonScrollView != null) seasonScrollView.setVisibility(View.GONE);

        if (detail.isUmrah) {
            if (roomTitle != null) roomTitle.setVisibility(View.VISIBLE);
            if (roomScrollView != null) roomScrollView.setVisibility(View.VISIBLE);
        } else {
            if (roomTitle != null) roomTitle.setVisibility(View.GONE);
            if (roomScrollView != null) roomScrollView.setVisibility(View.GONE);
        }

        renderSeasonOptions();
        renderRoomOptions();
        renderDepartureOptions();
        updateUi();
    }

    private List<PackageDetail.DepartureOption> getDepartureOptions() {
        if (detail != null && detail.availableDepartures != null && !detail.availableDepartures.isEmpty()) {
            return detail.availableDepartures;
        }
        // Legacy fallback: display-only labels without a backend ID (departure_id stays null).
        List<PackageDetail.DepartureOption> fallback = new java.util.ArrayList<>();
        if (detail != null && detail.availableDepartureDates != null && !detail.availableDepartureDates.isEmpty()) {
            for (String label : detail.availableDepartureDates) {
                fallback.add(new PackageDetail.DepartureOption(null, null, null, label));
            }
        } else {
            fallback.add(new PackageDetail.DepartureOption(null, null, null, "15 Nov - 26 Nov 2026"));
        }
        return fallback;
    }

    private String formatDepartureLabel(PackageDetail.DepartureOption opt) {
        if (opt == null) return "";
        if (opt.departureDate != null && !opt.departureDate.isEmpty()
                && opt.returnDate != null && !opt.returnDate.isEmpty()) {
            return getString(R.string.departure_range_format, opt.departureDate, opt.returnDate);
        }
        if (opt.departureDate != null && !opt.departureDate.isEmpty()) return opt.departureDate;
        return opt.label != null ? opt.label : "";
    }

    private void renderDepartureOptions() {
        if (containerDeparture == null) return;
        containerDeparture.removeAllViews();

        List<PackageDetail.DepartureOption> list = getDepartureOptions();
        if (selectedDepartureIndex >= list.size()) {
            selectedDepartureIndex = 0;
        }

        if (!detail.isUmrah && selectedDepartureIndex >= 0 && selectedDepartureIndex < list.size()) {
            PackageDetail.DepartureOption activeDep = list.get(selectedDepartureIndex);
            if (activeDep != null && activeDep.season != null && !activeDep.season.trim().isEmpty()) {
                selectedSeason = activeDep.season.trim();
            }
        }

        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            PackageDetail.DepartureOption opt = list.get(i);
            boolean isSelected = (i == selectedDepartureIndex);
            boolean isFull = opt.isFullyBooked();

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setClickable(true);
            card.setFocusable(true);

            if (isSelected) {
                card.setBackgroundResource(R.drawable.bg_room_card_selected);
            } else {
                card.setBackgroundResource(R.drawable.bg_room_card_unselected);
            }

            if (isFull) {
                card.setAlpha(isSelected ? 0.9f : 0.65f);
            } else {
                card.setAlpha(1.0f);
            }

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(8));
            card.setLayoutParams(cardParams);

            // Row 1: Departure Date & selection check
            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView dateText = new TextView(requireContext());
            dateText.setText(formatDepartureLabel(opt));
            dateText.setTextSize(12);
            dateText.setTypeface(null, Typeface.BOLD);
            dateText.setTextColor(getResources().getColor(isSelected ? R.color.brand_magenta : R.color.text_dark));
            headerRow.addView(dateText);

            if (isSelected) {
                TextView check = new TextView(requireContext());
                check.setText(" ✓");
                check.setTextSize(12);
                check.setTypeface(null, Typeface.BOLD);
                check.setTextColor(getResources().getColor(R.color.brand_magenta));
                headerRow.addView(check);
            }
            card.addView(headerRow);

            // Row 2: Real-time Seat availability counter & label
            TextView seatStatus = new TextView(requireContext());
            seatStatus.setTextSize(10);
            seatStatus.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams seatParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            seatParams.topMargin = dp(4);
            seatStatus.setLayoutParams(seatParams);

            if (isFull) {
                seatStatus.setText(getString(R.string.departure_zero_seats_format, opt.getTotalSeatsCount()) + " • " + getString(R.string.departure_fully_booked));
                seatStatus.setTextColor(getResources().getColor(R.color.error_red));
            } else {
                seatStatus.setText(getString(R.string.seats_left_format, opt.getAvailableSeatsCount(), opt.getTotalSeatsCount()));
                seatStatus.setTextColor(getResources().getColor(isSelected ? R.color.brand_magenta : R.color.text_gray));
            }
            card.addView(seatStatus);

            card.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                selectedDepartureIndex = index;
                if (!detail.isUmrah) {
                    if (opt.season != null && !opt.season.trim().isEmpty()) {
                        selectedSeason = opt.season.trim();
                    } else {
                        selectedSeason = com.hafiztraveltours.app.utils.PackagePricingCalculator.SEASON_STANDARD;
                    }
                }
                renderDepartureOptions();
                updateUi();
                if (isFull) {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.err_departure_fully_booked), android.widget.Toast.LENGTH_SHORT).show();
                }
            });

            containerDeparture.addView(card);
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
            label.setText(com.hafiztraveltours.app.utils.RoomLabels.resolve(requireContext(), opt));
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
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                selectedRoomIndex = index;
                renderRoomOptions();
                updateUi();
            });

            containerRoomOptions.addView(card);
        }
    }

    private void renderSeasonOptions() {
        if (containerSeason == null) return;
        containerSeason.removeAllViews();

        String[] seasons = new String[]{
                com.hafiztraveltours.app.utils.PackagePricingCalculator.SEASON_STANDARD,
                com.hafiztraveltours.app.utils.PackagePricingCalculator.SEASON_LOW_PEAK,
                com.hafiztraveltours.app.utils.PackagePricingCalculator.SEASON_HIGH_PEAK
        };

        for (String season : seasons) {
            boolean isSelected = season.equalsIgnoreCase(selectedSeason);
            double price = com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonPrice(detail, season);

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setClickable(true);
            card.setFocusable(true);
            card.setBackgroundResource(isSelected ? R.drawable.bg_room_card_selected : R.drawable.bg_room_card_unselected);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(8));
            card.setLayoutParams(cardParams);

            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView nameText = new TextView(requireContext());
            nameText.setText(com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonLabel(requireContext(), season));
            nameText.setTextSize(12);
            nameText.setTypeface(null, Typeface.BOLD);
            nameText.setTextColor(getResources().getColor(isSelected ? R.color.brand_magenta : R.color.text_dark));
            headerRow.addView(nameText);

            if (isSelected) {
                TextView check = new TextView(requireContext());
                check.setText(" ✓");
                check.setTextSize(12);
                check.setTypeface(null, Typeface.BOLD);
                check.setTextColor(getResources().getColor(R.color.brand_magenta));
                headerRow.addView(check);
            }
            card.addView(headerRow);

            TextView priceText = new TextView(requireContext());
            priceText.setText(BookingRequest.formatPrice(price));
            priceText.setTextSize(13);
            priceText.setTypeface(null, Typeface.BOLD);
            priceText.setTextColor(getResources().getColor(R.color.brand_magenta));
            LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            priceParams.topMargin = dp(4);
            priceText.setLayoutParams(priceParams);
            card.addView(priceText);

            card.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                selectedSeason = season;
                renderSeasonOptions();
                renderRoomOptions();
                updateUi();
            });

            containerSeason.addView(card);
        }
    }

    private double getSelectedUnitAmount() {
        if (detail.isUmrah) {
            if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size()) {
                PackageDetail.PriceOption opt = detail.priceOptions.get(selectedRoomIndex);
                double optPrice = BookingRequest.parsePriceAmount(opt.price);
                if (optPrice > 0) return optPrice;
            }
            return BookingRequest.parsePriceAmount(detail.price);
        } else {
            List<PackageDetail.DepartureOption> deps = getDepartureOptions();
            if (selectedDepartureIndex >= 0 && selectedDepartureIndex < deps.size()) {
                PackageDetail.DepartureOption dep = deps.get(selectedDepartureIndex);
                if (dep != null && dep.price != null && !dep.price.trim().isEmpty()) {
                    double depPrice = BookingRequest.parsePriceAmount(dep.price);
                    if (depPrice > 0) return depPrice;
                }
            }
            return com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonPrice(detail, selectedSeason);
        }
    }

    private void updateUi() {
        if (txtPaxCount != null) {
            txtPaxCount.setText(String.valueOf(paxCount));
        }
        if (txtKidsCount != null) {
            txtKidsCount.setText(String.valueOf(kidsCount));
        }

        String roomLabel = getString(R.string.room_standard);
        if (detail.isUmrah) {
            if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size()) {
                PackageDetail.PriceOption opt = detail.priceOptions.get(selectedRoomIndex);
                roomLabel = com.hafiztraveltours.app.utils.RoomLabels.resolve(requireContext(), opt);
            }
        } else {
            roomLabel = com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonLabel(requireContext(), selectedSeason);
        }

        double unitAmount = getSelectedUnitAmount();
        String roomPriceStr = BookingRequest.formatPrice(unitAmount);
        int totalPax = paxCount + kidsCount;
        double subtotal = unitAmount * totalPax;

        double totalAmount = Math.max(0, subtotal - appliedDiscount);

        txtTotalAmount.setText(BookingRequest.formatPrice(totalAmount));
        if (kidsCount > 0) {
            txtUnitPriceDetail.setText(getString(R.string.room_price_x_pax_format, roomPriceStr, totalPax));
        } else {
            txtUnitPriceDetail.setText(getString(R.string.room_price_x_pax_format, roomPriceStr, paxCount));
        }
    }

    private void proceedToTravellerDetails() {
        SessionManager session = new SessionManager(requireContext());
        if (!session.isLoggedIn()) {
            dismiss();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }

        String roomLabel = getString(R.string.room_standard);
        if (detail.isUmrah) {
            if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size()) {
                PackageDetail.PriceOption opt = detail.priceOptions.get(selectedRoomIndex);
                roomLabel = com.hafiztraveltours.app.utils.RoomLabels.resolve(requireContext(), opt);
            }
        } else {
            roomLabel = com.hafiztraveltours.app.utils.PackagePricingCalculator.getSeasonLabel(requireContext(), selectedSeason);
        }

        double unitAmount = getSelectedUnitAmount();
        String roomPriceStr = BookingRequest.formatPrice(unitAmount);

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
        req.kidsPaxCount = kidsCount;
        req.season = detail.isUmrah ? null : selectedSeason;
        req.packageDetail = detail;

        List<PackageDetail.DepartureOption> deps = getDepartureOptions();
        if (selectedDepartureIndex >= 0 && selectedDepartureIndex < deps.size()) {
            PackageDetail.DepartureOption selected = deps.get(selectedDepartureIndex);
            if (selected.isFullyBooked()) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.err_departure_fully_booked), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            int totalPax = paxCount + kidsCount;
            if (totalPax > selected.getAvailableSeatsCount()) {
                android.widget.Toast.makeText(requireContext(), getString(R.string.err_departure_not_enough_seats, selected.getAvailableSeatsCount()), android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            req.selectedDepartureDate = formatDepartureLabel(selected);
            req.selectedDepartureId = selected.id != null ? selected.id.trim() : "";

            if (selected.pricing != null && !selected.pricing.isEmpty()) {
                Integer matchedPricingId = null;
                String selectedKey = (detail.priceOptions != null && selectedRoomIndex >= 0 && selectedRoomIndex < detail.priceOptions.size())
                        ? detail.priceOptions.get(selectedRoomIndex).labelKey : null;

                if (selectedKey != null) {
                    for (com.hafiztraveltours.app.models.UmrahPackage.DepartureItem.PricingTier tier : selected.pricing) {
                        if (tier != null && tier.id != null && tier.label != null && tier.label.trim().equalsIgnoreCase(selectedKey.trim())) {
                            matchedPricingId = tier.id;
                            break;
                        }
                    }
                }
                if (matchedPricingId == null) {
                    for (com.hafiztraveltours.app.models.UmrahPackage.DepartureItem.PricingTier tier : selected.pricing) {
                        if (tier != null && tier.id != null && tier.amount != null) {
                            double tierAmount = BookingRequest.parsePriceAmount(tier.amount);
                            if (Math.abs(tierAmount - unitAmount) < 0.01) {
                                matchedPricingId = tier.id;
                                break;
                            }
                        }
                    }
                }
                if (matchedPricingId == null) {
                    for (com.hafiztraveltours.app.models.UmrahPackage.DepartureItem.PricingTier tier : selected.pricing) {
                        if (tier != null && tier.id != null && tier.label != null && roomLabel != null && tier.label.trim().equalsIgnoreCase(roomLabel.trim())) {
                            matchedPricingId = tier.id;
                            break;
                        }
                    }
                }
                if (matchedPricingId == null && !selected.pricing.isEmpty()) {
                    if (selectedRoomIndex >= 0 && selectedRoomIndex < selected.pricing.size() && selected.pricing.get(selectedRoomIndex) != null && selected.pricing.get(selectedRoomIndex).id != null) {
                        matchedPricingId = selected.pricing.get(selectedRoomIndex).id;
                    } else if (selected.pricing.get(0) != null) {
                        matchedPricingId = selected.pricing.get(0).id;
                    }
                }
                req.selectedPricingId = matchedPricingId;
            }
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
