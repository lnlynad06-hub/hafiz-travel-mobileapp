package com.example.hafiztraveltours;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImage);

        // Tap anywhere to skip straight to the Welcome screen
        findViewById(R.id.logoImage).getRootView().setOnClickListener(v -> goToWelcome());

        // Start small
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.5f, 1f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);

        AnimatorSet zoomIn = new AnimatorSet();
        zoomIn.playTogether(scaleX, scaleY, fadeIn);
        zoomIn.setDuration(700);

        zoomIn.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Pause after logo settles (feels less rushed), then move to Welcome.
                // User can also tap anywhere to skip immediately.
                logo.postDelayed(SplashActivity.this::goToWelcome, 1200);
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });

        zoomIn.start();
    }

    private void goToWelcome() {
        if (navigated) return; // avoid double-navigation if user taps right as the timer fires
        navigated = true;
        startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
