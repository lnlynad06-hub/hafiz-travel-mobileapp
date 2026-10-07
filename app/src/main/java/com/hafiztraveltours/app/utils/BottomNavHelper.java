package com.hafiztraveltours.app.utils;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.ui.AllPackagesActivity;
import com.hafiztraveltours.app.ui.FavoriteActivity;
import com.hafiztraveltours.app.ui.MainActivity;
import com.hafiztraveltours.app.ui.MyBookingsActivity;
import com.hafiztraveltours.app.ui.ProfileActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

/**
 * Shared controller and click/gesture-binder for the Bottom Navigation bar.
 * Handles active tab styling, tactile button feedback, seamless page transitions,
 * and fluid interactive tab bar dragging (iOS-style tab bar drag).
 */
public class BottomNavHelper {

    public enum Tab {
        HOME(0),
        BOOKING(1),
        EXPLORE(2),
        FAVORITE(3),
        PROFILE(4),
        NONE(-1);

        public final int index;

        Tab(int index) {
            this.index = index;
        }
    }

    private static boolean isTransitioning = false;

    @SuppressLint("ClickableViewAccessibility")
    public static void setup(Activity activity, Tab activeTab) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        View navHome = activity.findViewById(R.id.navHome);
        View navBooking = activity.findViewById(R.id.navBooking);
        View navExplore = activity.findViewById(R.id.navCenterAction);
        View navFavorite = activity.findViewById(R.id.navFavorite);
        View navProfile = activity.findViewById(R.id.navProfile);

        if (navHome == null) return; // Layout not present in this activity

        // Reset transition lock when setting up a new/resumed activity
        isTransitioning = false;

        View[] tabs = new View[]{navHome, navBooking, navExplore, navFavorite, navProfile};
        int[] iconIds = new int[]{R.id.icNavHome, R.id.icNavBooking, 0, R.id.icNavFavorite, R.id.icNavProfile};
        int[] textIds = new int[]{R.id.tvNavHome, R.id.tvNavBooking, R.id.tvNavCenterAction, R.id.tvNavFavorite, R.id.tvNavProfile};

        // 1. Configure active tab styles
        animateToActiveTab(activity, tabs, iconIds, textIds, activeTab);

        // 2. Bind click routing with tactile animation and seamless transitions
        setupTactileButton(navHome, () -> {
            if (activeTab == Tab.HOME) {
                NestedScrollView scrollView = activity.findViewById(R.id.mainScrollView);
                if (scrollView != null) scrollView.smoothScrollTo(0, 0);
            } else {
                switchToTab(activity, activeTab, Tab.HOME);
            }
        });

        setupTactileButton(navBooking, () -> {
            if (activeTab != Tab.BOOKING) {
                switchToTab(activity, activeTab, Tab.BOOKING);
            }
        });

        setupTactileButton(navExplore, () -> {
            if (activeTab != Tab.EXPLORE) {
                switchToTab(activity, activeTab, Tab.EXPLORE);
            }
        });

        setupTactileButton(navFavorite, () -> {
            if (activeTab != Tab.FAVORITE) {
                switchToTab(activity, activeTab, Tab.FAVORITE);
            }
        });

        setupTactileButton(navProfile, () -> {
            if (activeTab != Tab.PROFILE) {
                switchToTab(activity, activeTab, Tab.PROFILE);
            }
        });

