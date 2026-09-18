package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.textfield.TextInputEditText;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import com.hafiztraveltours.app.network.HomeDataResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {

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
    private String currentResolvedLocationName = "Johor Bahru";

    // M8 single-flight handles for identical Laravel requests.
    private retrofit2.Call<?> homeCall;
    private retrofit2.Call<?> homeSearchCall;

    
    @Override
    protected void onResume() {
        super.onResume();
        String currentSaved = LocaleHelper.getSavedLanguage(this);
        if (activeLanguage != null && !activeLanguage.equals(currentSaved)) {
            recreate();
            return;
        }
        activeLanguage = currentSaved;
        loadSessionState();
        setupHeroSection();
        startArcAutoRefresh();
        startHeroShowcase();
        updateFavoriteBadge();

        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (hasFineLocation || hasCoarseLocation) {
            loadPrayerTimesForCurrentLocation();
        }
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
        setupInfoSection();
        setupPodcastSection();
        setupPrayerTimesWidget();
        setupSwipeRefresh();
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

    private void setupSwipeRefresh() {
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.brand_magenta, R.color.gold_accent, R.color.brand_dark_pink);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadSessionState();
                setupHeroSection();
                setupPopularPackages();
                loadPrayerTimesForCurrentLocation();
                updateFavoriteBadge();
                swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
            });
        }
    }



    @Override
    protected void onDestroy() {
        stopHeroShowcase();
        stopArcAutoRefresh();
        if (homeCall != null) homeCall.cancel();
        if (homeSearchCall != null) homeSearchCall.cancel();
        showcaseHandler.removeCallbacksAndMessages(null);
        if (arcRefreshHandler != null) {
            arcRefreshHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
        networkExecutor.shutdown();
    }

    /**
     * Checks the MySQL / REST API session from SessionManager.
     * If a user is signed in, show their real name.
     */
    private void loadSessionState() {
        SessionManager session = SessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            isLoggedIn = true;
            loggedInUserName = session.getUserNickname();
        } else {
            isLoggedIn = false;
            loggedInUserName = getString(R.string.default_user_name);
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
            greetingText.setText(getString(R.string.user_greeting));
            heroSubtitle.setText(loggedInUserName != null && !loggedInUserName.isEmpty() ? loggedInUserName : getString(R.string.hero_subtitle_user));
            heroHeadline.setText(getString(R.string.hero_headline_user));
        } else {
            greetingText.setText(getString(R.string.guest_greeting));
            heroSubtitle.setText(getString(R.string.hero_subtitle_guest));
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

        View notificationButton = findViewById(R.id.notificationButton);
        View notificationDot = findViewById(R.id.viewNotificationDot);
        if (notificationDot != null) {
            boolean hasUnread = getSharedPreferences("app_prefs", MODE_PRIVATE).getBoolean("has_unread_notifications", false);
            notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> {
                com.hafiztraveltours.app.utils.HapticUtil.tap(v);
                if (notificationDot != null) {
                    notificationDot.setVisibility(View.GONE);
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putBoolean("has_unread_notifications", false).apply();
                }
                Toast.makeText(this, getString(R.string.no_notifications), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String getLanguageShortLabel(String langCode) {
        return LocaleHelper.getLanguageBadge(langCode);
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
                sheetView.findViewById(R.id.itemLangMalay)
        };

        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY
        };

        int[] radioIds = {
                R.id.icRadioEnglish,
                R.id.icRadioMalay
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
            com.hafiztraveltours.app.utils.HapticUtil.click(v);
            item.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        item.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(120)
                                .setInterpolator(new OvershootInterpolator(1.8f))
                                .withEndAction(() -> {
                                    dialog.dismiss();
                                    if (!langCode.equalsIgnoreCase(currentLang)) {
                                        LocaleHelper.applyAndSaveLanguage(MainActivity.this, langCode);
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

        setupTactileButton(heroShowcaseCard, () -> {
            if (!heroShowcaseList.isEmpty() && heroShowcaseIndex >= 0 && heroShowcaseIndex < heroShowcaseList.size()) {
                UmrahPackage pkg = heroShowcaseList.get(heroShowcaseIndex);
                String collection = (pkg.collectionName != null && !pkg.collectionName.trim().isEmpty())
                        ? pkg.collectionName
                        : (pkg.isUmrah() ? "umrah_packages" : "tour_packages");
                com.hafiztraveltours.app.utils.Navigator.openPackage(MainActivity.this, collection, pkg.id);
            } else {
                startActivity(new Intent(MainActivity.this, AllPackagesActivity.class));
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
        if (isFinishing() || isDestroyed()) return;
        if (heroShowcaseList.isEmpty()) return;
        heroShowcaseIndex = (heroShowcaseIndex + 1) % heroShowcaseList.size();
        displayHeroShowcase(heroShowcaseIndex);
    }

    private void displayHeroShowcase(int index) {
        if (isFinishing() || isDestroyed()) return;
        if (heroShowcaseList.isEmpty() || index >= heroShowcaseList.size()) return;
        UmrahPackage pkg = heroShowcaseList.get(index);

        ImageView image = findViewById(R.id.heroShowcaseImage);
        TextView tag = findViewById(R.id.heroShowcaseTag);
        TextView title = findViewById(R.id.heroShowcaseTitle);
        TextView price = findViewById(R.id.heroShowcasePrice);

        if (image != null) {
            try {
                if (pkg.imageUrl != null && !pkg.imageUrl.trim().isEmpty()) {
                    Glide.with(this)
                            .load(pkg.imageUrl)
                            .transition(DrawableTransitionOptions.withCrossFade(400))
                            .placeholder(R.drawable.bg_image_placeholder)
                            .error(R.drawable.bg_image_placeholder)
                            .into(image);
                } else {
                    Glide.with(this)
                            .load(R.drawable.bg_image_placeholder)
                            .into(image);
                }
            } catch (Exception ignored) {}
        }

        if (title != null) {
            title.setText(pkg.getDisplayName());
        }

        if (tag != null) {
            String dest = (pkg.destination != null && !pkg.destination.trim().isEmpty())
                    ? pkg.destination.toUpperCase()
                    : (pkg.isUmrah() ? "UMRAH" : "PELANCONGAN");
            if (dest.length() > 20) dest = dest.substring(0, 20);
            tag.setText(dest);
        }

        if (price != null) {
            String rawPrice = (pkg.price != null && !pkg.price.isEmpty()) ? pkg.price : "";
            String cleanPrice = com.hafiztraveltours.app.utils.MoneyFormat.numericString(rawPrice);
            if (!cleanPrice.isEmpty()) {
                price.setText(getString(R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));
            } else if (pkg.durationDays > 0) {
                price.setText(getString(R.string.duration_days_nights_format, pkg.durationDays, pkg.nightsCount));
            } else {
                price.setText(getString(R.string.label_contact_us));
            }
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

        // Panggil Laravel REST API (M8: cancel previous identical request first).
        if (homeCall != null) homeCall.cancel();
        Call<ApiResponse<HomeDataResponse>> homeRequest =
                ApiClient.getApiService().getHomeData();
        homeCall = homeRequest;
        homeRequest.enqueue(new Callback<ApiResponse<HomeDataResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<HomeDataResponse>> call, Response<ApiResponse<HomeDataResponse>> response) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.homePopularShimmer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
                recyclerView.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().data != null) {
                    allPopularPackages = new ArrayList<>();
                    heroShowcaseList.clear();
                    HomeDataResponse homeData = response.body().data;

                    if (homeData.featured != null && !homeData.featured.isEmpty()) {
                        for (UmrahPackage p : homeData.featured) {
                            if (p.collectionName == null || p.collectionName.trim().isEmpty()) {
                                p.collectionName = p.isUmrah() ? "umrah_packages" : "tour_packages";
                            }
                            allPopularPackages.add(p);
                            heroShowcaseList.add(p);
                        }
                    }
                    if (homeData.popularUmrah != null) {
                        for (UmrahPackage p : homeData.popularUmrah) {
                            p.collectionName = "umrah_packages";
                            allPopularPackages.add(p);
                        }
                    }
                    if (homeData.popularTour != null) {
                        for (UmrahPackage p : homeData.popularTour) {
                            p.collectionName = "tour_packages";
                            allPopularPackages.add(p);
                        }
                    }

                    View heroShowcaseCard = findViewById(R.id.heroShowcaseCard);
                    if (!heroShowcaseList.isEmpty()) {
                        if (heroShowcaseCard != null) heroShowcaseCard.setVisibility(View.VISIBLE);
                        startHeroShowcase();
                    } else {
                        if (heroShowcaseCard != null) heroShowcaseCard.setVisibility(View.GONE);
                        stopHeroShowcase();
                    }

                    recyclerView.setAdapter(new PackageCardAdapter(MainActivity.this, allPopularPackages));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HomeDataResponse>> call, Throwable t) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.homePopularShimmer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
                recyclerView.setVisibility(View.VISIBLE);
                View heroShowcaseCard = findViewById(R.id.heroShowcaseCard);
                if (heroShowcaseCard != null) heroShowcaseCard.setVisibility(View.GONE);
                stopHeroShowcase();
            }
        });

        findViewById(R.id.seeAllPopular).setOnClickListener(v ->
                startActivity(new Intent(this, AllPackagesActivity.class)));
    }

    /**
     * Search homepage - Tapping search opens the complete All Packages search & filter experience.
     */


    private void loadHomeSearchPackages(Runnable onLoaded) {
        if (homeSearchCall != null) homeSearchCall.cancel();
        Call<ApiResponse<List<UmrahPackage>>> searchRequest =
                ApiClient.getApiService().getPackages(null, null, null, null);
        homeSearchCall = searchRequest;
        searchRequest.enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                homeSearchUmrahCache.clear();
                homeSearchTourCache.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (UmrahPackage pkg : response.body().data) {
                        if (pkg.isUmrah()) {
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
            resultsRecyclerView.setAdapter(new PackageCardAdapter(this, results, PackageCardAdapter.CardStyle.LIST, null));
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
        String message = getString(R.string.guideline_dialog_message);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.guideline_dialog_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_close), null)
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
            Toast.makeText(this, getString(R.string.error_whatsapp_not_found), Toast.LENGTH_SHORT).show();
        }
    }



    private void openCategoryPage(String title, String url) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.EXTRA_TITLE, title);
        intent.putExtra(WebViewActivity.EXTRA_URL, url);
        startActivity(intent);
    }

    /**
     * Google Review + Register CTA + Help & Support - shown to EVERYONE (guest or logged in).
     * Main purpose of this section is lead capture (register), building trust
     * (reviews), and easy access to customer care / consultant inquiry.
     */
    private void setupInfoSection() {
        setupGoogleReview();
        setupRegisterCta();
        setupHelpSupport();
    }

    private void setupGoogleReview() {
        findViewById(R.id.googleReviewRatingRow).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_REVIEW_URL)));
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.err_open_review), Toast.LENGTH_SHORT).show();
            }
        });

        setupReviewSnippets();
    }

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
                    Toast.makeText(this, getString(R.string.err_open_youtube), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupHelpSupport() {
        View btnContactUs = findViewById(R.id.btnContactUs);
        if (btnContactUs != null) {
            setupTactileButton(btnContactUs, () ->
                    startActivity(new Intent(MainActivity.this, HubungiKamiActivity.class)));
        }

        View btnHelpWhatsapp = findViewById(R.id.btnHelpWhatsapp);
        if (btnHelpWhatsapp != null) {
            setupTactileButton(btnHelpWhatsapp, this::openWhatsApp);
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
        View btnRefresh = findViewById(R.id.btnRefreshPrayerLocation);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> refreshPrayerTimesLocation(true));
        }
        View ivRefresh = findViewById(R.id.ivRefreshPrayerLocation);
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> refreshPrayerTimesLocation(true));
        }

        View btnQibla = findViewById(R.id.btnQiblaAction);
        if (btnQibla != null) {
            btnQibla.setOnClickListener(v ->
                    startActivity(new Intent(this, QiblaActivity.class)));
        }

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

    private void refreshPrayerTimesLocation(boolean userInitiated) {
        ImageView ivRefresh = findViewById(R.id.ivRefreshPrayerLocation);
        if (ivRefresh != null) {
            ivRefresh.clearAnimation();
            android.view.animation.RotateAnimation rotate = new android.view.animation.RotateAnimation(
                    0, 360,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setDuration(700);
            rotate.setRepeatCount(1);
            ivRefresh.startAnimation(rotate);
        }

        if (userInitiated) {
            Toast.makeText(this, R.string.prayer_updating_location, Toast.LENGTH_SHORT).show();
        }

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

        resolveLocationName(lat, lon);
        fetchPrayerTimesFromJakimApi(lat, lon);
        requestFreshLocation();
    }

    private void requestFreshLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        boolean hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!hasFineLocation && !hasCoarseLocation) return;

        try {
            LocationListener singleLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location freshLocation) {
                    if (freshLocation != null) {
                        double lat = freshLocation.getLatitude();
                        double lon = freshLocation.getLongitude();
                        resolveLocationName(lat, lon);
                        fetchPrayerTimesFromJakimApi(lat, lon);
                    }
                    try {
                        locationManager.removeUpdates(this);
                    } catch (Exception ignored) {}
                }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            };

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleLocationListener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleLocationListener, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {
        } catch (Exception e) {
            android.util.Log.w("Location", "requestSingleUpdate failed", e);
        }
    }

    private void resolveLocationName(double lat, double lon) {
        if (networkExecutor.isShutdown() || isFinishing() || isDestroyed()) return;
        try {
            networkExecutor.execute(() -> {
                String locName = null;
                try {
                    Locale activeLocale = LocaleHelper.getCurrentLocale(this);
                    Geocoder geocoder = new Geocoder(this, activeLocale);
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String locality = addr.getLocality();
                        String subAdmin = addr.getSubAdminArea();
                        String admin = addr.getAdminArea();

                        if (locality != null && !locality.trim().isEmpty()) {
                            locName = locality.trim();
                        } else if (subAdmin != null && !subAdmin.trim().isEmpty()) {
                            locName = subAdmin.trim();
                        } else if (admin != null && !admin.trim().isEmpty()) {
                            locName = admin.trim();
                        }

                        if (locName != null) {
                            if (locName.contains(",")) {
                                locName = locName.split(",")[0].trim();
                            }
                            if (locName.toLowerCase().startsWith("daerah ")) {
                                locName = locName.substring(7).trim();
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (locName != null && !locName.isEmpty()) {
                    currentResolvedLocationName = locName;
                    runOnUiThread(() -> {
                        TextView locationLabel = findViewById(R.id.prayerLocationLabel);
                        if (locationLabel != null) {
                            locationLabel.setText(currentResolvedLocationName);
                        }
                    });
                }
            });
        } catch (Exception ignored) {}
    }

    /**
     * Attempts to fetch official JAKIM prayer times on the background network
     * thread. On any failure (no internet, bad response, parse error) it
     * falls back to the Adhan library estimate instead of leaving the
     * widget blank.
     */
    private void fetchPrayerTimesFromJakimApi(double lat, double lon) {
        if (networkExecutor.isShutdown() || isFinishing() || isDestroyed()) return;
        try {
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
        } catch (Exception ignored) {}
    }

    private String getDynamicHijriDate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                android.icu.util.IslamicCalendar islamicCalendar = new android.icu.util.IslamicCalendar();
                int day = islamicCalendar.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH);
                int month = islamicCalendar.get(android.icu.util.IslamicCalendar.MONTH);
                int year = islamicCalendar.get(android.icu.util.IslamicCalendar.YEAR);

                String lang = LocaleHelper.getSavedLanguage(this);
                String[] hijriMonths;
                if (LocaleHelper.LANGUAGE_MALAY.equalsIgnoreCase(lang)) {
                    hijriMonths = new String[]{
                            "Muharram", "Safar", "Rabiulawal", "Rabiulakhir",
                            "Jamadilawal", "Jamadilakhir", "Rejab", "Syaaban",
                            "Ramadhan", "Syawal", "Zulkaedah", "Zulhijjah"
                    };
                    return day + " " + (month >= 0 && month < hijriMonths.length ? hijriMonths[month] : "") + " " + year + "H";
                } else {
                    hijriMonths = new String[]{
                            "Muharram", "Safar", "Rabi\u2019 al-Awwal", "Rabi\u2019 al-Thani",
                            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha\u2019ban",
                            "Ramadan", "Shawwal", "Dhu al-Qa\u2019dah", "Dhu al-Hijjah"
                    };
                    return day + " " + (month >= 0 && month < hijriMonths.length ? hijriMonths[month] : "") + " " + year + "H";
                }
            } catch (Exception e) {
                return getString(R.string.hijri_date_fallback);
            }
        }
        return getString(R.string.hijri_date_fallback);
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
            dateText.setTextSize(15f);
            renderPrayerArc(names, epochs);
        });
    }

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
        dateText.setTextSize(15f);

        Calendar today = Calendar.getInstance();
        DateComponents dateComponents = new DateComponents(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));

        Coordinates coordinates = new Coordinates(latitude, longitude);
        CalculationParameters params = CalculationMethod.SINGAPORE.getParameters();
        params.madhab = Madhab.SHAFI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, params);

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

    private String getLocalizedPrayerName(String rawName) {
        if (rawName == null) return "";
        String lower = rawName.toLowerCase();
        if (lower.contains("subuh") || lower.contains("fajr")) {
            return getString(R.string.prayer_name_subuh);
        } else if (lower.contains("syuruk") || lower.contains("sunrise")) {
            return getString(R.string.prayer_name_syuruk);
        } else if (lower.contains("zohor") || lower.contains("dhuhr") || lower.contains("zuhr")) {
            return getString(R.string.prayer_name_zohor);
        } else if (lower.contains("asar") || lower.contains("asr")) {
            return getString(R.string.prayer_name_asar);
        } else if (lower.contains("maghrib")) {
            return getString(R.string.prayer_name_maghrib);
        } else if (lower.contains("isyak") || lower.contains("isha")) {
            return getString(R.string.prayer_name_isyak);
        }
        return rawName;
    }

    private void renderPrayerArc(String[] names, long[] epochSeconds) {
        TextView currentLabel = findViewById(R.id.prayerCurrentLabel);
        TextView nextLabel = findViewById(R.id.prayerNextLabel);
        TextView locationLabel = findViewById(R.id.prayerLocationLabel);
        TextView hijriLabel = findViewById(R.id.prayerHijriLabel);
        TextView countdownText = findViewById(R.id.prayerCountdownText);

        if (locationLabel != null) {
            locationLabel.setText(currentResolvedLocationName);
            locationLabel.setTextSize(12f);
        }
        if (hijriLabel != null) {
            hijriLabel.setText(getDynamicHijriDate());
            hijriLabel.setTextSize(12f);
        }

        long nowEpoch = System.currentTimeMillis() / 1000L;
        PrayerProgressCalculator.Result result =
                PrayerProgressCalculator.calculate(names, epochSeconds, nowEpoch);

        java.util.Locale appLocale = LocaleHelper.getCurrentLocale(this);
        String currentTime = com.hafiztraveltours.app.utils.DateFormats.formatClockTime(
                new Date(result.currentEpochSeconds * 1000L), appLocale);
        String nextTime = com.hafiztraveltours.app.utils.DateFormats.formatClockTime(
                new Date(result.nextEpochSeconds * 1000L), appLocale);

        String localizedCurrentName = getLocalizedPrayerName(result.currentName);
        String localizedNextName = getLocalizedPrayerName(result.nextName);

        if (nextLabel != null) {
            nextLabel.setText(localizedNextName.toUpperCase(Locale.getDefault()));
        }
        if (currentLabel != null) {
            currentLabel.setText(nextTime);
        }

        // Update Live Countdown Timer
        if (countdownText != null) {
            long remainingSec = result.nextEpochSeconds - nowEpoch;
            if (remainingSec <= 0) {
                countdownText.setText(getString(R.string.prayer_countdown_entered));
            } else if (remainingSec < 3600) {
                long mins = Math.max(1, remainingSec / 60);
                countdownText.setText(getString(R.string.prayer_countdown_min, localizedNextName, (int) mins));
            } else {
                long hours = remainingSec / 3600;
                long mins = (remainingSec % 3600) / 60;
                countdownText.setText(getString(R.string.prayer_countdown_hour_min, localizedNextName, (int) hours, (int) mins));
            }
        }

        // Update Dynamic Prayer Progression Timeline View & Labels
        PrayerProgressTimelineView timelineView = findViewById(R.id.prayerProgressTimelineView);
        if (timelineView != null && epochSeconds != null && epochSeconds.length >= 5) {
            timelineView.setPrayerData(names, epochSeconds, nowEpoch);
        }

        // Update 5 Daily Prayer Timeline Labels
        if (epochSeconds != null && epochSeconds.length >= 5) {
            LinearLayout pillSubuh = findViewById(R.id.pillSubuh);
            TextView tvNameSubuh = findViewById(R.id.tvNameSubuh);
            TextView tvTimeSubuh = findViewById(R.id.tvTimeSubuh);
            View dotSubuh = findViewById(R.id.dotSubuh);

            LinearLayout pillZohor = findViewById(R.id.pillZohor);
            TextView tvNameZohor = findViewById(R.id.tvNameZohor);
            TextView tvTimeZohor = findViewById(R.id.tvTimeZohor);
            View dotZohor = findViewById(R.id.dotZohor);

            LinearLayout pillAsar = findViewById(R.id.pillAsar);
            TextView tvNameAsar = findViewById(R.id.tvNameAsar);
            TextView tvTimeAsar = findViewById(R.id.tvTimeAsar);
            View dotAsar = findViewById(R.id.dotAsar);

            LinearLayout pillMaghrib = findViewById(R.id.pillMaghrib);
            TextView tvNameMaghrib = findViewById(R.id.tvNameMaghrib);
            TextView tvTimeMaghrib = findViewById(R.id.tvTimeMaghrib);
            View dotMaghrib = findViewById(R.id.dotMaghrib);

            LinearLayout pillIsyak = findViewById(R.id.pillIsyak);
            TextView tvNameIsyak = findViewById(R.id.tvNameIsyak);
            TextView tvTimeIsyak = findViewById(R.id.tvTimeIsyak);
            View dotIsyak = findViewById(R.id.dotIsyak);

            if (tvNameSubuh != null) tvNameSubuh.setText(getString(R.string.prayer_name_subuh));
            if (tvNameZohor != null) tvNameZohor.setText(getString(R.string.prayer_name_zohor));
            if (tvNameAsar != null) tvNameAsar.setText(getString(R.string.prayer_name_asar));
            if (tvNameMaghrib != null) tvNameMaghrib.setText(getString(R.string.prayer_name_maghrib));
            if (tvNameIsyak != null) tvNameIsyak.setText(getString(R.string.prayer_name_isyak));

            if (tvTimeSubuh != null) tvTimeSubuh.setText(com.hafiztraveltours.app.utils.DateFormats.formatEpochSeconds(epochSeconds[0], appLocale));
            if (tvTimeZohor != null) tvTimeZohor.setText(com.hafiztraveltours.app.utils.DateFormats.formatEpochSeconds(epochSeconds[1], appLocale));
            if (tvTimeAsar != null) tvTimeAsar.setText(com.hafiztraveltours.app.utils.DateFormats.formatEpochSeconds(epochSeconds[2], appLocale));
            if (tvTimeMaghrib != null) tvTimeMaghrib.setText(com.hafiztraveltours.app.utils.DateFormats.formatEpochSeconds(epochSeconds[3], appLocale));
            if (tvTimeIsyak != null) tvTimeIsyak.setText(com.hafiztraveltours.app.utils.DateFormats.formatEpochSeconds(epochSeconds[4], appLocale));

            int activeIndex = -1;
            for (int i = 0; i < names.length; i++) {
                if (names[i].equalsIgnoreCase(result.currentName)) {
                    activeIndex = i;
                    break;
                }
            }

            updatePrayerPill(pillSubuh, tvNameSubuh, tvTimeSubuh, dotSubuh, activeIndex == 0);
            updatePrayerPill(pillZohor, tvNameZohor, tvTimeZohor, dotZohor, activeIndex == 1);
            updatePrayerPill(pillAsar, tvNameAsar, tvTimeAsar, dotAsar, activeIndex == 2);
            updatePrayerPill(pillMaghrib, tvNameMaghrib, tvTimeMaghrib, dotMaghrib, activeIndex == 3);
            updatePrayerPill(pillIsyak, tvNameIsyak, tvTimeIsyak, dotIsyak, activeIndex == 4);
        }
    }

    private void updatePrayerPill(LinearLayout pill, TextView tvName, TextView tvTime, View dotNode, boolean isActive) {
        if (pill == null || tvName == null || tvTime == null) return;
        pill.setBackgroundColor(Color.TRANSPARENT);
        if (isActive) {
            tvName.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            tvName.setTypeface(null, Typeface.BOLD);
            tvTime.setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            tvTime.setTypeface(null, Typeface.BOLD);
            if (dotNode != null) {
                dotNode.setBackgroundResource(R.drawable.bg_timeline_dot_active);
            }
        } else {
            tvName.setTextColor(Color.parseColor("#64748B"));
            tvName.setTypeface(null, Typeface.NORMAL);
            tvTime.setTextColor(Color.parseColor("#1E293B"));
            tvTime.setTypeface(null, Typeface.NORMAL);
            if (dotNode != null) {
                dotNode.setBackgroundResource(R.drawable.bg_timeline_dot_inactive);
            }
        }
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
        TextView currentLabel = findViewById(R.id.prayerCurrentLabel);
        TextView nextLabel = findViewById(R.id.prayerNextLabel);
        TextView countdownText = findViewById(R.id.prayerCountdownText);

        if (dateText != null) {
            dateText.setText(getString(R.string.prayer_location_denied_text));
            dateText.setTextColor(getResources().getColor(R.color.prayer_card_text_secondary));
            dateText.setTextSize(16f);
        }

        if (countdownText != null) {
            countdownText.setText("--");
        }

        if (currentLabel != null) currentLabel.setText("");

        if (nextLabel != null) {
            nextLabel.setText(getString(R.string.prayer_use_my_location));
            nextLabel.setTextColor(getResources().getColor(R.color.prayer_card_text_primary));
            nextLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            nextLabel.setClickable(true);
            nextLabel.setFocusable(true);
            nextLabel.setOnClickListener(v2 ->
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));
        }
    }

    // ================== end waktu solat ==================

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}