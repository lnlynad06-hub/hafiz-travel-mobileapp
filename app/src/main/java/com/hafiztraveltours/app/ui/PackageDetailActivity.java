package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PackageDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_ID = "extra_package_id";
    public static final String EXTRA_COLLECTION = "extra_collection";

    private static final String WHATSAPP_PHONE_NUMBER = "60197859867";

    private LinearLayout container;
    private ImageView heroImage;
    private TextView heroCategoryBadge;
    private TextView bottomPrice;
    private TextView bottomPriceSublabel;
    private TextView bottomPriceLabel;
    private View bookButton;
    private ImageView favoriteButton;
    private ImageView shareButton;

    private PackageDetail detail;
    private UmrahPackage rawPackage;
    private int selectedPriceOptionIndex = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_detail);

        findViewById(R.id.detailBackButton).setOnClickListener(v -> finish());

        heroImage = findViewById(R.id.detailHeroImage);
        heroCategoryBadge = findViewById(R.id.detailHeroCategoryBadge);
        container = findViewById(R.id.detailContainer);
        bottomPrice = findViewById(R.id.detailBottomPrice);
        bottomPriceSublabel = findViewById(R.id.detailBottomPriceSublabel);
        bottomPriceLabel = findViewById(R.id.detailBottomPriceLabel);
        bookButton = findViewById(R.id.detailBookButton);
        if (bookButton != null) {
            bookButton.setOnClickListener(v -> onBookNowClicked());
        }
        favoriteButton = findViewById(R.id.detailFavoriteButton);
        shareButton = findViewById(R.id.detailShareButton);

        String packageId = getIntent().getStringExtra(EXTRA_PACKAGE_ID);
        String collection = getIntent().getStringExtra(EXTRA_COLLECTION);
        if (collection == null) collection = "umrah_packages";

        if (packageId == null) {
            Toast.makeText(this, getString(R.string.err_package_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPackage(collection, packageId);
    }

    private void loadPackage(String collection, String packageId) {
        ApiClient.getApiService().getPackageDetail(packageId).enqueue(new Callback<ApiResponse<UmrahPackage>>() {
            @Override
            public void onResponse(Call<ApiResponse<UmrahPackage>> call, Response<ApiResponse<UmrahPackage>> response) {
                if (isFinishing() || isDestroyed()) return;
                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    rawPackage = response.body().data;
                    rawPackage.collectionName = collection;
                    detail = PackageDetail.fromUmrahPackage(rawPackage);
                    renderAll();
                } else {
                    Toast.makeText(PackageDetailActivity.this, getString(R.string.err_package_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UmrahPackage>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(PackageDetailActivity.this, getString(R.string.err_package_load_failed), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderAll() {
        if (isFinishing() || isDestroyed() || detail == null) return;

        // 1. Hero Image
        try {
            if (detail.imageUrl != null && !detail.imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(detail.imageUrl)
                        .placeholder(R.color.surface)
                        .error(R.color.surface)
                        .into(heroImage);
            }
        } catch (Exception ignored) {}

        // 2. Category badge on Hero
        if (heroCategoryBadge != null) {
            heroCategoryBadge.setVisibility(View.GONE);
        }

        // 3. Top Actions (Favorite & Share)
        setupTopActions();

        // 4. Bottom Price & CTA setup
        selectedPriceOptionIndex = 0;
        updateBottomPriceDisplay();

        View whatsappBtn = findViewById(R.id.detailWhatsappButton);
        if (whatsappBtn != null) {
            whatsappBtn.setOnClickListener(v -> openWhatsAppForPackage());
        }

        // 5. Build Content Sections
        container.removeAllViews();
        addTitleSection();
        addQuickSpecsSection();
        addNightsBreakdownSection();
        addHotelsSection();
        addPriceOptionsSection();
        addItinerarySection();
        addIncludedExcludedSection();
        addPackingGuideSection();
        addImportantNotesSection();
        addGallerySection();
    }

    private void updateBottomPriceDisplay() {
        if (detail == null) return;
        if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedPriceOptionIndex >= 0 && selectedPriceOptionIndex < detail.priceOptions.size()) {
            PackageDetail.PriceOption opt = detail.priceOptions.get(selectedPriceOptionIndex);
            if (bottomPrice != null) bottomPrice.setText(opt.price);
            if (bottomPriceSublabel != null) {
                bottomPriceSublabel.setText(getString(R.string.selected_room_label) + ": " + opt.occupancyLabel);
                bottomPriceSublabel.setVisibility(View.VISIBLE);
            }
        } else {
            if (bottomPrice != null) bottomPrice.setText(detail.price);
            if (bottomPriceSublabel != null) bottomPriceSublabel.setVisibility(View.GONE);
        }
    }

    private void onBookNowClicked() {
        if (detail == null) return;
        BookingConfigurationBottomSheet sheet = BookingConfigurationBottomSheet.newInstance(detail, selectedPriceOptionIndex);
        sheet.show(getSupportFragmentManager(), "BookingConfigSheet");
    }

    private void setupTopActions() {
        if (rawPackage != null && favoriteButton != null) {
            updateFavoriteState();
            favoriteButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                FavoritesManager.handleFavoriteToggle(this, rawPackage, favoriteButton, isFav -> updateFavoriteState());
            });
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = "🕋 *" + detail.name + "*\n" +
                        getString(R.string.share_price_from, detail.price) + "\n" +
                        (detail.durationDays > 0 ? (getString(R.string.share_duration, detail.durationDays, detail.nightsCount) + "\n\n") : "\n") +
                        (detail.summaryLine != null && !detail.summaryLine.isEmpty() ? (detail.summaryLine + "\n\n") : "") +
                        getString(R.string.share_cta) + ": https://wa.me/" + WHATSAPP_PHONE_NUMBER;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_package_via)));
            });
        }
    }

    private void updateFavoriteState() {
        if (favoriteButton != null && rawPackage != null) {
            boolean isFav = FavoritesManager.isFavorite(this, rawPackage);
            favoriteButton.setColorFilter(isFav ? Color.parseColor("#E91E63") : Color.WHITE);
        }
    }

    // =========================================================================
    // SECTION BUILDERS
    // =========================================================================

    private void addTitleSection() {
        String categoryStr = null;
        if (rawPackage != null && rawPackage.category != null && !rawPackage.category.trim().isEmpty()) {
            categoryStr = rawPackage.category.trim().toUpperCase();
        } else if (detail != null && detail.category != null && !detail.category.trim().isEmpty()) {
            categoryStr = detail.category.trim().toUpperCase();
        }

        if (categoryStr != null && !categoryStr.isEmpty()) {
            TextView catText = new TextView(this);
            catText.setText(categoryStr);
            catText.setTextSize(12);
            catText.setTypeface(null, Typeface.BOLD);
            catText.setTextColor(getResources().getColor(R.color.brand_magenta));
            catText.setLetterSpacing(0.04f);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.bottomMargin = dp(4);
            catText.setLayoutParams(cp);
            container.addView(catText);
        }

        TextView title = new TextView(this);
        title.setText(detail.name);
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));
        container.addView(title);

        if (detail.summaryLine != null && !detail.summaryLine.isEmpty()) {
            TextView summary = new TextView(this);
            summary.setText(detail.summaryLine);
            summary.setTextSize(13);
            summary.setTextColor(getResources().getColor(R.color.text_gray));
            summary.setLineSpacing(dp(2), 1.15f);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(6);
            summary.setLayoutParams(p);
            container.addView(summary);
        }
    }

    private void addQuickSpecsSection() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(14);
        row.setLayoutParams(rowParams);

        if (detail.durationDays > 0) {
            row.addView(buildSpecChip(getString(R.string.duration_days_nights, detail.durationDays, detail.nightsCount)));
        }

        if (rawPackage != null && rawPackage.airlineName != null && !rawPackage.airlineName.isEmpty()) {
            row.addView(buildSpecChip(rawPackage.airlineName));
        }

        if (rawPackage != null && rawPackage.hotelMakkahRating != null && !rawPackage.hotelMakkahRating.isEmpty()) {
            row.addView(buildSpecChip(rawPackage.hotelMakkahRating));
        }

        if (row.getChildCount() > 0) {
            container.addView(row);
        }
    }

    private View buildSpecChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(11);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setTextColor(getResources().getColor(R.color.brand_magenta));
        chip.setBackgroundResource(R.drawable.bg_prayer_countdown_pill);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMarginEnd(dp(6));
        chip.setLayoutParams(p);
        return chip;
    }

    private void addNightsBreakdownSection() {
        if (detail.nightsBreakdown.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_accommodation_summary)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);

        for (PackageDetail.NightBreakdown nb : detail.nightsBreakdown) {
            // Build city label from slot
            String cityLabel;
            if (detail.isUmrah) {
                if (nb.slot == 0) cityLabel = getString(R.string.nights_label_makkah);
                else if (nb.slot == 1) cityLabel = getString(R.string.nights_label_madinah);
                else cityLabel = getString(R.string.nights_label_taif);
            } else {
                if (nb.slot == 0) {
                    cityLabel = (nb.destinationHint != null && !nb.destinationHint.isEmpty())
                            ? nb.destinationHint : getString(R.string.nights_label_hotel1);
                } else if (nb.slot == 1) {
                    cityLabel = getString(R.string.nights_label_hotel2);
                } else {
                    cityLabel = getString(R.string.nights_label_hotel3);
                }
            }

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(8), dp(12), dp(8), dp(12));

            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            cp.setMarginEnd(dp(8));
            card.setLayoutParams(cp);

            TextView count = new TextView(this);
            count.setText(nb.nights + " " + getString(R.string.nights_unit));
            count.setTextSize(15);
            count.setTypeface(null, Typeface.BOLD);
            count.setTextColor(getResources().getColor(R.color.brand_magenta));

            TextView city = new TextView(this);
            city.setText(cityLabel);
            city.setTextSize(12);
            city.setTextColor(getResources().getColor(R.color.text_gray));
            city.setTypeface(null, Typeface.NORMAL);

            card.addView(count);
            card.addView(city);
            row.addView(card);
        }
        container.addView(row);
    }

    private void addHotelsSection() {
        boolean isUmrah = detail.isUmrah;
        container.addView(sectionHeading(getString(
                isUmrah ? R.string.detail_section_hotels_umrah : R.string.detail_section_hotels_tour)));

        if (detail.hotels.isEmpty()) {
            container.addView(buildEmptyNoticeCard(getString(R.string.detail_hotels_empty_notice)));
            return;
        }

        for (PackageDetail.HotelInfo hotel : detail.hotels) {
            // Build translated title from slot
            String hotelTitle;
            if (hotel.slot == -1) {
                // Flight card
                String flightType = !hotel.rawRating.isEmpty()
                        ? hotel.rawRating
                        : getString(R.string.flight_type_fallback);
                String airline = (hotel.airlineName != null && !hotel.airlineName.trim().isEmpty())
                        ? hotel.airlineName.trim()
                        : "";
                if (!airline.isEmpty()) {
                    hotelTitle = flightType + " (" + airline + ")";
                } else {
                    hotelTitle = flightType;
                }
                if (hotel.subtitle.isEmpty()) hotel.subtitle = getString(R.string.flight_route_fallback);
            } else {
                int[] umrahKeys = {R.string.hotel_title_umrah_makkah, R.string.hotel_title_umrah_madinah, R.string.hotel_title_umrah_taif};
                int[] tourKeys  = {R.string.hotel_title_tour_hotel1, R.string.hotel_title_tour_hotel2, R.string.hotel_title_tour_extra};
                int slot = Math.min(hotel.slot, 2);
                String base = getString(isUmrah ? umrahKeys[slot] : tourKeys[slot]);
                if (!hotel.rawRating.trim().isEmpty()) {
                    hotelTitle = base + " (" + hotel.rawRating.trim() + ")";
                } else {
                    hotelTitle = base;
                }
            }

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            int pad = dp(14);
            card.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(8);
            card.setLayoutParams(p);

            TextView title = new TextView(this);
            title.setText(hotelTitle);
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));

            TextView subtitle = new TextView(this);
            subtitle.setText(hotel.subtitle);
            subtitle.setTextSize(12);
            subtitle.setTextColor(getResources().getColor(R.color.text_gray));
            subtitle.setLineSpacing(dp(2), 1.1f);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subParams.topMargin = dp(4);
            subtitle.setLayoutParams(subParams);

            card.addView(title);
            card.addView(subtitle);
            container.addView(card);
        }
    }

    private void addPriceOptionsSection() {
        if (detail.priceOptions.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_room_pricing)));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setScrollBarSize(0);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(10);
        scroll.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        renderRoomOptionCards(row);

        scroll.addView(row);
        container.addView(scroll);
    }

    private void renderRoomOptionCards(LinearLayout row) {
        row.removeAllViews();
        for (int i = 0; i < detail.priceOptions.size(); i++) {
            final int index = i;
            PackageDetail.PriceOption option = detail.priceOptions.get(i);
            boolean isSelected = (i == selectedPriceOptionIndex);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(isSelected ? R.drawable.bg_room_card_selected : R.drawable.bg_room_card_unselected);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(10));
            card.setLayoutParams(cardParams);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView occupancy = new TextView(this);
            occupancy.setText(option.occupancyLabel);
            occupancy.setTextSize(12);
            occupancy.setTypeface(null, Typeface.BOLD);
            occupancy.setTextColor(getResources().getColor(isSelected ? R.color.brand_magenta : R.color.text_dark));

            LinearLayout.LayoutParams occParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            occupancy.setLayoutParams(occParams);
            headerRow.addView(occupancy);

            if (isSelected) {
                TextView check = new TextView(this);
                check.setText("✓");
                check.setTextSize(13);
                check.setTypeface(null, Typeface.BOLD);
                check.setTextColor(getResources().getColor(R.color.brand_magenta));
                headerRow.addView(check);
            }

            TextView priceText = new TextView(this);
            priceText.setText(option.price);
            priceText.setTextSize(16);
            priceText.setTypeface(null, Typeface.BOLD);
            priceText.setTextColor(getResources().getColor(R.color.brand_magenta));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pp.topMargin = dp(4);
            priceText.setLayoutParams(pp);

            TextView perPax = new TextView(this);
            perPax.setText(getString(R.string.detail_per_pax));
            perPax.setTextSize(11);
            perPax.setTextColor(getResources().getColor(R.color.text_gray));

            card.addView(headerRow);
            card.addView(priceText);
            card.addView(perPax);

            card.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                selectedPriceOptionIndex = index;
                renderRoomOptionCards(row);
                updateBottomPriceDisplay();
            });

            row.addView(card);
        }
    }

    private void addItinerarySection() {
        container.addView(sectionHeading(getString(R.string.detail_section_itinerary)));

        if (detail.itinerary.isEmpty()) {
            container.addView(buildEmptyNoticeCard(getString(R.string.detail_itinerary_empty_notice)));
            return;
        }

        TextView hint = new TextView(this);
        hint.setText(getString(R.string.detail_itinerary_hint));
        hint.setTextSize(11);
        hint.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(2);
        hint.setLayoutParams(hintParams);
        container.addView(hint);

        LinearLayout itineraryContainer = new LinearLayout(this);
        itineraryContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(itineraryContainer);

        renderItinerary(itineraryContainer);
    }

    private void renderItinerary(LinearLayout targetContainer) {
        if (detail.itinerary == null || detail.itinerary.isEmpty() || targetContainer == null) return;
        targetContainer.removeAllViews();

        for (int i = 0; i < detail.itinerary.size(); i++) {
            PackageDetail.ItineraryDay day = detail.itinerary.get(i);
            boolean isFirst = (i == 0);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(10);
            card.setLayoutParams(cardParams);

            // Dual-tone Header Ribbon Banner (Navy Blue Left + Brand Magenta Right)
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView dayLabel = new TextView(this);
            dayLabel.setText(getString(R.string.day_label_format, day.dayNumber).toUpperCase());
            dayLabel.setTextSize(13);
            dayLabel.setTypeface(null, Typeface.BOLD);
            dayLabel.setTextColor(Color.WHITE);
            dayLabel.setBackgroundResource(R.drawable.bg_itinerary_day_pill);
            dayLabel.setPadding(dp(14), dp(9), dp(14), dp(9));

            LinearLayout titleContainer = new LinearLayout(this);
            titleContainer.setOrientation(LinearLayout.HORIZONTAL);
            titleContainer.setGravity(Gravity.CENTER_VERTICAL);
            titleContainer.setBackgroundResource(R.drawable.bg_itinerary_title_pill);
            titleContainer.setPadding(dp(14), dp(9), dp(14), dp(9));

            LinearLayout.LayoutParams tcParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            titleContainer.setLayoutParams(tcParams);

            TextView titleText = new TextView(this);
            String pillTextStr;
            boolean showBodyTitle = false;

            if (day.dateLabel != null && !day.dateLabel.trim().isEmpty()) {
                pillTextStr = day.dateLabel.trim().toUpperCase();
                if (day.title != null && !day.title.trim().isEmpty()) {
                    showBodyTitle = true;
                }
            } else {
                pillTextStr = (day.title != null && !day.title.trim().isEmpty())
                        ? day.title.trim().toUpperCase()
                        : getString(R.string.day_label_format, day.dayNumber).toUpperCase();
            }

            titleText.setText(pillTextStr);
            titleText.setTextSize(13);
            titleText.setTypeface(null, Typeface.BOLD);
            titleText.setTextColor(Color.WHITE);

            LinearLayout.LayoutParams ttParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            titleText.setLayoutParams(ttParams);

            TextView chevron = new TextView(this);
            chevron.setText(isFirst ? "▲" : "▼");
            chevron.setTextSize(11);
            chevron.setTypeface(null, Typeface.BOLD);
            chevron.setTextColor(Color.WHITE);

            titleContainer.addView(titleText);
            titleContainer.addView(chevron);

            headerRow.addView(dayLabel);
            headerRow.addView(titleContainer);

            card.addView(headerRow);

            // Card Body Container
            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(14), dp(12), dp(14), dp(14));
            body.setVisibility(isFirst ? View.VISIBLE : View.GONE);

            if (showBodyTitle) {
                TextView itemTitle = new TextView(this);
                itemTitle.setText(day.title.trim());
                itemTitle.setTextSize(13);
                itemTitle.setTypeface(null, Typeface.BOLD);
                itemTitle.setTextColor(getResources().getColor(R.color.text_dark));
                LinearLayout.LayoutParams itp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                itp.bottomMargin = dp(8);
                itemTitle.setLayoutParams(itp);
                body.addView(itemTitle);
            }

            for (String activity : day.activities) {
                TextView bullet = new TextView(this);
                bullet.setText("• " + activity);
                bullet.setTextSize(13);
                bullet.setTextColor(getResources().getColor(R.color.text_dark));
                bullet.setLineSpacing(dp(2), 1.15f);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.bottomMargin = dp(6);
                bullet.setLayoutParams(bp);
                body.addView(bullet);
            }

            if (!day.hotelNote.isEmpty() || !day.mealNote.isEmpty()) {
                TextView footer = new TextView(this);
                StringBuilder sb = new StringBuilder();
                if (!day.hotelNote.isEmpty()) sb.append("Hotel: ").append(day.hotelNote);
                if (!day.mealNote.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("Makan: ").append(day.mealNote);
                }
                footer.setText(sb.toString());
                footer.setTextSize(11);
                footer.setTextColor(getResources().getColor(R.color.brand_magenta));
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                fp.topMargin = dp(6);
                footer.setLayoutParams(fp);
                body.addView(footer);
            }

            card.addView(body);

            card.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                boolean isExpanded = (body.getVisibility() == View.VISIBLE);
                body.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                chevron.setText(isExpanded ? "▼" : "▲");
            });

            targetContainer.addView(card);
        }
    }

    private void addIncludedExcludedSection() {
        if (detail.included.isEmpty() && detail.excluded.isEmpty()) {
            return;
        }

        container.addView(sectionHeading(getString(R.string.detail_section_inclusions)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        if (!detail.included.isEmpty()) {
            TextView incHeading = new TextView(this);
            incHeading.setText(getString(R.string.detail_included_heading));
            incHeading.setTextSize(13);
            incHeading.setTypeface(null, Typeface.BOLD);
            incHeading.setTextColor(Color.parseColor("#2E7D32"));
            card.addView(incHeading);

            for (String item : detail.included) {
                TextView row = new TextView(this);
                row.setText("• " + item);
                row.setTextSize(12);
                row.setTextColor(getResources().getColor(R.color.text_dark));
                row.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.topMargin = dp(4);
                row.setLayoutParams(p);
                card.addView(row);
            }
        }

        if (!detail.excluded.isEmpty()) {
            TextView excHeading = new TextView(this);
            excHeading.setText(getString(R.string.detail_excluded_heading));
            excHeading.setTextSize(13);
            excHeading.setTypeface(null, Typeface.BOLD);
            excHeading.setTextColor(Color.parseColor("#C62828"));
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ep.topMargin = dp(14);
            excHeading.setLayoutParams(ep);
            card.addView(excHeading);

            for (String item : detail.excluded) {
                TextView row = new TextView(this);
                row.setText("• " + item);
                row.setTextSize(12);
                row.setTextColor(getResources().getColor(R.color.text_gray));
                row.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.topMargin = dp(4);
                row.setLayoutParams(p);
                card.addView(row);
            }
        }

        container.addView(card);
    }

    private void addPackingGuideSection() {
        if (detail.packingSummer.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_packing)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.detail_packing_heading));
        heading.setTextSize(13);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.brand_magenta));
        card.addView(heading);

        for (String item : detail.packingSummer) {
            TextView row = new TextView(this);
            row.setText("• " + item);
            row.setTextSize(12);
            row.setTextColor(getResources().getColor(R.color.text_dark));
            row.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(4);
            row.setLayoutParams(p);
            card.addView(row);
        }

        container.addView(card);
    }

    private void addImportantNotesSection() {
        if (detail.importantNotes.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_notes)));

        for (PackageDetail.ImportantNote note : detail.importantNotes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.topMargin = dp(8);
            card.setLayoutParams(cp);

            TextView title = new TextView(this);
            title.setText(note.title);
            title.setTextSize(13);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));
            card.addView(title);

            for (String bullet : note.bullets) {
                TextView bulletView = new TextView(this);
                bulletView.setText("• " + bullet);
                bulletView.setTextSize(12);
                bulletView.setTextColor(getResources().getColor(R.color.text_gray));
                bulletView.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.topMargin = dp(4);
                bulletView.setLayoutParams(bp);
                card.addView(bulletView);
            }

            container.addView(card);
        }
    }

    private void addGallerySection() {
        if (detail.galleryImageUrls.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_gallery)));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setScrollBarSize(0);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(10);
        scroll.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String url : detail.galleryImageUrls) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundResource(R.drawable.bg_detail_card);
            image.setClipToOutline(true);

            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(160), dp(110));
            ip.setMarginEnd(dp(10));
            image.setLayoutParams(ip);
            image.setClickable(true);
            image.setFocusable(true);

            if (!isFinishing() && !isDestroyed()) {
                try {
                    Glide.with(this).load(url).placeholder(R.color.surface).into(image);
                } catch (Exception ignored) {}
            }
            image.setOnClickListener(v -> showZoomedImage(url));
            row.addView(image);
        }

        scroll.addView(row);
        container.addView(scroll);
    }

    private View buildEmptyNoticeCard(String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        card.setLayoutParams(p);

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(12);
        tv.setTextColor(getResources().getColor(R.color.text_gray));
        tv.setTypeface(null, Typeface.ITALIC);
        card.addView(tv);

        return card;
    }

    private void showZoomedImage(String url) {
        if (isFinishing() || isDestroyed()) return;
        ImageView fullImage = new ImageView(this);
        fullImage.setAdjustViewBounds(true);
        fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        try {
            Glide.with(this).load(url).into(fullImage);
        } catch (Exception ignored) {}

        AlertDialog dialog = new AlertDialog.Builder(this).setView(fullImage).create();
        fullImage.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private TextView sectionHeading(String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(16);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(20);
        heading.setLayoutParams(p);
        return heading;
    }

    private void openWhatsAppForPackage() {
        String pkgName = (detail != null && detail.name != null && !detail.name.trim().isEmpty())
                ? detail.name.trim()
                : getString(R.string.app_name);
        String message = getString(R.string.package_detail_whatsapp_default_message, pkgName);

        try {
            Uri uri = Uri.parse("https://wa.me/" + WHATSAPP_PHONE_NUMBER
                    + "?text=" + Uri.encode(message));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_whatsapp_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
