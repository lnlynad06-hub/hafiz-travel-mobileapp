package com.hafiztraveltours.app;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AllPackagesActivity extends AppCompatActivity {

    private final List<UmrahPackage> allUmrah = new ArrayList<>();
    private final List<UmrahPackage> allTour = new ArrayList<>();
    private final List<UmrahPackage> popularCombined = new ArrayList<>();

    private View browseContainer, searchResultsContainer, emptyText;
    private RecyclerView popularRecyclerView, umrahRecyclerView, tourRecyclerView, searchResultsRecyclerView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_packages);

        findViewById(R.id.allPackagesBackButton).setOnClickListener(v -> finish());

        browseContainer = findViewById(R.id.browseContainer);
        searchResultsContainer = findViewById(R.id.searchResultsContainer);
        emptyText = findViewById(R.id.allPackagesEmptyText);

        popularRecyclerView = findViewById(R.id.allPopularRecyclerView);
        umrahRecyclerView = findViewById(R.id.allUmrahRecyclerView);
        tourRecyclerView = findViewById(R.id.allTourRecyclerView);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        umrahRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tourRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        TextInputEditText searchInput = findViewById(R.id.allPackagesSearchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                onSearchChanged(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadAllPackages();
    }

    private void loadAllPackages() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Task<QuerySnapshot> umrahTask = db.collection("umrah_packages").get();
        Task<QuerySnapshot> tourTask = db.collection("tour_packages").get();

        Tasks.whenAllSuccess(umrahTask, tourTask)
                .addOnSuccessListener(results -> {
                    allUmrah.clear();
                    allTour.clear();
                    popularCombined.clear();

                    QuerySnapshot umrahSnapshot = (QuerySnapshot) results.get(0);
                    for (DocumentSnapshot doc : umrahSnapshot.getDocuments()) {
                        UmrahPackage pkg = mapDoc(doc, "umrah_packages");
                        allUmrah.add(pkg);
                        if (Boolean.TRUE.equals(doc.getBoolean("isPopular"))) popularCombined.add(pkg);
                    }

                    QuerySnapshot tourSnapshot = (QuerySnapshot) results.get(1);
                    for (DocumentSnapshot doc : tourSnapshot.getDocuments()) {
                        UmrahPackage pkg = mapDoc(doc, "tour_packages");
                        allTour.add(pkg);
                        if (Boolean.TRUE.equals(doc.getBoolean("isPopular"))) popularCombined.add(pkg);
                    }

                    popularRecyclerView.setAdapter(new PackagePopularAdapter(this, popularCombined, true));
                    umrahRecyclerView.setAdapter(new PackagePopularAdapter(this, allUmrah, false));
                    tourRecyclerView.setAdapter(new PackagePopularAdapter(this, allTour, false));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Gagal muat pakej", Toast.LENGTH_SHORT).show());
    }

    private UmrahPackage mapDoc(DocumentSnapshot doc, String collectionName) {
        UmrahPackage pkg = new UmrahPackage(
                doc.getId(),
                doc.getString("name"),
                doc.getLong("durationDays") != null ? doc.getLong("durationDays").intValue() : 0,
                doc.getLong("nightsCount") != null ? doc.getLong("nightsCount").intValue() : 0,
                doc.getString("price"),
                doc.getString("url"),
                doc.getString("imageUrl"));
        pkg.collectionName = collectionName;
        return pkg;
    }

    private void onSearchChanged(String query) {
        String q = query.trim();

        if (q.isEmpty()) {
            browseContainer.setVisibility(View.VISIBLE);
            searchResultsContainer.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            return;
        }

        List<UmrahPackage> results = new ArrayList<>();
        results.addAll(filterByName(allUmrah, q));
        results.addAll(filterByName(allTour, q));

        browseContainer.setVisibility(View.GONE);
        searchResultsContainer.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);

        searchResultsRecyclerView.setAdapter(new UmrahPackageAdapter(this, results, null));
    }

    private List<UmrahPackage> filterByName(List<UmrahPackage> source, String query) {
        String q = query.toLowerCase();
        List<UmrahPackage> result = new ArrayList<>();
        for (UmrahPackage pkg : source) {
            if (pkg.name != null && pkg.name.toLowerCase().contains(q)) {
                result.add(pkg);
            }
        }
        return result;
    }
}