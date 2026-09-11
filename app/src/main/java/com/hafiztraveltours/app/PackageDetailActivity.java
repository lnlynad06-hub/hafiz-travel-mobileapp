package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 * Native package detail screen (itinerary, hotels, pricing tiers, included/
 * excluded, packing guide, gallery) - replaces the old WebViewActivity flow
 * for package cards. Content is pulled from a single Firestore document; see
 * firestore_schema_package_detail.md for the field layout each package
 * document should follow.
 *
 * Launch with:
 *   Intent intent = new Intent(context, PackageDetailActivity.class);
 *   intent.putExtra(PackageDetailActivity.EXTRA_COLLECTION, "umrah_packages"); // or "tour_packages"
 *   intent.putExtra(PackageDetailActivity.EXTRA_PACKAGE_ID, pkg.id);
 *   startActivity(intent);
 *
 * REQUIRES Glide - add to app/build.gradle.kts if not already present:
 *   implementation("com.github.bumptech.glide:glide:4.16.0")
 * (swap the Glide.with(...).load(...).into(...) calls below for your own
 * image loader if the project already uses something else, e.g. Coil/Picasso.)
 */
public class PackageDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PACKAGE_ID = "packageId";
    public static final String EXTRA_COLLECTION = "collection"; // "umrah_packages" | "tour_packages"

    // TODO: keep this in sync with MainActivity.WHATSAPP_PHONE_NUMBER (consider
    // moving both into a shared Constants class so there's only one place to update).
    private static final String WHATSAPP_PHONE_NUMBER = "60197859867";

    private LinearLayout container;
    private ImageView heroImage;
    private TextView bottomPrice;

    private PackageDetail detail;

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
        container = findViewById(R.id.detailContainer);
        bottomPrice = findViewById(R.id.detailBottomPrice);

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
                    detail = PackageDetail.fromUmrahPackage(response.body().data);
                    renderAll();
                } else {
                    Toast.makeText(PackageDetailActivity.this, "Pakej tidak dijumpai", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UmrahPackage>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(PackageDetailActivity.this, "Gagal menyambung ke pelayan backend", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderAll() {
        if (isFinishing() || isDestroyed()) return;
        try {
            Glide.with(this).load(detail.imageUrl).into(heroImage);
        } catch (Exception ignored) {}

        findViewById(R.id.detailWhatsappButton).setOnClickListener(v -> openWhatsAppForPackage());
        bottomPrice.setText(detail.price);

        addTitleSection();
        addPosterSection();
        addNightsBreakdownSection();
        addDepartureDatesSection();
        addHotelsSection();
        addItinerarySection();
        addImportantNotesSection();
        addIncludedExcludedSection();
        addPackingGuideSection();
        addPriceOptionsSection();
        addGallerySection();
    }

    // ---------------------------------------------------------------------
    // Sections
    // ---------------------------------------------------------------------

    private void addTitleSection() {
        TextView title = new TextView(this);
        title.setText(detail.name);
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.text_dark));
        container.addView(title);

        if (!detail.summaryLine.isEmpty()) {
            TextView summary = new TextView(this);
            summary.setText(detail.summaryLine);
            summary.setTextSize(14);
            summary.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(4);
            summary.setLayoutParams(p);
            container.addView(summary);
        }

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        badgeRow.setLayoutParams(rowParams);

        if (detail.durationDays > 0) {
            badgeRow.addView(buildBadge(detail.durationDays + " Hari " + detail.nightsCount + " Malam"));
        }
        if (!detail.price.isEmpty()) {
            LinearLayout.LayoutParams marginParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            marginParams.setMarginStart(dp(8));
            TextView priceBadge = buildBadge(getString(R.string.package_detail_price_from_label) + " " + detail.price);
            priceBadge.setLayoutParams(marginParams);
            badgeRow.addView(priceBadge);
        }
        container.addView(badgeRow);
    }

    private TextView buildBadge(String text) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(12);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(getResources().getColor(R.color.pink_dark));
        badge.setBackgroundResource(R.drawable.circle_bg_light);
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));
        return badge;
    }

    private void addPosterSection() {
        if (detail.posterImageUrl.isEmpty()) return;

        TextView heading = sectionHeading(getString(R.string.package_detail_poster_title));
        container.addView(heading);

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setAdjustViewBounds(true);
        poster.setBackgroundResource(R.drawable.bg_search_white);
        poster.setClickable(true);
        poster.setFocusable(true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220));
        p.topMargin = dp(12);
        poster.setLayoutParams(p);
        if (!isFinishing() && !isDestroyed()) {
            try {
                Glide.with(this).load(detail.posterImageUrl).into(poster);
            } catch (Exception ignored) {}
        }
        poster.setOnClickListener(v -> showZoomedImage(detail.posterImageUrl));
        container.addView(poster);
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

    private void addNightsBreakdownSection() {
        if (detail.nightsBreakdown.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_summary_title)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(12);
        row.setLayoutParams(rowParams);

        for (PackageDetail.NightBreakdown nb : detail.nightsBreakdown) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            chipParams.setMarginEnd(dp(8));
            chip.setLayoutParams(chipParams);
            chip.setPadding(dp(8), dp(12), dp(8), dp(12));

            TextView num = new TextView(this);
            num.setText(String.valueOf(nb.nights));
            num.setTextSize(18);
            num.setTypeface(null, Typeface.BOLD);
            num.setTextColor(getResources().getColor(R.color.pink_dark));
            num.setGravity(android.view.Gravity.CENTER);

            TextView label = new TextView(this);
            label.setText(nb.city);
            label.setTextSize(12);
            label.setTextColor(getResources().getColor(R.color.text_gray));
            label.setGravity(android.view.Gravity.CENTER);

            chip.addView(num);
            chip.addView(label);
            row.addView(chip);
        }
        container.addView(row);
    }

    private void addDepartureDatesSection() {
        if (detail.departureDatesNote.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_departure_title)));

        TextView note = new TextView(this);
        note.setText(detail.departureDatesNote);
        note.setTextSize(13);
        note.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(8);
        note.setLayoutParams(p);
        container.addView(note);
    }

    private void addHotelsSection() {
        if (detail.hotels.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_stay_title)));

        for (PackageDetail.HotelInfo hotel : detail.hotels) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(10);
            row.setLayoutParams(p);
            int pad = dp(14);
            row.setPadding(pad, pad, pad, pad);

            TextView title = new TextView(this);
            title.setText(hotel.title);
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));

            TextView subtitle = new TextView(this);
            subtitle.setText(hotel.subtitle);
            subtitle.setTextSize(13);
            subtitle.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subParams.topMargin = dp(2);
            subtitle.setLayoutParams(subParams);

            row.addView(title);
            row.addView(subtitle);
            container.addView(row);
        }
    }

    /** Each day renders as a collapsible card - tap the header to expand/collapse, matching the website's accordion. */
    private void addItinerarySection() {
        if (detail.itinerary.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_itinerary_title)));

        for (PackageDetail.ItineraryDay day : detail.itinerary) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.topMargin = dp(10);
            card.setLayoutParams(cardParams);
            int pad = dp(14);
            card.setPadding(pad, pad, pad, pad);
            card.setClickable(true);
            card.setFocusable(true);

            TextView dayLabel = new TextView(this);
            dayLabel.setText(day.dayLabel + (day.tag.isEmpty() ? "" : " \u00b7 " + day.tag));
            dayLabel.setTextSize(11);
            dayLabel.setTypeface(null, Typeface.BOLD);
            dayLabel.setTextColor(getResources().getColor(R.color.pink_dark));

            TextView title = new TextView(this);
            title.setText(day.title);
            title.setTextSize(15);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.topMargin = dp(4);
            title.setLayoutParams(titleParams);

            TextView route = new TextView(this);
            route.setText(day.routeText);
            route.setTextSize(12);
            route.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams routeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            routeParams.topMargin = dp(2);
            route.setLayoutParams(routeParams);

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setVisibility(View.GONE);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dp(10);
            body.setLayoutParams(bodyParams);

            for (String activity : day.activities) {
                TextView bullet = new TextView(this);
                bullet.setText("\u2022 " + activity);
                bullet.setTextSize(13);
                bullet.setTextColor(getResources().getColor(R.color.text_gray));
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.bottomMargin = dp(4);
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
                footer.setTextSize(12);
                footer.setTypeface(null, Typeface.ITALIC);
                footer.setTextColor(getResources().getColor(R.color.text_gray));
                LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                fp.topMargin = dp(6);
                footer.setLayoutParams(fp);
                body.addView(footer);
            }

            card.addView(dayLabel);
            card.addView(title);
            card.addView(route);
            card.addView(body);

            card.setOnClickListener(v ->
                    body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

            container.addView(card);
        }
    }

    private void addImportantNotesSection() {
        if (detail.importantNotes.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_notes_title)));

        for (PackageDetail.ImportantNote note : detail.importantNotes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.topMargin = dp(10);
            card.setLayoutParams(p);
            int pad = dp(14);
            card.setPadding(pad, pad, pad, pad);

            TextView title = new TextView(this);
            title.setText(note.title);
            title.setTextSize(14);
            title.setTypeface(null, Typeface.BOLD);
            title.setTextColor(getResources().getColor(R.color.text_dark));
            card.addView(title);

            if (!note.badge.isEmpty()) {
                TextView badge = buildBadge(note.badge);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bp.topMargin = dp(6);
                badge.setLayoutParams(bp);
                card.addView(badge);
            }

            for (String bullet : note.bullets) {
                TextView bulletView = new TextView(this);
                bulletView.setText("\u2022 " + bullet);
                bulletView.setTextSize(13);
                bulletView.setTextColor(getResources().getColor(R.color.text_gray));
                LinearLayout.LayoutParams bvp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bvp.topMargin = dp(6);
                bulletView.setLayoutParams(bvp);
                card.addView(bulletView);
            }

            container.addView(card);
        }
    }

    private void addIncludedExcludedSection() {
        if (!detail.included.isEmpty()) {
            container.addView(sectionHeading(getString(R.string.package_detail_included_title)));
            for (String item : detail.included) {
                container.addView(buildCheckRow("\u2713 " + item, getResources().getColor(R.color.text_dark)));
            }
        }
        if (!detail.excluded.isEmpty()) {
            container.addView(sectionHeading(getString(R.string.package_detail_excluded_title)));
            for (String item : detail.excluded) {
                container.addView(buildCheckRow("\u2715 " + item, getResources().getColor(R.color.text_gray)));
            }
        }
    }

    private TextView buildCheckRow(String text, int color) {
        TextView row = new TextView(this);
        row.setText(text);
        row.setTextSize(13);
        row.setTextColor(color);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(6);
        row.setLayoutParams(p);
        return row;
    }

    private void addPackingGuideSection() {
        if (detail.packingSummer.isEmpty() && detail.packingWinter.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_packing_title)));

        if (!detail.packingSummer.isEmpty()) {
            container.addView(buildSubHeading(getString(R.string.package_detail_packing_summer)));
            for (String item : detail.packingSummer) {
                container.addView(buildCheckRow("\u2022 " + item, getResources().getColor(R.color.text_gray)));
            }
        }
        if (!detail.packingWinter.isEmpty()) {
            container.addView(buildSubHeading(getString(R.string.package_detail_packing_winter)));
            for (String item : detail.packingWinter) {
                container.addView(buildCheckRow("\u2022 " + item, getResources().getColor(R.color.text_gray)));
            }
        }
    }

    private TextView buildSubHeading(String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(13);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.pink_dark));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(12);
        heading.setLayoutParams(p);
        return heading;
    }

    private void addPriceOptionsSection() {
        if (detail.priceOptions.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_price_options_title)));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setScrollBarSize(0);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(12);
        scroll.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (PackageDetail.PriceOption option : detail.priceOptions) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(10));
            card.setLayoutParams(cardParams);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));

            TextView priceText = new TextView(this);
            priceText.setText(option.price);
            priceText.setTextSize(16);
            priceText.setTypeface(null, Typeface.BOLD);
            priceText.setTextColor(getResources().getColor(R.color.pink_dark));

            TextView occupancy = new TextView(this);
            occupancy.setText(option.occupancyLabel);
            occupancy.setTextSize(12);
            occupancy.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            op.topMargin = dp(4);
            occupancy.setLayoutParams(op);

            card.addView(priceText);
            card.addView(occupancy);
            row.addView(card);
        }

        scroll.addView(row);
        container.addView(scroll);
    }

    private void addGallerySection() {
        if (detail.galleryImageUrls.isEmpty()) return;

        container.addView(sectionHeading(getString(R.string.package_detail_gallery_title)));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setScrollBarSize(0);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(12);
        scroll.setLayoutParams(scrollParams);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        for (String url : detail.galleryImageUrls) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(160), dp(120));
            ip.setMarginEnd(dp(10));
            image.setLayoutParams(ip);
            image.setBackgroundResource(R.drawable.bg_search_white);
            image.setClickable(true);
            image.setFocusable(true);
            if (!isFinishing() && !isDestroyed()) {
                try {
                    Glide.with(this).load(url).into(image);
                } catch (Exception ignored) {}
            }
            image.setOnClickListener(v -> showZoomedImage(url));
            row.addView(image);
        }

        scroll.addView(row);
        container.addView(scroll);
    }

    private TextView sectionHeading(String text) {
        TextView heading = new TextView(this);
        heading.setText(text);
        heading.setTextSize(17);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(getResources().getColor(R.color.text_dark));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(24);
        heading.setLayoutParams(p);
        return heading;
    }

    /** Opens WhatsApp with a prefilled message about this package - never opens a web browser/link. */
    private void openWhatsAppForPackage() {
        String message = !detail.whatsappMessage.isEmpty()
                ? detail.whatsappMessage
                : getString(R.string.package_detail_whatsapp_default_message, detail.name);

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
