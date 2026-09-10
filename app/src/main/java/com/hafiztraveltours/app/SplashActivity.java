package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View splashLogo = findViewById(R.id.splashLogo);
        View splashTitle = findViewById(R.id.splashTitle);
        View splashSubtitle = findViewById(R.id.splashSubtitle);
        View splashFooter = findViewById(R.id.splashFooter);

        // Initial invisible states
        if (splashLogo != null) {
            splashLogo.setAlpha(0f);
            splashLogo.setScaleX(0.85f);
            splashLogo.setScaleY(0.85f);

            splashLogo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(650)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();
        }

        if (splashTitle != null) {
            splashTitle.setAlpha(0f);
            splashTitle.setTranslationY(18f);
            splashTitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(550)
                    .setStartDelay(120)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }

        if (splashSubtitle != null) {
            splashSubtitle.setAlpha(0f);
            splashSubtitle.setTranslationY(18f);
            splashSubtitle.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(550)
                    .setStartDelay(200)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .start();
        }

        if (splashFooter != null) {
            splashFooter.setAlpha(0f);
            splashFooter.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .start();
        }

        // Seamless transition into WelcomeActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
            finish();
        }, 1350);
    }
}