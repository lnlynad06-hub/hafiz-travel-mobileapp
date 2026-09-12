package com.hafiztraveltours.app;

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
    private ImageView favoriteButton;
    private ImageView shareButton;

    private PackageDetail detail;
    private UmrahPackage rawPackage;

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
        favoriteButton = findViewById(R.id.detailFavoriteButton);
        shareButton = findViewById(R.id.detailShareButton);

        String packageId = getIntent().getStringExtra(EXTRA_PACKAGE_ID);
        String collection = getIntent().getStringExtra(EXTRA_COLLECTION);
        if (collection == null) collection = "umrah_packages";

        if (packageId == null) {
            Toast.makeText(this, "Pakej tidak dijumpai", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PackageDetailActivity.this, "Pakej tidak dijumpai", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UmrahPackage>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(PackageDetailActivity.this, "Gagal memuatkan data pakej", Toast.LENGTH_SHORT).show();
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
            boolean isUmrah = rawPackage != null ? rawPackage.isUmrah() : detail.name.toLowerCase().contains("umrah");
            heroCategoryBadge.setText(isUmrah ? "✨ PAKEJ UMRAH" : "✈️ PAKEJ PELANCONGAN");
        }

        // 3. Top Actions (Favorite & Share)
        setupTopActions();

        // 4. Bottom Price & WhatsApp CTA
        bottomPrice.setText(detail.price);
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
                        "💰 Harga Dari: *" + detail.price + "*\n" +
                        (detail.durationDays > 0 ? ("⏱️ Tempoh: *" + detail.durationDays + " Hari " + detail.nightsCount + " Malam*\n\n") : "\n") +
                        (detail.summaryLine != null && !detail.summaryLine.isEmpty() ? (detail.summaryLine + "\n\n") : "") +
                        "📲 Tempah atau tanya lanjut: https://wa.me/" + WHATSAPP_PHONE_NUMBER;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "Kongsi Pakej Melalui"));
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
            row.addView(buildSpecChip("⏱️ " + detail.durationDays + " Hari " + detail.nightsCount + " Malam"));
        }

        if (rawPackage != null && rawPackage.airlineName != null && !rawPackage.airlineName.isEmpty()) {
            row.addView(buildSpecChip("✈️ " + rawPackage.airlineName));
        }

        if (rawPackage != null && rawPackage.hotelMakkahRating != null && !rawPackage.hotelMakkahRating.isEmpty()) {
            row.addView(buildSpecChip("🏨 " + rawPackage.hotelMakkahRating));
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

        container.addView(sectionHeading("Ringkasan Penginapan"));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);

        for (PackageDetail.NightBreakdown nb : detail.nightsBreakdown) {
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
            count.setText(nb.nights + " Malam");
            count.setTextSize(15);
            count.setTypeface(null, Typeface.BOLD);
            count.setTextColor(getResources().getColor(R.color.brand_magenta));

            TextView city = new TextView(this);
            city.setText(nb.city);
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
        container.addView(sectionHeading("Hotel & Penerbangan"));

        if (detail.hotels.isEmpty()) {
            container.addView(buildEmptyNoticeCard("Maklumat hotel & penerbangan akan dikemaskini kemudian."));
            return;
        }

        for (PackageDetail.HotelInfo hotel : detail.hotels) {
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
            title.setText(hotel.title);
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

        container.addView(sectionHeading("Pilihan Bilik & Harga"));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setScrollBarSize(0);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(10);
        scroll.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (PackageDetail.PriceOption option : detail.priceOptions) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(10));
            card.setLayoutParams(cardParams);

            TextView occupancy = new TextView(this);
            occupancy.setText(option.occupancyLabel);
            occupancy.setTextSize(12);
            occupancy.setTypeface(null, Typeface.BOLD);
            occupancy.setTextColor(getResources().getColor(R.color.text_dark));

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
            perPax.setText("/ seorang");
            perPax.setTextSize(11);
            perPax.setTextColor(getResources().getColor(R.color.text_gray));

            card.addView(occupancy);
            card.addView(priceText);
            card.addView(perPax);
            row.addView(card);
        }

        scroll.addView(row);
        container.addView(scroll);
    }

    private void addItinerarySection() {
        container.addView(sectionHeading("Jadual Perjalanan (Itinerary)"));

        if (detail.itinerary.isEmpty()) {
            container.addView(buildEmptyNoticeCard("Jadual perjalanan terperinci akan dikemaskini kemudian."));
            return;
        }

        TextView hint = new TextView(this);
        hint.setText("Tekan mana-mana hari untuk melihat aktiviti terperinci.");
        hint.setTextSize(11);
        hint.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(2);
        hint.setLayoutParams(hintParams);
        container.addView(hint);

        for (int i = 0; i < detail.itinerary.size(); i++) {
            PackageDetail.ItineraryDay day = detail.itinerary.get(i);
            boolean isFirst = (i == 0);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_detail_card);
            card.setClickable(true);
            card.setFocusable(true);
            int pad = dp(14);
            card.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(8);
            card.setLayoutParams(cardParams);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView badge = new TextView(this);
            badge.setText(day.dayLabel + " \u2022 " + day.tag);
            badge.setTextSize(11);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(getResources().getColor(R.color.brand_magenta));

            View spacer = new View(this);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
            spacer.setLayoutParams(sp);

            TextView chevron = new TextView(this);
            chevron.setText(isFirst ? "▲" : "▼");
            chevron.setTextSize(11);
            chevron.setTextColor(getResources().getColor(R.color.brand_magenta));

            headerRow.addView(badge);
            headerRow.addView(spacer);
            headerRow.addView(chevron);

            TextView title = new TextView(this);
            title.setText(day.title);
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(4);
            title.setLayoutParams(titleParams);

            TextView route = new TextView(this);
            route.setText("📍 " + (day.routeText != null && !day.routeText.isEmpty() ? day.routeText : "Destinasi Perjalanan"));
            route.setTextSize(12);
            route.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams routeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            routeParams.topMargin = dp(2);
            route.setLayoutParams(routeParams);

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setVisibility(isFirst ? View.VISIBLE : View.GONE);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dp(10);
            body.setLayoutParams(bodyParams);

            for (String activity : day.activities) {
                TextView bullet = new TextView(this);
                bullet.setText("• " + activity);
                bullet.setTextSize(12);
                bullet.setTextColor(getResources().getColor(R.color.text_dark));
                bullet.setLineSpacing(dp(1), 1.15f);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.bottomMargin = dp(5);
                bullet.setLayoutParams(bp);
                body.addView(bullet);
            }

            if (!day.hotelNote.isEmpty() || !day.mealNote.isEmpty()) {
                TextView footer = new TextView(this);
                StringBuilder sb = new StringBuilder();
                if (!day.hotelNote.isEmpty()) sb.append("🏨 Hotel: ").append(day.hotelNote);
                if (!day.mealNote.isEmpty()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("🍽️ Makan: ").append(day.mealNote);
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

            card.addView(headerRow);
            card.addView(title);
            card.addView(route);
            card.addView(body);

            card.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                boolean isExpanded = (body.getVisibility() == View.VISIBLE);
                body.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
                chevron.setText(isExpanded ? "▼" : "▲");
            });

            container.addView(card);
        }
    }

    private void addIncludedExcludedSection() {
        if (detail.included.isEmpty() && detail.excluded.isEmpty()) {
            return;
        }

        container.addView(sectionHeading("Pakej Termasuk & Tidak Termasuk"));

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
            incHeading.setText("✅ Termasuk Dalam Pakej:");
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
            excHeading.setText("❌ Tidak Termasuk:");
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

        container.addView(sectionHeading("Panduan & Senarai Keperluan"));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_detail_card);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(10);
        card.setLayoutParams(cp);

        TextView heading = new TextView(this);
        heading.setText("🎒 Barang Wajib Dibawa:");
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

        container.addView(sectionHeading("Nota Penting & Syarat"));

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
            title.setText("ℹ️ " + note.title);
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

        container.addView(sectionHeading("Galeri Foto"));

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
        String message = (detail != null && detail.whatsappMessage != null && !detail.whatsappMessage.isEmpty())
                ? detail.whatsappMessage
                : "Salam, saya berminat dengan pakej " + (detail != null ? detail.name : "");

        try {
            Uri uri = Uri.parse("https://wa.me/" + WHATSAPP_PHONE_NUMBER
                    + "?text=" + Uri.encode(message));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp tidak dijumpai", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
