package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The System Splash Screen (pink background + logo) is shown
        // automatically by Android while this Activity is starting up.
        // We don't need our own custom splash UI/animation on top of it.
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        startActivity(new Intent(this, WelcomeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}