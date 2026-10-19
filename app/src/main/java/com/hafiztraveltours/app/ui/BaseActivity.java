package com.hafiztraveltours.app.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

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

    @Override
    public void applyOverrideConfiguration(Configuration overrideConfiguration) {
        if (overrideConfiguration != null) {
            Context context = LocaleHelper.applySavedLocale(getBaseContext());
            if (context != null) {
                overrideConfiguration.setTo(context.getResources().getConfiguration());
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LocaleHelper.applySavedLocale(this);
        super.onCreate(savedInstanceState);
    }
}
