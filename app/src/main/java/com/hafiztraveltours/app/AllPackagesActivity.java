package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.hafiztraveltours.app.network.ApiClient;
import com.hafiztraveltours.app.network.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllPackagesActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_FILTER = "extra_open_filter";

    private final List<UmrahPackage> allPackagesMaster = new ArrayList<>();
    private final List<UmrahPackage> allUmrah = new ArrayList<>();
    private final List<UmrahPackage> allTour = new ArrayList<>();
    private final List<UmrahPackage> popularCombined = new ArrayList<>();

    private FilterCriteria currentFilter = new FilterCriteria();

    private View browseContainer, searchResultsContainer, emptyText, filterActiveBadge;
    private HorizontalScrollView activeFiltersScrollView;
    private LinearLayout activeFiltersContainer;
    private RecyclerView popularRecyclerView, umrahRecyclerView, tourRecyclerView, searchResultsRecyclerView;
    private TextInputEditText searchInput;

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
        filterActiveBadge = findViewById(R.id.filterActiveBadge);
        activeFiltersScrollView = findViewById(R.id.activeFiltersScrollView);
        activeFiltersContainer = findViewById(R.id.activeFiltersContainer);

        popularRecyclerView = findViewById(R.id.allPopularRecyclerView);
        umrahRecyclerView = findViewById(R.id.allUmrahRecyclerView);
        tourRecyclerView = findViewById(R.id.allTourRecyclerView);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        umrahRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tourRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        View btnFilter = findViewById(R.id.btnAllPackagesFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> openFilterBottomSheet());
        }

        searchInput = findViewById(R.id.allPackagesSearchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadAllPackages();
    }

    private void openFilterBottomSheet() {
        PackageFilterBottomSheet sheet = PackageFilterBottomSheet.newInstance(currentFilter, allPackagesMaster);
        sheet.setOnFilterAppliedListener(criteria -> {
            currentFilter = new FilterCriteria(criteria);
            renderActiveFilterChips();
            applyFilterAndSearch();
        });
        sheet.show(getSupportFragmentManager(), "PackageFilterBottomSheet");
    }

    private void loadAllPackages() {
        ApiClient.getApiService().getPackages(null, null, null, null).enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.allPackagesShimmer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
                browseContainer.setVisibility(View.VISIBLE);

                allPackagesMaster.clear();
                allUmrah.clear();
                allTour.clear();
                popularCombined.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (UmrahPackage pkg : response.body().data) {
                        if ("umrah".equalsIgnoreCase(pkg.category)) {
                            pkg.collectionName = "umrah_packages";
                            allUmrah.add(pkg);
                        } else {
                            pkg.collectionName = "tour_packages";
                            allTour.add(pkg);
                        }

                        allPackagesMaster.add(pkg);

                        if (pkg.isFeatured) {
                            popularCombined.add(pkg);
                        }
                    }
                }

                popularRecyclerView.setAdapter(new PackagePopularAdapter(AllPackagesActivity.this, popularCombined, true));
                umrahRecyclerView.setAdapter(new PackagePopularAdapter(AllPackagesActivity.this, allUmrah, false));
                tourRecyclerView.setAdapter(new PackagePopularAdapter(AllPackagesActivity.this, allTour, false));

                if (getIntent().getBooleanExtra(EXTRA_OPEN_FILTER, false)) {
                    openFilterBottomSheet();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.allPackagesShimmer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }
                browseContainer.setVisibility(View.VISIBLE);

                allPackagesMaster.clear();
                allUmrah.clear();
                allTour.clear();
                popularCombined.clear();
                Toast.makeText(AllPackagesActivity.this, "Gagal menyambung ke pelayan backend", Toast.LENGTH_SHORT).show();

                if (getIntent().getBooleanExtra(EXTRA_OPEN_FILTER, false)) {
                    openFilterBottomSheet();
                }
            }
        });
    }

    private void applyFilterAndSearch() {
        String q = (searchInput != null && searchInput.getText() != null)
                ? searchInput.getText().toString().trim().toLowerCase()
                : "";

        boolean isSearchActive = !q.isEmpty();
        boolean isFilterActive = !currentFilter.isDefault();

        if (!isSearchActive && !isFilterActive) {
            browseContainer.setVisibility(View.VISIBLE);
            searchResultsContainer.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            return;
        }

        List<UmrahPackage> filteredResults = new ArrayList<>();
        for (UmrahPackage pkg : allPackagesMaster) {
            boolean matchesCriteria = pkg.matchesCriteria(currentFilter);
            boolean matchesQuery = true;
            if (isSearchActive) {
                String combined = ((pkg.name != null ? pkg.name : "") + " " +
                        (pkg.title != null ? pkg.title : "") + " " +
                        (pkg.destination != null ? pkg.destination : "")).toLowerCase();
                matchesQuery = combined.contains(q);
            }

            if (matchesCriteria && matchesQuery) {
                filteredResults.add(pkg);
            }
        }

        browseContainer.setVisibility(View.GONE);
        searchResultsContainer.setVisibility(filteredResults.isEmpty() ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(filteredResults.isEmpty() ? View.VISIBLE : View.GONE);

        searchResultsRecyclerView.setAdapter(new UmrahPackageAdapter(this, filteredResults, null));
    }

    private void renderActiveFilterChips() {
        activeFiltersContainer.removeAllViews();
        if (currentFilter.isDefault()) {
            activeFiltersScrollView.setVisibility(View.GONE);
            filterActiveBadge.setVisibility(View.GONE);
            return;
        }

        activeFiltersScrollView.setVisibility(View.VISIBLE);
        filterActiveBadge.setVisibility(View.VISIBLE);

        // 1. Category Chip
        if (!"ALL".equalsIgnoreCase(currentFilter.category)) {
            String catLabel = "UMRAH".equalsIgnoreCase(currentFilter.category)
                    ? getString(R.string.filter_cat_umrah)
                    : getString(R.string.filter_cat_tour);
            addFilterChip(getString(R.string.filter_active_tag_category, catLabel), () -> {
                currentFilter.category = "ALL";
                renderActiveFilterChips();
                applyFilterAndSearch();
            });
        }

        // 2. Destination Chip
        if (!"ALL".equalsIgnoreCase(currentFilter.destination)) {
            String destLabel = getDestinationLabel(currentFilter.destination);
            addFilterChip(getString(R.string.filter_active_tag_destination, destLabel), () -> {
                currentFilter.destination = "ALL";
                renderActiveFilterChips();
                applyFilterAndSearch();
            });
        }

        // 3. Price Chip
        if (currentFilter.minPrice > FilterCriteria.DEFAULT_MIN_PRICE || currentFilter.maxPrice < FilterCriteria.DEFAULT_MAX_PRICE) {
            NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
            String minStr = fmt.format((int) currentFilter.minPrice);
            String maxStr = fmt.format((int) currentFilter.maxPrice) + (currentFilter.maxPrice >= 20000 ? "+" : "");
            addFilterChip(getString(R.string.filter_active_tag_price, minStr, maxStr), () -> {
                currentFilter.minPrice = FilterCriteria.DEFAULT_MIN_PRICE;
                currentFilter.maxPrice = FilterCriteria.DEFAULT_MAX_PRICE;
                renderActiveFilterChips();
                applyFilterAndSearch();
            });
        }

        // 4. Clear All Chip
        addClearAllChip();
    }

    private void addFilterChip(String label, Runnable onRemove) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackgroundResource(R.drawable.bg_chip_filter_selected);
        chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dpToPx(8));
        chip.setLayoutParams(params);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        tv.setTextSize(12);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        chip.addView(tv);

        TextView close = new TextView(this);
        close.setText(" ✕");
        close.setTextColor(ContextCompat.getColor(this, R.color.brand_magenta));
        close.setTextSize(12);
        close.setTypeface(null, android.graphics.Typeface.BOLD);
        close.setPadding(dpToPx(4), 0, dpToPx(4), 0);
        chip.addView(close);

        chip.setOnClickListener(v -> onRemove.run());
        activeFiltersContainer.addView(chip);
    }

    private void addClearAllChip() {
        TextView clearChip = new TextView(this);
        clearChip.setText(getString(R.string.filter_clear_all));
        clearChip.setTextColor(Color.parseColor("#E53935"));
        clearChip.setTextSize(12);
        clearChip.setTypeface(null, android.graphics.Typeface.BOLD);
        clearChip.setBackgroundResource(R.drawable.bg_chip_filter_unselected);
        clearChip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dpToPx(8));
        clearChip.setLayoutParams(params);

        clearChip.setOnClickListener(v -> {
            currentFilter.reset();
            renderActiveFilterChips();
            applyFilterAndSearch();
        });
        activeFiltersContainer.addView(clearChip);
    }

    private String getDestinationLabel(String destCode) {
        if (destCode == null) return "";
        switch (destCode.toUpperCase()) {
            case "TURKEY": return getString(R.string.dest_turkey);
            case "KOREA": return getString(R.string.dest_korea);
            case "JAPAN": return getString(R.string.dest_japan);
            case "SAUDI": return getString(R.string.dest_saudi);
            case "EUROPE": return getString(R.string.dest_europe);
            case "VIETNAM": return getString(R.string.dest_vietnam);
            case "CHINA": return getString(R.string.dest_china);
            default: return destCode;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.nav_seamless_fade_in, R.anim.nav_seamless_fade_out);
    }
}