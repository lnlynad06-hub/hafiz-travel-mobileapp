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
    private LinearLayout containerChildDetails;
    private LinearLayout containerCostBreakdownItems;
    private final List<BookingRequest.ChildConfig> childConfigs = new java.util.ArrayList<>();
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
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                ViewGroup.LayoutParams lp = bottomSheet.getLayoutParams();
                if (lp != null) {
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.setLayoutParams(lp);
                }
            }
            if (getDialog().getWindow() != null) {
                getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        }
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
        containerChildDetails = view.findViewById(R.id.containerChildDetails);
        containerCostBreakdownItems = view.findViewById(R.id.containerCostBreakdownItems);
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
                    syncChildConfigsSize();
                    renderChildDetails();
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
                    syncChildConfigsSize();
                    renderChildDetails();
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
                    appliedDiscount = 0.0;
                    android.widget.Toast.makeText(requireContext(), getString(R.string.promo_pending_validation_format, code), android.widget.Toast.LENGTH_SHORT).show();
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
        renderChildDetails();
        updateUi();
    }

    private void syncChildConfigsSize() {
        while (childConfigs.size() < kidsCount) {
            BookingRequest.ChildConfig cfg = new BookingRequest.ChildConfig();
            cfg.childIndex = childConfigs.size() + 1;
            cfg.dateOfBirth = "";
            cfg.withBed = null;
            childConfigs.add(cfg);
        }
        while (childConfigs.size() > kidsCount) {
            childConfigs.remove(childConfigs.size() - 1);
        }
    }

    private String getSelectedDepartureDate() {
        List<PackageDetail.DepartureOption> deps = getDepartureOptions();
        if (selectedDepartureIndex >= 0 && selectedDepartureIndex < deps.size()) {
            PackageDetail.DepartureOption selected = deps.get(selectedDepartureIndex);
            if (selected != null && selected.departureDate != null && !selected.departureDate.trim().isEmpty()) {
                return selected.departureDate.trim();
            }
        }
        return null;
    }

    private int calculateChildAge(String dobIso, String refDateIso) {
        if (dobIso == null || dobIso.trim().isEmpty()) return 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date dobDate = sdf.parse(dobIso.trim());
            if (dobDate == null) return 0;
            java.util.Calendar dobCal = java.util.Calendar.getInstance();
            dobCal.setTime(dobDate);

            java.util.Calendar refCal = java.util.Calendar.getInstance();
            if (refDateIso != null && !refDateIso.trim().isEmpty()) {
                java.util.Date refDate = sdf.parse(refDateIso.trim());
                if (refDate != null) refCal.setTime(refDate);
            }

            int age = refCal.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR);
            if (refCal.get(java.util.Calendar.MONTH) < dobCal.get(java.util.Calendar.MONTH) ||
                    (refCal.get(java.util.Calendar.MONTH) == dobCal.get(java.util.Calendar.MONTH) && refCal.get(java.util.Calendar.DAY_OF_MONTH) < dobCal.get(java.util.Calendar.DAY_OF_MONTH))) {
                age--;
            }
            return Math.max(0, age);
        } catch (Exception e) {
            return 0;
        }
    }

    private void showChildDobPicker(int index, BookingRequest.ChildConfig config) {
        String[] dateHolder = new String[]{config.dateOfBirth != null ? config.dateOfBirth.trim() : ""};
        String title = getString(R.string.child_title_format, index + 1) + " - " + getString(R.string.child_dob_label);
        String sub = getString(R.string.profile_dob_sheet_sub);

        com.hafiztraveltours.app.utils.DatePickerBottomSheetHelper.show(
                requireContext(),
                title,
                sub,
                false,
                true,
                null,
                null,
                dateHolder,
                (isoDate, formattedDisplayDate, age) -> {
                    config.dateOfBirth = isoDate;
                    renderChildDetails();
                    updateUi();
                }
        );
    }

    private void renderChildDetails() {
        if (containerChildDetails == null) return;
        containerChildDetails.removeAllViews();

        if (kidsCount <= 0) {
            containerChildDetails.setVisibility(View.GONE);
            return;
        }

        containerChildDetails.setVisibility(View.VISIBLE);
        String depDateIso = getSelectedDepartureDate();

        for (int i = 0; i < kidsCount && i < childConfigs.size(); i++) {
            final int childIdx = i;
            final BookingRequest.ChildConfig config = childConfigs.get(i);

            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(8);
            card.setLayoutParams(cardParams);

            // Child Title Header
            TextView txtChildTitle = new TextView(requireContext());
            txtChildTitle.setText(getString(R.string.child_title_format, i + 1));
            txtChildTitle.setTextSize(13);
            txtChildTitle.setTypeface(null, Typeface.BOLD);
            txtChildTitle.setTextColor(getResources().getColor(R.color.brand_magenta));
            card.addView(txtChildTitle);

            // DOB Selector Row
            LinearLayout dobRow = new LinearLayout(requireContext());
            dobRow.setOrientation(LinearLayout.HORIZONTAL);
            dobRow.setGravity(Gravity.CENTER_VERTICAL);
            dobRow.setBackgroundResource(R.drawable.bg_luxury_form_field);
            dobRow.setPadding(dp(10), dp(8), dp(10), dp(8));
            dobRow.setClickable(true);
            dobRow.setFocusable(true);

            LinearLayout.LayoutParams dobParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dobParams.topMargin = dp(6);
            dobRow.setLayoutParams(dobParams);

            LinearLayout dobTextCol = new LinearLayout(requireContext());
            dobTextCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams dobColParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dobTextCol.setLayoutParams(dobColParams);

            TextView lblDob = new TextView(requireContext());
            lblDob.setText(getString(R.string.child_dob_label));
            lblDob.setTextSize(10);
            lblDob.setTextColor(getResources().getColor(R.color.text_gray));
            dobTextCol.addView(lblDob);

            TextView valDob = new TextView(requireContext());
            boolean hasDob = config.dateOfBirth != null && !config.dateOfBirth.trim().isEmpty();
            valDob.setText(hasDob ? ProfileActivity.formatDateDisplay(requireContext(), config.dateOfBirth.trim()) : getString(R.string.child_select_dob_hint));
            valDob.setTextSize(13);
            valDob.setTypeface(null, Typeface.BOLD);
            valDob.setTextColor(getResources().getColor(hasDob ? R.color.text_dark : R.color.input_hint));
            dobTextCol.addView(valDob);

            dobRow.addView(dobTextCol);

            // Helper Age Text chip
            if (hasDob) {
                int age = calculateChildAge(config.dateOfBirth.trim(), depDateIso);
                TextView txtAgeChip = new TextView(requireContext());
                txtAgeChip.setText(age < 2 ? getString(R.string.child_age_under_2) : getString(R.string.child_age_format, age));
                txtAgeChip.setTextSize(10);
                txtAgeChip.setTypeface(null, Typeface.BOLD);
                txtAgeChip.setTextColor(getResources().getColor(R.color.brand_magenta));
                txtAgeChip.setBackgroundResource(R.drawable.bg_status_pending);
                txtAgeChip.setPadding(dp(6), dp(2), dp(6), dp(2));
                dobRow.addView(txtAgeChip);
            }

            dobRow.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                showChildDobPicker(childIdx, config);
            });

            card.addView(dobRow);

            // Tour Bed Selector (Only for Tour package when child age is 2-11)
            if (!detail.isUmrah && hasDob) {
                int age = calculateChildAge(config.dateOfBirth.trim(), depDateIso);
                if (age >= 2 && age <= 11) {
                    LinearLayout bedContainer = new LinearLayout(requireContext());
                    bedContainer.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams bedParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    bedParams.topMargin = dp(8);
                    bedContainer.setLayoutParams(bedParams);

                    TextView lblBed = new TextView(requireContext());
                    lblBed.setText(getString(R.string.child_bed_label));
                    lblBed.setTextSize(11);
                    lblBed.setTypeface(null, Typeface.BOLD);
                    lblBed.setTextColor(getResources().getColor(R.color.text_dark));
                    bedContainer.addView(lblBed);

                    LinearLayout bedSegmentRow = new LinearLayout(requireContext());
                    bedSegmentRow.setOrientation(LinearLayout.HORIZONTAL);
                    LinearLayout.LayoutParams segParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    segParams.topMargin = dp(4);
                    bedSegmentRow.setLayoutParams(segParams);

                    // Segment 1: With Bed
                    boolean isWithBedSelected = Boolean.TRUE.equals(config.withBed);
                    TextView btnWithBed = new TextView(requireContext());
                    btnWithBed.setText(getString(R.string.with_bed));
                    btnWithBed.setTextSize(12);
                    btnWithBed.setTypeface(null, Typeface.BOLD);
                    btnWithBed.setGravity(Gravity.CENTER);
                    btnWithBed.setPadding(dp(8), dp(8), dp(8), dp(8));
                    btnWithBed.setBackgroundResource(isWithBedSelected ? R.drawable.bg_room_card_selected : R.drawable.bg_room_card_unselected);
                    btnWithBed.setTextColor(getResources().getColor(isWithBedSelected ? R.color.brand_magenta : R.color.text_dark));
                    LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b1Lp.setMarginEnd(dp(4));
                    btnWithBed.setLayoutParams(b1Lp);

                    btnWithBed.setOnClickListener(v -> {
                        com.hafiztraveltours.app.utils.HapticUtil.click(v);
                        config.withBed = true;
                        renderChildDetails();
                        updateUi();
                    });

                    // Segment 2: Without Bed
                    boolean isWithoutBedSelected = Boolean.FALSE.equals(config.withBed);
                    TextView btnWithoutBed = new TextView(requireContext());
                    btnWithoutBed.setText(getString(R.string.without_bed));
                    btnWithoutBed.setTextSize(12);
                    btnWithoutBed.setTypeface(null, Typeface.BOLD);
                    btnWithoutBed.setGravity(Gravity.CENTER);
                    btnWithoutBed.setPadding(dp(8), dp(8), dp(8), dp(8));
                    btnWithoutBed.setBackgroundResource(isWithoutBedSelected ? R.drawable.bg_room_card_selected : R.drawable.bg_room_card_unselected);
                    btnWithoutBed.setTextColor(getResources().getColor(isWithoutBedSelected ? R.color.brand_magenta : R.color.text_dark));
                    LinearLayout.LayoutParams b2Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    b2Lp.setMarginStart(dp(4));
                    btnWithoutBed.setLayoutParams(b2Lp);

                    btnWithoutBed.setOnClickListener(v -> {
                        com.hafiztraveltours.app.utils.HapticUtil.click(v);
                        config.withBed = false;
                        renderChildDetails();
                        updateUi();
                    });

                    bedSegmentRow.addView(btnWithBed);
                    bedSegmentRow.addView(btnWithoutBed);
                    bedContainer.addView(bedSegmentRow);

                    card.addView(bedContainer);
                }
            }

            containerChildDetails.addView(card);
        }
    }

    private List<PackageDetail.DepartureOption> getDepartureOptions() {
        if (detail != null && detail.availableDepartures != null && !detail.availableDepartures.isEmpty()) {
            return detail.availableDepartures;
        }
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
                renderChildDetails();
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

        double calculatedSubtotal = unitAmount * paxCount;
        String depDateIso = getSelectedDepartureDate();

        for (int i = 0; i < kidsCount && i < childConfigs.size(); i++) {
            BookingRequest.ChildConfig cfg = childConfigs.get(i);
            if (cfg.dateOfBirth != null && !cfg.dateOfBirth.trim().isEmpty()) {
                try {
                    double childPrice = com.hafiztraveltours.app.utils.PackagePricingCalculator.calculatePassengerPrice(
                            detail, selectedSeason, detail.isUmrah, unitAmount, cfg.dateOfBirth.trim(), depDateIso, cfg.withBed);
                    calculatedSubtotal += childPrice;
                } catch (com.hafiztraveltours.app.utils.PackagePricingCalculator.PricingConfigurationException e) {
                    calculatedSubtotal += unitAmount;
                }
            } else {
                calculatedSubtotal += unitAmount;
            }
        }

        double totalAmount = Math.max(0, calculatedSubtotal - appliedDiscount);

        if (txtTotalAmount != null) {
            txtTotalAmount.setText(BookingRequest.formatPrice(totalAmount));
        }
        if (txtUnitPriceDetail != null) {
            if (kidsCount > 0) {
                txtUnitPriceDetail.setText(getString(R.string.room_price_x_pax_format, roomPriceStr, totalPax));
            } else {
                txtUnitPriceDetail.setText(getString(R.string.room_price_x_pax_format, roomPriceStr, paxCount));
            }
        }

        renderCostBreakdown();
    }

    private void renderCostBreakdown() {
        if (containerCostBreakdownItems == null) return;
        containerCostBreakdownItems.removeAllViews();

        double unitAmount = getSelectedUnitAmount();
        String depDateIso = getSelectedDepartureDate();

        // 1. Adult(s) item card
        LinearLayout adultCard = createBreakdownItemCard();
        double adultTotal = unitAmount * paxCount;
        String adultHeaderStr = getString(R.string.cost_breakdown_adult_format, paxCount);
        String adultPriceStr = BookingRequest.formatPrice(adultTotal);

        addBreakdownHeader(adultCard, adultHeaderStr, adultPriceStr, true);

        if (paxCount > 1) {
            addBreakdownLine(adultCard, getString(R.string.cost_breakdown_base_package), BookingRequest.formatPrice(unitAmount) + " " + getString(R.string.detail_per_pax));
        } else {
            addBreakdownLine(adultCard, getString(R.string.cost_breakdown_base_package), BookingRequest.formatPrice(unitAmount));
        }

        containerCostBreakdownItems.addView(adultCard);

        // 2. Children item cards
        for (int i = 0; i < kidsCount && i < childConfigs.size(); i++) {
            BookingRequest.ChildConfig cfg = childConfigs.get(i);
            boolean hasDob = cfg.dateOfBirth != null && !cfg.dateOfBirth.trim().isEmpty();

            LinearLayout childCard = createBreakdownItemCard();

            if (!hasDob) {
                String title = getString(R.string.cost_breakdown_child_title_dob_required, i + 1);
                addBreakdownHeader(childCard, title, BookingRequest.formatPrice(unitAmount), false);
                addBreakdownLine(childCard, getString(R.string.cost_breakdown_base_package), BookingRequest.formatPrice(unitAmount));
            } else {
                int age = calculateChildAge(cfg.dateOfBirth.trim(), depDateIso);
                double childPrice = unitAmount;

                try {
                    childPrice = com.hafiztraveltours.app.utils.PackagePricingCalculator.calculatePassengerPrice(
                            detail, selectedSeason, detail.isUmrah, unitAmount, cfg.dateOfBirth.trim(), depDateIso, cfg.withBed);
                } catch (com.hafiztraveltours.app.utils.PackagePricingCalculator.PricingConfigurationException e) {
                    childPrice = unitAmount;
                }

                String childTitle;
                if (age < 2) {
                    childTitle = getString(R.string.cost_breakdown_child_title_under_2_format, i + 1);
                } else {
                    childTitle = getString(R.string.cost_breakdown_child_title_format, i + 1, age);
                }

                addBreakdownHeader(childCard, childTitle, BookingRequest.formatPrice(childPrice), true);

                // Base Package line
                addBreakdownLine(childCard, getString(R.string.cost_breakdown_base_package), BookingRequest.formatPrice(unitAmount));

                // Age category & Bed lines
                if (detail.isUmrah) {
                    if (age < 2) {
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_category_label), getString(R.string.category_under_2));
                        double diff = unitAmount - childPrice;
                        if (diff > 0) {
                            addBreakdownLine(childCard, getString(R.string.cost_breakdown_adjustment), "- " + BookingRequest.formatPrice(diff));
                        }
                    } else if (age >= 2 && age <= 4) {
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_category_label), getString(R.string.category_2_to_4));
                        double diff = unitAmount - childPrice;
                        if (diff > 0) {
                            addBreakdownLine(childCard, getString(R.string.cost_breakdown_adjustment), "- " + BookingRequest.formatPrice(diff));
                        }
                    } else {
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_category_label), getString(R.string.category_adult_rate));
                    }
                } else {
                    // TOUR package
                    if (age < 2) {
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_category_label), getString(R.string.category_under_2));
                        double diff = unitAmount - childPrice;
                        if (diff > 0) {
                            addBreakdownLine(childCard, getString(R.string.cost_breakdown_adjustment), "- " + BookingRequest.formatPrice(diff));
                        }
                    } else if (age >= 2 && age <= 11) {
                        boolean withBed = Boolean.TRUE.equals(cfg.withBed);
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_bed_label), withBed ? getString(R.string.cost_breakdown_yes) : getString(R.string.cost_breakdown_no));

                        double diff = unitAmount - childPrice;
                        if (diff > 0) {
                            String adjLabel = withBed ? getString(R.string.cost_breakdown_bed_adjustment) : getString(R.string.cost_breakdown_adjustment);
                            addBreakdownLine(childCard, adjLabel, "- " + BookingRequest.formatPrice(diff));
                        }
                    } else {
                        addBreakdownLine(childCard, getString(R.string.cost_breakdown_category_label), getString(R.string.category_adult_rate));
                    }
                }

                addBreakdownLine(childCard, getString(R.string.cost_breakdown_final_child_price), BookingRequest.formatPrice(childPrice), true);
            }

            containerCostBreakdownItems.addView(childCard);
        }

        // 3. Promo discount card (if applied)
        if (appliedDiscount > 0) {
            LinearLayout promoCard = createBreakdownItemCard();
            String promoLabel = getString(R.string.cost_breakdown_promo_discount, appliedPromoCode);
            addBreakdownHeader(promoCard, promoLabel, "- " + BookingRequest.formatPrice(appliedDiscount), false);
            containerCostBreakdownItems.addView(promoCard);
        }
    }

    private LinearLayout createBreakdownItemCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_luxury_form_field);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        card.setLayoutParams(lp);
        return card;
    }

    private void addBreakdownHeader(LinearLayout container, String titleText, String priceText, boolean isHighlight) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(requireContext());
        title.setText(titleText);
        title.setTextSize(12);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(isHighlight ? R.color.text_dark : R.color.text_gray));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);

        TextView price = new TextView(requireContext());
        price.setText(priceText);
        price.setTextSize(13);
        price.setTypeface(null, Typeface.BOLD);
        price.setTextColor(getResources().getColor(R.color.brand_magenta));

        row.addView(title);
        row.addView(price);
        container.addView(row);
    }

    private void addBreakdownLine(LinearLayout container, String labelText, String valueText) {
        addBreakdownLine(container, labelText, valueText, false);
    }

    private void addBreakdownLine(LinearLayout container, String labelText, String valueText, boolean isBoldValue) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(3);
        row.setLayoutParams(lp);

        TextView label = new TextView(requireContext());
        label.setText("  • " + labelText);
        label.setTextSize(11);
        label.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        label.setLayoutParams(labelLp);

        TextView value = new TextView(requireContext());
        value.setText(valueText);
        value.setTextSize(11);
        value.setTypeface(null, isBoldValue ? Typeface.BOLD : Typeface.NORMAL);
        value.setTextColor(getResources().getColor(isBoldValue ? R.color.brand_magenta : R.color.text_dark));

        row.addView(label);
        row.addView(value);
        container.addView(row);
    }

    private void proceedToTravellerDetails() {
        SessionManager session = new SessionManager(requireContext());
        if (!session.isLoggedIn()) {
            dismiss();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }

        if (kidsCount > 0) {
            String depDateIso = getSelectedDepartureDate();
            for (int i = 0; i < kidsCount && i < childConfigs.size(); i++) {
                BookingRequest.ChildConfig cfg = childConfigs.get(i);
                if (cfg.dateOfBirth == null || cfg.dateOfBirth.trim().isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), getString(R.string.err_child_dob_required, i + 1), android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                if (!detail.isUmrah) {
                    int age = calculateChildAge(cfg.dateOfBirth.trim(), depDateIso);
                    if (age >= 2 && age <= 11) {
                        if (cfg.withBed == null) {
                            android.widget.Toast.makeText(requireContext(), getString(R.string.err_child_bed_required, i + 1), android.widget.Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
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
        req.childConfigs = new java.util.ArrayList<>(childConfigs);
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
        req.discountAmount = 0.0;

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
