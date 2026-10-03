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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends BaseActivity {

    public static final int STEP_START_EXPLORING = 0;
    public static final int STEP_PRAYER = 1;
    public static final int STEP_NOTIFICATION = 2;

    private int currentStep = STEP_START_EXPLORING;

    private View layoutStartExploring;
    private View layoutPrayerSetup;
    private View layoutNotificationSetup;

    private String activeLanguage;
    private TextView tvActiveLanguage;

    // Permissions
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                goToNotificationSetup();
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                completeOnboardingAndGoHome();
            });

    // Showcase Carousel
    private ImageView heroImageMain;
    private TextView tvHeroTag;

    private static class ShowcaseItem {
        final String packageId;
        final String collectionName;
        final Object imageSource; // String URL or Integer Drawable
        final String tag;

        ShowcaseItem(String packageId, String collectionName, Object imageSource, String tag) {
            this.packageId = packageId;
            this.collectionName = collectionName;
            this.imageSource = imageSource;
            this.tag = tag;
        }
    }

    private final List<ShowcaseItem> showcaseList = new ArrayList<>();
    private int currentShowcaseIndex = 0;
    private final Handler showcaseHandler = new Handler(Looper.getMainLooper());
    private final Runnable showcaseRunnable = new Runnable() {
        @Override
        public void run() {
            advanceShowcase();
            showcaseHandler.postDelayed(this, 3800);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        activeLanguage = LocaleHelper.getSavedLanguage(this);
        updateActiveLanguageLabel();

        fetchLivePackagesFromApi();
        startShowcase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopShowcase();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        layoutStartExploring = findViewById(R.id.layoutStartExploring);
        layoutPrayerSetup = findViewById(R.id.layoutPrayerSetup);
        layoutNotificationSetup = findViewById(R.id.layoutNotificationSetup);

        tvActiveLanguage = findViewById(R.id.tvActiveLanguage);
        heroImageMain = findViewById(R.id.heroImageMain);
        tvHeroTag = findViewById(R.id.tvHeroTag);

        setupFallbackShowcase();
        updateActiveLanguageLabel();

        // Luxury Language Picker Bottom Sheet (Discreet on Start Exploring screen)
        findViewById(R.id.btnLanguagePicker).setOnClickListener(v -> showLanguageBottomSheet());

        // Category chips quick navigation
        findViewById(R.id.chipWorldwide).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, TourActivity.class))
        );
        findViewById(R.id.chipUmrah).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, UmrahActivity.class))
        );
        findViewById(R.id.chipCustom).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, HubungiKamiActivity.class))
        );

        TextView tvLogin = findViewById(R.id.loginLinkText);
        if (tvLogin != null) {
            tvLogin.setText(android.text.Html.fromHtml(getString(R.string.welcome_already_have_account_full)));
            tvLogin.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
            });
        }

        TextView tvSignUp = findViewById(R.id.signupLinkText);
        if (tvSignUp != null) {
            tvSignUp.setText(android.text.Html.fromHtml(getString(R.string.welcome_no_account_full)));
            tvSignUp.setOnClickListener(v -> {
                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
                overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
            });
        }

        // Wire Up Onboarding Setup Full-Screen Flow
        setupOnboardingFlow();

        int startAt = getIntent().getIntExtra("start_at_step", STEP_START_EXPLORING);
        if (startAt == STEP_PRAYER) {
            if (layoutStartExploring != null) layoutStartExploring.setVisibility(View.GONE);
            if (layoutPrayerSetup != null) {
                layoutPrayerSetup.setVisibility(View.VISIBLE);
                layoutPrayerSetup.setAlpha(1f);
            }
            currentStep = STEP_PRAYER;
        } else {
            // Calm, premium entrance animation
            playEntranceAnimation();
        }
    }

    private void setupOnboardingFlow() {
        // Step 1: Start Exploring CTA -> Transitions to full-screen Prayer Times & Qibla
        findViewById(R.id.getStartedButton).setOnClickListener(v -> {
            HapticUtil.click(v);
            goToPrayerSetup();
        });

        // Step 2: Prayer Times & Qibla Actions (Grouped with Azan & Background setup)
        findViewById(R.id.btnEnablePrayer).setOnClickListener(v -> {
            HapticUtil.click(v);
            OnboardingManager.setPrayerFeatureEnabled(this, true);
            OnboardingManager.setQiblaFeatureEnabled(this, true);
            OnboardingManager.setAzanFeatureEnabled(this, true);
            boolean hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (hasFine || hasCoarse) {
                goToNotificationSetup();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        findViewById(R.id.btnSkipPrayer).setOnClickListener(v -> {
            HapticUtil.click(v);
            OnboardingManager.setPrayerFeatureEnabled(this, false);
            OnboardingManager.setQiblaFeatureEnabled(this, false);
            OnboardingManager.setAzanFeatureEnabled(this, false);
            PrayerTimeScheduler.cancelAllAlarms(this);
            goToNotificationSetup();
        });

        // Step 3: Notification Setup Actions
        findViewById(R.id.btnAllowNotif).setOnClickListener(v -> {
            HapticUtil.click(v);
            OnboardingManager.setNotificationFeatureEnabled(this, true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    completeOnboardingAndGoHome();
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                completeOnboardingAndGoHome();
            }
        });

        findViewById(R.id.btnSkipNotif).setOnClickListener(v -> {
            HapticUtil.click(v);
            OnboardingManager.setNotificationFeatureEnabled(this, false);
            completeOnboardingAndGoHome();
        });
    }

    private void goToPrayerSetup() {
        currentStep = STEP_PRAYER;
        crossFade(layoutStartExploring, layoutPrayerSetup);
    }

    private void goToNotificationSetup() {
        currentStep = STEP_NOTIFICATION;
        crossFade(layoutPrayerSetup, layoutNotificationSetup);
    }

    /**
     * Pure, subtle fade-out and fade-in between full-screen onboarding screens.
     */
    private void crossFade(final View outgoing, final View incoming) {
        if (outgoing == null || incoming == null) return;
        outgoing.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    outgoing.setVisibility(View.GONE);
                    incoming.setVisibility(View.VISIBLE);
                    incoming.setAlpha(0f);
                    incoming.animate()
                            .alpha(1f)
                            .setDuration(240)
                            .start();
                })
                .start();
    }

    /**
     * Completes onboarding, permanently persists the first-launch flag, and routes to Home.
     */
    private void completeOnboardingAndGoHome() {
        OnboardingManager.setOnboardingCompleted(this, true);
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentStep == STEP_NOTIFICATION) {
            currentStep = STEP_PRAYER;
            crossFade(layoutNotificationSetup, layoutPrayerSetup);
        } else if (currentStep == STEP_PRAYER) {
            currentStep = STEP_START_EXPLORING;
            crossFade(layoutPrayerSetup, layoutStartExploring);
        } else {
            super.onBackPressed();
        }
    }

    private void setupFallbackShowcase() {
        showcaseList.clear();
        showcaseList.add(new ShowcaseItem(null, "tour", R.drawable.img_turkiye, "TURKIYE"));
        showcaseList.add(new ShowcaseItem(null, "tour", R.drawable.img_korea1, "KOREA"));
        showcaseList.add(new ShowcaseItem(null, "umrah", R.drawable.img_korea2, "MAKKAH & MADINAH"));
        displayCurrentShowcase(false);
    }

    /**
     * Fetches real packages from Laravel REST API backend (MySQL)
     */
    private void fetchLivePackagesFromApi() {
        ApiClient.getApiService().getPackages(null, null, null, null)
                .enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                            List<UmrahPackage> pkgs = response.body().data;
                            if (!pkgs.isEmpty()) {
                                showcaseList.clear();
                                for (UmrahPackage p : pkgs) {
                                    String tag = formatShortDestinationTag(p);

                                    Object img = (p.imageUrl != null && !p.imageUrl.trim().isEmpty())
                                            ? p.imageUrl
                                            : R.drawable.img_turkiye;

                                    showcaseList.add(new ShowcaseItem(p.id, p.category != null ? p.category : "tour", img, tag));
                                }
                                currentShowcaseIndex = 0;
                                displayCurrentShowcase(true);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                        // Keeps fallback smoothly
                    }
                });
    }

    private String formatShortDestinationTag(UmrahPackage p) {
        String dest = (p.destination != null) ? p.destination.toLowerCase() : "";
        String name = (p.name != null) ? p.name.toLowerCase() : "";
        String cat = (p.category != null) ? p.category.toLowerCase() : "";

        if (dest.contains("turki") || dest.contains("turkey") || dest.contains("istanbul") || dest.contains("cappadocia")
                || name.contains("turki") || name.contains("turkey")) {
            return "TURKIYE";
        }
        if (dest.contains("swiss") || dest.contains("switzerland") || name.contains("swiss") || name.contains("switzerland")) {
            return "SWITZERLAND";
        }
        if (dest.contains("korea") || dest.contains("seoul") || name.contains("korea")) {
            return "KOREA";
        }
        if (dest.contains("jepun") || dest.contains("japan") || dest.contains("tokyo") || name.contains("jepun") || name.contains("japan")) {
            return "JAPAN";
        }
        if (dest.contains("balkan") || name.contains("balkan")) {
            return "BALKAN";
        }
        if (dest.contains("china") || dest.contains("beijing") || name.contains("china")) {
            return "CHINA";
        }
        if (dest.contains("london") || dest.contains("uk") || dest.contains("england") || name.contains("london") || name.contains("uk")) {
            return "UNITED KINGDOM";
        }
        if (dest.contains("vietnam") || dest.contains("hanoi") || dest.contains("da nang") || name.contains("vietnam")) {
            return "VIETNAM";
        }
        if (dest.contains("indonesia") || dest.contains("bali") || name.contains("indonesia")) {
            return "INDONESIA";
        }
        if (dest.contains("makkah") || dest.contains("madinah") || dest.contains("saudi")
                || name.contains("umrah") || name.contains("ziarah") || name.contains("ramadan") || name.contains("syawal")
                || "umrah".equalsIgnoreCase(cat)) {
            return "MAKKAH & MADINAH";
        }

        if (p.destination != null && !p.destination.trim().isEmpty()) {
            String[] parts = p.destination.split("[,/•-]");
            String first = parts[0].trim();
            if (first.length() > 18) {
                first = first.substring(0, 18).trim();
            }
            return first.toUpperCase();
        }

        return "DESTINATION";
    }

    private void startShowcase() {
        showcaseHandler.removeCallbacks(showcaseRunnable);
        showcaseHandler.postDelayed(showcaseRunnable, 3800);
    }

    private void stopShowcase() {
        showcaseHandler.removeCallbacks(showcaseRunnable);
    }

    private void advanceShowcase() {
        if (showcaseList.isEmpty()) return;
        currentShowcaseIndex = (currentShowcaseIndex + 1) % showcaseList.size();
        displayCurrentShowcase(true);
    }

    private void displayCurrentShowcase(boolean animate) {
        if (isFinishing() || isDestroyed()) return;
        if (showcaseList.isEmpty() || currentShowcaseIndex >= showcaseList.size()) return;
        ShowcaseItem item = showcaseList.get(currentShowcaseIndex);

        if (heroImageMain != null) {
            try {
                Glide.with(WelcomeActivity.this)
                        .load(item.imageSource)
                        .transition(DrawableTransitionOptions.withCrossFade(400))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.img_turkiye)
                        .centerCrop()
                        .into(heroImageMain);
            } catch (Exception ignored) {}
        }

        if (tvHeroTag != null) {
            if (animate) {
                tvHeroTag.animate()
                        .alpha(0f)
                        .setDuration(140)
                        .withEndAction(() -> {
                            ViewGroup parent = findViewById(R.id.heroCardContent);
                            if (parent != null) {
                                TransitionSet set = new TransitionSet();
                                set.addTransition(new ChangeBounds()
                                        .setDuration(420)
                                        .setInterpolator(new DecelerateInterpolator(1.8f)));
                                TransitionManager.beginDelayedTransition(parent, set);
                            }
                            tvHeroTag.setText(item.tag);
                            tvHeroTag.animate()
                                    .alpha(1f)
                                    .setDuration(220)
                                    .start();
                        })
                        .start();
            } else {
                tvHeroTag.setText(item.tag);
            }
        }
    }

    private void updateActiveLanguageLabel() {
        String current = LocaleHelper.getSavedLanguage(this);
        if (tvActiveLanguage != null) {
            tvActiveLanguage.setText(LocaleHelper.getLanguageBadge(current));
        }
    }

    private void showLanguageBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, com.google.android.material.R.style.Theme_Design_BottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_language_picker, null);
        dialog.setContentView(sheetView);

        if (sheetView.getParent() instanceof View) {
            ((View) sheetView.getParent()).setBackgroundColor(Color.TRANSPARENT);
        }

        View btnClose = sheetView.findViewById(R.id.btnCloseSheet);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String current = LocaleHelper.getSavedLanguage(this);

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
            HapticUtil.click(v);
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
                                        LocaleHelper.applyAndSaveLanguage(WelcomeActivity.this, langCode);
                                    }
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void playEntranceAnimation() {
        View heroCard = findViewById(R.id.heroCard);
        View eyebrowText = findViewById(R.id.eyebrowText);
        View titleText = findViewById(R.id.titleText);
        View subtitleText = findViewById(R.id.subtitleText);
        View categoryChipsLayout = findViewById(R.id.categoryChipsLayout);
        View getStartedButton = findViewById(R.id.getStartedButton);
        View authGroupContainer = findViewById(R.id.authGroupContainer);
        View loginLinkText = findViewById(R.id.loginLinkText);

        View authView = authGroupContainer != null ? authGroupContainer : loginLinkText;
        View[] views = {heroCard, eyebrowText, titleText, subtitleText, categoryChipsLayout, getStartedButton, authView};

        long delay = 60;
        for (View v : views) {
            if (v != null) {
                v.setAlpha(0f);
                v.setTranslationY(20f);
                v.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(460)
                        .setStartDelay(delay)
                        .setInterpolator(new DecelerateInterpolator(1.4f))
                        .start();
                delay += 50;
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopShowcase();
        showcaseHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}