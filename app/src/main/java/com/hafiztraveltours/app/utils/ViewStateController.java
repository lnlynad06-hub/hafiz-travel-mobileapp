package com.hafiztraveltours.app.utils;

import android.view.View;

/**
 * Tiny loading/content/empty state switch (H9). Binds existing layout views —
 * no XML changes, no new visuals. All methods are null-safe; states are
 * mutually exclusive. Error UX stays as-is per screen (usually a Toast via
 * ApiErrors); this helper only switches the three content views.
 *
 * <p>Screens with bespoke loading (shimmer lifecycles, multi-section browse,
 * swipe-only refresh) intentionally do NOT use this — see report.
 */
public final class ViewStateController {

    private final View loadingView;
    private final View contentView;
    private final View emptyView;

    public ViewStateController(View loadingView, View contentView, View emptyView) {
        this.loadingView = loadingView;
        this.contentView = contentView;
        this.emptyView = emptyView;
    }

    private static void show(View v, boolean visible) {
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** Loading visible; content + empty hidden. */
    public void showLoading() {
        show(loadingView, true);
        show(contentView, false);
        show(emptyView, false);
    }

    /** Content visible; loading + empty hidden. */
    public void showContent() {
        show(loadingView, false);
        show(contentView, true);
        show(emptyView, false);
    }

    /** Empty visible; loading + content hidden. */
    public void showEmpty() {
        show(loadingView, false);
        show(contentView, false);
        show(emptyView, true);
    }

    /** True while the loading view is visible. Useful as a re-entry guard. */
    public boolean isLoading() {
        return loadingView != null && loadingView.getVisibility() == View.VISIBLE;
    }
}
