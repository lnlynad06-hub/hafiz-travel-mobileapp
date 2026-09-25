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

import com.bumptech.glide.Glide;
import com.hafiztraveltours.app.network.ApiClient;

import android.graphics.drawable.GradientDrawable;
import java.util.Locale;

public class PackageDetailActivity extends BaseActivity {

    public static final String EXTRA_PACKAGE_ID = "extra_package_id";
    public static final String EXTRA_COLLECTION = "extra_collection";

    private static final String DEFAULT_WHATSAPP_NUMBER = "60197859867";

    private void setupTopActions() {
        if (rawPackage != null && favoriteButton != null) {
            updateFavoriteState();
            favoriteButton.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                FavoritesManager.handleFavoriteToggle(this, rawPackage, favoriteButton, isFav -> updateFavoriteState());
            });
        }

        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
                String waNum = (detail != null && detail.companyWhatsapp != null && !detail.companyWhatsapp.trim().isEmpty())
                        ? detail.companyWhatsapp.trim()
                        : DEFAULT_WHATSAPP_NUMBER;
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = "*" + detail.name + "*\n" +
                        getString(R.string.share_price_from, detail.price) + "\n" +
                        (detail.durationDays > 0 ? (getString(R.string.share_duration, detail.durationDays, detail.nightsCount) + "\n\n") : "\n") +
                        (detail.summaryLine != null && !detail.summaryLine.isEmpty() ? (detail.summaryLine + "\n\n") : "") +
                        getString(R.string.share_cta) + ": https://wa.me/" + waNum;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_package_via)));
            });
        }
    }

    private LinearLayout container;
    private ImageView heroImage;
    private TextView heroCategoryBadge;
    private TextView bottomPrice;
    private TextView bottomPriceSublabel;
    private TextView bottomPriceLabel;
    private View bookButton;
    private ImageView favoriteButton;
    private ImageView shareButton;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

    private PackageDetail detail;
    private UmrahPackage rawPackage;
    private int selectedPriceOptionIndex = 0;
    private PackageDetailViewModel packageViewModel;

    
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

        packageViewModel = new androidx.lifecycle.ViewModelProvider(this).get(PackageDetailViewModel.class);
        observePackageState();

        String packageId = getIntent().getStringExtra(EXTRA_PACKAGE_ID);
        String collection = getIntent().getStringExtra(EXTRA_COLLECTION);
        if (collection == null) collection = "umrah_packages";

        swipeRefreshLayout = findViewById(R.id.packageDetailSwipeRefresh);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.brand_magenta,
                    R.color.gold_accent,
                    R.color.brand_dark_pink
            );
            final String finalCollection = collection;
            final String finalPackageId = packageId;
            swipeRefreshLayout.setOnRefreshListener(() -> loadPackage(finalCollection, finalPackageId));
        }

        if (packageId == null) {
            Toast.makeText(this, getString(R.string.err_package_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadPackage(collection, packageId);
    }

    private void loadPackage(String collection, String packageId) {
        packageViewModel.loadPackage(collection, packageId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detail != null) {
            String packageId = getIntent().getStringExtra(EXTRA_PACKAGE_ID);
            String collection = getIntent().getStringExtra(EXTRA_COLLECTION);
            if (collection == null) collection = "umrah_packages";
            if (packageId != null) {
                loadPackage(collection, packageId);
            }
        }
    }

    /** Wires ViewModel state to rendering + one-shot error (H1/Step 5). */
    private void observePackageState() {
        packageViewModel.getDetailData().observe(this, loaded -> {
            if (loaded == null) return;
            rawPackage = loaded.raw;
            detail = loaded.detail;
            renderAll();
        });
        packageViewModel.getDetailLoading().observe(this, loading -> {
            if (swipeRefreshLayout != null && (loading == null || !loading)) {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
        packageViewModel.getDetailError().observe(this, event -> {
            com.hafiztraveltours.app.utils.ApiOpResult result =
                    event != null ? event.consume() : null;
            if (result == null) return;
            Toast.makeText(this, result.resolveMessage(this), Toast.LENGTH_SHORT).show();
            finish();
        });
        packageViewModel.getRelatedData().observe(this, related -> {
            if (related != null) renderRelatedPackages(related);
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

        // 5. Build Content Sections following exact hierarchy
        container.removeAllViews();
        addTitleSection();              // 1. Hero Gallery & Package Info Header
        addQuickSpecsSection();          // 2. Quick Highlights
        addPriceOptionsSection();        // 3. Room & Pricing
        addHotelsSection();              // 4. Accommodation
        addFlightTransportSection();     // 5. Flight
        addGallerySection();             // 6. Photo Gallery
        addItinerarySection();           // 7. Full Itinerary
        addIncludedExcludedSection();    // 8 & 9. What's Included / Not Included
        addBookingGuideSection();        // 10. Booking Guide
        addImportantNotesSection();      // 11. Important Notes & Guidelines
        addCancellationPolicySection();  // 12. Cancellation & Refund Policy
        addRelatedPackagesSection();     // 13. Related Packages
    }

    /** Localized "departure to return" label for a departure option (C1/M7). */
    private String formatDepartureOption(PackageDetail.DepartureOption opt) {
        if (opt == null) return "";
        if (opt.departureDate != null && !opt.departureDate.isEmpty()
                && opt.returnDate != null && !opt.returnDate.isEmpty()) {
            return getString(R.string.departure_range_format, opt.departureDate, opt.returnDate);
        }
        if (opt.departureDate != null && !opt.departureDate.isEmpty()) return opt.departureDate;
        return opt.label != null ? opt.label : "";
    }

    private void updateBottomPriceDisplay() {
        if (detail == null) return;
        if (detail.priceOptions != null && !detail.priceOptions.isEmpty() && selectedPriceOptionIndex >= 0 && selectedPriceOptionIndex < detail.priceOptions.size()) {
            PackageDetail.PriceOption opt = detail.priceOptions.get(selectedPriceOptionIndex);
            if (bottomPrice != null) bottomPrice.setText(opt.price);
            if (bottomPriceSublabel != null) {
                bottomPriceSublabel.setText(getString(R.string.selected_room_label) + ": " + com.hafiztraveltours.app.utils.RoomLabels.resolve(this, opt));
                bottomPriceSublabel.setVisibility(View.VISIBLE);
            }
        } else {
            if (bottomPrice != null) {
                bottomPrice.setText((detail.price != null && !detail.price.trim().isEmpty())
                        ? detail.price : getString(R.string.label_contact_us));
            }
            if (bottomPriceSublabel != null) bottomPriceSublabel.setVisibility(View.GONE);
        }
    }

    private void onBookNowClicked() {
        if (detail == null) return;

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            return;
        }

        BookingConfigurationBottomSheet sheet = BookingConfigurationBottomSheet.newInstance(detail, selectedPriceOptionIndex);
        sheet.show(getSupportFragmentManager(), "BookingConfigSheet");
    }

    private void showProfileIncompleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.booking_blocked_profile_title)
                .setMessage(R.string.booking_blocked_profile_msg)
                .setPositiveButton(R.string.btn_edit_profile_now, (dialog, which) -> {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra(ProfileActivity.EXTRA_ACTION, ProfileActivity.ACTION_EDIT_PROFILE);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDocumentsIncompleteDialog(java.util.List<String> missingDocCodes) {
        String formattedList = BookingEligibility.formatMissingDocs(this, missingDocCodes);
        String message = getString(R.string.booking_blocked_docs_msg, formattedList);

        new AlertDialog.Builder(this)
                .setTitle(R.string.booking_blocked_docs_title)
                .setMessage(message)
                .setPositiveButton(R.string.btn_upload_docs_now, (dialog, which) -> {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra(ProfileActivity.EXTRA_ACTION, ProfileActivity.ACTION_TRAVEL_DOCS);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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

        // Departure Date, Duration, Route Header Bar — localized via availableDepartures (C1/M7).
        StringBuilder metaSb = new StringBuilder();
        if (detail.availableDepartures != null && !detail.availableDepartures.isEmpty()) {
            PackageDetail.DepartureOption first = detail.availableDepartures.get(0);
            String label = formatDepartureOption(first);
            if (!label.isEmpty()) metaSb.append("📅 ").append(label);
        } else if (detail.availableDepartureDates != null && !detail.availableDepartureDates.isEmpty()) {
            metaSb.append("📅 ").append(detail.availableDepartureDates.get(0));
        } else if (rawPackage != null && rawPackage.departures != null && !rawPackage.departures.isEmpty()) {
            UmrahPackage.DepartureItem dep = rawPackage.departures.get(0);
            if (dep.departureDate != null) {
                metaSb.append("📅 ").append(dep.departureDate);
                if (dep.returnDate != null) metaSb.append(" - ").append(dep.returnDate);
            }
        }
        if (rawPackage != null && rawPackage.flightRoute != null && !rawPackage.flightRoute.trim().isEmpty()) {
            if (metaSb.length() > 0) metaSb.append(" • ");
            metaSb.append("✈ ").append(rawPackage.flightRoute.trim());
        } else if (rawPackage != null && rawPackage.destination != null && !rawPackage.destination.trim().isEmpty()) {
            if (metaSb.length() > 0) metaSb.append(" • ");
            metaSb.append("📍 ").append(rawPackage.destination.trim());
        }

        if (metaSb.length() > 0) {
            TextView metaText = new TextView(this);
            metaText.setText(metaSb.toString());
            metaText.setTextSize(12);
            metaText.setTypeface(null, Typeface.BOLD);
            metaText.setTextColor(getResources().getColor(R.color.brand_dark_pink));
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mp.topMargin = dp(4);
            metaText.setLayoutParams(mp);
            container.addView(metaText);
        }

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

        if (rawPackage != null && rawPackage.hotelMakkahRating != null && !rawPackage.hotelMakkahRating.trim().isEmpty()) {
            row.addView(buildSpecChip("⭐ " + rawPackage.hotelMakkahRating.trim()));
        }

        if (rawPackage != null && rawPackage.airlineName != null && !rawPackage.airlineName.trim().isEmpty()) {
            row.addView(buildSpecChip(rawPackage.airlineName.trim()));
        }

        if (rawPackage != null && rawPackage.flightType != null && !rawPackage.flightType.trim().isEmpty()) {
            row.addView(buildSpecChip(rawPackage.flightType.trim()));
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
            if (nb.nights <= 0) continue;

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
        if (row.getChildCount() > 0) {
            container.addView(row);
        }
    }

    private void addHotelsSection() {
        addNightsBreakdownSection();

        boolean isUmrah = detail.isUmrah;
        container.addView(sectionHeading(getString(
                isUmrah ? R.string.detail_section_hotels_umrah : R.string.detail_section_hotels_tour)));

        if (detail.hotels.isEmpty()) {
            container.addView(buildEmptyNoticeCard(getString(R.string.detail_hotels_empty_notice)));
            return;
        }

        for (PackageDetail.HotelInfo hotel : detail.hotels) {
            if (hotel.slot == -1) continue;

            int[] umrahKeys = {R.string.hotel_title_umrah_makkah, R.string.hotel_title_umrah_madinah, R.string.hotel_title_umrah_taif};
            int[] tourKeys  = {R.string.hotel_title_tour_hotel1, R.string.hotel_title_tour_hotel2, R.string.hotel_title_tour_extra};
            int slot = Math.max(0, Math.min(hotel.slot, 2));
            String base = getString(isUmrah ? umrahKeys[slot] : tourKeys[slot]);
            String hotelTitle = !hotel.rawRating.trim().isEmpty() ? base + " (" + hotel.rawRating.trim() + ")" : base;

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
            String labelStr = com.hafiztraveltours.app.utils.RoomLabels.resolve(this, option);
            occupancy.setText(labelStr);
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
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
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
                if (!day.hotelNote.isEmpty()) sb.append(getString(R.string.package_detail_hotel_label)).append(": ").append(day.hotelNote);
                if (!day.mealNote.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(getString(R.string.package_detail_meals_label)).append(": ").append(day.mealNote);
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
                com.hafiztraveltours.app.utils.HapticUtil.click(v);
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

        // 1. INCLUDED SECTION ("What's Covered")
        if (!detail.included.isEmpty()) {
            TextView whatsCoveredHeader = sectionHeading(getString(R.string.detail_whats_covered));
            container.addView(whatsCoveredHeader);

            LinearLayout coveredCard = new LinearLayout(this);
            coveredCard.setOrientation(LinearLayout.VERTICAL);
            coveredCard.setBackgroundResource(R.drawable.bg_inc_cat_card);
            coveredCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams ccp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ccp.topMargin = dp(10);
            coveredCard.setLayoutParams(ccp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hrp.bottomMargin = dp(12);
            headerRow.setLayoutParams(hrp);

            LinearLayout iconBox = new LinearLayout(this);
            iconBox.setGravity(Gravity.CENTER);
            GradientDrawable iconBoxBg = new GradientDrawable();
            iconBoxBg.setShape(GradientDrawable.RECTANGLE);
            iconBoxBg.setCornerRadius(dp(10));
            iconBoxBg.setColor(Color.parseColor("#F0FDFA"));
            iconBox.setBackground(iconBoxBg);

            ImageView iconView = new ImageView(this);
            iconView.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams ivp = new LinearLayout.LayoutParams(dp(22), dp(22));
            iconView.setLayoutParams(ivp);
            iconBox.addView(iconView);

            LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
            ibp.rightMargin = dp(12);
            iconBox.setLayoutParams(ibp);
            headerRow.addView(iconBox);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(tcp);

            TextView catTitle = new TextView(this);
            catTitle.setText(getString(R.string.detail_whats_covered));
            catTitle.setTextSize(13);
            catTitle.setTypeface(null, Typeface.BOLD);
            catTitle.setTextColor(Color.parseColor("#0F766E"));

            TextView catSub = new TextView(this);
            catSub.setText(getString(R.string.detail_whats_covered_sub));
            catSub.setTextSize(11);
            catSub.setTextColor(Color.parseColor("#64748B"));

            titleCol.addView(catTitle);
            titleCol.addView(catSub);
            headerRow.addView(titleCol);

            coveredCard.addView(headerRow);

            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dpParams.bottomMargin = dp(10);
            divider.setLayoutParams(dpParams);
            coveredCard.addView(divider);

            LinearLayout itemsLayout = new LinearLayout(this);
            itemsLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemsLayout.setLayoutParams(ilp);

            for (String itemText : detail.included) {
                if (itemText == null || itemText.trim().isEmpty()) continue;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.topMargin = dp(4);
                rp.bottomMargin = dp(4);
                row.setLayoutParams(rp);

                ImageView checkImg = new ImageView(this);
                checkImg.setImageResource(R.drawable.ic_check_green);
                LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
                cip.rightMargin = dp(8);
                cip.topMargin = dp(2);
                checkImg.setLayoutParams(cip);

                TextView itemTv = new TextView(this);
                itemTv.setText(itemText);
                itemTv.setTextSize(12);
                itemTv.setTextColor(Color.parseColor("#334155"));
                itemTv.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams itvp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                itemTv.setLayoutParams(itvp);

                row.addView(checkImg);
                row.addView(itemTv);

                itemsLayout.addView(row);
            }

            coveredCard.addView(itemsLayout);
            container.addView(coveredCard);
        }

        // 2. NOT COVERED SECTION ("Not Covered")
        if (!detail.excluded.isEmpty()) {
            TextView notCoveredHeader = sectionHeading(getString(R.string.detail_not_covered));
            container.addView(notCoveredHeader);

            LinearLayout excCard = new LinearLayout(this);
            excCard.setOrientation(LinearLayout.VERTICAL);
            excCard.setBackgroundResource(R.drawable.bg_exc_card);
            excCard.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams ecp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ecp.topMargin = dp(10);
            excCard.setLayoutParams(ecp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hrp.bottomMargin = dp(12);
            headerRow.setLayoutParams(hrp);

            LinearLayout iconBox = new LinearLayout(this);
            iconBox.setGravity(Gravity.CENTER);
            GradientDrawable iconBoxBg = new GradientDrawable();
            iconBoxBg.setShape(GradientDrawable.RECTANGLE);
            iconBoxBg.setCornerRadius(dp(10));
            iconBoxBg.setColor(Color.parseColor("#FEF2F2"));
            iconBox.setBackground(iconBoxBg);

            ImageView minusIcon = new ImageView(this);
            minusIcon.setImageResource(R.drawable.ic_minus_gray);
            LinearLayout.LayoutParams mip = new LinearLayout.LayoutParams(dp(16), dp(16));
            minusIcon.setLayoutParams(mip);
            iconBox.addView(minusIcon);

            LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
            ibp.rightMargin = dp(12);
            iconBox.setLayoutParams(ibp);
            headerRow.addView(iconBox);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(tcp);

            TextView heading = new TextView(this);
            heading.setText(getString(R.string.detail_not_covered));
            heading.setTextSize(13);
            heading.setTypeface(null, Typeface.BOLD);
            heading.setTextColor(Color.parseColor("#991B1B"));

            TextView subHeading = new TextView(this);
            subHeading.setText(getString(R.string.detail_not_covered_sub));
            subHeading.setTextSize(11);
            subHeading.setTextColor(Color.parseColor("#64748B"));

            titleCol.addView(heading);
            titleCol.addView(subHeading);
            headerRow.addView(titleCol);

            excCard.addView(headerRow);

            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dpParams.bottomMargin = dp(10);
            divider.setLayoutParams(dpParams);
            excCard.addView(divider);

            for (String itemText : detail.excluded) {
                if (itemText == null || itemText.trim().isEmpty()) continue;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.topMargin = dp(4);
                rp.bottomMargin = dp(4);
                row.setLayoutParams(rp);

                ImageView minusImg = new ImageView(this);
                minusImg.setImageResource(R.drawable.ic_minus_gray);
                LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
                cip.rightMargin = dp(8);
                cip.topMargin = dp(2);
                minusImg.setLayoutParams(cip);

                TextView itemTv = new TextView(this);
                itemTv.setText(itemText);
                itemTv.setTextSize(12);
                itemTv.setTextColor(Color.parseColor("#374151"));
                itemTv.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams itvp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                itemTv.setLayoutParams(itvp);

                row.addView(minusImg);
                row.addView(itemTv);

                excCard.addView(row);
            }

            container.addView(excCard);
        }
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

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = dp(12);
        headerRow.setLayoutParams(hrp);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setShape(GradientDrawable.RECTANGLE);
        iconBoxBg.setCornerRadius(dp(10));
        iconBoxBg.setColor(Color.parseColor("#F0FDFA"));
        iconBox.setBackground(iconBoxBg);

        ImageView luggageIcon = new ImageView(this);
        luggageIcon.setImageResource(R.drawable.ic_packing_luggage);
        LinearLayout.LayoutParams lip = new LinearLayout.LayoutParams(dp(22), dp(22));
        luggageIcon.setLayoutParams(lip);
        iconBox.addView(luggageIcon);

        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
        ibp.rightMargin = dp(12);
        iconBox.setLayoutParams(ibp);
        headerRow.addView(iconBox);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(tcp);

        TextView heading = new TextView(this);
        heading.setText(getString(R.string.detail_packing_heading));
        heading.setTextSize(13);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.parseColor("#0F766E"));

        TextView subHeading = new TextView(this);
        subHeading.setText(getString(R.string.detail_packing_sub));
        subHeading.setTextSize(11);
        subHeading.setTextColor(Color.parseColor("#64748B"));

        titleCol.addView(heading);
        titleCol.addView(subHeading);
        headerRow.addView(titleCol);

        card.addView(headerRow);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dpParams.bottomMargin = dp(10);
        divider.setLayoutParams(dpParams);
        card.addView(divider);

        for (String item : detail.packingSummer) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(4);
            rp.bottomMargin = dp(4);
            row.setLayoutParams(rp);

            ImageView checkImg = new ImageView(this);
            checkImg.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
            cip.rightMargin = dp(8);
            cip.topMargin = dp(2);
            checkImg.setLayoutParams(cip);

            TextView itemTv = new TextView(this);
            itemTv.setText(item);
            itemTv.setTextSize(12);
            itemTv.setTextColor(Color.parseColor("#334155"));
            itemTv.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams itvp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            itemTv.setLayoutParams(itvp);

            row.addView(checkImg);
            row.addView(itemTv);

            card.addView(row);
        }

        container.addView(card);
    }

    private void addImportantNotesSection() {
        if (detail == null || detail.importantNotes == null || detail.importantNotes.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.detail_section_notes)));

        for (PackageDetail.ImportantNote note : detail.importantNotes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.topMargin = dp(10);
            card.setLayoutParams(cp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hrp.bottomMargin = dp(12);
            headerRow.setLayoutParams(hrp);

            LinearLayout iconBox = new LinearLayout(this);
            iconBox.setGravity(Gravity.CENTER);
            GradientDrawable iconBoxBg = new GradientDrawable();
            iconBoxBg.setShape(GradientDrawable.RECTANGLE);
            iconBoxBg.setCornerRadius(dp(10));
            iconBoxBg.setColor(Color.parseColor("#EEF2FF"));
            iconBox.setBackground(iconBoxBg);

            ImageView noteIcon = new ImageView(this);
            noteIcon.setImageResource(R.drawable.ic_terms_shield);
            LinearLayout.LayoutParams nip = new LinearLayout.LayoutParams(dp(22), dp(22));
            noteIcon.setLayoutParams(nip);
            iconBox.addView(noteIcon);

            LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
            ibp.rightMargin = dp(12);
            iconBox.setLayoutParams(ibp);
            headerRow.addView(iconBox);

            LinearLayout titleCol = new LinearLayout(this);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            titleCol.setLayoutParams(tcp);

            TextView title = new TextView(this);
            title.setText(getString(R.string.detail_terms_guidelines));
            title.setTextSize(13);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(Color.parseColor("#4338CA"));

            TextView subHeading = new TextView(this);
            subHeading.setText(getString(R.string.detail_terms_sub));
            subHeading.setTextSize(11);
            subHeading.setTextColor(Color.parseColor("#64748B"));

            titleCol.addView(title);
            titleCol.addView(subHeading);
            headerRow.addView(titleCol);

            card.addView(headerRow);

            View divider = new View(this);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            dpParams.bottomMargin = dp(10);
            divider.setLayoutParams(dpParams);
            card.addView(divider);

            for (String bullet : note.bullets) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rp.topMargin = dp(4);
                rp.bottomMargin = dp(4);
                row.setLayoutParams(rp);

                ImageView checkImg = new ImageView(this);
                checkImg.setImageResource(R.drawable.ic_check_green);
                LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
                cip.rightMargin = dp(8);
                cip.topMargin = dp(2);
                checkImg.setLayoutParams(cip);

                TextView bulletView = new TextView(this);
                bulletView.setText(bullet);
                bulletView.setTextSize(12);
                bulletView.setTextColor(Color.parseColor("#374151"));
                bulletView.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                bulletView.setLayoutParams(bp);

                row.addView(checkImg);
                row.addView(bulletView);

                card.addView(row);
            }

            container.addView(card);
        }
    }

    private void addRequiredDocumentsSection() {
        if (detail == null || detail.requiredDocuments == null || detail.requiredDocuments.isEmpty()) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = dp(12);
        headerRow.setLayoutParams(hrp);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setShape(GradientDrawable.RECTANGLE);
        iconBoxBg.setCornerRadius(dp(10));
        iconBoxBg.setColor(Color.parseColor("#E0F2FE"));
        iconBox.setBackground(iconBoxBg);

        ImageView docIcon = new ImageView(this);
        docIcon.setImageResource(R.drawable.ic_doc_passport);
        LinearLayout.LayoutParams nip = new LinearLayout.LayoutParams(dp(22), dp(22));
        docIcon.setLayoutParams(nip);
        iconBox.addView(docIcon);

        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
        ibp.rightMargin = dp(12);
        iconBox.setLayoutParams(ibp);
        headerRow.addView(iconBox);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(tcp);

        TextView title = new TextView(this);
        title.setText(getString(R.string.detail_required_documents));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#0369A1"));

        TextView subHeading = new TextView(this);
        subHeading.setText(getString(R.string.detail_docs_sub));
        subHeading.setTextSize(11);
        subHeading.setTextColor(Color.parseColor("#64748B"));

        titleCol.addView(title);
        titleCol.addView(subHeading);
        headerRow.addView(titleCol);

        card.addView(headerRow);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dpParams.bottomMargin = dp(10);
        divider.setLayoutParams(dpParams);
        card.addView(divider);

        for (String bullet : detail.requiredDocuments) {
            if (bullet == null || bullet.trim().isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(4);
            rp.bottomMargin = dp(4);
            row.setLayoutParams(rp);

            ImageView checkImg = new ImageView(this);
            checkImg.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
            cip.rightMargin = dp(8);
            cip.topMargin = dp(2);
            checkImg.setLayoutParams(cip);

            TextView bulletView = new TextView(this);
            bulletView.setText(bullet);
            bulletView.setTextSize(12);
            bulletView.setTextColor(Color.parseColor("#374151"));
            bulletView.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            bulletView.setLayoutParams(bp);

            row.addView(checkImg);
            row.addView(bulletView);

            card.addView(row);
        }

        container.addView(card);
    }

    private void addCancellationPolicySection() {
        if (detail == null || detail.cancellationPolicy == null || detail.cancellationPolicy.isEmpty()) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = dp(12);
        headerRow.setLayoutParams(hrp);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setShape(GradientDrawable.RECTANGLE);
        iconBoxBg.setCornerRadius(dp(10));
        iconBoxBg.setColor(Color.parseColor("#FCE7F3"));
        iconBox.setBackground(iconBoxBg);

        ImageView policyIcon = new ImageView(this);
        policyIcon.setImageResource(R.drawable.ic_cancellation_policy);
        LinearLayout.LayoutParams nip = new LinearLayout.LayoutParams(dp(22), dp(22));
        policyIcon.setLayoutParams(nip);
        iconBox.addView(policyIcon);

        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
        ibp.rightMargin = dp(12);
        iconBox.setLayoutParams(ibp);
        headerRow.addView(iconBox);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(tcp);

        TextView title = new TextView(this);
        title.setText(getString(R.string.detail_section_cancellation_policy));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#BE185D"));

        TextView subHeading = new TextView(this);
        subHeading.setText(getString(R.string.detail_cancellation_sub));
        subHeading.setTextSize(11);
        subHeading.setTextColor(Color.parseColor("#64748B"));

        titleCol.addView(title);
        titleCol.addView(subHeading);
        headerRow.addView(titleCol);

        card.addView(headerRow);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dpParams.bottomMargin = dp(10);
        divider.setLayoutParams(dpParams);
        card.addView(divider);

        for (String bullet : detail.cancellationPolicy) {
            if (bullet == null || bullet.trim().isEmpty()) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(4);
            rp.bottomMargin = dp(4);
            row.setLayoutParams(rp);

            ImageView checkImg = new ImageView(this);
            checkImg.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
            cip.rightMargin = dp(8);
            cip.topMargin = dp(2);
            checkImg.setLayoutParams(cip);

            TextView bulletView = new TextView(this);
            bulletView.setText(bullet);
            bulletView.setTextSize(12);
            bulletView.setTextColor(Color.parseColor("#374151"));
            bulletView.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            bulletView.setLayoutParams(bp);

            row.addView(checkImg);
            row.addView(bulletView);

            card.addView(row);
        }

        container.addView(card);
    }

    private void addBookingGuideSection() {
        container.addView(sectionHeading(getString(R.string.detail_booking_guide)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hrp.bottomMargin = dp(12);
        headerRow.setLayoutParams(hrp);

        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBoxBg = new GradientDrawable();
        iconBoxBg.setShape(GradientDrawable.RECTANGLE);
        iconBoxBg.setCornerRadius(dp(10));
        iconBoxBg.setColor(Color.parseColor("#FEF3C7"));
        iconBox.setBackground(iconBoxBg);

        ImageView guideIcon = new ImageView(this);
        guideIcon.setImageResource(R.drawable.ic_terms_shield);
        LinearLayout.LayoutParams gip = new LinearLayout.LayoutParams(dp(22), dp(22));
        guideIcon.setLayoutParams(gip);
        iconBox.addView(guideIcon);

        LinearLayout.LayoutParams ibp = new LinearLayout.LayoutParams(dp(40), dp(40));
        ibp.rightMargin = dp(12);
        iconBox.setLayoutParams(ibp);
        headerRow.addView(iconBox);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleCol.setLayoutParams(tcp);

        TextView title = new TextView(this);
        title.setText(getString(R.string.detail_booking_guide));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#B45309"));

        TextView subHeading = new TextView(this);
        subHeading.setText(getString(R.string.detail_booking_guide_sub));
        subHeading.setTextSize(11);
        subHeading.setTextColor(Color.parseColor("#64748B"));

        titleCol.addView(title);
        titleCol.addView(subHeading);
        headerRow.addView(titleCol);

        card.addView(headerRow);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dpParams.bottomMargin = dp(10);
        divider.setLayoutParams(dpParams);
        card.addView(divider);

        String[] steps = {
            "1. Pilih Pilihan Bilik (Quint, Quad, Triple, Double, Single) & Tarikh Pelepasan yang dikehendaki.",
            "2. Tekan butang 'Tempah Sekarang' di bahagian bawah untuk membuka tetapan tempahan.",
            "3. Masukkan bilangan jemaah / pengembara & butiran maklumat peribadi.",
            "4. Sahkan tempahan & teruskan ke bayaran deposit secara dalam talian atau muat naik resit pindahan bank."
        };

        for (String stepText : steps) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(4);
            rp.bottomMargin = dp(4);
            row.setLayoutParams(rp);

            ImageView checkImg = new ImageView(this);
            checkImg.setImageResource(R.drawable.ic_check_green);
            LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(14), dp(14));
            cip.rightMargin = dp(8);
            cip.topMargin = dp(2);
            checkImg.setLayoutParams(cip);

            TextView stepTv = new TextView(this);
            stepTv.setText(stepText);
            stepTv.setTextSize(12);
            stepTv.setTextColor(Color.parseColor("#374151"));
            stepTv.setLineSpacing(dp(1), 1.15f);
            LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            stepTv.setLayoutParams(stp);

            row.addView(checkImg);
            row.addView(stepTv);
            card.addView(row);
        }

        container.addView(card);
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
        String waNum = (detail != null && detail.companyWhatsapp != null && !detail.companyWhatsapp.trim().isEmpty())
                ? detail.companyWhatsapp.trim()
                : DEFAULT_WHATSAPP_NUMBER;

        try {
            Uri uri = Uri.parse("https://wa.me/" + waNum
                    + "?text=" + Uri.encode(message));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_whatsapp_not_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void addFlightTransportSection() {
        if (rawPackage == null) return;
        boolean hasAirline = rawPackage.airlineName != null && !rawPackage.airlineName.trim().isEmpty();
        boolean hasRoute = rawPackage.flightRoute != null && !rawPackage.flightRoute.trim().isEmpty();
        boolean hasType = rawPackage.flightType != null && !rawPackage.flightType.trim().isEmpty();

        if (!hasAirline && !hasRoute && !hasType) {
            return;
        }

        container.addView(sectionHeading(getString(R.string.cat_flights)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        int pad = dp(14);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        card.setLayoutParams(p);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_flight);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(22), dp(22));
        ip.setMarginEnd(dp(10));
        icon.setLayoutParams(ip);
        headerRow.addView(icon);

        TextView title = new TextView(this);
        String airlineStr = hasAirline ? rawPackage.airlineName.trim() : getString(R.string.cat_flights);
        if (hasType) {
            title.setText(airlineStr + " • " + rawPackage.flightType.trim());
        } else {
            title.setText(airlineStr);
        }
        title.setTextSize(14);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));
        headerRow.addView(title);

        card.addView(headerRow);

        if (hasRoute) {
            TextView routeText = new TextView(this);
            routeText.setText(rawPackage.flightRoute.trim());
            routeText.setTextSize(12);
            routeText.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = dp(6);
            routeText.setLayoutParams(rp);
            card.addView(routeText);
        }

        container.addView(card);
    }

    private void addRelatedPackagesSection() {
        if (rawPackage == null || rawPackage.category == null || rawPackage.category.trim().isEmpty()) return;
        packageViewModel.loadRelated(rawPackage.category.trim(), rawPackage.id);
    }

    /** Renders related packages from observed ViewModel state (pure view code). */
    private void renderRelatedPackages(java.util.List<UmrahPackage> filtered) {
        if (filtered == null || filtered.isEmpty()) return;
        container.addView(sectionHeading(getString(R.string.detail_section_related_packages)));

        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(PackageDetailActivity.this);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                PackageDetailActivity.this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        rv.setClipToPadding(false);
        rv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rvParams.topMargin = dp(10);
        rvParams.bottomMargin = dp(16);
        rv.setLayoutParams(rvParams);

        PackageCardAdapter adapter = new PackageCardAdapter(PackageDetailActivity.this, filtered, false);
        rv.setAdapter(adapter);

        container.addView(rv);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
