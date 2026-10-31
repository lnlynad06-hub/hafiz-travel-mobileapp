package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.BookingRequest;
import com.hafiztraveltours.app.models.PackageDetail;
import com.hafiztraveltours.app.models.PackageUpgradeDto;
import com.hafiztraveltours.app.models.QuoteRequest;
import com.hafiztraveltours.app.models.QuoteResponse;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.utils.HapticUtil;
import com.hafiztraveltours.app.utils.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuidedCustomizationActivity extends BaseActivity {

    public static final String EXTRA_BOOKING_REQUEST = "extra_booking_request";

    private static final String KEY_CURRENT_STEP = "state_current_step";
    private static final String KEY_MAKKAH_UPGRADE = "state_makkah_upgrade";
    private static final String KEY_MADINAH_UPGRADE = "state_madinah_upgrade";
    private static final String KEY_MEALS_UPGRADE = "state_meals_upgrade";
    private static final String KEY_TRAIN_UPGRADE = "state_train_upgrade";
    private static final String KEY_TRANSPORT_UPGRADE = "state_transport_upgrade";
    private static final String KEY_ACCESSORIES_UPGRADE = "state_accessories_upgrade";
    private static final String KEY_LUGGAGE_ALLOCATIONS = "state_luggage_allocations";

    // Step identifiers
    public static final int STEP_HOTEL_MAKKAH = 0;
    public static final int STEP_HOTEL_MADINAH = 1;
    public static final int STEP_FULL_BOARD = 2;
    public static final int STEP_SPEED_TRAIN = 3;
    public static final int STEP_TRANSPORTATION = 4;
    public static final int STEP_LUGGAGE = 5;
    public static final int STEP_ACCESSORIES = 6;
    public static final int STEP_REVIEW = 7;
    public static final int TOTAL_STEPS = 8;

    private int currentStep = STEP_HOTEL_MAKKAH;
    private BookingRequest bookingRequest;
    private PackageDetail packageDetail;
    private int paxCount = 1;

    // Selections state
    private PackageUpgradeDto selectedMakkahUpgrade = null;
    private PackageUpgradeDto selectedMadinahUpgrade = null;
    private PackageUpgradeDto selectedFullBoardUpgrade = null;
    private PackageUpgradeDto selectedSpeedTrainUpgrade = null;
    private PackageUpgradeDto selectedTransportUpgrade = null; // Car or Van
    private PackageUpgradeDto selectedAccessoriesUpgrade = null;

    // Luggage allocations: pilgrimIndex -> Map<upgradeId, quantity>
    private final Map<Integer, Map<Integer, Integer>> luggageAllocations = new HashMap<>();

    // UI elements
    private TextView txtStepIndicator;
    private TextView txtStepTitle;
    private TextView txtStepDescription;
    private TextView txtStepNotice;
    private LinearLayout containerStepOptions;
    private ProgressBar progressBar;
    private TextView txtBottomBarLabel;
    private TextView txtBottomBarAddonsSubtitle;
    private TextView txtBottomBarTotalAmount;
    private MaterialButton btnStepBack;
    private MaterialButton btnStepNext;

    private Call<ApiResponse<QuoteResponse>> quoteCall;
    private QuoteResponse authoritativeQuote = null;
    private int quoteSequence = 0;

    @Override
    protected void onDestroy() {
        if (quoteCall != null) {
            quoteCall.cancel();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_STEP, currentStep);
        if (bookingRequest != null) outState.putSerializable(EXTRA_BOOKING_REQUEST, bookingRequest);
        if (selectedMakkahUpgrade != null) outState.putSerializable(KEY_MAKKAH_UPGRADE, selectedMakkahUpgrade);
        if (selectedMadinahUpgrade != null) outState.putSerializable(KEY_MADINAH_UPGRADE, selectedMadinahUpgrade);
        if (selectedFullBoardUpgrade != null) outState.putSerializable(KEY_MEALS_UPGRADE, selectedFullBoardUpgrade);
        if (selectedSpeedTrainUpgrade != null) outState.putSerializable(KEY_TRAIN_UPGRADE, selectedSpeedTrainUpgrade);
        if (selectedTransportUpgrade != null) outState.putSerializable(KEY_TRANSPORT_UPGRADE, selectedTransportUpgrade);
        if (selectedAccessoriesUpgrade != null) outState.putSerializable(KEY_ACCESSORIES_UPGRADE, selectedAccessoriesUpgrade);
        outState.putSerializable(KEY_LUGGAGE_ALLOCATIONS, new HashMap<>(luggageAllocations));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_customization);

        bookingRequest = (BookingRequest) getIntent().getSerializableExtra(EXTRA_BOOKING_REQUEST);
        if (bookingRequest == null || bookingRequest.packageDetail == null || !bookingRequest.isUmrahPackage()) {
            if (bookingRequest != null && !bookingRequest.isUmrahPackage()) {
                Intent fallback = new Intent(this, PassengerDetailsActivity.class);
                fallback.putExtra(PassengerDetailsActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
                startActivity(fallback);
            }
            finish();
            return;
        }

        packageDetail = bookingRequest.packageDetail;
        paxCount = Math.max(1, bookingRequest.adultPaxCount + bookingRequest.kidsPaxCount);

        initViews();

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_BOOKING_REQUEST)) {
            bookingRequest = (BookingRequest) savedInstanceState.getSerializable(EXTRA_BOOKING_REQUEST);
        }
        currentStep = savedInstanceState.getInt(KEY_CURRENT_STEP, STEP_HOTEL_MAKKAH);
            if (savedInstanceState.containsKey(KEY_MAKKAH_UPGRADE)) {
                selectedMakkahUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_MAKKAH_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_MADINAH_UPGRADE)) {
                selectedMadinahUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_MADINAH_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_MEALS_UPGRADE)) {
                selectedFullBoardUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_MEALS_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_TRAIN_UPGRADE)) {
                selectedSpeedTrainUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_TRAIN_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_TRANSPORT_UPGRADE)) {
                selectedTransportUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_TRANSPORT_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_ACCESSORIES_UPGRADE)) {
                selectedAccessoriesUpgrade = (PackageUpgradeDto) savedInstanceState.getSerializable(KEY_ACCESSORIES_UPGRADE);
            }
            if (savedInstanceState.containsKey(KEY_LUGGAGE_ALLOCATIONS)) {
                Map<?, ?> map = (Map<?, ?>) savedInstanceState.getSerializable(KEY_LUGGAGE_ALLOCATIONS);
                if (map != null) {
                    luggageAllocations.clear();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() instanceof Integer && entry.getValue() instanceof Map) {
                            Map<?, ?> subMap = (Map<?, ?>) entry.getValue();
                            Map<Integer, Integer> typedSub = new HashMap<>();
                            for (Map.Entry<?, ?> subEntry : subMap.entrySet()) {
                                if (subEntry.getKey() instanceof Integer && subEntry.getValue() instanceof Integer) {
                                    typedSub.put((Integer) subEntry.getKey(), (Integer) subEntry.getValue());
                                }
                            }
                            luggageAllocations.put((Integer) entry.getKey(), typedSub);
                        }
                    }
                }
            }
        } else {
            restoreSelectionsIfAvailable();
        }

        renderCurrentStep();
    }

    private void initViews() {
        // Setup Unified 5-Stage Booking Funnel Header
        View funnelHeader = findViewById(R.id.funnelHeader);
        if (funnelHeader != null) {
            BookingFunnelHelper.setup(funnelHeader, true, BookingFunnelHelper.STAGE_CUSTOMIZE);
        }

        findViewById(R.id.btnCustomizationBack).setOnClickListener(v -> {
            HapticUtil.click(v);
            handleBackNavigation();
        });

        txtStepIndicator = findViewById(R.id.txtCustomizationStepIndicator);
        txtStepTitle = findViewById(R.id.txtStepTitle);
        txtStepDescription = findViewById(R.id.txtStepDescription);
        txtStepNotice = findViewById(R.id.txtStepNotice);
        containerStepOptions = findViewById(R.id.containerStepOptions);
        progressBar = findViewById(R.id.progressBarCustomization);
        txtBottomBarLabel = findViewById(R.id.txtBottomBarLabel);
        txtBottomBarAddonsSubtitle = findViewById(R.id.txtBottomBarAddonsSubtitle);
        txtBottomBarTotalAmount = findViewById(R.id.txtBottomBarTotalAmount);
        btnStepBack = findViewById(R.id.btnStepBack);
        btnStepNext = findViewById(R.id.btnStepNext);

        btnStepBack.setOnClickListener(v -> {
            HapticUtil.click(v);
            handleBackNavigation();
        });

        btnStepNext.setOnClickListener(v -> {
            HapticUtil.click(v);
            handleNextNavigation();
        });
    }

    private void restoreSelectionsIfAvailable() {
        if (bookingRequest.selectedUpgrades == null || bookingRequest.selectedUpgrades.isEmpty()) {
            return;
        }
        for (BookingRequest.SelectedUpgrade su : bookingRequest.selectedUpgrades) {
            PackageUpgradeDto dto = findUpgradeById(su.upgradeId);
            if (dto == null) continue;

            switch (dto.category) {
                case PackageUpgradeDto.CATEGORY_HOTEL_MAKKAH:
                    selectedMakkahUpgrade = dto;
                    break;
                case PackageUpgradeDto.CATEGORY_HOTEL_MADINAH:
                    selectedMadinahUpgrade = dto;
                    break;
                case PackageUpgradeDto.CATEGORY_MEALS:
                    selectedFullBoardUpgrade = dto;
                    break;
                case PackageUpgradeDto.CATEGORY_SPEED_TRAIN:
                    selectedSpeedTrainUpgrade = dto;
                    break;
                case PackageUpgradeDto.CATEGORY_TRANSPORT:
                    selectedTransportUpgrade = dto;
                    break;
                case PackageUpgradeDto.CATEGORY_LUGGAGE:
                    int tIdx = su.travellerIndex != null ? su.travellerIndex : 0;
                    Map<Integer, Integer> map = luggageAllocations.computeIfAbsent(tIdx, k -> new HashMap<>());
                    map.put(dto.id, su.quantity);
                    break;
                case PackageUpgradeDto.CATEGORY_ACCESSORIES:
                    selectedAccessoriesUpgrade = dto;
                    break;
            }
        }
    }

    private PackageUpgradeDto findUpgradeById(int id) {
        if (packageDetail.availableUpgrades != null) {
            for (PackageUpgradeDto u : packageDetail.availableUpgrades) {
                if (u.id == id) return u;
            }
        }
        return null;
    }

    private List<PackageUpgradeDto> getUpgradesByCategory(String category) {
        List<PackageUpgradeDto> list = new ArrayList<>();
        if (packageDetail.availableUpgrades != null) {
            for (PackageUpgradeDto u : packageDetail.availableUpgrades) {
                if (category.equalsIgnoreCase(u.category)) {
                    list.add(u);
                }
            }
        }
        return list;
    }

    private String getStepName(int step) {
        switch (step) {
            case STEP_HOTEL_MAKKAH:
                return getString(R.string.step_hotel_makkah_title);
            case STEP_HOTEL_MADINAH:
                return getString(R.string.step_hotel_madinah_title);
            case STEP_FULL_BOARD:
                return getString(R.string.step_meals_title);
            case STEP_SPEED_TRAIN:
                return getString(R.string.step_train_title);
            case STEP_TRANSPORTATION:
                return getString(R.string.step_transport_title);
            case STEP_LUGGAGE:
                return getString(R.string.step_luggage_title);
            case STEP_ACCESSORIES:
                return getString(R.string.step_accessories_title);
            case STEP_REVIEW:
                return getString(R.string.step_review_title);
            default:
                return "";
        }
    }

    private void renderCurrentStep() {
        containerStepOptions.removeAllViews();
        txtStepNotice.setVisibility(View.GONE);

        String stepPrefix = getString(R.string.customization_step_format, (currentStep + 1), TOTAL_STEPS);
        txtStepIndicator.setText(stepPrefix + " · " + getStepName(currentStep));
        progressBar.setProgress(currentStep + 1);

        btnStepBack.setText(currentStep == 0 ? getString(R.string.btn_back) : getString(R.string.btn_back_step));
        btnStepNext.setText(currentStep == STEP_REVIEW ? getString(R.string.btn_confirm_customization) : getString(R.string.btn_next_step));

        switch (currentStep) {
            case STEP_HOTEL_MAKKAH:
                renderHotelStep(true);
                break;
            case STEP_HOTEL_MADINAH:
                renderHotelStep(false);
                break;
            case STEP_FULL_BOARD:
                renderFullBoardStep();
                break;
            case STEP_SPEED_TRAIN:
                renderSpeedTrainStep();
                break;
            case STEP_TRANSPORTATION:
                renderTransportationStep();
                break;
            case STEP_LUGGAGE:
                renderLuggageStep();
                break;
            case STEP_ACCESSORIES:
                renderAccessoriesStep();
                break;
            case STEP_REVIEW:
                renderReviewStep();
                break;
        }

        updateBottomPriceBar();
    }

    // -------------------------------------------------------------
    // STEP 1 & 2: HOTEL UPGRADES (MAKKAH & MADINAH)
    // -------------------------------------------------------------
    private void renderHotelStep(boolean isMakkah) {
        txtStepTitle.setText(isMakkah ? getString(R.string.step_hotel_makkah_title) : getString(R.string.step_hotel_madinah_title));
        txtStepDescription.setText(isMakkah ? getString(R.string.step_hotel_makkah_desc) : getString(R.string.step_hotel_madinah_desc));

        String category = isMakkah ? PackageUpgradeDto.CATEGORY_HOTEL_MAKKAH : PackageUpgradeDto.CATEGORY_HOTEL_MADINAH;
        List<PackageUpgradeDto> upgrades = getUpgradesByCategory(category);

        String origName = isMakkah ? (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMakkahName : getString(R.string.customization_original_hotel))
                : (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMadinahName : getString(R.string.customization_original_hotel));
        String origDist = isMakkah ? (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMakkahDistance : "")
                : (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMadinahDistance : "");
        String origTier = isMakkah ? packageDetail.hotelMakkahDistanceTier : packageDetail.hotelMadinahDistanceTier;

        PackageUpgradeDto currentSelected = isMakkah ? selectedMakkahUpgrade : selectedMadinahUpgrade;

        // Card 1: Original Hotel Option (Free / Included)
        boolean isOrigSelected = (currentSelected == null);
        View origCard = createHotelCardView(
                origName != null ? origName : getString(R.string.original_package_hotel),
                getString(R.string.original_package_hotel),
                origDist,
                origTier,
                0.0,
                isOrigSelected,
                v -> {
                    if (isMakkah) {
                        selectedMakkahUpgrade = null;
                    } else {
                        selectedMadinahUpgrade = null;
                    }
                    validateFullBoardEligibilityPostHotelChange();
                    renderCurrentStep();
                }
        );
        containerStepOptions.addView(origCard);

        // Upgrade Hotel Cards
        for (PackageUpgradeDto u : upgrades) {
            boolean isSel = (currentSelected != null && currentSelected.id == u.id);
            View uCard = createHotelCardView(
                    u.name,
                    u.hotelStarRating != null ? u.hotelStarRating : getString(R.string.customization_selected_hotel),
                    u.hotelDistanceText,
                    u.distanceTier,
                    u.getPrice(),
                    isSel,
                    v -> {
                        if (isMakkah) {
                            selectedMakkahUpgrade = u;
                        } else {
                            selectedMadinahUpgrade = u;
                        }
                        validateFullBoardEligibilityPostHotelChange();
                        renderCurrentStep();
                    }
            );
            containerStepOptions.addView(uCard);
        }
    }

    private View createHotelCardView(String name, String sub, String dist, String tier, double price, boolean isSelected, View.OnClickListener onClick) {
        CardView card = new CardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(isSelected ? dp(4) : dp(1));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(isSelected ? ContextCompat.getColor(this, R.color.brand_magenta_soft) : ContextCompat.getColor(this, R.color.surface));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.setBackgroundResource(isSelected ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        root.setOnClickListener(onClick);

        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setGravity(Gravity.CENTER_VERTICAL);

        RadioButton rb = new RadioButton(this);
        rb.setChecked(isSelected);
        rb.setClickable(false);
        rb.setFocusable(false);
        rowTop.addView(rb);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);
        info.setPadding(dp(8), 0, dp(8), 0);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(14);
        tvName.setMaxLines(3);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        info.addView(tvName);

        if (sub != null && !sub.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(sub);
            tvSub.setTextSize(11);
            tvSub.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            info.addView(tvSub);
        }

        // Distance & Tier Row
        if ((dist != null && !dist.trim().isEmpty()) || (tier != null && !tier.trim().isEmpty())) {
            LinearLayout locRow = new LinearLayout(this);
            locRow.setOrientation(LinearLayout.HORIZONTAL);
            locRow.setGravity(Gravity.CENTER_VERTICAL);
            locRow.setPadding(0, dp(4), 0, 0);

            if (dist != null && !dist.trim().isEmpty()) {
                TextView tvDist = new TextView(this);
                tvDist.setText(getString(R.string.customization_distance_prefix) + ": " + dist.trim());
                tvDist.setTextSize(11);
                tvDist.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                locRow.addView(tvDist);
            }

            if (tier != null && !tier.trim().isEmpty()) {
                TextView tvTier = new TextView(this);
                tvTier.setText(formatDistanceTier(tier));
                tvTier.setTextSize(10);
                tvTier.setTypeface(null, Typeface.BOLD);
                tvTier.setPadding(dp(6), dp(2), dp(6), dp(2));
                tvTier.setBackgroundResource(isNearOrModerate(tier) ? R.drawable.bg_status_verified : R.drawable.bg_status_rejected);
                tvTier.setTextColor(ContextCompat.getColor(this, isNearOrModerate(tier) ? R.color.status_paid_text : R.color.brand_magenta));
                LinearLayout.LayoutParams tierLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tierLp.leftMargin = dp(6);
                tvTier.setLayoutParams(tierLp);
                locRow.addView(tvTier);
            }

            info.addView(locRow);
        }

        rowTop.addView(info);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(price > 0 ? ("+" + MoneyFormat.formatRM(price) + " " + getString(R.string.per_pax_label)) : getString(R.string.customization_included_badge));
        tvPrice.setTextSize(12);
        tvPrice.setGravity(Gravity.END);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setTextColor(price > 0 ? ContextCompat.getColor(this, R.color.brand_magenta) : ContextCompat.getColor(this, R.color.status_paid_text));
        rowTop.addView(tvPrice);

        root.addView(rowTop);
        card.addView(root);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private String formatDistanceTier(String tier) {
        if (tier == null) return getString(R.string.hotel_tier_unverified);
        switch (tier.toLowerCase().trim()) {
            case "near": return getString(R.string.hotel_tier_near);
            case "moderate": return getString(R.string.hotel_tier_moderate);
            case "far": return getString(R.string.hotel_tier_far);
            default: return getString(R.string.hotel_tier_unverified);
        }
    }

    private boolean isNearOrModerate(String tier) {
        if (tier == null) return false;
        String t = tier.trim().toLowerCase();
        return "near".equals(t) || "moderate".equals(t);
    }

    private boolean checkFullBoardEligibility() {
        String mTier = selectedMakkahUpgrade != null ? selectedMakkahUpgrade.distanceTier : packageDetail.hotelMakkahDistanceTier;
        String dTier = selectedMadinahUpgrade != null ? selectedMadinahUpgrade.distanceTier : packageDetail.hotelMadinahDistanceTier;
        return isNearOrModerate(mTier) && isNearOrModerate(dTier);
    }

    private void validateFullBoardEligibilityPostHotelChange() {
        if (selectedFullBoardUpgrade != null && !checkFullBoardEligibility()) {
            selectedFullBoardUpgrade = null;
            Toast.makeText(this, R.string.full_board_removed_notice, Toast.LENGTH_LONG).show();
        }
    }

    // -------------------------------------------------------------
    // STEP 3: FULL BOARD MEALS
    // -------------------------------------------------------------
    private void renderFullBoardStep() {
        txtStepTitle.setText(getString(R.string.step_meals_title));
        txtStepDescription.setText(getString(R.string.step_meals_desc));

        boolean includedInPackage = packageDetail.rawPackage != null && packageDetail.rawPackage.isFeatureIncluded("full_board_meals");
        if (includedInPackage) {
            showAlreadyIncludedNotice(getString(R.string.feature_already_included));
            return;
        }

        boolean eligible = checkFullBoardEligibility();
        if (!eligible) {
            txtStepNotice.setText(getString(R.string.full_board_ineligible_notice));
            txtStepNotice.setVisibility(View.VISIBLE);
        }

        List<PackageUpgradeDto> mealUpgrades = getUpgradesByCategory(PackageUpgradeDto.CATEGORY_MEALS);
        if (mealUpgrades.isEmpty()) {
            showEmptyOptionsNotice(getString(R.string.customization_empty_meals));
            return;
        }

        for (PackageUpgradeDto u : mealUpgrades) {
            boolean isSel = (selectedFullBoardUpgrade != null && selectedFullBoardUpgrade.id == u.id);
            View card = createToggleCardView(
                    u.name,
                    u.description != null ? u.description : getString(R.string.customization_default_meals_desc),
                    u.getPrice(),
                    true,
                    isSel,
                    eligible,
                    v -> {
                        if (!eligible) {
                            Toast.makeText(this, R.string.full_board_ineligible_notice, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (selectedFullBoardUpgrade != null && selectedFullBoardUpgrade.id == u.id) {
                            selectedFullBoardUpgrade = null;
                        } else {
                            selectedFullBoardUpgrade = u;
                        }
                        renderCurrentStep();
                    }
            );
            containerStepOptions.addView(card);
        }
    }

    // -------------------------------------------------------------
    // STEP 4: SPEED TRAIN
    // -------------------------------------------------------------
    private void renderSpeedTrainStep() {
        txtStepTitle.setText(getString(R.string.step_train_title));
        txtStepDescription.setText(getString(R.string.step_train_desc));

        boolean included = packageDetail.rawPackage != null && packageDetail.rawPackage.isFeatureIncluded("speed_train");
        if (included) {
            showAlreadyIncludedNotice(getString(R.string.feature_already_included));
            return;
        }

        List<PackageUpgradeDto> trainUpgrades = getUpgradesByCategory(PackageUpgradeDto.CATEGORY_SPEED_TRAIN);
        if (trainUpgrades.isEmpty()) {
            showEmptyOptionsNotice(getString(R.string.customization_empty_train));
            return;
        }

        for (PackageUpgradeDto u : trainUpgrades) {
            boolean isSel = (selectedSpeedTrainUpgrade != null && selectedSpeedTrainUpgrade.id == u.id);
            View card = createToggleCardView(
                    u.name,
                    u.description != null ? u.description : getString(R.string.customization_default_train_desc),
                    u.getPrice(),
                    true,
                    isSel,
                    true,
                    v -> {
                        if (selectedSpeedTrainUpgrade != null && selectedSpeedTrainUpgrade.id == u.id) {
                            selectedSpeedTrainUpgrade = null;
                        } else {
                            selectedSpeedTrainUpgrade = u;
                        }
                        renderCurrentStep();
                    }
            );
            containerStepOptions.addView(card);
        }
    }

    // -------------------------------------------------------------
    // STEP 5: PRIVATE TRANSPORTATION (CAR / VAN)
    // -------------------------------------------------------------
    private void renderTransportationStep() {
        txtStepTitle.setText(getString(R.string.step_transport_title));
        txtStepDescription.setText(getString(R.string.step_transport_desc));

        List<PackageUpgradeDto> transportUpgrades = getUpgradesByCategory(PackageUpgradeDto.CATEGORY_TRANSPORT);
        if (transportUpgrades.isEmpty()) {
            showEmptyOptionsNotice(getString(R.string.customization_empty_transport));
            return;
        }

        // Option: No Private Transport (Standard Group Bus)
        boolean isNone = (selectedTransportUpgrade == null);
        View standardCard = createToggleCardView(
                getString(R.string.customization_transport_bus_title),
                getString(R.string.customization_transport_bus_desc),
                0.0,
                false,
                isNone,
                true,
                v -> {
                    selectedTransportUpgrade = null;
                    renderCurrentStep();
                }
        );
        containerStepOptions.addView(standardCard);

        for (PackageUpgradeDto u : transportUpgrades) {
            boolean isSel = (selectedTransportUpgrade != null && selectedTransportUpgrade.id == u.id);
            View card = createToggleCardView(
                    u.name,
                    u.description != null ? u.description : (u.serviceScope != null ? u.serviceScope : getString(R.string.customization_transport_all_routes)),
                    u.getPrice(),
                    u.isPerPax(),
                    isSel,
                    true,
                    v -> {
                        selectedTransportUpgrade = u;
                        renderCurrentStep();
                    }
            );
            containerStepOptions.addView(card);
        }
    }

    // -------------------------------------------------------------
    // STEP 6: LUGGAGE UPGRADES (PER PILGRIM ALLOCATION)
    // -------------------------------------------------------------
    private void renderLuggageStep() {
        txtStepTitle.setText(getString(R.string.step_luggage_title));
        txtStepDescription.setText(getString(R.string.step_luggage_desc));

        int includedLuggage = (packageDetail.customization != null && packageDetail.customization.packageInclusions != null)
                ? packageDetail.customization.packageInclusions.companyLuggageCount
                : (packageDetail.rawPackage != null && packageDetail.rawPackage.isFeatureIncluded("company_luggage") ? 1 : 0);

        if (includedLuggage >= 2) {
            showAlreadyIncludedNotice(getString(R.string.customization_luggage_quota_max));
            return;
        }

        TextView infoTv = new TextView(this);
        infoTv.setText(getString(R.string.luggage_included_info, includedLuggage) + " " + getString(R.string.customization_luggage_extra_note, (2 - includedLuggage)));
        infoTv.setTextSize(12);
        infoTv.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        infoTv.setPadding(0, 0, 0, dp(12));
        containerStepOptions.addView(infoTv);

        List<PackageUpgradeDto> luggageList = getUpgradesByCategory(PackageUpgradeDto.CATEGORY_LUGGAGE);
        if (luggageList.isEmpty()) {
            showEmptyOptionsNotice(getString(R.string.customization_empty_luggage));
            return;
        }

        int maxExtraPerPax = Math.max(0, 2 - includedLuggage);

        for (int pIdx = 0; pIdx < paxCount; pIdx++) {
            final int travellerIndex = pIdx;
            Map<Integer, Integer> pilgrimMap = luggageAllocations.computeIfAbsent(travellerIndex, k -> new HashMap<>());

            int currentTotalAllocated = 0;
            for (int qty : pilgrimMap.values()) {
                currentTotalAllocated += qty;
            }

            final int remainingAllowance = Math.max(0, maxExtraPerPax - currentTotalAllocated);

            CardView pCard = new CardView(this);
            pCard.setRadius(dp(14));
            pCard.setCardElevation(dp(2));
            pCard.setUseCompatPadding(true);
            pCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));

            LinearLayout pLayout = new LinearLayout(this);
            pLayout.setOrientation(LinearLayout.VERTICAL);
            pLayout.setPadding(dp(16), dp(12), dp(16), dp(12));

            // Pilgrim Header
            LinearLayout pHeader = new LinearLayout(this);
            pHeader.setOrientation(LinearLayout.VERTICAL);
            pHeader.setPadding(0, 0, 0, dp(6));

            TextView tvPilgrimTitle = new TextView(this);
            tvPilgrimTitle.setText(getString(R.string.luggage_allocated_format, (travellerIndex + 1), (includedLuggage + currentTotalAllocated)));
            tvPilgrimTitle.setTextSize(13);
            tvPilgrimTitle.setTypeface(null, Typeface.BOLD);
            tvPilgrimTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            pHeader.addView(tvPilgrimTitle);

            TextView tvAllowanceSub = new TextView(this);
            tvAllowanceSub.setText(getString(R.string.customization_luggage_allowance_sub, remainingAllowance));
            tvAllowanceSub.setTextSize(11);
            tvAllowanceSub.setTextColor(ContextCompat.getColor(this, remainingAllowance > 0 ? R.color.text_gray : R.color.status_paid_text));
            pHeader.addView(tvAllowanceSub);

            pLayout.addView(pHeader);

            // Products list under this pilgrim
            for (PackageUpgradeDto lug : luggageList) {
                final int currentQty = pilgrimMap.getOrDefault(lug.id, 0);

                LinearLayout itemRow = new LinearLayout(this);
                itemRow.setOrientation(LinearLayout.HORIZONTAL);
                itemRow.setGravity(Gravity.CENTER_VERTICAL);
                itemRow.setPadding(0, dp(8), 0, dp(8));

                LinearLayout lugInfo = new LinearLayout(this);
                lugInfo.setOrientation(LinearLayout.VERTICAL);
                lugInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvLugName = new TextView(this);
                tvLugName.setText(lug.name);
                tvLugName.setTextSize(13);
                tvLugName.setTypeface(null, Typeface.BOLD);
                tvLugName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                lugInfo.addView(tvLugName);

                TextView tvLugPrice = new TextView(this);
                tvLugPrice.setText("+" + MoneyFormat.formatRM(lug.getPrice()) + " " + getString(R.string.per_unit_label));
                tvLugPrice.setTextSize(11);
                tvLugPrice.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
                lugInfo.addView(tvLugPrice);

                itemRow.addView(lugInfo);

                // Stepper: [-] Qty [+]
                LinearLayout stepper = new LinearLayout(this);
                stepper.setOrientation(LinearLayout.HORIZONTAL);
                stepper.setGravity(Gravity.CENTER_VERTICAL);

                MaterialButton btnMinus = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnMinus.setText("-");
                btnMinus.setPadding(0, 0, 0, 0);
                btnMinus.setInsetBottom(0);
                btnMinus.setInsetTop(0);
                btnMinus.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
                btnMinus.setEnabled(currentQty > 0);
                btnMinus.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    if (currentQty > 0) {
                        pilgrimMap.put(lug.id, currentQty - 1);
                        renderCurrentStep();
                    }
                });
                stepper.addView(btnMinus);

                TextView tvQty = new TextView(this);
                tvQty.setText(String.valueOf(currentQty));
                tvQty.setTextSize(13);
                tvQty.setTypeface(null, Typeface.BOLD);
                tvQty.setGravity(Gravity.CENTER);
                tvQty.setWidth(dp(32));
                stepper.addView(tvQty);

                MaterialButton btnPlus = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                btnPlus.setText("+");
                btnPlus.setPadding(0, 0, 0, 0);
                btnPlus.setInsetBottom(0);
                btnPlus.setInsetTop(0);
                btnPlus.setLayoutParams(new LinearLayout.LayoutParams(dp(36), dp(36)));
                btnPlus.setEnabled(remainingAllowance > 0);
                btnPlus.setOnClickListener(v -> {
                    HapticUtil.click(v);
                    if (remainingAllowance > 0) {
                        pilgrimMap.put(lug.id, currentQty + 1);
                        renderCurrentStep();
                    } else {
                        Toast.makeText(this, R.string.luggage_limit_reached, Toast.LENGTH_SHORT).show();
                    }
                });
                stepper.addView(btnPlus);

                itemRow.addView(stepper);
                pLayout.addView(itemRow);
            }

            pCard.addView(pLayout);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8);
            pCard.setLayoutParams(lp);
            containerStepOptions.addView(pCard);
        }
    }

    // -------------------------------------------------------------
    // STEP 7: ACCESSORIES & MERCHANDISE
    // -------------------------------------------------------------
    private void renderAccessoriesStep() {
        txtStepTitle.setText(getString(R.string.step_accessories_title));
        txtStepDescription.setText(getString(R.string.step_accessories_desc));

        List<PackageUpgradeDto> accList = getUpgradesByCategory(PackageUpgradeDto.CATEGORY_ACCESSORIES);
        if (accList.isEmpty()) {
            showEmptyOptionsNotice(getString(R.string.customization_empty_accessories));
            return;
        }

        for (PackageUpgradeDto u : accList) {
            boolean isSel = (selectedAccessoriesUpgrade != null && selectedAccessoriesUpgrade.id == u.id);
            View card = createToggleCardView(
                    u.name,
                    u.description != null ? u.description : getString(R.string.customization_default_accessories_desc),
                    u.getPrice(),
                    u.isPerPax(),
                    isSel,
                    true,
                    v -> {
                        if (selectedAccessoriesUpgrade != null && selectedAccessoriesUpgrade.id == u.id) {
                            selectedAccessoriesUpgrade = null;
                        } else {
                            selectedAccessoriesUpgrade = u;
                        }
                        renderCurrentStep();
                    }
            );
            containerStepOptions.addView(card);
        }
    }

    // -------------------------------------------------------------
    // STEP 8: FINAL REVIEW & LIVE QUOTATION
    // -------------------------------------------------------------
    private void renderReviewStep() {
        txtStepTitle.setText(getString(R.string.step_review_title));
        txtStepDescription.setText(getString(R.string.step_review_desc));

        // Build list of active selections
        List<BookingRequest.SelectedUpgrade> currentList = buildCurrentSelectedUpgradesList();

        LinearLayout reviewBox = new LinearLayout(this);
        reviewBox.setOrientation(LinearLayout.VERTICAL);
        reviewBox.setBackgroundResource(R.drawable.bg_luxury_form_field);
        reviewBox.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView tvHeader = new TextView(this);
        tvHeader.setText(getString(R.string.customization_review_header));
        tvHeader.setTextSize(14);
        tvHeader.setTypeface(null, Typeface.BOLD);
        tvHeader.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        reviewBox.addView(tvHeader);

        // 1. Hotel Summary Row
        LinearLayout makkahRow = createReviewSummaryRow(
                "Makkah Hotel",
                selectedMakkahUpgrade != null ? selectedMakkahUpgrade.name : (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMakkahName : getString(R.string.original_package_hotel)),
                selectedMakkahUpgrade != null ? ("+" + MoneyFormat.formatRM(selectedMakkahUpgrade.getPrice() * paxCount)) : getString(R.string.customization_included_badge),
                selectedMakkahUpgrade != null
        );
        reviewBox.addView(makkahRow);

        LinearLayout madinahRow = createReviewSummaryRow(
                "Madinah Hotel",
                selectedMadinahUpgrade != null ? selectedMadinahUpgrade.name : (packageDetail.rawPackage != null ? packageDetail.rawPackage.hotelMadinahName : getString(R.string.original_package_hotel)),
                selectedMadinahUpgrade != null ? ("+" + MoneyFormat.formatRM(selectedMadinahUpgrade.getPrice() * paxCount)) : getString(R.string.customization_included_badge),
                selectedMadinahUpgrade != null
        );
        reviewBox.addView(madinahRow);

        // Divider
        View div1 = new View(this);
        div1.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div1.setBackgroundColor(ContextCompat.getColor(this, R.color.card_border));
        div1.setPadding(0, dp(8), 0, dp(8));
        reviewBox.addView(div1);

        // 2. Add-ons Breakdown
        if (currentList.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText(getString(R.string.customization_review_empty));
            tvEmpty.setTextSize(12);
            tvEmpty.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            tvEmpty.setPadding(0, dp(8), 0, 0);
            reviewBox.addView(tvEmpty);
        } else {
            for (BookingRequest.SelectedUpgrade su : currentList) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(6), 0, dp(6));

                TextView tvName = new TextView(this);
                String desc = su.name + (su.details != null ? " (" + su.details + ")" : "");
                tvName.setText("• " + desc);
                tvName.setTextSize(12);
                tvName.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(tvName);

                TextView tvPrice = new TextView(this);
                tvPrice.setText("+" + MoneyFormat.formatRM(su.calculatedTotal));
                tvPrice.setTextSize(12);
                tvPrice.setTypeface(null, Typeface.BOLD);
                tvPrice.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
                row.addView(tvPrice);

                reviewBox.addView(row);
            }
        }

        // Divider
        View div2 = new View(this);
        div2.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div2.setBackgroundColor(ContextCompat.getColor(this, R.color.card_border));
        reviewBox.addView(div2);

        // 3. Price Totals Card
        double basePkgTotal = bookingRequest.unitPriceAmount * paxCount;
        double addonsTotal = 0.0;
        for (BookingRequest.SelectedUpgrade su : currentList) {
            addonsTotal += su.calculatedTotal;
        }
        double estTotal = basePkgTotal + addonsTotal;

        LinearLayout rowBase = createReviewSummaryRow(
                getString(R.string.customization_review_base_package, paxCount),
                null,
                MoneyFormat.formatRM(basePkgTotal),
                false
        );
        reviewBox.addView(rowBase);

        if (addonsTotal > 0) {
            LinearLayout rowAddons = createReviewSummaryRow(
                    getString(R.string.subtotal_addons),
                    null,
                    "+" + MoneyFormat.formatRM(addonsTotal),
                    true
            );
            reviewBox.addView(rowAddons);
        }

        LinearLayout rowTotal = createReviewSummaryRow(
                getString(R.string.customization_review_grand_total),
                null,
                MoneyFormat.formatRM(estTotal),
                true
        );
        reviewBox.addView(rowTotal);

        containerStepOptions.addView(reviewBox);

        // Fetch live authoritative quotation
        fetchAuthoritativeQuote();
    }

    private LinearLayout createReviewSummaryRow(String label, String detail, String amount, boolean isHighlight) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout labelLayout = new LinearLayout(this);
        labelLayout.setOrientation(LinearLayout.VERTICAL);
        labelLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(12);
        tvLabel.setTypeface(null, isHighlight ? Typeface.BOLD : Typeface.NORMAL);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        labelLayout.addView(tvLabel);

        if (detail != null && !detail.isEmpty()) {
            TextView tvDetail = new TextView(this);
            tvDetail.setText(detail);
            tvDetail.setTextSize(11);
            tvDetail.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            labelLayout.addView(tvDetail);
        }
        row.addView(labelLayout);

        TextView tvAmt = new TextView(this);
        tvAmt.setText(amount);
        tvAmt.setTextSize(12);
        tvAmt.setTypeface(null, Typeface.BOLD);
        tvAmt.setTextColor(ContextCompat.getColor(this, isHighlight ? R.color.brand_magenta : R.color.text_dark));
        row.addView(tvAmt);

        return row;
    }

    private void fetchAuthoritativeQuote() {
        final int reqSeq = ++quoteSequence;
        txtStepNotice.setText(getString(R.string.quote_updating));
        txtStepNotice.setVisibility(View.VISIBLE);

        QuoteRequest qReq = new QuoteRequest();
        qReq.packageId = bookingRequest.packageId;
        try {
            qReq.departureId = Integer.parseInt(bookingRequest.selectedDepartureId);
        } catch (Exception ignored) {}

        if (bookingRequest.rooms != null) {
            for (BookingRequest.RoomConfig rc : bookingRequest.rooms) {
                QuoteRequest.RoomQuoteItem rqi = new QuoteRequest.RoomQuoteItem(rc.roomNumber, rc.roomType, rc.assignedTravellerIndices);
                rqi.gender = rc.gender;
                qReq.rooms.add(rqi);
            }
        }

        for (int i = 0; i < paxCount; i++) {
            QuoteRequest.TravellerQuoteItem tq = new QuoteRequest.TravellerQuoteItem();
            tq.index = i;
            tq.fullName = getString(R.string.customization_pilgrim_label, (i + 1));
            tq.isLead = (i == 0);
            qReq.travellers.add(tq);
        }

        List<BookingRequest.SelectedUpgrade> selections = buildCurrentSelectedUpgradesList();
        for (BookingRequest.SelectedUpgrade su : selections) {
            qReq.upgrades.add(new QuoteRequest.SelectedUpgradeItem(su.upgradeId, su.quantity, su.travellerIndex));
        }

        if (quoteCall != null) quoteCall.cancel();
        quoteCall = ApiClient.getApiService().getBookingQuote(qReq);
        quoteCall.enqueue(new Callback<ApiResponse<QuoteResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<QuoteResponse>> call, Response<ApiResponse<QuoteResponse>> response) {
                if (isFinishing() || isDestroyed() || reqSeq != quoteSequence || currentStep != STEP_REVIEW) return;
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    authoritativeQuote = response.body().data;
                    txtStepNotice.setText("✓ " + getString(R.string.server_quote_confirmed));
                    txtStepNotice.setBackgroundResource(R.drawable.bg_status_pending);
                    txtStepNotice.setTextColor(ContextCompat.getColor(GuidedCustomizationActivity.this, R.color.status_paid_text));
                    txtStepNotice.setVisibility(View.VISIBLE);

                    // Update bottom bar with server exact figure
                    txtBottomBarTotalAmount.setText(MoneyFormat.formatRM(authoritativeQuote.total));
                } else {
                    authoritativeQuote = null;
                    txtStepNotice.setText(getString(R.string.customization_quote_server_unavailable));
                    txtStepNotice.setVisibility(View.VISIBLE);
                    updateBottomPriceBar();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<QuoteResponse>> call, Throwable t) {
                if (isFinishing() || isDestroyed() || call.isCanceled() || reqSeq != quoteSequence || currentStep != STEP_REVIEW) return;
                authoritativeQuote = null;
                txtStepNotice.setText(getString(R.string.customization_quote_conn_error));
                txtStepNotice.setVisibility(View.VISIBLE);
                updateBottomPriceBar();
            }
        });
    }

    // -------------------------------------------------------------
    // CARD BUILDERS & HELPERS
    // -------------------------------------------------------------
    private View createToggleCardView(String title, String desc, double price, boolean isPerPax, boolean isSelected, boolean isEnabled, View.OnClickListener onClick) {
        CardView card = new CardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(isSelected ? dp(4) : dp(1));
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(isSelected ? ContextCompat.getColor(this, R.color.brand_magenta_soft) : ContextCompat.getColor(this, R.color.surface));
        card.setAlpha(isEnabled ? 1.0f : 0.5f);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(14));
        root.setBackgroundResource(isSelected ? R.drawable.bg_selection_card_selected : R.drawable.bg_selection_card_unselected);
        if (isEnabled) {
            root.setOnClickListener(onClick);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        RadioButton rb = new RadioButton(this);
        rb.setChecked(isSelected);
        rb.setEnabled(isEnabled);
        rb.setClickable(false);
        row.addView(rb);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.setPadding(dp(8), 0, dp(8), 0);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
        info.addView(tvTitle);

        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(desc);
            tvDesc.setTextSize(11);
            tvDesc.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            info.addView(tvDesc);
        }
        row.addView(info);

        TextView tvPrice = new TextView(this);
        if (price > 0) {
            tvPrice.setText("+" + MoneyFormat.formatRM(price) + " " + (isPerPax ? getString(R.string.per_pax_label) : getString(R.string.per_unit_label)));
            tvPrice.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        } else {
            tvPrice.setText(getString(R.string.customization_standard_badge));
            tvPrice.setTextColor(ContextCompat.getColor(this, R.color.status_paid_text));
        }
        tvPrice.setTextSize(12);
        tvPrice.setGravity(Gravity.END);
        tvPrice.setTypeface(null, Typeface.BOLD);
        row.addView(tvPrice);

        root.addView(row);
        card.addView(root);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private void showAlreadyIncludedNotice(String text) {
        TextView tv = new TextView(this);
        tv.setText("✓ " + text);
        tv.setTextSize(13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.status_paid_text));
        tv.setBackgroundResource(R.drawable.bg_status_pending);
        tv.setPadding(dp(14), dp(12), dp(14), dp(12));
        containerStepOptions.addView(tv);
    }

    private void showEmptyOptionsNotice(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        tv.setPadding(0, dp(12), 0, dp(12));
        containerStepOptions.addView(tv);
    }

    // -------------------------------------------------------------
    // BOTTOM RUNNING TOTAL & SELECTIONS HARVEST
    // -------------------------------------------------------------
    private List<BookingRequest.SelectedUpgrade> buildCurrentSelectedUpgradesList() {
        List<BookingRequest.SelectedUpgrade> list = new ArrayList<>();

        if (selectedMakkahUpgrade != null) {
            double total = selectedMakkahUpgrade.getPrice() * paxCount;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedMakkahUpgrade.id,
                    selectedMakkahUpgrade.category,
                    selectedMakkahUpgrade.name,
                    paxCount,
                    null,
                    selectedMakkahUpgrade.getPrice(),
                    total,
                    "Makkah Hotel Upgrade"
            ));
        }

        if (selectedMadinahUpgrade != null) {
            double total = selectedMadinahUpgrade.getPrice() * paxCount;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedMadinahUpgrade.id,
                    selectedMadinahUpgrade.category,
                    selectedMadinahUpgrade.name,
                    paxCount,
                    null,
                    selectedMadinahUpgrade.getPrice(),
                    total,
                    "Madinah Hotel Upgrade"
            ));
        }

        if (selectedFullBoardUpgrade != null) {
            double total = selectedFullBoardUpgrade.getPrice() * paxCount;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedFullBoardUpgrade.id,
                    selectedFullBoardUpgrade.category,
                    selectedFullBoardUpgrade.name,
                    paxCount,
                    null,
                    selectedFullBoardUpgrade.getPrice(),
                    total,
                    "Full Board Meals"
            ));
        }

        if (selectedSpeedTrainUpgrade != null) {
            double total = selectedSpeedTrainUpgrade.getPrice() * paxCount;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedSpeedTrainUpgrade.id,
                    selectedSpeedTrainUpgrade.category,
                    selectedSpeedTrainUpgrade.name,
                    paxCount,
                    null,
                    selectedSpeedTrainUpgrade.getPrice(),
                    total,
                    "Speed Train"
            ));
        }

        if (selectedTransportUpgrade != null) {
            int qty = selectedTransportUpgrade.isPerPax() ? paxCount : 1;
            double total = selectedTransportUpgrade.getPrice() * qty;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedTransportUpgrade.id,
                    selectedTransportUpgrade.category,
                    selectedTransportUpgrade.name,
                    qty,
                    null,
                    selectedTransportUpgrade.getPrice(),
                    total,
                    "Pengangkutan Persendirian"
            ));
        }

        for (Map.Entry<Integer, Map<Integer, Integer>> pilgrimEntry : luggageAllocations.entrySet()) {
            int tIdx = pilgrimEntry.getKey();
            for (Map.Entry<Integer, Integer> lugEntry : pilgrimEntry.getValue().entrySet()) {
                int lugId = lugEntry.getKey();
                int qty = lugEntry.getValue();
                if (qty <= 0) continue;

                PackageUpgradeDto lugDto = findUpgradeById(lugId);
                if (lugDto != null) {
                    double total = lugDto.getPrice() * qty; // per-unit
                    list.add(new BookingRequest.SelectedUpgrade(
                            lugDto.id,
                            lugDto.category,
                            lugDto.name,
                            qty,
                            tIdx,
                            lugDto.getPrice(),
                            total,
                            "Jemaah " + (tIdx + 1) + " (x" + qty + ")"
                    ));
                }
            }
        }

        if (selectedAccessoriesUpgrade != null) {
            int qty = selectedAccessoriesUpgrade.isPerPax() ? paxCount : 1;
            double total = selectedAccessoriesUpgrade.getPrice() * qty;
            list.add(new BookingRequest.SelectedUpgrade(
                    selectedAccessoriesUpgrade.id,
                    selectedAccessoriesUpgrade.category,
                    selectedAccessoriesUpgrade.name,
                    qty,
                    null,
                    selectedAccessoriesUpgrade.getPrice(),
                    total,
                    "Aksesori"
            ));
        }

        return list;
    }

    private void updateBottomPriceBar() {
        double basePackageSubtotal = bookingRequest.unitPriceAmount * paxCount;
        List<BookingRequest.SelectedUpgrade> selections = buildCurrentSelectedUpgradesList();

        double addonsTotal = 0.0;
        for (BookingRequest.SelectedUpgrade su : selections) {
            addonsTotal += su.calculatedTotal;
        }

        double grandTotal = basePackageSubtotal + addonsTotal;

        if (authoritativeQuote != null && currentStep == STEP_REVIEW) {
            grandTotal = authoritativeQuote.total;
        }

        txtBottomBarTotalAmount.setText(MoneyFormat.formatRM(grandTotal));

        if (addonsTotal > 0) {
            txtBottomBarAddonsSubtitle.setText("+" + MoneyFormat.formatRM(addonsTotal) + " " + getString(R.string.subtotal_addons));
            txtBottomBarAddonsSubtitle.setVisibility(View.VISIBLE);
        } else {
            txtBottomBarAddonsSubtitle.setVisibility(View.GONE);
        }
    }

    // -------------------------------------------------------------
    // NAVIGATION
    // -------------------------------------------------------------
    private void handleBackNavigation() {
        if (currentStep > 0) {
            currentStep--;
            renderCurrentStep();
        } else {
            finish();
        }
    }

    private void handleNextNavigation() {
        if (currentStep < STEP_REVIEW) {
            currentStep++;
            renderCurrentStep();
        } else {
            // Confirm & Launch Passenger Details
            commitSelectionsToBookingRequest();

            Intent intent = new Intent(this, PassengerDetailsActivity.class);
            intent.putExtra(PassengerDetailsActivity.EXTRA_BOOKING_REQUEST, bookingRequest);
            startActivity(intent);
        }
    }

    private void commitSelectionsToBookingRequest() {
        bookingRequest.selectedUpgrades = buildCurrentSelectedUpgradesList();
        bookingRequest.selectedMakkahHotelName = selectedMakkahUpgrade != null ? selectedMakkahUpgrade.name : null;
        bookingRequest.selectedMadinahHotelName = selectedMadinahUpgrade != null ? selectedMadinahUpgrade.name : null;
        bookingRequest.selectedMakkahHotelTier = selectedMakkahUpgrade != null ? selectedMakkahUpgrade.distanceTier : packageDetail.hotelMakkahDistanceTier;
        bookingRequest.selectedMadinahHotelTier = selectedMadinahUpgrade != null ? selectedMadinahUpgrade.distanceTier : packageDetail.hotelMadinahDistanceTier;
        bookingRequest.fullBoardSelected = (selectedFullBoardUpgrade != null);

        if (authoritativeQuote != null) {
            bookingRequest.serverQuoteTotal = authoritativeQuote.total;
            bookingRequest.serverQuoteDeposit = authoritativeQuote.requiredDeposit;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