        // 3. Attach interactive tab bar drag gesture (scoped strictly to Bottom Navigation bar)
        if (activeTab != Tab.NONE) {
            attachTabBarGesture(activity, activeTab, tabs, iconIds, textIds);
        }
    }

    private static void attachTabBarGesture(Activity activity, Tab activeTab, View[] tabs, int[] iconIds, int[] textIds) {
        Window window = activity.getWindow();
        if (window == null) return;

        Window.Callback currentCallback = window.getCallback();
        if (currentCallback == null) return;

        // Unwrap existing custom callback to avoid infinite wrapping on recreate/resume
        if (currentCallback instanceof WindowCallbackWrapper) {
            currentCallback = ((WindowCallbackWrapper) currentCallback).getWrapped();
        }

        final View bottomNavCard = activity.findViewById(R.id.bottomNavCard);
        final int touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

        window.setCallback(new WindowCallbackWrapper(currentCallback) {
            private float startX = 0f;
            private float startY = 0f;
            private boolean isNavTouch = false;
            private boolean isDragging = false;
            private boolean cancelSent = false;
            private Tab highlightedTab = activeTab;
            private final Rect navRect = new Rect();

            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (bottomNavCard == null || !bottomNavCard.isShown()) {
                    return super.dispatchTouchEvent(event);
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        bottomNavCard.getGlobalVisibleRect(navRect);

                        // Check if touch down occurs inside the Bottom Navigation bar
                        if (navRect.contains((int) startX, (int) startY)) {
                            isNavTouch = true;
                            isDragging = false;
                            cancelSent = false;
                            highlightedTab = activeTab;
                        } else {
                            isNavTouch = false;
                        }
                        return super.dispatchTouchEvent(event);

                    case MotionEvent.ACTION_MOVE:
                        if (!isNavTouch) {
                            return super.dispatchTouchEvent(event);
                        }

                        float deltaX = event.getRawX() - startX;
                        float deltaY = event.getRawY() - startY;

                        if (!isDragging) {
                            if (Math.abs(deltaX) > touchSlop && Math.abs(deltaX) > Math.abs(deltaY)) {
                                isDragging = true;
                            }
                        }

                        if (isDragging) {
                            if (!cancelSent) {
                                // Cancel child button animations/press states once drag initiates
                                MotionEvent cancelEvent = MotionEvent.obtain(event);
                                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                                super.dispatchTouchEvent(cancelEvent);
                                cancelEvent.recycle();
                                cancelSent = true;
                            }

                            // Calculate finger progress [0.0f .. 5.0f] across the bottom navigation bar
                            float relativeX = event.getRawX() - navRect.left;
                            float width = navRect.width() > 0 ? navRect.width() : 1f;
                            float fingerProgress = (relativeX / width) * 5.0f;
                            fingerProgress = Math.max(0.0f, Math.min(5.0f, fingerProgress));

                            int tabIndex = Math.max(0, Math.min(4, (int) fingerProgress));
                            highlightedTab = getTabByIndex(tabIndex);

                            // Continuous real-time visual interpolation of color, scale, and alpha
                            updateTabStylesProgress(activity, tabs, iconIds, textIds, fingerProgress);
                            return true; // Consume touch event exclusively for tab bar drag
                        }
                        return super.dispatchTouchEvent(event);

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isNavTouch) {
                            boolean wasDragging = isDragging;
                            Tab finalTarget = highlightedTab;

                            isNavTouch = false;
                            isDragging = false;
                            cancelSent = false;

                            if (wasDragging) {
                                animateToActiveTab(activity, tabs, iconIds, textIds, finalTarget);
                                if (finalTarget != activeTab) {
                                    switchToTab(activity, activeTab, finalTarget);
                                }
                                return true;
                            }
                        }
                        return super.dispatchTouchEvent(event);
                }
                return super.dispatchTouchEvent(event);
            }
        });
    }

    private static void updateTabStylesProgress(Activity activity, View[] tabs, int[] iconIds, int[] textIds, float fingerProgress) {
        int activeColor = ContextCompat.getColor(activity, R.color.brand_magenta);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        for (int i = 0; i < tabs.length; i++) {
            View tabView = tabs[i];
            if (tabView == null) continue;

            ImageView icon = iconIds[i] != 0 ? tabView.findViewById(iconIds[i]) : null;
            TextView text = tabView.findViewById(textIds[i]);

            float tabCenter = i + 0.5f;
            float dist = Math.abs(fingerProgress - tabCenter);
            float activeRatio = Math.max(0.0f, 1.0f - (dist * 1.1f));

            int currentColor = interpolateColor(inactiveColor, activeColor, activeRatio);
            float scale = 1.0f + (activeRatio * 0.15f);
            float alpha = 0.7f + (activeRatio * 0.3f);

            if (icon != null) {
                icon.setColorFilter(currentColor);
                icon.setScaleX(scale);
                icon.setScaleY(scale);
                icon.setAlpha(alpha);
            }
            if (text != null) {
                text.setTextColor(currentColor);
                text.setScaleX(1.0f + (activeRatio * 0.04f));
                text.setScaleY(1.0f + (activeRatio * 0.04f));
                text.setAlpha(alpha);
                text.setTypeface(null, activeRatio > 0.5f ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
    }

    private static void animateToActiveTab(Activity activity, View[] tabs, int[] iconIds, int[] textIds, Tab activeTab) {
        int activeColor = ContextCompat.getColor(activity, R.color.brand_magenta);
        int inactiveColor = ContextCompat.getColor(activity, R.color.text_gray);

        for (int i = 0; i < tabs.length; i++) {
            View tabView = tabs[i];
            if (tabView == null) continue;

            ImageView icon = iconIds[i] != 0 ? tabView.findViewById(iconIds[i]) : null;
            TextView text = textIds[i] != 0 ? tabView.findViewById(textIds[i]) : null;
            boolean isActive = (i == activeTab.index);

            int targetColor = isActive ? activeColor : inactiveColor;
            float targetScale = isActive ? 1.15f : 1.0f;
            float targetAlpha = isActive ? 1.0f : 0.75f;

            if (icon != null) {
                icon.setColorFilter(targetColor);
                icon.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .alpha(targetAlpha)
                    .setDuration(150)
                    .setInterpolator(new OvershootInterpolator(1.2f))
                    .start();
            }
            if (text != null) {
                text.setTextColor(targetColor);
                text.setTypeface(null, isActive ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                text.animate()
                    .scaleX(isActive ? 1.04f : 1.0f)
                    .scaleY(isActive ? 1.04f : 1.0f)
                    .alpha(targetAlpha)
                    .setDuration(150)
                    .start();
            }
        }
    }

    private static int interpolateColor(int colorFrom, int colorTo, float factor) {
        float f = Math.max(0.0f, Math.min(1.0f, factor));
        int a = (int) (Color.alpha(colorFrom) + f * (Color.alpha(colorTo) - Color.alpha(colorFrom)));
        int r = (int) (Color.red(colorFrom) + f * (Color.red(colorTo) - Color.red(colorFrom)));
        int g = (int) (Color.green(colorFrom) + f * (Color.green(colorTo) - Color.green(colorFrom)));
        int b = (int) (Color.blue(colorFrom) + f * (Color.blue(colorTo) - Color.blue(colorFrom)));
        return Color.argb(a, r, g, b);
    }

    private static Tab getTabByIndex(int index) {
        for (Tab tab : Tab.values()) {
            if (tab.index == index) return tab;
        }
        return Tab.HOME;
    }

    private static void switchToTab(Activity activity, Tab fromTab, Tab targetTab) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (fromTab == targetTab) return;
        if (isTransitioning) return;
        isTransitioning = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> isTransitioning = false, 300);

        Intent intent = null;
        switch (targetTab) {
            case HOME:
                intent = new Intent(activity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case BOOKING:
                intent = new Intent(activity, MyBookingsActivity.class);
                break;
            case EXPLORE:
                intent = new Intent(activity, AllPackagesActivity.class);
                break;
            case FAVORITE:
                intent = new Intent(activity, FavoriteActivity.class);
                break;
            case PROFILE:
                intent = new Intent(activity, ProfileActivity.class);
                break;
        }

        if (intent != null) {
            activity.startActivity(intent);
            applyTabTransition(activity, fromTab, targetTab);
            if (!(activity instanceof MainActivity)) {
                activity.finish();
            }
        }
    }

    public static void applyTabTransition(Activity activity, Tab fromTab, Tab toTab) {
        if (activity == null || fromTab == null || toTab == null || fromTab == toTab) return;
        activity.overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
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

    public static void updateFavoriteBadge(Activity activity) {
        // Preserved helper method for caller compatibility
    }

    private static class WindowCallbackWrapper implements Window.Callback {
        private final Window.Callback wrapped;

        public WindowCallbackWrapper(Window.Callback wrapped) {
            this.wrapped = wrapped;
        }

        public Window.Callback getWrapped() {
            return wrapped;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return wrapped.dispatchKeyEvent(event);
        }

        @Override
        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
            return wrapped.dispatchKeyShortcutEvent(event);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            return wrapped.dispatchTouchEvent(event);
        }

        @Override
        public boolean dispatchTrackballEvent(MotionEvent event) {
            return wrapped.dispatchTrackballEvent(event);
        }

        @Override
        public boolean dispatchGenericMotionEvent(MotionEvent event) {
            return wrapped.dispatchGenericMotionEvent(event);
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            return wrapped.dispatchPopulateAccessibilityEvent(event);
        }

        @Override
        public View onCreatePanelView(int featureId) {
            return wrapped.onCreatePanelView(featureId);
        }

        @Override
        public boolean onCreatePanelMenu(int featureId, Menu menu) {
            return wrapped.onCreatePanelMenu(featureId, menu);
        }

        @Override
        public boolean onPreparePanel(int featureId, View view, Menu menu) {
            return wrapped.onPreparePanel(featureId, view, menu);
        }

        @Override
        public boolean onMenuOpened(int featureId, Menu menu) {
            return wrapped.onMenuOpened(featureId, menu);
        }

        @Override
        public boolean onMenuItemSelected(int featureId, MenuItem item) {
            return wrapped.onMenuItemSelected(featureId, item);
        }

        @Override
        public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) {
            wrapped.onWindowAttributesChanged(attrs);
        }

        @Override
        public void onContentChanged() {
            wrapped.onContentChanged();
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            wrapped.onWindowFocusChanged(hasFocus);
        }

        @Override
        public void onAttachedToWindow() {
            wrapped.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            wrapped.onDetachedFromWindow();
        }

        @Override
        public void onPanelClosed(int featureId, Menu menu) {
            wrapped.onPanelClosed(featureId, menu);
        }

        @Override
        public boolean onSearchRequested() {
            return wrapped.onSearchRequested();
        }

        @Override
        public boolean onSearchRequested(SearchEvent searchEvent) {
            return wrapped.onSearchRequested(searchEvent);
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
            return wrapped.onWindowStartingActionMode(callback);
        }

        @Override
        public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) {
            return wrapped.onWindowStartingActionMode(callback, type);
        }

        @Override
        public void onActionModeStarted(ActionMode mode) {
            wrapped.onActionModeStarted(mode);
        }

        @Override
        public void onActionModeFinished(ActionMode mode) {
            wrapped.onActionModeFinished(mode);
        }
    }
}
