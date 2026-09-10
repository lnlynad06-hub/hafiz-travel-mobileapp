package com.hafiztraveltours.app;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.HomeDataResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // TODO: replace with your actual WhatsApp business number, format: countrycode+number, no + or spaces
    private static final String WHATSAPP_PHONE_NUMBER = "60197859867";
    // Official Nusuk app by the Ministry of Hajj and Umrah (verified package name)
    private static final String NUSUK_PACKAGE_NAME = "com.moh.nusukapp";

    // Google Business review link - confirmed via Place ID (Hafiz Travel & Tours Sdn Bhd)
    private static final String GOOGLE_REVIEW_URL = "https://search.google.com/local/writereview?placeid=ChIJhyLSxhBt2jERN8jHNbZ59y4";

    // Bottom nav Umrah/Tour open these pages in-app via WebViewActivity (confirmed real URLs)
    private static final String URL_UMRAH = "https://hafiztraveltours.com/pakej-umrah";
    private static final String URL_TOUR = "https://hafiztraveltours.com/tour";

    // Malaysia Waktu Solat API (mptwaktusolat) - free, MIT licensed, sources data
    // directly from JAKIM e-solat. Official domain as of 2026: api.waktusolat.app
    private static final String JAKIM_API_BASE = "https://api.waktusolat.app/v2/solat/gps/";
    private static final String ZONE_LOOKUP_URL = "https://api.waktusolat.app/zones/gps";
    private static final String ZONE_SOLAT_URL = "https://api.waktusolat.app/v2/solat/";

    // YouTube channel - JELAJAH HAFIZ (Podcast Jumaat series)
    private static final String YOUTUBE_CHANNEL_URL = "https://www.youtube.com/@hafiztravelandtours";

    private String activeLanguage;

    // Now backed by real Firebase Authentication session (see loadSessionState()).
    private boolean isLoggedIn;
    private String loggedInUserName;

    // Prayer times location permission flow
    private ActivityResultLauncher<String> locationPermissionLauncher;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private android.os.Handler arcRefreshHandler;
    private Runnable arcRefreshRunnable;

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
        startArcAutoRefresh();
        startHeroShowcase();
        updateFavoriteBadge();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopArcAutoRefresh();
        stopHeroShowcase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
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
        setupHeroShowcase();
        setupPopularPackages();
        setupQuickActions();
        setupBottomNav();
        setupMenu();
        setupInfoSection();
        setupPodcastSection();
        setupPrayerTimesWidget();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
        PrayerTimeScheduler.requestExactAlarmPermissionIfNeeded(this);
        PrayerTimeScheduler.requestBatteryOptimizationExemption(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
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
            heroSubtitle.setText("Guest");
            heroHeadline.setText(getString(R.string.hero_headline_guest));
        }

        // Profile Avatar click
        View profileAvatar = findViewById(R.id.profileAvatar);
        if (profileAvatar != null) {
            profileAvatar.setOnClickListener(v -> {
                if (isLoggedIn) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else {
                    startActivity(new Intent(this, SignUpActivity.class));
                }
            });
        }

        // Luxury Language Pill Button
        View btnLanguagePicker = findViewById(R.id.btnLanguagePicker);
        if (btnLanguagePicker != null) {
            TextView tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
            if (tvActiveLanguage != null) {
                String savedLang = LocaleHelper.getSavedLanguage(this);
                tvActiveLanguage.setText(getLanguageShortLabel(savedLang));
            }
            btnLanguagePicker.setOnClickListener(v -> showLanguageBottomSheet());
        }

        findViewById(R.id.notificationButton).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.no_notifications), Toast.LENGTH_SHORT).show());

        setupHomeSearch();
    }

    private String getLanguageShortLabel(String langCode) {
        if (langCode == null) return "EN";
        switch (langCode.toLowerCase()) {
            case "ms": return "BM";
            case "ar": return "AR";
            case "ko": return "KO";
            case "ja": return "JA";
            case "zh": return "ZH";
            default: return "EN";
        }
    }

    /**
     * Shows a unified, luxury bottom sheet language picker with spring animations.
     */
    private void showLanguageBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_language_picker, null);
        dialog.setContentView(sheetView);

        if (sheetView.getParent() instanceof View) {
            ((View) sheetView.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String current = LocaleHelper.getSavedLanguage(this);
        if (current == null) current = LocaleHelper.LANGUAGE_ENGLISH;

        View[] items = {
                sheetView.findViewById(R.id.itemLangEnglish),
                sheetView.findViewById(R.id.itemLangMalay),
                sheetView.findViewById(R.id.itemLangArabic),
                sheetView.findViewById(R.id.itemLangKorean),
                sheetView.findViewById(R.id.itemLangJapanese),
                sheetView.findViewById(R.id.itemLangChinese)
        };

        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY,
                LocaleHelper.LANGUAGE_ARABIC,
                LocaleHelper.LANGUAGE_KOREAN,
                LocaleHelper.LANGUAGE_JAPANESE,
                LocaleHelper.LANGUAGE_CHINESE
        };

        int[] radioIds = {
                R.id.icRadioEnglish,
                R.id.icRadioMalay,
                R.id.icRadioArabic,
                R.id.icRadioKorean,
                R.id.icRadioJapanese,
                R.id.icRadioChinese
        };

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            setupLanguageItem(sheetView, items[i], radioIds[i], codes[i], current, dialog, index);
        }

        dialog.show();
    }

    private void setupLanguageItem(View sheet, View item, int radioId, String langCode, String currentLang, BottomSheetDialog dialog, int index) {
        if (item == null) return;
        ImageView radio = sheet.findViewById(radioId);

        boolean isSelected = langCode.equalsIgnoreCase(currentLang);
        if (isSelected) {
            item.setBackgroundResource(R.drawable.bg_language_item_selected);
            if (radio != null) radio.setImageResource(R.drawable.ic_check_circle_magenta);
        } else {
            item.setBackgroundResource(R.drawable.bg_language_item_normal);
            if (radio != null) radio.setImageResource(R.drawable.ic_circle_unselected);
        }

        item.setAlpha(0f);
        item.setTranslationY(32f);
        item.setScaleX(0.96f);
        item.setScaleY(0.96f);
        item.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(35L * index)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .start();

        if (isSelected && radio != null) {
            radio.setScaleX(0f);
            radio.setScaleY(0f);
            radio.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setStartDelay(35L * index + 80)
                    .setDuration(240)
                    .setInterpolator(new OvershootInterpolator(2.4f))
                    .start();
        }

        item.setOnClickListener(v -> {
            item.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(70)
                    .withEndAction(() -> {
                        item.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(90)
                                .withEndAction(() -> {
                                    dialog.dismiss();
                                    if (!langCode.equalsIgnoreCase(currentLang)) {
                                        LocaleHelper.saveLanguage(MainActivity.this, langCode);
                                        recreate();
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    private final List<UmrahPackage> heroShowcaseList = new ArrayList<>();
    private int heroShowcaseIndex = 0;
    private final android.os.Handler showcaseHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable showcaseRunnable = new Runnable() {
        @Override
        public void run() {
            advanceHeroShowcase();
            showcaseHandler.postDelayed(this, 4200);
        }
    };

    private void setupHeroShowcase() {
        View heroShowcaseCard = findViewById(R.id.heroShowcaseCard);
        if (heroShowcaseCard == null) return;

        heroShowcaseCard.setOnClickListener(v -> {
            if (!heroShowcaseList.isEmpty() && heroShowcaseIndex < heroShowcaseList.size()) {
                UmrahPackage pkg = heroShowcaseList.get(heroShowcaseIndex);
                Intent intent = new Intent(MainActivity.this, PackageDetailActivity.class);
                intent.putExtra(PackageDetailActivity.EXTRA_COLLECTION, pkg.collectionName != null ? pkg.collectionName : "umrah_packages");
                intent.putExtra(PackageDetailActivity.EXTRA_PACKAGE_ID, pkg.id);
                startActivity(intent);
            }
        });
    }

    private void startHeroShowcase() {
        showcaseHandler.removeCallbacks(showcaseRunnable);
        if (!heroShowcaseList.isEmpty()) {
            displayHeroShowcase(heroShowcaseIndex);
            showcaseHandler.postDelayed(showcaseRunnable, 4200);
        }
    }

    private void stopHeroShowcase() {
        showcaseHandler.removeCallbacks(showcaseRunnable);
    }

    private void advanceHeroShowcase() {
        if (heroShowcaseList.isEmpty()) return;
        heroShowcaseIndex = (heroShowcaseIndex + 1) % heroShowcaseList.size();
        displayHeroShowcase(heroShowcaseIndex);
    }

    private void displayHeroShowcase(int index) {
        if (heroShowcaseList.isEmpty() || index >= heroShowcaseList.size()) return;
        UmrahPackage pkg = heroShowcaseList.get(index);

        ImageView image = findViewById(R.id.heroShowcaseImage);
        TextView tag = findViewById(R.id.heroShowcaseTag);
        TextView title = findViewById(R.id.heroShowcaseTitle);
        TextView price = findViewById(R.id.heroShowcasePrice);

        if (image != null && pkg.imageUrl != null) {
            Glide.with(this)
                    .load(pkg.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(400))
                    .placeholder(R.drawable.bg_image_placeholder)
                    .into(image);
        }

        if (title != null && pkg.name != null) {
            title.setText(pkg.name);
        }

        if (tag != null) {
            String dest = (pkg.destination != null) ? pkg.destination.toUpperCase() : "MAKKAH & MADINAH";
            if (dest.length() > 20) dest = dest.substring(0, 20);
            tag.setText(dest);
        }

        if (price != null) {
            String rawPrice = (pkg.price != null && !pkg.price.isEmpty()) ? pkg.price : "7,990";
            String cleanPrice = rawPrice.replace("RM", "").replace("rm", "").trim();
            price.setText(getString(R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));
        }
    }

    private List<UmrahPackage> allPopularPackages = new ArrayList<>();
    private final List<UmrahPackage> homeSearchUmrahCache = new ArrayList<>();
    private final List<UmrahPackage> homeSearchTourCache = new ArrayList<>();
    private boolean homeSearchPackagesLoaded = false;
    private String currentCategoryFilter = "all";

    /**
     * Pakej Popular - Muat turun daripada Laravel REST API Backend (Database MySQL).
     */
    private void setupPopularPackages() {
        RecyclerView recyclerView = findViewById(R.id.popularPackagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        setupCategoryFilters(recyclerView);

        // Panggil Laravel REST API
        ApiClient.getApiService().getHomeData().enqueue(new Callback<ApiResponse<HomeDataResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomeDataResponse>> call, Response<ApiResponse<HomeDataResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().data != null) {
                    allPopularPackages = new ArrayList<>();
                    heroShowcaseList.clear();
                    HomeDataResponse homeData = response.body().data;

                    if (homeData.featured != null && !homeData.featured.isEmpty()) {
                        allPopularPackages.addAll(homeData.featured);
                        heroShowcaseList.addAll(homeData.featured);
                    }
                    if (homeData.popularUmrah != null) {
                        for (UmrahPackage p : homeData.popularUmrah) {
                            p.collectionName = "umrah_packages";
                            allPopularPackages.add(p);
                            if (heroShowcaseList.size() < 5) heroShowcaseList.add(p);
                        }
                    }
                    if (homeData.popularTour != null) {
                        for (UmrahPackage p : homeData.popularTour) {
                            p.collectionName = "tour_packages";
                            allPopularPackages.add(p);
                            if (heroShowcaseList.size() < 5) heroShowcaseList.add(p);
                        }
                    }

                    startHeroShowcase();
                    filterPopularPackages(recyclerView, currentCategoryFilter);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HomeDataResponse>> call, Throwable t) {
                // Tiada sambungan API atau pelayan luar talian
            }
        });

        findViewById(R.id.seeAllPopular).setOnClickListener(v ->
                startActivity(new Intent(this, AllPackagesActivity.class)));
    }

    private void setupCategoryFilters(RecyclerView recyclerView) {
        TextView chipAll = findViewById(R.id.chipFilterAll);
        TextView chipUmrah = findViewById(R.id.chipFilterUmrah);
        TextView chipTour = findViewById(R.id.chipFilterTour);
        TextView chipPopular = findViewById(R.id.chipFilterPopular);

        if (chipAll == null || chipUmrah == null || chipTour == null || chipPopular == null) return;

        TextView[] chips = {chipAll, chipUmrah, chipTour, chipPopular};
        String[] filterKeys = {"all", "umrah", "tour", "popular"};

        for (int i = 0; i < chips.length; i++) {
            final String key = filterKeys[i];
            final TextView clickedChip = chips[i];

            clickedChip.setOnClickListener(v -> {
                // Tactile feedback animation
                clickedChip.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction(() ->
                        clickedChip.animate().scaleX(1f).scaleY(1f).setDuration(100).start()).start();

                currentCategoryFilter = key;

                // Update chip styles
                for (TextView chip : chips) {
                    if (chip == clickedChip) {
                        chip.setBackgroundResource(R.drawable.bg_category_chip_active);
                        chip.setTextColor(android.graphics.Color.WHITE);
                    } else {
                        chip.setBackgroundResource(R.drawable.bg_category_chip_inactive);
                        chip.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.chip_inactive_text));
                    }
                }

                filterPopularPackages(recyclerView, key);
            });
        }
    }

    private void filterPopularPackages(RecyclerView recyclerView, String category) {
        if (allPopularPackages == null || allPopularPackages.isEmpty()) return;

        List<UmrahPackage> filtered = new ArrayList<>();
        if ("all".equalsIgnoreCase(category)) {
            filtered.addAll(allPopularPackages);
        } else if ("umrah".equalsIgnoreCase(category)) {
            for (UmrahPackage p : allPopularPackages) {
                String cat = (p.category != null) ? p.category.toLowerCase() : "";
                String name = (p.name != null) ? p.name.toLowerCase() : "";
                String dest = (p.destination != null) ? p.destination.toLowerCase() : "";
                if ("umrah_packages".equals(p.collectionName) || "umrah".equals(cat)
                        || name.contains("umrah") || name.contains("ziarah") || dest.contains("makkah") || dest.contains("madinah")) {
                    filtered.add(p);
                }
            }
        } else if ("tour".equalsIgnoreCase(category)) {
            for (UmrahPackage p : allPopularPackages) {
                String cat = (p.category != null) ? p.category.toLowerCase() : "";
                String name = (p.name != null) ? p.name.toLowerCase() : "";
                String dest = (p.destination != null) ? p.destination.toLowerCase() : "";
                if ("tour_packages".equals(p.collectionName) || "tour".equals(cat)
                        || (!name.contains("umrah") && !dest.contains("makkah") && !dest.contains("madinah"))) {
                    filtered.add(p);
                }
            }
        } else if ("popular".equalsIgnoreCase(category)) {
            for (UmrahPackage p : allPopularPackages) {
                if (p.isFeatured) {
                    filtered.add(p);
                }
            }
            if (filtered.isEmpty()) {
                filtered.addAll(allPopularPackages);
            }
        }

        recyclerView.setAdapter(new PackagePopularAdapter(MainActivity.this, filtered));
    }

    /**
     * Search homepage - Data pakej di-fetch SEKALI dari API bila user mula taip.
     */
    private void setupHomeSearch() {
        android.widget.EditText searchInput = findViewById(R.id.searchInputHero);
        RecyclerView resultsRecyclerView = findViewById(R.id.homeSearchResultsRecyclerView);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    resultsRecyclerView.setVisibility(View.GONE);
                    return;
                }
                if (!homeSearchPackagesLoaded) {
                    loadHomeSearchPackages(() -> filterAndShowHomeSearch(query, resultsRecyclerView));
                } else {
                    filterAndShowHomeSearch(query, resultsRecyclerView);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadHomeSearchPackages(Runnable onLoaded) {
        ApiClient.getApiService().getPackages(null, null, null, null).enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                homeSearchUmrahCache.clear();
                homeSearchTourCache.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (UmrahPackage pkg : response.body().data) {
                        if ("umrah".equalsIgnoreCase(pkg.category)) {
                            pkg.collectionName = "umrah_packages";
                            homeSearchUmrahCache.add(pkg);
                        } else {
                            pkg.collectionName = "tour_packages";
                            homeSearchTourCache.add(pkg);
                        }
                    }
                }

                homeSearchPackagesLoaded = true;
                onLoaded.run();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                homeSearchPackagesLoaded = true;
                onLoaded.run();
            }
        });
    }

    private void filterAndShowHomeSearch(String query, RecyclerView resultsRecyclerView) {
        String q = query.toLowerCase();
        List<UmrahPackage> results = new ArrayList<>();
        for (UmrahPackage pkg : homeSearchUmrahCache) {
            if (pkg.name != null && pkg.name.toLowerCase().contains(q)) results.add(pkg);
        }
        for (UmrahPackage pkg : homeSearchTourCache) {
            if (pkg.name != null && pkg.name.toLowerCase().contains(q)) results.add(pkg);
        }
        resultsRecyclerView.setVisibility(View.VISIBLE);
        resultsRecyclerView.setAdapter(new UmrahPackageAdapter(this, results, null));
    }

    private void setupTactileButton(View view, Runnable onClick) {
        if (view == null) return;
        view.setOnClickListener(v -> {
            view.animate()
                    .scaleX(0.92f)
                    .scaleY(0.92f)
                    .setDuration(70)
                    .withEndAction(() -> {
                        view.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(90)
                                .withEndAction(() -> {
                                    if (onClick != null) onClick.run();
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void setupQuickActions() {
        // Nusuk - links out to the official Nusuk app on the Play Store
        setupTactileButton(findViewById(R.id.featureNusuk), this::openNusukOnPlayStore);

        // Guideline - persediaan/checklist Umrah & Tour
        setupTactileButton(findViewById(R.id.featureGuideline), () ->
                startActivity(new Intent(this, PanduanUmrahActivity.class)));

        // Checklist - shows a picker first (Umrah / Tour), then the matching interactive checklist
        setupTactileButton(findViewById(R.id.featureChecklist), this::showChecklistCategoryPicker);

        setupTactileButton(findViewById(R.id.featureWhatsapp), this::openWhatsApp);
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, BottomNavHelper.Tab.HOME);
    }

    public void updateFavoriteBadge() {
        BottomNavHelper.updateFavoriteBadge(this);
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
     * Small picker overlay shown when tapping the Checklist quick action -
     * lets the user pick which checklist they want (Umrah or Tour) before
     * the actual interactive checklist opens.
     */
    private void showChecklistCategoryPicker() {
        String[] options = {getString(R.string.checklist_option_umrah), getString(R.string.checklist_option_tour)};

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.checklist_picker_title))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showChecklistDialog("umrah", getString(R.string.checklist_dialog_title_umrah), buildUmrahChecklistSections());
                    } else {
                        showChecklistDialog("tour", getString(R.string.checklist_dialog_title_tour), buildTourChecklistSections());
                    }
                })
                .show();
    }

    /**
     * Checklist items specific to Umrah/Haj trips - taken from the company's
     * official "Checklist Keperluan Umrah" poster (Jemaah Lelaki / Jemaah
     * Perempuan sections), plus extra practical notes folded into the
     * relevant items (e.g. bring extra towel, nail clippers go in checked
     * baggage, pharmacy prices in Makkah/Madinah are 2-3x higher).
     */
    private LinkedHashMap<String, String[]> buildUmrahChecklistSections() {
        LinkedHashMap<String, String[]> sections = new LinkedHashMap<>();
        sections.put(getString(R.string.checklist_section_travel_docs), new String[]{
                getString(R.string.checklist_umrah_docs_1),
                getString(R.string.checklist_umrah_docs_2),
                getString(R.string.checklist_umrah_docs_3),
                getString(R.string.checklist_umrah_docs_4),
                getString(R.string.checklist_umrah_docs_5),
                getString(R.string.checklist_umrah_docs_6)
        });
        sections.put(getString(R.string.checklist_section_men_clothing), new String[]{
                getString(R.string.checklist_umrah_men_1),
                getString(R.string.checklist_umrah_men_2),
                getString(R.string.checklist_umrah_men_3),
                getString(R.string.checklist_umrah_men_4),
                getString(R.string.checklist_umrah_men_5),
                getString(R.string.checklist_umrah_men_6),
                getString(R.string.checklist_umrah_men_7),
                getString(R.string.checklist_umrah_men_8),
                getString(R.string.checklist_umrah_men_9),
                getString(R.string.checklist_umrah_men_10),
                getString(R.string.checklist_umrah_men_11),
                getString(R.string.checklist_umrah_men_12),
                getString(R.string.checklist_umrah_men_13),
                getString(R.string.checklist_umrah_men_14)
        });
        sections.put(getString(R.string.checklist_section_women_clothing), new String[]{
                getString(R.string.checklist_umrah_women_1),
                getString(R.string.checklist_umrah_women_2),
                getString(R.string.checklist_umrah_women_3),
                getString(R.string.checklist_umrah_women_4),
                getString(R.string.checklist_umrah_women_5),
                getString(R.string.checklist_umrah_women_6),
                getString(R.string.checklist_umrah_women_7),
                getString(R.string.checklist_umrah_women_8),
                getString(R.string.checklist_umrah_women_9),
                getString(R.string.checklist_umrah_women_10),
                getString(R.string.checklist_umrah_women_11),
                getString(R.string.checklist_umrah_women_12),
                getString(R.string.checklist_umrah_women_13)
        });
        sections.put(getString(R.string.checklist_section_medicine), new String[]{
                getString(R.string.checklist_umrah_med_1),
                getString(R.string.checklist_umrah_med_2),
                getString(R.string.checklist_umrah_med_3),
                getString(R.string.checklist_umrah_med_4),
                getString(R.string.checklist_umrah_med_5),
                getString(R.string.checklist_umrah_med_6),
                getString(R.string.checklist_umrah_med_7),
                getString(R.string.checklist_umrah_med_8),
                getString(R.string.checklist_umrah_med_9),
                getString(R.string.checklist_umrah_med_10),
                getString(R.string.checklist_umrah_med_11),
                getString(R.string.checklist_umrah_med_12),
                getString(R.string.checklist_umrah_med_13)
        });
        sections.put(getString(R.string.checklist_section_electronics), new String[]{
                getString(R.string.checklist_umrah_elec_1),
                getString(R.string.checklist_umrah_elec_2),
                getString(R.string.checklist_umrah_elec_3),
                getString(R.string.checklist_umrah_elec_4),
                getString(R.string.checklist_umrah_elec_5)
        });
        sections.put(getString(R.string.checklist_section_women_accessories), new String[]{
                getString(R.string.checklist_umrah_acc_1),
                getString(R.string.checklist_umrah_acc_2),
                getString(R.string.checklist_umrah_acc_3)
        });
        sections.put(getString(R.string.checklist_section_food), new String[]{
                getString(R.string.checklist_umrah_food_1),
                getString(R.string.checklist_umrah_food_2),
                getString(R.string.checklist_umrah_food_3),
                getString(R.string.checklist_umrah_food_4)
        });
        sections.put(getString(R.string.checklist_section_finance), new String[]{
                getString(R.string.checklist_umrah_finance_1),
                getString(R.string.checklist_umrah_finance_2),
                getString(R.string.checklist_umrah_finance_3)
        });
        sections.put(getString(R.string.checklist_section_general), new String[]{
                getString(R.string.checklist_umrah_general_1),
                getString(R.string.checklist_umrah_general_2),
                getString(R.string.checklist_umrah_general_3)
        });
        return sections;
    }

    private LinkedHashMap<String, String[]> buildTourChecklistSections() {
        LinkedHashMap<String, String[]> sections = new LinkedHashMap<>();
        sections.put(getString(R.string.checklist_section_travel_docs), new String[]{
                getString(R.string.checklist_tour_docs_1),
                getString(R.string.checklist_tour_docs_2),
                getString(R.string.checklist_tour_docs_3),
                getString(R.string.checklist_tour_docs_4)
        });
        sections.put(getString(R.string.checklist_section_medicine), new String[]{
                getString(R.string.checklist_tour_med_1),
                getString(R.string.checklist_tour_med_2),
                getString(R.string.checklist_tour_med_3),
                getString(R.string.checklist_tour_med_4),
                getString(R.string.checklist_tour_med_5),
                getString(R.string.checklist_tour_med_6),
                getString(R.string.checklist_tour_med_7),
                getString(R.string.checklist_tour_med_8)
        });
        sections.put(getString(R.string.checklist_section_clothing), new String[]{
                getString(R.string.checklist_tour_clothing_1),
                getString(R.string.checklist_tour_clothing_2),
                getString(R.string.checklist_tour_clothing_3),
                getString(R.string.checklist_tour_clothing_4),
                getString(R.string.checklist_tour_clothing_5)
        });
        sections.put(getString(R.string.checklist_section_electronics), new String[]{
                getString(R.string.checklist_tour_elec_1),
                getString(R.string.checklist_tour_elec_2)
        });
        sections.put(getString(R.string.checklist_section_finance), new String[]{
                getString(R.string.checklist_tour_finance_1),
                getString(R.string.checklist_tour_finance_2)
        });
        return sections;
    }

    /**
     * Renders the interactive tickable checklist dialog for a given category.
     * Each category uses its own SharedPreferences file (checklist_state_umrah
     * / checklist_state_tour) so ticking an item in one checklist never
     * affects the other, even if item text happens to be similar.
     */
    /**
     * Renders the interactive tickable checklist dialog for a given category.
     * Each category uses its own SharedPreferences file (checklist_state_umrah
     * / checklist_state_tour) so ticking an item in one checklist never
     * affects the other, even if item text happens to be similar.
     */
    private void showChecklistDialog(String category, String dialogTitle, LinkedHashMap<String, String[]> sections) {
        SharedPreferences checklistPrefs = getSharedPreferences("checklist_state_" + category, Context.MODE_PRIVATE);

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
            // Header dikekalkan warna pink / boleh tukar ke hitam jika mahu
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

                // --- PERUBAHAN DI SINI ---
                // Tetapkan warna teks menjadi HITAM PEKAT (#000000)
                checkBox.setTextColor(android.graphics.Color.BLACK);
                // Tetapkan warna kotak semakan (checkbox) juga ke warna hitam pekat
                checkBox.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK));
                // --------------------------

                checkBox.setChecked(checklistPrefs.getBoolean(itemText, false));
                checkBox.setOnCheckedChangeListener((btn, isChecked) ->
                        checklistPrefs.edit().putBoolean(itemText, isChecked).apply());
                content.addView(checkBox);
            }
        }

        // Membina Dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(dialogTitle)
                .setView(scrollView)
                .setPositiveButton(getString(R.string.dialog_close), null)
                .create();

        // Pastikan butang "Close" / "Tutup" juga berwarna hitam pekat
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.BLACK);
        });

        dialog.show();
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
     * Uses the unified BottomSheet language picker.
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
                                showLanguageBottomSheet();
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

    /**
     * Register CTA is only useful for guests (logged-in users are already
     * registered), so it's hidden entirely once isLoggedIn is true. For
     * guests, the Sign Up button now actually navigates to SignUpActivity
     * instead of showing a "coming soon" toast.
     */
    private void setupRegisterCta() {
        View ctaCard = findViewById(R.id.registerCtaCard);

        if (isLoggedIn) {
            ctaCard.setVisibility(View.GONE);
            return;
        }

        ctaCard.setVisibility(View.VISIBLE);

        findViewById(R.id.registerCtaButton).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));

        findViewById(R.id.registerCtaDismiss).setOnClickListener(v ->
                ctaCard.setVisibility(View.GONE));
    }

    /**
     * Podcast section (carousel) - "Podcast Jumaat" series from the JELAJAH HAFIZ
     * YouTube channel. Tapping a card opens the video (YouTube app if installed,
     * browser otherwise). "Lihat Semua" opens the full channel page.
     * TODO: replace this hardcoded list with a real API/RSS feed call once
     * the channel uploads more regularly, so new episodes show automatically.
     */
    private void setupPodcastSection() {
        List<Podcast> podcasts = new ArrayList<>();
        podcasts.add(new Podcast("Podcast Jumaat - Ustazah Hjh. Zalina", "_w1WTK3E2_w"));
        podcasts.add(new Podcast("Podcast Jumaat - Almarhum Tn. Zaidee", "iBwVgdg1obc"));

        RecyclerView recyclerView = findViewById(R.id.podcastRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new PodcastAdapter(podcasts));

        View seeAllPodcasts = findViewById(R.id.seeAllPodcasts);
        if (seeAllPodcasts != null) {
            seeAllPodcasts.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_CHANNEL_URL)));
                } catch (Exception e) {
                    Toast.makeText(this, "Tidak dapat membuka YouTube", Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            item.setBackgroundResource(R.drawable.bg_prayer_compact_card);
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

    // ================== WAKTU SOLAT ==================
    // PRIMARY SOURCE: Malaysia Waktu Solat API (mptwaktusolat), which mirrors
    // JAKIM e-solat data directly - this is what actually matches Google/
    // MuslimPro for Malaysian users, since none of them hand-calculate either.
    // FALLBACK: Adhan library (offline / API unreachable) - clearly labelled
    // "(anggaran)" so nobody mistakes an estimate for the official JAKIM time.

    /**
     * "Daily use" hook so people open the app even when they're not booking.
     *
     * REQUIRED before this compiles/runs:
     * 1) Add to app/build.gradle.kts:  implementation("com.batoulapps.adhan:adhan:1.2.1")
     * 2) Add to AndroidManifest.xml (outside <application>):
     *      <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
     *      <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
     *      <uses-permission android:name="android.permission.INTERNET" />
     *    (INTERNET is very likely already present since Firebase needs it too,
     *    but double-check - without it the JAKIM API call will silently fail
     *    and every request will fall back to the Adhan estimate.)
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
        Location location = getBestLastKnownLocation();
        double lat = 1.4927;   // fallback: Johor Bahru (company's own city)
        double lon = 103.7414;

        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
        }

        fetchPrayerTimesFromJakimApi(lat, lon);
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

    /**
     * Calls the Malaysia Waktu Solat API (JAKIM-sourced) on a background
     * thread. On any failure (no internet, bad response, parse error) it
     * falls back to the Adhan library estimate instead of leaving the
     * widget blank.
     */
    private void fetchPrayerTimesFromJakimApi(double lat, double lon) {
        networkExecutor.execute(() -> {
            try {
                String body = httpGet(JAKIM_API_BASE + lat + "/" + lon);
                handleSolatV2Response(body);
            } catch (Exception gpsBetaFailed) {
                android.util.Log.w("PrayerTimesAPI", "GPS-beta endpoint failed, trying zone lookup", gpsBetaFailed);
                try {
                    String zone = resolveZoneFromGps(lat, lon);
                    String body = httpGet(ZONE_SOLAT_URL + zone);
                    handleSolatV2Response(body);
                } catch (Exception zoneFailed) {
                    android.util.Log.e("PrayerTimesAPI", "Zone-based lookup also failed, falling back to Adhan estimate", zoneFailed);
                    runOnUiThread(() -> showFallbackCalculatedPrayerTimes(lat, lon));
                }
            }
        });
    }

    /** Resolves GPS coordinates to a JAKIM zone code (e.g. "JHR01") via the stable /zones/gps endpoint. */
    private String resolveZoneFromGps(double lat, double lon) throws Exception {
        String url = ZONE_LOOKUP_URL + "?lat=" + lat + "&long=" + lon;
        String body = httpGet(url);
        JSONObject json = new JSONObject(body);
        String zone = json.optString("zone", "");
        if (zone.isEmpty()) {
            throw new RuntimeException("No zone returned for GPS coordinates");
        }
        return zone;
    }

    /** Shared plain HTTP GET helper - throws on any non-200 response or network error. */
    private String httpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP " + responseCode + " for " + urlString);
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line);
        }
        reader.close();
        connection.disconnect();
        return responseBuilder.toString();
    }

    /** Parses a SolatV2 JSON response (same shape for both the GPS-beta and zone endpoints) and renders it. */
    /** Parses a SolatV2 JSON response (same shape for both the GPS-beta and zone endpoints) and renders it. */
    private void handleSolatV2Response(String responseBody) throws Exception {
        JSONObject json = new JSONObject(responseBody);
        String zone = json.optString("zone", "");
        JSONArray prayersArray = json.getJSONArray("prayers");

        int todayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        JSONObject todayPrayers = null;
        for (int i = 0; i < prayersArray.length(); i++) {
            JSONObject entry = prayersArray.getJSONObject(i);
            if (entry.getInt("day") == todayOfMonth) {
                todayPrayers = entry;
                break;
            }
        }

        if (todayPrayers == null) {
            throw new RuntimeException("No entry for today in API response");
        }

        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());

        JSONObject finalTodayPrayers = todayPrayers;
        String[] names = {
                getString(R.string.prayer_subuh), getString(R.string.prayer_zohor),
                getString(R.string.prayer_asar), getString(R.string.prayer_maghrib),
                getString(R.string.prayer_isyak)
        };
        long[] epochs = {
                todayPrayers.getLong("fajr"), todayPrayers.getLong("dhuhr"),
                todayPrayers.getLong("asr"), todayPrayers.getLong("maghrib"),
                todayPrayers.getLong("isha")
        };

        // TAMBAHAN BARU: schedule alarm azan untuk setiap waktu solat hari ini
        PrayerTimeScheduler.scheduleAll(this,
                finalTodayPrayers.getLong("fajr"),
                finalTodayPrayers.getLong("dhuhr"),
                finalTodayPrayers.getLong("asr"),
                finalTodayPrayers.getLong("maghrib"),
                finalTodayPrayers.getLong("isha"));

        String finalZone = zone;
        runOnUiThread(() -> {
            TextView dateText = findViewById(R.id.prayerTimesDateText);
            dateText.setText(finalZone.isEmpty()
                    ? getString(R.string.prayer_times_title_jakim)
                    : getString(R.string.prayer_times_title_jakim_zone, finalZone));
            dateText.setTextSize(20f);
            renderPrayerArc(names, epochs);
        });
    }

    private String formatEpochSeconds(long epochSeconds, SimpleDateFormat formatter) {
        return formatter.format(new Date(epochSeconds * 1000L));
    }

    /**
     * OFFLINE FALLBACK ONLY - used when the JAKIM-sourced API call fails.
     * These are calculated estimates (Adhan library, SINGAPORE method), NOT
     * the official JAKIM times, and are labelled "(anggaran)" so that's clear
     * to the user. Do not treat this path as equally authoritative.
     */
    /**
     * OFFLINE FALLBACK ONLY - used when the JAKIM-sourced API call fails.
     * These are calculated estimates (Adhan library, SINGAPORE method), NOT
     * the official JAKIM times, and are labelled "(anggaran)" so that's clear
     * to the user. Do not treat this path as equally authoritative.
     */
    private void showFallbackCalculatedPrayerTimes(double latitude, double longitude) {
        TextView dateText = findViewById(R.id.prayerTimesDateText);
        dateText.setText(getString(R.string.prayer_times_title_fallback));
        dateText.setTextColor(getResources().getColor(R.color.prayer_card_text_secondary));
        dateText.setTextSize(20f);

        Calendar today = Calendar.getInstance();
        DateComponents dateComponents = new DateComponents(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));

        Coordinates coordinates = new Coordinates(latitude, longitude);
        CalculationParameters params = CalculationMethod.SINGAPORE.getParameters();
        params.madhab = Madhab.SHAFI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, params);

        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());

        String[] names = {
                getString(R.string.prayer_subuh), getString(R.string.prayer_zohor),
                getString(R.string.prayer_asar), getString(R.string.prayer_maghrib),
                getString(R.string.prayer_isyak)
        };
        long[] epochs = {
                prayerTimes.fajr.getTime() / 1000L, prayerTimes.dhuhr.getTime() / 1000L,
                prayerTimes.asr.getTime() / 1000L, prayerTimes.maghrib.getTime() / 1000L,
                prayerTimes.isha.getTime() / 1000L
        };

        // TAMBAHAN BARU: schedule alarm azan guna waktu anggaran (epoch seconds = milliseconds / 1000)
        PrayerTimeScheduler.scheduleAll(this,
                prayerTimes.fajr.getTime() / 1000,
                prayerTimes.dhuhr.getTime() / 1000,
                prayerTimes.asr.getTime() / 1000,
                prayerTimes.maghrib.getTime() / 1000,
                prayerTimes.isha.getTime() / 1000);

        renderPrayerArc(names, epochs);
    }

    // renderPrayerArc — dikemaskini untuk masa 16sp bold + label lokasi/Hijrah placeholder
    private void renderPrayerArc(String[] names, long[] epochSeconds) {
        PrayerArcView arcView = findViewById(R.id.prayerArcView);
        TextView currentLabel = findViewById(R.id.prayerCurrentLabel);
        TextView nextLabel = findViewById(R.id.prayerNextLabel);
        TextView locationLabel = findViewById(R.id.prayerLocationLabel);
        TextView hijriLabel = findViewById(R.id.prayerHijriLabel);

        // TODO: gantikan dengan data lokasi & Hijrah sebenar bila sedia
        locationLabel.setText("Larkin, Johor Bahru");
        hijriLabel.setText("1 Rejab 1448H");
        locationLabel.setTextSize(16f);
        hijriLabel.setTextSize(20f);

        long nowEpoch = System.currentTimeMillis() / 1000L;
        PrayerProgressCalculator.Result result =
                PrayerProgressCalculator.calculate(names, epochSeconds, nowEpoch);

        arcView.setProgress(result.progress);

        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());

        String currentTime = formatter.format(new Date(result.currentEpochSeconds * 1000L));
        String nextTime = formatter.format(new Date(result.nextEpochSeconds * 1000L));

        currentLabel.setText(buildLabelSpanned(result.currentName, currentTime));
        nextLabel.setText(buildLabelSpanned(result.nextName, nextTime));
    }

    private void startArcAutoRefresh() {
        arcRefreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        arcRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshArcFromCache();
                arcRefreshHandler.postDelayed(this, 60_000L); // refresh setiap 60 saat
            }
        };
        arcRefreshHandler.post(arcRefreshRunnable);
    }

    private void stopArcAutoRefresh() {
        if (arcRefreshHandler != null && arcRefreshRunnable != null) {
            arcRefreshHandler.removeCallbacks(arcRefreshRunnable);
        }
    }

    private void refreshArcFromCache() {
        long[] epochs = PrayerTimeScheduler.getCachedEpochs(this);
        if (epochs == null) return; // takde cache lagi, skip

        String[] names = {
                getString(R.string.prayer_subuh), getString(R.string.prayer_zohor),
                getString(R.string.prayer_asar), getString(R.string.prayer_maghrib),
                getString(R.string.prayer_isyak)
        };

        renderPrayerArc(names, epochs);
    }

    private CharSequence buildLabelSpanned(String name, String time) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append(name).append("\n");
        int start = sb.length();
        sb.append(time);
        sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new AbsoluteSizeSpan(16, true), start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    /**
     * Permission denied - show a retry button instead of silently failing or
     * making up numbers. Tapping it re-requests permission.
     */
    private void showPrayerTimesLocationDenied() {
        TextView dateText = findViewById(R.id.prayerTimesDateText);
        PrayerArcView arcView = findViewById(R.id.prayerArcView);
        TextView currentLabel = findViewById(R.id.prayerCurrentLabel);
        TextView nextLabel = findViewById(R.id.prayerNextLabel);

        dateText.setText(getString(R.string.prayer_location_denied_text));
        dateText.setTextColor(getResources().getColor(R.color.prayer_card_text_secondary));
        dateText.setTextSize(16f);

        // Kosongkan arc & label sebab takde data waktu solat
        arcView.setProgress(0f);
        currentLabel.setText("");

        nextLabel.setText(getString(R.string.prayer_use_my_location));
        nextLabel.setTextColor(getResources().getColor(R.color.prayer_card_text_primary));
        nextLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        nextLabel.setClickable(true);
        nextLabel.setFocusable(true);
        nextLabel.setOnClickListener(v2 ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    // ================== end waktu solat ==================

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}