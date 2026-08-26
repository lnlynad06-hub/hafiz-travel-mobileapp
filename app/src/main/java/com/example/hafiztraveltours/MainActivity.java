package com.example.hafiztraveltours;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.batoulapps.adhan.CalculationMethod;
import com.batoulapps.adhan.CalculationParameters;
import com.batoulapps.adhan.Coordinates;
import com.batoulapps.adhan.data.DateComponents;
import com.batoulapps.adhan.Madhab;
import com.batoulapps.adhan.PrayerTimes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    // TODO: replace with your actual WhatsApp business number, format: countrycode+number, no + or spaces
    private static final String WHATSAPP_PHONE_NUMBER = "60123456789";

    // Official Nusuk app by the Ministry of Hajj and Umrah (verified package name)
    private static final String NUSUK_PACKAGE_NAME = "com.moh.nusukapp";

    // Google Business review link - confirmed via Place ID (Hafiz Travel & Tours Sdn Bhd)
    private static final String GOOGLE_REVIEW_URL = "https://search.google.com/local/writereview?placeid=ChIJhyLSxhBt2jERN8jHNbZ59y4";

    // Bottom nav Umrah/Tour open these pages in-app via WebViewActivity (confirmed real URLs)
    private static final String URL_UMRAH = "https://hafiztraveltours.com/pakej-umrah";
    private static final String URL_TOUR = "https://hafiztraveltours.com/tour";

    private String activeLanguage;

    // Now backed by real Firebase Authentication session (see loadSessionState()).
    private boolean isLoggedIn;
    private String loggedInUserName;

    // Prayer times location permission flow
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentSaved = LocaleHelper.getSavedLanguage(this);
        if (activeLanguage != null && !activeLanguage.equals(currentSaved)) {
            recreate();
        }
        activeLanguage = currentSaved;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadSessionState();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        loadPrayerTimesForCurrentLocation();
                    } else {
                        showPrayerTimesLocationDenied();
                    }
                });

        setupHeroSection();
        setupPopularPackages();
        setupQuickActions();
        setupBottomNav();
        setupMenu();
        setupInfoSection();
        setupPrayerTimesWidget();
    }

    /**
     * Checks the real Firebase Authentication session instead of a manual
     * SharedPreferences flag. If a user is signed in, show their real name
     * (falls back to their email if no display name was set).
     */
    private void loadSessionState() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            isLoggedIn = true;
            String name = currentUser.getDisplayName();
            loggedInUserName = (name != null && !name.isEmpty()) ? name : currentUser.getEmail();
        } else {
            isLoggedIn = false;
            loggedInUserName = "Pengguna";
        }
    }

    /**
     * Greeting text is personalized when logged in, purely cosmetic - every
     * feature below (quick actions, packages, register CTA, FAQ, review) is
     * identical for guests and logged-in users.
     */
    private void setupHeroSection() {
        TextView greetingText = findViewById(R.id.greetingText);
        TextView heroSubtitle = findViewById(R.id.heroSubtitle);
        TextView heroHeadline = findViewById(R.id.heroHeadline);

        if (isLoggedIn) {
            greetingText.setText(getString(R.string.user_greeting, loggedInUserName));
            heroSubtitle.setText(getString(R.string.hero_subtitle_user));
            heroHeadline.setText(getString(R.string.hero_headline_user));
        } else {
            greetingText.setText(getString(R.string.guest_greeting));
            heroSubtitle.setText("");
            heroHeadline.setText(getString(R.string.hero_headline_guest));
        }

        findViewById(R.id.notificationButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.no_notifications), Toast.LENGTH_SHORT).show());

        findViewById(R.id.searchBarHero).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.search_coming_soon), Toast.LENGTH_SHORT).show());
    }

    /**
     * Reuses the same LocaleHelper that WelcomeActivity's language pills use,
     * so switching language here stays consistent with the rest of the app.
     * Now opened from the hamburger menu instead of a dedicated top icon.
     */
    private void showLanguagePicker() {
        String[] labels = {"English", "Bahasa Melayu", "\u0627\u0644\u0639\u0631\u0628\u064a\u0629", "\ud55c\uad6d\uc5b4", "\u65e5\u672c\u8a9e", "\u4e2d\u6587"};
        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY,
                LocaleHelper.LANGUAGE_ARABIC,
                LocaleHelper.LANGUAGE_KOREAN,
                LocaleHelper.LANGUAGE_JAPANESE,
                LocaleHelper.LANGUAGE_CHINESE
        };

        String current = LocaleHelper.getSavedLanguage(this);
        int checkedIndex = -1;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                checkedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_language))
                .setSingleChoiceItems(labels, checkedIndex, (dialog, which) -> {
                    LocaleHelper.saveLanguage(this, codes[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private List<Package> allPopularPackages = new ArrayList<>();

    /**
     * Pakej Popular - pulled from the "Popular" sections on hafiztraveltours.com
     * (/pakej-umrah and /tour) on 2026-08-24, including each package's real
     * detail page URL and a local image. Tapping a card opens that URL in
     * WebViewActivity. Prices/durations/links WILL drift as the website changes.
     * TODO: replace with real API call to the backend so this stays in sync
     * automatically instead of needing manual updates here.
     *
     * IMAGES: put each file in res/drawable using the exact names below
     * (lowercase, no spaces, .png or .jpg). If a drawable is missing, the
     * project will fail to build - add all 6 before running.
     */
    private void setupPopularPackages() {
        allPopularPackages = new ArrayList<>();
        // Umrah Popular (from hafiztraveltours.com/pakej-umrah)
        allPopularPackages.add(new Package("ASB", getString(R.string.package_duration_price, 10, 8, "6,050"),
                "https://hafiztraveltours.com/pakej-umrah/asb",
                R.drawable.img_asb));
        allPopularPackages.add(new Package("EMAS MH", getString(R.string.package_duration_price, 12, 10, "8,450"),
                "https://hafiztraveltours.com/pakej-umrah/umrah-emas",
                R.drawable.img_emas));
        allPopularPackages.add(new Package("SUKUK MH", getString(R.string.package_duration_price, 12, 10, "8,850"),
                "https://hafiztraveltours.com/pakej-umrah/umrah-sukuk",
                R.drawable.img_sukuk));
        // Tour Popular (from hafiztraveltours.com/tour)
        allPopularPackages.add(new Package("Korea (Seoul)", getString(R.string.package_duration_price, 6, 4, "3,250"),
                "https://hafiztraveltours.com/tour/pakej/korea-seoul-6h4m",
                R.drawable.img_korea1));
        allPopularPackages.add(new Package("Korea (Seoul)", getString(R.string.package_duration_price, 4, 3, "2,950"),
                "https://hafiztraveltours.com/tour/pakej/korea-seoul-4h3m",
                R.drawable.img_korea2));
        allPopularPackages.add(new Package("Turkiye", getString(R.string.package_duration_price, 9, 7, "4,850"),
                "https://hafiztraveltours.com/tour/pakej/turkiye",
                R.drawable.img_turkiye));

        RecyclerView recyclerView = findViewById(R.id.popularPackagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new PackagePopularAdapter(allPopularPackages));

        findViewById(R.id.seeAllPopular).setOnClickListener(v ->
                openCategoryPage("Hafiz Travel & Tours", "https://hafiztraveltours.com/"));
    }

    private void setupQuickActions() {
        // Nusuk - links out to the official Nusuk app on the Play Store
        findViewById(R.id.featureNusuk).setOnClickListener(v -> openNusukOnPlayStore());

        // Guideline - persediaan/checklist Umrah & Tour
        findViewById(R.id.featureGuideline).setOnClickListener(v -> showGuidelineDialog());

        // Checklist - same items as Guideline, but interactive (tickable)
        findViewById(R.id.featureChecklist).setOnClickListener(v -> showChecklistDialog());

        findViewById(R.id.featureWhatsapp).setOnClickListener(v -> openWhatsApp());
    }

    /**
     * TODO: replace with real content (can adapt from the "Checklist Persediaan
     * Sebelum Menunaikan Umrah" article on hafiztraveltours.com), and consider a
     * proper screen/artifact instead of a dialog once content grows.
     */
    private void showGuidelineDialog() {
        String message = "\u2713 Pasport sah sekurang-kurangnya 6 bulan\n\n"
                + "\u2713 Suntikan meningitis (jika diperlukan)\n\n"
                + "\u2713 Pakaian ihram / pakaian sesuai\n\n"
                + "\u2713 Ubat-ubatan peribadi\n\n"
                + "\u2713 Salinan dokumen penting (pasport, tiket, visa)\n\n"
                + "\u2713 Wang tunai secukupnya (Riyal / USD)";

        new AlertDialog.Builder(this)
                .setTitle("Guideline Persediaan")
                .setMessage(message)
                .setPositiveButton("Tutup", null)
                .show();
    }

    /**
     * Checklist adapted from the real "Travel Guide" PDF (Korea trip) sections:
     * Dokumen Perjalanan, Keperluan Ubat-ubatan, Musim & Pakaian, Peralatan
     * Elektronik, Pertukaran Mata Wang. Grouped with category headers so it
     * matches how the PDF itself is organized.
     *
     * TODO: this is currently one generic checklist for the whole app. Once
     * each package/trip can have its own guide (like this Korea PDF), pull
     * the items per-trip from the backend instead of this hardcoded list -
     * e.g. destination-specific items like "suntikan meningitis" only apply
     * to Umrah/Haj trips, not a Korea tour.
     */
    private void showChecklistDialog() {
        LinkedHashMap<String, String[]> sections = new LinkedHashMap<>();
        sections.put("Dokumen Perjalanan", new String[]{
                "Pasport sah 6 bulan ke atas",
                "Bawa pasport bersama",
                "Bawa pasport lama (jika baru perbaharui)",
                "Semak status perjalanan luar negara (SSPI)"
        });
        sections.put("Ubat-ubatan", new String[]{
                "Panadol",
                "Ubat sakit tekak",
                "Ubat cirit-birit",
                "Ubat tahan muntah",
                "Salonpas",
                "Minyak angin",
                "Olive oil / moisturizer / lipbalm",
                "Heat pad"
        });
        sections.put("Pakaian", new String[]{
                "Jaket nipis (lapisan sederhana)",
                "Jeans / seluar panjang nipis",
                "Jaket tambahan (jika cuaca sejuk/berangin)",
                "Kasut bertutup & selesa (sneakers)",
                "Sunglasses & cap"
        });
        sections.put("Peralatan Elektronik", new String[]{
                "Travel adaptor",
                "Powerbank (maks. 20,000mAh - letak handcarry)"
        });
        sections.put("Kewangan", new String[]{
                "Tukar mata wang mengikut destinasi",
                "Aktifkan kad debit/credit untuk kegunaan luar negara"
        });

        SharedPreferences checklistPrefs = getSharedPreferences("checklist_state", Context.MODE_PRIVATE);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, dp(8), pad, dp(8));
        scrollView.addView(content);

        for (Map.Entry<String, String[]> section : sections.entrySet()) {
            TextView header = new TextView(this);
            header.setText(section.getKey());
            header.setTextSize(14);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setTextColor(getResources().getColor(R.color.pink_dark));
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headerParams.topMargin = dp(14);
            header.setLayoutParams(headerParams);
            content.addView(header);

            for (String itemText : section.getValue()) {
                android.widget.CheckBox checkBox = new android.widget.CheckBox(this);
                checkBox.setText(itemText);
                checkBox.setTextSize(14);
                checkBox.setTextColor(getResources().getColor(R.color.text_dark));
                checkBox.setChecked(checklistPrefs.getBoolean(itemText, false));
                checkBox.setOnCheckedChangeListener((btn, isChecked) ->
                        checklistPrefs.edit().putBoolean(itemText, isChecked).apply());
                content.addView(checkBox);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Checklist Persediaan")
                .setView(scrollView)
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void openNusukOnPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + NUSUK_PACKAGE_NAME)));
        } catch (android.content.ActivityNotFoundException e) {
            // Play Store app not available - fall back to the web link
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + NUSUK_PACKAGE_NAME)));
        }
    }

    private void openWhatsApp() {
        try {
            Uri uri = Uri.parse("https://wa.me/" + WHATSAPP_PHONE_NUMBER);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp tidak dijumpai", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Umrah / Tour on the bottom nav now open the real website pages in-app
     * (via WebViewActivity) instead of filtering the Pakej Popular list.
     */
    private void setupBottomNav() {
        findViewById(R.id.navUmrah).setOnClickListener(v -> openCategoryPage("Umrah", URL_UMRAH));
        findViewById(R.id.navTour).setOnClickListener(v -> openCategoryPage("Tour", URL_TOUR));
        findViewById(R.id.navFavorite).setOnClickListener(v ->
                Toast.makeText(this, "Favorite - akan datang", Toast.LENGTH_SHORT).show());
    }

    private void openCategoryPage(String title, String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, title);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    /**
     * Hamburger menu -> Profil Saya / Bahasa / Tentang Kami / Hubungi Kami.
     * Theme toggle and Log Keluar live inside ProfileActivity.
     * Session state comes from real Firebase Authentication - see
     * loadSessionState() - so guests are simply routed to sign up.
     * Language picker moved here from the top hero row (previously its own icon).
     * TODO: swap this simple dialog for a proper navigation drawer / bottom sheet later.
     */
    private void setupMenu() {
        findViewById(R.id.menuButton).setOnClickListener(v -> {
            String[] options = new String[]{
                    getString(R.string.menu_profile),
                    getString(R.string.menu_language),
                    getString(R.string.menu_about_us),
                    getString(R.string.menu_contact_us)
            };

            new AlertDialog.Builder(this)
                    .setTitle("Menu")
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                if (isLoggedIn) {
                                    startActivity(new Intent(this, ProfileActivity.class));
                                } else {
                                    startActivity(new Intent(this, SignUpActivity.class));
                                }
                                break;
                            case 1:
                                showLanguagePicker();
                                break;
                            case 2:
                                startActivity(new Intent(this, TentangKamiActivity.class));
                                break;
                            case 3:
                                startActivity(new Intent(this, HubungiKamiActivity.class));
                                break;
                        }
                    })
                    .show();
        });
    }

    /**
     * Google Review + Register CTA + FAQ - shown to EVERYONE (guest or logged in).
     * Main purpose of this section is lead capture (register) and building trust
     * (reviews, FAQ), not gating any feature.
     */
    private void setupInfoSection() {
        setupGoogleReview();
        setupRegisterCta();
        setupFaq();
    }

    /**
     * TODO: replace the rating/count text and review snippets with real data
     * (ideally pulled from the Google Places API using GOOGLE_REVIEW_PLACE_ID),
     * and confirm GOOGLE_REVIEW_URL once you have the Place ID.
     */
    private void setupGoogleReview() {
        findViewById(R.id.googleReviewRatingRow).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_REVIEW_URL)));
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat membuka pautan review", Toast.LENGTH_SHORT).show();
            }
        });

        setupReviewSnippets();
    }

    /**
     * A few short review snippets shown next to the rating, so people see
     * real feedback AND notice where to leave their own review.
     * TODO: replace dummy snippets with real reviews (Google Places API or manual curation).
     */
    private void setupReviewSnippets() {
        LinearLayout container = findViewById(R.id.reviewSnippetsContainer);
        container.removeAllViews();

        String[][] reviews = {
                {getString(R.string.review_1_name), getString(R.string.review_1_quote)},
                {getString(R.string.review_2_name), getString(R.string.review_2_quote)},
                {getString(R.string.review_3_name), getString(R.string.review_3_quote)}
        };

        for (String[] r : reviews) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_pill_active_nav);
            int padding = dp(12);
            card.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMarginEnd(dp(10));
            card.setLayoutParams(cardParams);

            TextView stars = new TextView(this);
            stars.setText("\u2605\u2605\u2605\u2605\u2605");
            stars.setTextSize(11);
            stars.setTextColor(getResources().getColor(R.color.pink_dark));

            TextView quote = new TextView(this);
            quote.setText("\u201C" + r[1] + "\u201D");
            quote.setTextSize(13);
            quote.setTextColor(getResources().getColor(R.color.text_gray));
            LinearLayout.LayoutParams quoteParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            quoteParams.topMargin = dp(4);
            quote.setLayoutParams(quoteParams);

            TextView name = new TextView(this);
            name.setText("\u2014 " + r[0]);
            name.setTextSize(12);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            name.setTextColor(getResources().getColor(R.color.text_dark));
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            nameParams.topMargin = dp(6);
            name.setLayoutParams(nameParams);

            card.addView(stars);
            card.addView(quote);
            card.addView(name);
            container.addView(card);
        }
    }

    private void setupRegisterCta() {
        View ctaCard = findViewById(R.id.registerCtaCard);

        findViewById(R.id.registerCtaButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.signup_coming_soon), Toast.LENGTH_SHORT).show());
        // TODO: startActivity(new Intent(this, SignUpActivity.class));

        findViewById(R.id.registerCtaDismiss).setOnClickListener(v ->
                ctaCard.setVisibility(View.GONE));
    }

    /**
     * Simple expandable FAQ: tap question to reveal/hide the answer.
     * TODO: replace with real content once agreed with the team.
     */
    private void setupFaq() {
        LinearLayout container = findViewById(R.id.faqContainer);
        container.removeAllViews();

        String[][] faqs = {
                {getString(R.string.faq_q1), getString(R.string.faq_a1)},
                {getString(R.string.faq_q2), getString(R.string.faq_a2)},
                {getString(R.string.faq_q3), getString(R.string.faq_a3)}
        };

        for (String[] faq : faqs) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setBackgroundResource(R.drawable.bg_search_white);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            itemParams.bottomMargin = dp(10);
            item.setLayoutParams(itemParams);
            int padding = dp(14);
            item.setPadding(padding, padding, padding, padding);
            item.setClickable(true);
            item.setFocusable(true);

            TextView question = new TextView(this);
            question.setText(faq[0]);
            question.setTextSize(15);
            question.setTypeface(null, android.graphics.Typeface.BOLD);
            question.setTextColor(getResources().getColor(R.color.text_dark));

            TextView answer = new TextView(this);
            answer.setText(faq[1]);
            answer.setTextSize(13);
            answer.setTextColor(getResources().getColor(R.color.text_gray));
            answer.setVisibility(View.GONE);
            LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            answerParams.topMargin = dp(6);
            answer.setLayoutParams(answerParams);

            item.addView(question);
            item.addView(answer);

            item.setOnClickListener(v ->
                    answer.setVisibility(answer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

            container.addView(item);
        }
    }

    // ================== WAKTU SOLAT (REAL, using Adhan library) ==================

    /**
     * "Daily use" hook so people open the app even when they're not booking.
     *
     * Uses the Adhan library (com.batoulapps.adhan:adhan:1.2.1, MIT licensed,
     * astronomical formulas from Jean Meeus' "Astronomical Algorithms") fed by
     * the device's real GPS/network location + today's date. This replaces the
     * old hardcoded placeholder times.
     *
     * REQUIRED before this compiles/runs:
     * 1) Add to app/build.gradle(.kts):  implementation("com.batoulapps.adhan:adhan:1.2.1")
     * 2) Add to AndroidManifest.xml (outside <application>):
     *      <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
     *      <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
     *
     * Calculation method used: SINGAPORE (Fajr 20 deg, Isha 18 deg) - this is
     * the closest built-in preset to JAKIM's Malaysian parameters, but it is
     * NOT an official JAKIM method. Cross-check against e-solat.gov.my before
     * relying on this for real worship - getting this wrong is a
     * trust-breaking bug for a Muslim-facing app.
     */
    private void setupPrayerTimesWidget() {
        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (hasFineLocation || hasCoarseLocation) {
            loadPrayerTimesForCurrentLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void loadPrayerTimesForCurrentLocation() {
        TextView dateText = findViewById(R.id.prayerTimesDateText);
        LinearLayout row = findViewById(R.id.prayerTimesRow);

        Location location = getBestLastKnownLocation();
        if (location == null) {
            dateText.setText(getString(R.string.prayer_default_location, "Johor Bahru"));
            // Fallback to the office's own city so the widget isn't empty on
            // first run before a GPS fix is available.
            calculateAndDisplayPrayerTimes(1.4927, 103.7414, row);
            return;
        }

        dateText.setText("Waktu Solat Hari Ini");
        calculateAndDisplayPrayerTimes(location.getLatitude(), location.getLongitude(), row);
    }

    private Location getBestLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return null;

        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasFineLocation && !hasCoarseLocation) return null;

        Location best = null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                Location candidate = locationManager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getTime() > best.getTime())) {
                    best = candidate;
                }
            } catch (SecurityException ignored) {
                // Permission was revoked between the check above and this call - skip.
            }
        }
        return best;
    }

    private void calculateAndDisplayPrayerTimes(double latitude, double longitude, LinearLayout row) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        DateComponents dateComponents = new DateComponents(
                today.get(java.util.Calendar.YEAR),
                today.get(java.util.Calendar.MONTH) + 1,
                today.get(java.util.Calendar.DAY_OF_MONTH));

        Coordinates coordinates = new Coordinates(latitude, longitude);
        CalculationParameters params = CalculationMethod.SINGAPORE.getParameters();
        params.madhab = Madhab.SHAFI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, params);

        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());

        String[][] times = {
                {getString(R.string.prayer_subuh), formatter.format(prayerTimes.fajr)},
                {getString(R.string.prayer_zohor), formatter.format(prayerTimes.dhuhr)},
                {getString(R.string.prayer_asar), formatter.format(prayerTimes.asr)},
                {getString(R.string.prayer_maghrib), formatter.format(prayerTimes.maghrib)},
                {getString(R.string.prayer_isyak), formatter.format(prayerTimes.isha)}
        };

        row.removeAllViews();
        for (String[] t : times) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER);
            col.setPadding(dp(4), dp(6), dp(4), dp(6));
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(colParams);

            TextView name = new TextView(this);
            name.setText(t[0]);
            name.setTextSize(11);
            name.setTextColor(getResources().getColor(R.color.text_gray));
            name.setGravity(android.view.Gravity.CENTER);

            TextView time = new TextView(this);
            time.setText(t[1]);
            time.setTextSize(13);
            time.setTypeface(null, android.graphics.Typeface.BOLD);
            time.setTextColor(getResources().getColor(R.color.text_dark));
            time.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            timeParams.topMargin = dp(2);
            time.setLayoutParams(timeParams);

            col.addView(name);
            col.addView(time);
            row.addView(col);
        }
    }

    /**
     * Permission denied - show a retry button instead of silently failing or
     * making up numbers. Tapping it re-requests permission.
     */
    private void showPrayerTimesLocationDenied() {
        TextView dateText = findViewById(R.id.prayerTimesDateText);
        LinearLayout row = findViewById(R.id.prayerTimesRow);

        dateText.setText("Aktifkan lokasi untuk lihat waktu solat");
        row.removeAllViews();

        TextView retryButton = new TextView(this);
        retryButton.setText("Guna Lokasi Saya");
        retryButton.setTextSize(13);
        retryButton.setTypeface(null, android.graphics.Typeface.BOLD);
        retryButton.setTextColor(getResources().getColor(R.color.pink_dark));
        retryButton.setBackgroundResource(R.drawable.bg_pill_active_nav);
        int h = dp(10);
        int v = dp(8);
        retryButton.setPadding(h, v, h, v);
        retryButton.setClickable(true);
        retryButton.setFocusable(true);
        retryButton.setOnClickListener(v2 ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retryButton.setLayoutParams(params);
        row.addView(retryButton);
    }

    // ================== end waktu solat ==================

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}