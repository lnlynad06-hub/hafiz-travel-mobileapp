package com.hafiztraveltours.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

public class FavoriteActivity extends AppCompatActivity {

    private UmrahPackageAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyContainer;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        BottomNavHelper.setup(this, BottomNavHelper.Tab.FAVORITE);

        recyclerView = findViewById(R.id.favoriteRecyclerView);
        emptyContainer = findViewById(R.id.favoriteEmptyContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UmrahPackageAdapter(this, java.util.Collections.emptyList(),
                (pkg, isFavoriteNow, position, itemView) -> {
                    if (!isFavoriteNow) {
                        handleUnfavoriteWithAnimation(pkg, position, itemView);
                    }
                });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        BottomNavHelper.updateFavoriteBadge(this);
    }

    private void handleUnfavoriteWithAnimation(UmrahPackage pkg, int position, View itemView) {
        if (itemView != null) {
            itemView.animate()
                    .translationX(-itemView.getWidth() * 0.9f)
                    .alpha(0f)
                    .scaleY(0.8f)
                    .setDuration(220)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(() -> {
                        itemView.setTranslationX(0);
                        itemView.setAlpha(1f);
                        itemView.setScaleY(1f);
                        adapter.removeItemAt(position);
                        BottomNavHelper.updateFavoriteBadge(FavoriteActivity.this);
                        checkEmptyState();
                    })
                    .start();
        } else {
            adapter.removeItemAt(position);
            BottomNavHelper.updateFavoriteBadge(FavoriteActivity.this);
            checkEmptyState();
        }

        showUndoSnackbar(pkg, position);
    }

    private void showUndoSnackbar(UmrahPackage pkg, int position) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                getString(R.string.removed_from_favorites),
                Snackbar.LENGTH_LONG
        );

        snackbar.setAction(getString(R.string.undo), v -> {
            FavoritesManager.toggleFavorite(FavoriteActivity.this, pkg);
            adapter.insertItemAt(position, pkg);
            BottomNavHelper.updateFavoriteBadge(FavoriteActivity.this);
            checkEmptyState();
            recyclerView.smoothScrollToPosition(position);
        });

        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.brand_magenta));

        View snackView = snackbar.getView();
        snackView.setBackgroundResource(R.drawable.bg_snackbar_luxury);
        if (snackView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackView.getLayoutParams();
            params.setMargins(params.leftMargin + 36, params.topMargin, params.rightMargin + 36, params.bottomMargin + 200);
            snackView.setLayoutParams(params);
        }
        snackbar.show();
    }

    private void checkEmptyState() {
        boolean isEmpty = adapter.getItemCount() == 0;
        if (isEmpty) {
            emptyContainer.setAlpha(0f);
            emptyContainer.setVisibility(View.VISIBLE);
            emptyContainer.animate().alpha(1f).setDuration(250).start();
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void refreshList() {
        com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.favoriteShimmerContainer);
        if (shimmer != null) {
            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
        }
        java.util.List<UmrahPackage> favorites = FavoritesManager.getAllFavorites(this);
        adapter.setItems(favorites);
        checkEmptyState();
    }
}