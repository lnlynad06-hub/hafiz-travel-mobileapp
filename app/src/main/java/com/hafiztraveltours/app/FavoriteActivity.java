package com.hafiztraveltours.app;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FavoriteActivity extends AppCompatActivity {

    private UmrahPackageAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyText;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        findViewById(R.id.favoriteBackButton).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.favoriteRecyclerView);
        emptyText = findViewById(R.id.favoriteEmptyText);
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

    private void refreshList() {
        java.util.List<UmrahPackage> favorites = FavoritesManager.getAllFavorites(this);
        adapter.setItems(favorites);
        emptyText.setVisibility(favorites.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(favorites.isEmpty() ? View.GONE : View.VISIBLE);
    }
}