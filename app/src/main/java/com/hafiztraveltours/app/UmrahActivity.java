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
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    renderSections(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        View btnFilter = findViewById(R.id.btnUmrahFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(this, AllPackagesActivity.class);
                intent.putExtra(AllPackagesActivity.EXTRA_OPEN_FILTER, true);
                startActivity(intent);
            });
        }

        BottomNavHelper.setup(this, BottomNavHelper.Tab.UMRAH);
        loadPackagesFromApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavHelper.updateFavoriteBadge(this);
    }

    private void loadPackagesFromApi() {
        // Panggil Laravel REST API
        ApiClient.getApiService().getPackages("umrah", null, null, null).enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.umrahShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                popularPackages.clear();
                khasPackages.clear();
                ziarahPackages.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    List<UmrahPackage> apiPackages = response.body().data;
                    for (UmrahPackage pkg : apiPackages) {
                        pkg.collectionName = "umrah_packages";
                        if (pkg.isFeatured) {
                            popularPackages.add(pkg);
                        } else if ("ziarah".equalsIgnoreCase(pkg.category) || (pkg.name != null && pkg.name.toLowerCase().contains("ziarah"))) {
                            ziarahPackages.add(pkg);
                        } else {
                            khasPackages.add(pkg);
                        }
                    }
                }
                renderSections("");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.umrahShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                popularPackages.clear();
                khasPackages.clear();
                ziarahPackages.clear();
                renderSections("");
                Toast.makeText(UmrahActivity.this, "Gagal menyambung ke pelayan backend", Toast.LENGTH_SHORT).show();
            }
        });
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
