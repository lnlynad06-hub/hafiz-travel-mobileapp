package com.hafiztraveltours.app.ui;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Standalone "Tentang Kami" page (not a dialog) - a proper full-screen
 * Activity, consistent with HubungiKamiActivity and WebViewActivity.
 *
 * Content below is paraphrased from the "Tentang Kami" page on
 * hafiztraveltours.com/tentang-kami (checked 2026-08-24). Facts kept
 * accurate to the source: founded 2022, MOTAC KPK/LN 10751, HQ in Larkin
 * Johor Bahru, 10+ active branches across 10 states in Peninsular Malaysia.
 *
 * TODO: once the company updates their profile on the website, update the
 * text in activity_tentang_kami.xml (or better, pull from a backend API
 * once one exists instead of hardcoding it here).
 */
public class TentangKamiActivity extends BaseActivity {

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tentang_kami);

        findViewById(R.id.tentangBackButton).setOnClickListener(v -> finish());
    }
}
