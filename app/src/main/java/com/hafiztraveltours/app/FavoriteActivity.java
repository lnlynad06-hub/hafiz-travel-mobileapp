package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

        setupBottomNav();

        recyclerView = findViewById(R.id.favoriteRecyclerView);
        emptyContainer = findViewById(R.id.favoriteEmptyContainer);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UmrahPackageAdapter(this, java.util.Collections.emptyList(),
                (pkg, isFavoriteNow) -> {
                    // Unfavorite dalam page ni sendiri -> refresh terus supaya hilang dari senarai
                    if (!isFavoriteNow) refreshList();
                });
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(); // refresh setiap kali page ni dibuka semula (contoh lepas favorite dari UmrahActivity)
    }

    /**
     * Same fixed bottom nav as MainActivity/UmrahActivity/TourActivity. This
     * page IS the Favorite tab, so navFavorite is just shown as the active
     * tab (no click action needed) while the other tabs navigate away and
     * finish() this activity.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());

        findViewById(R.id.navUmrah).setOnClickListener(v -> {
            startActivity(new Intent(this, UmrahActivity.class));
            finish();
        });

        findViewById(R.id.navTour).setOnClickListener(v -> {
            startActivity(new Intent(this, TourActivity.class));
            finish();
        });
    }

    private void refreshList() {
        java.util.List<UmrahPackage> favorites = FavoritesManager.getAllFavorites(this);
        adapter.setItems(favorites);
        emptyContainer.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(favorites.isEmpty() ? View.GONE : View.VISIBLE);
    }
}