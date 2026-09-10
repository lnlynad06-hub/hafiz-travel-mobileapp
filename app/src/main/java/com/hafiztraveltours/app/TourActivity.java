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
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class TourActivity extends AppCompatActivity {

    private final List<UmrahPackage> allPackages = new ArrayList<>();
    private final List<UmrahPackage> popularPackages = new ArrayList<>();

    private View popularSection, availableSection, emptyText;
    private RecyclerView popularRecyclerView, availableRecyclerView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);

        setupBoundToggle();

        popularSection = findViewById(R.id.tourPopularSection);
        availableSection = findViewById(R.id.tourAvailableSection);
        emptyText = findViewById(R.id.tourEmptyText);

        popularRecyclerView = findViewById(R.id.tourPopularRecyclerView);
        availableRecyclerView = findViewById(R.id.tourAvailableRecyclerView);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        availableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        TextInputEditText searchInput = findViewById(R.id.tourSearchInput);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderSections(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        View btnFilter = findViewById(R.id.btnTourFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllPackagesActivity.class);
                intent.putExtra(AllPackagesActivity.EXTRA_OPEN_FILTER, true);
                startActivity(intent);
            });
        }

        BottomNavHelper.setup(this, BottomNavHelper.Tab.TOUR);
        loadPackagesFromApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.updateFavoriteBadge(this);
    }

    private void setupBoundToggle() {
        View inboundTab = findViewById(R.id.tourInboundTab);
        View outboundTab = findViewById(R.id.tourOutboundTab);
        View inboundContainer = findViewById(R.id.tourInboundContainer);
        View outboundContainer = findViewById(R.id.tourOutboundContainer);

        inboundTab.setOnClickListener(v -> {
            inboundContainer.setVisibility(View.VISIBLE);
            outboundContainer.setVisibility(View.GONE);
            inboundTab.setBackgroundResource(R.drawable.bg_button_pink);
            ((android.widget.TextView) inboundTab).setTextColor(getResources().getColor(R.color.white));
            outboundTab.setBackgroundResource(R.drawable.circle_bg_light);
            ((android.widget.TextView) outboundTab).setTextColor(getResources().getColor(R.color.pink_dark));
        });

        outboundTab.setOnClickListener(v -> {
            outboundContainer.setVisibility(View.VISIBLE);
            inboundContainer.setVisibility(View.GONE);
            outboundTab.setBackgroundResource(R.drawable.bg_button_pink);
            ((android.widget.TextView) outboundTab).setTextColor(getResources().getColor(R.color.white));
            inboundTab.setBackgroundResource(R.drawable.circle_bg_light);
            ((android.widget.TextView) inboundTab).setTextColor(getResources().getColor(R.color.pink_dark));
        });
    }

    private void loadPackagesFromApi() {
        ApiClient.getApiService().getPackages("tour", null, null, null).enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.tourShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                allPackages.clear();
                popularPackages.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (UmrahPackage pkg : response.body().data) {
                        pkg.collectionName = "tour_packages";
                        allPackages.add(pkg);
                        if (pkg.isFeatured) {
                            popularPackages.add(pkg);
                        }
                    }
                }
                renderSections("");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.tourShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                allPackages.clear();
                popularPackages.clear();
                renderSections("");
                Toast.makeText(TourActivity.this, "Gagal menyambung ke pelayan backend", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** "Available" papar SEMUA pakej (termasuk yang popular juga - sama macam website). */
    private void renderSections(String query) {
        List<UmrahPackage> filteredPopular = filterByName(popularPackages, query);
        List<UmrahPackage> filteredAvailable = filterByName(allPackages, query);

        popularRecyclerView.setAdapter(new PackagePopularAdapter(this, filteredPopular, true));
        availableRecyclerView.setAdapter(new UmrahPackageAdapter(this, filteredAvailable, null));

        popularSection.setVisibility(filteredPopular.isEmpty() ? View.GONE : View.VISIBLE);
        availableSection.setVisibility(filteredAvailable.isEmpty() ? View.GONE : View.VISIBLE);

        boolean allEmpty = filteredPopular.isEmpty() && filteredAvailable.isEmpty();
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