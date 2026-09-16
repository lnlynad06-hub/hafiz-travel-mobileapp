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
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
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

public class TourActivity extends AppCompatActivity {

    private final List<UmrahPackage> allTourMaster = new ArrayList<>();
    private final List<UmrahPackage> popularPackages = new ArrayList<>();

    private FilterCriteria currentFilter = new FilterCriteria();

    private View popularSection, availableSection, emptyText, filterActiveBadge;
    private View tourBrowseContainer, tourSearchResultsContainer;
    private TextView tvTourResultsCount;
    private HorizontalScrollView activeFiltersScrollView;
    private LinearLayout activeFiltersContainer;
    private RecyclerView popularRecyclerView, availableRecyclerView, searchResultsRecyclerView;
    private TextInputEditText searchInput;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tour);

        currentFilter.category = "TOUR";

        setupBoundToggle();

        tourBrowseContainer = findViewById(R.id.tourBrowseContainer);
        tourSearchResultsContainer = findViewById(R.id.tourSearchResultsContainer);
        tvTourResultsCount = findViewById(R.id.tvTourResultsCount);
        emptyText = findViewById(R.id.tourEmptyText);
        filterActiveBadge = findViewById(R.id.filterActiveBadgeTour);
        activeFiltersScrollView = findViewById(R.id.activeFiltersScrollViewTour);
        activeFiltersContainer = findViewById(R.id.activeFiltersContainerTour);

        popularSection = findViewById(R.id.tourPopularSection);
        availableSection = findViewById(R.id.tourAvailableSection);

        popularRecyclerView = findViewById(R.id.tourPopularRecyclerView);
        availableRecyclerView = findViewById(R.id.tourAvailableRecyclerView);
        searchResultsRecyclerView = findViewById(R.id.tourSearchResultsRecyclerView);

        popularRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        availableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchInput = findViewById(R.id.tourSearchInput);
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilterAndSearch();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        View btnFilter = findViewById(R.id.btnTourFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> openFilterBottomSheet());
        }

        BottomNavHelper.setup(this, BottomNavHelper.Tab.NONE);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.tourSwipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.brand_magenta, R.color.gold_accent, R.color.brand_dark_pink);
            swipeRefresh.setOnRefreshListener(this::loadPackagesFromApi);
        }
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

        if (inboundTab != null && outboundTab != null && inboundContainer != null && outboundContainer != null) {
            inboundTab.setOnClickListener(v -> {
                inboundContainer.setVisibility(View.VISIBLE);
                outboundContainer.setVisibility(View.GONE);
                inboundTab.setBackgroundResource(R.drawable.bg_button_pink);
                ((TextView) inboundTab).setTextColor(ContextCompat.getColor(this, R.color.white));
                outboundTab.setBackgroundResource(R.drawable.circle_bg_light);
                ((TextView) outboundTab).setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            });

            outboundTab.setOnClickListener(v -> {
                outboundContainer.setVisibility(View.VISIBLE);
                inboundContainer.setVisibility(View.GONE);
                outboundTab.setBackgroundResource(R.drawable.bg_button_pink);
                ((TextView) outboundTab).setTextColor(ContextCompat.getColor(this, R.color.white));
                inboundTab.setBackgroundResource(R.drawable.circle_bg_light);
                ((TextView) inboundTab).setTextColor(ContextCompat.getColor(this, R.color.pink_dark));
            });
        }
    }

    private void openFilterBottomSheet() {
        PackageFilterBottomSheet sheet = PackageFilterBottomSheet.newInstance(currentFilter, allTourMaster, "TOUR");
        sheet.setOnFilterAppliedListener(criteria -> {
            currentFilter = new FilterCriteria(criteria);
            currentFilter.category = "TOUR";
            renderActiveFilterChips();
            applyFilterAndSearch();
        });
        sheet.show(getSupportFragmentManager(), "PackageFilterBottomSheet");
    }

    private void loadPackagesFromApi() {
        ApiClient.getApiService().getPackages("tour", null, null, null).enqueue(new Callback<ApiResponse<List<UmrahPackage>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<UmrahPackage>>> call, Response<ApiResponse<List<UmrahPackage>>> response) {
                androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.tourSwipeRefresh);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.tourShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                allTourMaster.clear();
                popularPackages.clear();

                if (response.isSuccessful() && response.body() != null && response.body().data != null) {
                    for (UmrahPackage pkg : response.body().data) {
                        pkg.collectionName = "tour_packages";
                        allTourMaster.add(pkg);
                        if (pkg.isFeatured) {
                            popularPackages.add(pkg);
                        }
                    }
                }

                popularRecyclerView.setAdapter(new PackagePopularAdapter(TourActivity.this, popularPackages, true));
                availableRecyclerView.setAdapter(new UmrahPackageAdapter(TourActivity.this, allTourMaster, null));

                popularSection.setVisibility(popularPackages.isEmpty() ? View.GONE : View.VISIBLE);
                availableSection.setVisibility(allTourMaster.isEmpty() ? View.GONE : View.VISIBLE);

                applyFilterAndSearch();
            }

            @Override
            public void onFailure(Call<ApiResponse<List<UmrahPackage>>> call, Throwable t) {
                androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh = findViewById(R.id.tourSwipeRefresh);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                com.facebook.shimmer.ShimmerFrameLayout shimmer = findViewById(R.id.tourShimmerContainer);
                if (shimmer != null) {
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                }

                allTourMaster.clear();
                popularPackages.clear();
                applyFilterAndSearch();
                Toast.makeText(TourActivity.this, getString(R.string.err_server_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isFilterActive() {
        return !"ALL".equalsIgnoreCase(currentFilter.destination) ||
                currentFilter.minPrice > FilterCriteria.DEFAULT_MIN_PRICE ||
                currentFilter.maxPrice < FilterCriteria.DEFAULT_MAX_PRICE;
    }

    private void applyFilterAndSearch() {
        String q = (searchInput != null && searchInput.getText() != null)
                ? searchInput.getText().toString().trim().toLowerCase()
                : "";

        boolean isSearchActive = !q.isEmpty();
        boolean isFilterActive = isFilterActive();

        if (!isSearchActive && !isFilterActive) {
            tourBrowseContainer.setVisibility(View.VISIBLE);
            tourSearchResultsContainer.setVisibility(View.GONE);
            emptyText.setVisibility(allTourMaster.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<UmrahPackage> filteredResults = new ArrayList<>();
        for (UmrahPackage pkg : allTourMaster) {
            boolean matchesCriteria = pkg.matchesCriteria(currentFilter);
            boolean matchesQuery = true;
            if (isSearchActive) {
                String combined = ((pkg.name != null ? pkg.name : "") + " " +
                        (pkg.title != null ? pkg.title : "") + " " +
                        (pkg.destination != null ? pkg.destination : "") + " " +
                        (pkg.category != null ? pkg.category : "") + " " +
                        (pkg.summary != null ? pkg.summary : "")).toLowerCase();
                matchesQuery = combined.contains(q);
            }

            if (matchesCriteria && matchesQuery) {
                filteredResults.add(pkg);
            }
        }

        tourBrowseContainer.setVisibility(View.GONE);
        tourSearchResultsContainer.setVisibility(filteredResults.isEmpty() ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(filteredResults.isEmpty() ? View.VISIBLE : View.GONE);

        if (tvTourResultsCount != null) {
            String countText = getString(R.string.results_packages_format, filteredResults.size());
            tvTourResultsCount.setText(countText);
        }

        searchResultsRecyclerView.setAdapter(new UmrahPackageAdapter(this, filteredResults, null));
    }

    private void renderActiveFilterChips() {
        activeFiltersContainer.removeAllViews();
        if (!isFilterActive()) {
            activeFiltersScrollView.setVisibility(View.GONE);
            filterActiveBadge.setVisibility(View.GONE);
            return;
        }

        activeFiltersScrollView.setVisibility(View.VISIBLE);
        filterActiveBadge.setVisibility(View.VISIBLE);

        // 1. Destination Chip
        if (!"ALL".equalsIgnoreCase(currentFilter.destination)) {
            String destLabel = getDestinationLabel(currentFilter.destination);
            addFilterChip(getString(R.string.filter_active_tag_destination, destLabel), () -> {
                currentFilter.destination = "ALL";
                renderActiveFilterChips();
                applyFilterAndSearch();
            });
        }

        // 2. Price Chip
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

        // 3. Clear All Chip
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
            currentFilter.destination = "ALL";
            currentFilter.minPrice = FilterCriteria.DEFAULT_MIN_PRICE;
            currentFilter.maxPrice = FilterCriteria.DEFAULT_MAX_PRICE;
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
}
