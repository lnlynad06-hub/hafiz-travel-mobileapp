package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UmrahActivity extends AppCompatActivity {

    private final List<UmrahPackage> popularPackages = new ArrayList<>();
    private final List<UmrahPackage> khasPackages = new ArrayList<>();
    private final List<UmrahPackage> ziarahPackages = new ArrayList<>();

    private View popularSection, khasSection, ziarahSection, emptyText;
    private RecyclerView popularRecyclerView, khasRecyclerView, ziarahRecyclerView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_umrah);

        setupBottomNav();

        popularSection = findViewById(R.id.umrahPopularSection);
        khasSection = findViewById(R.id.umrahKhasSection);
        ziarahSection = findViewById(R.id.umrahZiarahSection);
        emptyText = findViewById(R.id.umrahEmptyText);

        popularRecyclerView = findViewById(R.id.umrahPopularRecyclerView);
        khasRecyclerView = findViewById(R.id.umrahKhasRecyclerView);
        ziarahRecyclerView = findViewById(R.id.umrahZiarahRecyclerView);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        khasRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ziarahRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        TextInputEditText searchInput = findViewById(R.id.umrahSearchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderSections(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadPackagesFromFirestore();
    }

    /**
     * Same fixed bottom nav as MainActivity. This page IS the Umrah tab, so
     * navUmrah is just shown as the active tab (no click action needed) while
     * the other tabs navigate away and finish() this activity - matches how
     * MainActivity does NOT keep stacking activities when switching tabs.
     */
    private void setupBottomNav() {
        findViewById(R.id.navHome).setOnClickListener(v -> finish());

        findViewById(R.id.navTour).setOnClickListener(v -> {
            startActivity(new Intent(this, TourActivity.class));
            finish();
        });

        findViewById(R.id.navFavorite).setOnClickListener(v -> {
            startActivity(new Intent(this, FavoriteActivity.class));
            finish();
        });
    }

    private void loadPackagesFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("umrah_packages")
                .get()
                .addOnSuccessListener(snapshot -> {
                    popularPackages.clear();
                    khasPackages.clear();
                    ziarahPackages.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        UmrahPackage pkg = new UmrahPackage(
                                doc.getId(),
                                doc.getString("name"),
                                doc.getLong("durationDays") != null ? doc.getLong("durationDays").intValue() : 0,
                                doc.getLong("nightsCount") != null ? doc.getLong("nightsCount").intValue() : 0,
                                doc.getString("price"),
                                doc.getString("url"),
                                doc.getString("imageUrl"));

                        String category = doc.getString("category");
                        pkg.collectionName = "umrah_packages";
                        if ("popular".equals(category)) {
                            popularPackages.add(pkg);
                        } else if ("ziarah".equals(category)) {
                            ziarahPackages.add(pkg);
                        } else {
                            khasPackages.add(pkg); // default fallback kalau category tak set
                        }
                    }

                    renderSections("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal muat pakej Umrah. Sila cuba lagi.", Toast.LENGTH_SHORT).show());
    }

    /** Filter setiap seksyen ikut nama pakej, sembunyi seksyen yang kosong selepas filter. */
    private void renderSections(String query) {
        List<UmrahPackage> filteredPopular = filterByName(popularPackages, query);
        List<UmrahPackage> filteredKhas = filterByName(khasPackages, query);
        List<UmrahPackage> filteredZiarah = filterByName(ziarahPackages, query);

        popularRecyclerView.setAdapter(new PackagePopularAdapter(this, filteredPopular, true));
        khasRecyclerView.setAdapter(new PackagePopularAdapter(this, filteredKhas));
        ziarahRecyclerView.setAdapter(new PackagePopularAdapter(this, filteredZiarah));

        popularSection.setVisibility(filteredPopular.isEmpty() ? View.GONE : View.VISIBLE);
        khasSection.setVisibility(filteredKhas.isEmpty() ? View.GONE : View.VISIBLE);
        ziarahSection.setVisibility(filteredZiarah.isEmpty() ? View.GONE : View.VISIBLE);

        boolean allEmpty = filteredPopular.isEmpty() && filteredKhas.isEmpty() && filteredZiarah.isEmpty();
        emptyText.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
    }

    private List<UmrahPackage> filterByName(List<UmrahPackage> source, String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) return new ArrayList<>(source);

        List<UmrahPackage> result = new ArrayList<>();
        for (UmrahPackage pkg : source) {
            if (pkg.name != null && pkg.name.toLowerCase().contains(q)) {
                result.add(pkg);
            }
        }
        return result;
    }
}
