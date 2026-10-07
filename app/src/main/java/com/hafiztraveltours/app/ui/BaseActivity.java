package com.hafiztraveltours.app.ui;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.hafiztraveltours.app.utils.LocaleHelper;

/**
 * Shared Activity base (M2). Applies the saved app locale (EN/MS only) to every
 * screen. No other behavior lives here — Screens keep their own logic.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }
}
