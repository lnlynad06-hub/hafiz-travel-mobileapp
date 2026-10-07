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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

public class FavoriteActivity extends BaseActivity {

    private com.hafiztraveltours.app.adapters.PackageCardAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyContainer;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        BottomNavHelper.setup(this, BottomNavHelper.Tab.FAVORITE);

        recyclerView = findViewById(R.id.favoriteRecyclerView);
        emptyContainer = findViewById(R.id.favoriteEmptyContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new com.hafiztraveltours.app.adapters.PackageCardAdapter(this, java.util.Collections.emptyList(),
                com.hafiztraveltours.app.adapters.PackageCardAdapter.CardStyle.LIST,
                (pkg, isFavoriteNow, position, itemView) -> {
                    if (!isFavoriteNow) {
                        handleUnfavoriteWithAnimation(pkg, position, itemView);
                    } else {
                        refreshList();
                    }
                });
        recyclerView.setAdapter(adapter);

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.favoriteSwipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.brand_magenta, R.color.gold_accent, R.color.brand_dark_pink);
            swipeRefresh.setOnRefreshListener(() -> {
                refreshList();
                swipeRefresh.postDelayed(() -> swipeRefresh.setRefreshing(false), 600);
            });
        }
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