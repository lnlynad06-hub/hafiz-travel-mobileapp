package com.hafiztraveltours.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

/**
 * Shared controller and click-binder for the Floating Luxury Bottom Navigation Dock.
 * Eliminates duplicate navigation code across all main screens.
 */
public class BottomNavHelper {

    public enum Tab {
        HOME,
        UMRAH,
        EXPLORE,
        TOUR,
        FAVORITE,
        NONE
    }

    public static void setup(Activity activity, Tab activeTab) {
        View navHome = activity.findViewById(R.id.navHome);
        View navUmrah = activity.findViewById(R.id.navUmrah);
        View navCenterAction = activity.findViewById(R.id.navCenterAction);
        View navTour = activity.findViewById(R.id.navTour);
        View navFavorite = activity.findViewById(R.id.navFavorite);

        if (navHome == null) return; // Layout not present in this activity

        // 1. Configure active tab styles
        setTabStyle(activity, navHome, R.id.icNavHome, R.id.tvNavHome, activeTab == Tab.HOME);
        setTabStyle(activity, navUmrah, R.id.icNavUmrah, R.id.tvNavUmrah, activeTab == Tab.UMRAH);
        setTabStyle(activity, navTour, R.id.icNavTour, R.id.tvNavTour, activeTab == Tab.TOUR);
        setTabStyle(activity, navFavorite, R.id.icNavFavorite, R.id.tvNavFavorite, activeTab == Tab.FAVORITE);

        // 2. Bind click routing with tactile bounce animations
        setupTactileButton(navHome, () -> {
            if (activeTab == Tab.HOME) {
                NestedScrollView scrollView = activity.findViewById(R.id.mainScrollView);
                if (scrollView != null) scrollView.smoothScrollTo(0, 0);
            } else {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
                if (!(activity instanceof MainActivity)) activity.finish();
            }
        });

        setupTactileButton(navUmrah, () -> {
            if (activeTab != Tab.UMRAH) {
                Intent intent = new Intent(activity, UmrahActivity.class);
                activity.startActivity(intent);
                if (!(activity instanceof MainActivity)) activity.finish();
            }
        });

        setupTactileButton(navCenterAction, () -> {
            Intent intent = new Intent(activity, AllPackagesActivity.class);
            activity.startActivity(intent);
        });

        setupTactileButton(navTour, () -> {
            if (activeTab != Tab.TOUR) {
                Intent intent = new Intent(activity, TourActivity.class);
                activity.startActivity(intent);
                if (!(activity instanceof MainActivity)) activity.finish();
            }
        });

        setupTactileButton(navFavorite, () -> {
            if (activeTab != Tab.FAVORITE) {
                Intent intent = new Intent(activity, FavoriteActivity.class);
                activity.startActivity(intent);
                if (!(activity instanceof MainActivity)) activity.finish();
            }
        });

        updateFavoriteBadge(activity);
    }

    private static void setTabStyle(Activity activity, View tabView, int iconId, int textId, boolean isActive) {
        if (tabView == null) return;
        ImageView icon = tabView.findViewById(iconId);
        TextView text = tabView.findViewById(textId);

        int activeColor = ContextCompat.getColor(activity, R.color.brand_magenta);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_pill_active_nav);
            if (icon != null) icon.setColorFilter(activeColor);
            if (text != null) {
                text.setTextColor(activeColor);
                text.setTypeface(null, android.graphics.Typeface.BOLD);
            }
        } else {
            tabView.setBackgroundColor(Color.TRANSPARENT);
            if (icon != null) icon.setColorFilter(inactiveColor);
            if (text != null) {
                text.setTextColor(inactiveColor);
                text.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    public static void updateFavoriteBadge(Activity activity) {
        TextView tvBadge = activity.findViewById(R.id.tvFavoriteBadge);
        if (tvBadge == null) return;
        int count = FavoritesManager.getFavoriteCount(activity);
        if (count > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(count > 9 ? "9+" : String.valueOf(count));
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    private static void setupTactileButton(View view, Runnable onClick) {
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
}
