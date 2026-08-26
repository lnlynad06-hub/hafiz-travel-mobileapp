package com.example.hafiztraveltours;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Applies whatever language the user picked last time (default: English)
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private String activeLanguage;

    @Override
    protected void onResume() {
        super.onResume();
        String currentSaved = LocaleHelper.getSavedLanguage(this);
        if (activeLanguage != null && !activeLanguage.equals(currentSaved)) {
            recreate();
        }
        activeLanguage = currentSaved;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ImageView imageLeft = findViewById(R.id.imageLeft);
        ImageView imageCenter = findViewById(R.id.imageCenter);
        ImageView imageRight = findViewById(R.id.imageRight);

        // Floating/bounce loop, each image starts at a slightly different time
        // so they don't move perfectly in sync (feels more "alive")
        startFloatingLoop(imageLeft, 0);
        startFloatingLoop(imageCenter, 150);
        startFloatingLoop(imageRight, 300);

        // "Mula Sekarang" takes the user straight into the app as a guest.
        // Sign In / Sign Up is only required later, e.g. at checkout/booking.
        // TODO: replace MainActivity.class with your real Homepage activity once it's built.
        findViewById(R.id.getStartedButton).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class))
        );

        findViewById(R.id.loginLinkText).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );

        setupLanguageSelector();
    }

    private void setupLanguageSelector() {
        TextView[] buttons = {
                findViewById(R.id.btnLangEnglish),
                findViewById(R.id.btnLangMalay),
                findViewById(R.id.btnLangArabic),
                findViewById(R.id.btnLangKorean),
                findViewById(R.id.btnLangJapanese),
                findViewById(R.id.btnLangChinese)
        };

        String[] codes = {
                LocaleHelper.LANGUAGE_ENGLISH,
                LocaleHelper.LANGUAGE_MALAY,
                LocaleHelper.LANGUAGE_ARABIC,
                LocaleHelper.LANGUAGE_KOREAN,
                LocaleHelper.LANGUAGE_JAPANESE,
                LocaleHelper.LANGUAGE_CHINESE
        };

        String current = LocaleHelper.getSavedLanguage(this);

        for (int i = 0; i < buttons.length; i++) {
            setPillStyle(buttons[i], codes[i].equals(current));
            String code = codes[i];
            buttons[i].setOnClickListener(v -> switchLanguage(code));
        }
    }

    private void switchLanguage(String languageCode) {
        LocaleHelper.saveLanguage(this, languageCode);
        recreate();
    }

    private void setPillStyle(TextView button, boolean isSelected) {
        button.setBackgroundResource(isSelected ? R.drawable.bg_pill_selected : R.drawable.bg_pill_unselected);
        button.setTextColor(getResources().getColor(isSelected ? R.color.white : R.color.pink_dark));
    }

    /**
     * Moves the view up and down gently, forever, like it's floating.
     * startDelayMs staggers each image so the group doesn't bounce in perfect unison.
     */
    private void startFloatingLoop(ImageView view, int startDelayMs) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -18f);
        floatAnim.setDuration(900);
        floatAnim.setStartDelay(startDelayMs);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.start();
    }
}