package com.hafiztraveltours.app.views;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PackageFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFilterAppliedListener {
        void onFilterApplied(FilterCriteria criteria);
    }

    private FilterCriteria criteria;
    private List<UmrahPackage> allPackages = new ArrayList<>();
    private OnFilterAppliedListener listener;
    private String scopedCategory = null;

    private ChipGroup chipGroupCategory, chipGroupDestination, chipGroupPricePreset;
    private RangeSlider priceRangeSlider;
    private TextView tvSliderRangeDisplay;
    private MaterialButton btnApplyFilter;

    public static PackageFilterBottomSheet newInstance(FilterCriteria current, List<UmrahPackage> packages) {
        return newInstance(current, packages, null);
    }

    public static PackageFilterBottomSheet newInstance(FilterCriteria current, List<UmrahPackage> packages, String scopedCategory) {
        PackageFilterBottomSheet sheet = new PackageFilterBottomSheet();
        sheet.criteria = new FilterCriteria(current);
        sheet.scopedCategory = scopedCategory;
        if (scopedCategory != null) {
            sheet.criteria.category = scopedCategory;
        }
        if (packages != null) {
            sheet.allPackages = new ArrayList<>(packages);
        }
        return sheet;
    }

    public void setOnFilterAppliedListener(OnFilterAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (criteria == null) {
            criteria = new FilterCriteria();
            if (scopedCategory != null) {
                criteria.category = scopedCategory;
            }
        }
        return inflater.inflate(R.layout.layout_filter_bottom_sheet, container, false);
    }

    private Chip chipCatAll, chipCatUmrah, chipCatTour;
    private Chip chipDestAll, chipDestTurkey, chipDestKorea, chipDestJapan, chipDestSaudi, chipDestEurope, chipDestVietnam, chipDestChina;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipGroupDestination = view.findViewById(R.id.chipGroupDestination);
        chipGroupPricePreset = view.findViewById(R.id.chipGroupPricePreset);
        priceRangeSlider = view.findViewById(R.id.priceRangeSlider);
        tvSliderRangeDisplay = view.findViewById(R.id.tvSliderRangeDisplay);
        btnApplyFilter = view.findViewById(R.id.btnApplyFilter);

        chipCatAll = view.findViewById(R.id.chipCatAll);
        chipCatUmrah = view.findViewById(R.id.chipCatUmrah);
        chipCatTour = view.findViewById(R.id.chipCatTour);

        chipDestAll = view.findViewById(R.id.chipDestAll);
        chipDestTurkey = view.findViewById(R.id.chipDestTurkey);
        chipDestKorea = view.findViewById(R.id.chipDestKorea);
        chipDestJapan = view.findViewById(R.id.chipDestJapan);
        chipDestSaudi = view.findViewById(R.id.chipDestSaudi);
        chipDestEurope = view.findViewById(R.id.chipDestEurope);
        chipDestVietnam = view.findViewById(R.id.chipDestVietnam);
        chipDestChina = view.findViewById(R.id.chipDestChina);

        view.findViewById(R.id.btnResetFilter).setOnClickListener(v -> resetFilters());

        if (scopedCategory != null) {
            View secCat = view.findViewById(R.id.sectionCategoryContainer);
            if (secCat != null) {
                secCat.setVisibility(View.GONE);
            }
        }

        bindCriteriaToViews();
        applyCategoryDestinationRules();
        setupListeners();
        updateMatchingCount();
    }

    private void applyCategoryDestinationRules() {
        boolean isUmrah = "UMRAH".equalsIgnoreCase(criteria.category);

        Chip[] nonUmrahChips = new Chip[] {
            chipDestTurkey, chipDestKorea, chipDestJapan,
            chipDestEurope, chipDestVietnam, chipDestChina
        };

        for (Chip chip : nonUmrahChips) {
            if (chip != null) {
                chip.setEnabled(!isUmrah);
                chip.setAlpha(isUmrah ? 0.35f : 1.0f);
            }
        }

        if (isUmrah) {
            int checkedId = chipGroupDestination.getCheckedChipId();
            if (checkedId == R.id.chipDestTurkey || checkedId == R.id.chipDestKorea ||
                checkedId == R.id.chipDestJapan || checkedId == R.id.chipDestEurope ||
                checkedId == R.id.chipDestVietnam || checkedId == R.id.chipDestChina) {
                chipGroupDestination.check(R.id.chipDestSaudi);
                criteria.destination = "SAUDI";
            }
        }
    }

    private void bindCriteriaToViews() {
        // 1. Category
        if ("UMRAH".equalsIgnoreCase(criteria.category)) {
            chipGroupCategory.check(R.id.chipCatUmrah);
        } else if ("TOUR".equalsIgnoreCase(criteria.category)) {
            chipGroupCategory.check(R.id.chipCatTour);
        } else {
            chipGroupCategory.check(R.id.chipCatAll);
        }

        // 2. Destination
        if ("TURKEY".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestTurkey);
        } else if ("KOREA".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestKorea);
        } else if ("JAPAN".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestJapan);
        } else if ("SAUDI".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestSaudi);
        } else if ("EUROPE".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestEurope);
        } else if ("VIETNAM".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestVietnam);
        } else if ("CHINA".equalsIgnoreCase(criteria.destination)) {
            chipGroupDestination.check(R.id.chipDestChina);
        } else {
            chipGroupDestination.check(R.id.chipDestAll);
        }

        // 3. Slider
        float min = Math.max(0f, criteria.minPrice);
        float max = Math.min(20000f, criteria.maxPrice);
        if (min >= max) max = min + 500f;
        priceRangeSlider.setValues(Arrays.asList(min, max));
        updateSliderDisplay(min, max);
    }

    private void setupListeners() {
        // Category change
        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCatUmrah) {
                criteria.category = "UMRAH";
            } else if (checkedId == R.id.chipCatTour) {
                criteria.category = "TOUR";
            } else {
                criteria.category = "ALL";
            }
            applyCategoryDestinationRules();
            updateMatchingCount();
        });

        // Destination change
        chipGroupDestination.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipDestTurkey) {
                criteria.destination = "TURKEY";
            } else if (checkedId == R.id.chipDestKorea) {
                criteria.destination = "KOREA";
            } else if (checkedId == R.id.chipDestJapan) {
                criteria.destination = "JAPAN";
            } else if (checkedId == R.id.chipDestSaudi) {
                criteria.destination = "SAUDI";
            } else if (checkedId == R.id.chipDestEurope) {
                criteria.destination = "EUROPE";
            } else if (checkedId == R.id.chipDestVietnam) {
                criteria.destination = "VIETNAM";
            } else if (checkedId == R.id.chipDestChina) {
                criteria.destination = "CHINA";
            } else {
                criteria.destination = "ALL";
            }
            updateMatchingCount();
        });

        // Price Presets change
        chipGroupPricePreset.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPriceBelow3k) {
                setSliderRange(0f, 3000f);
            } else if (checkedId == R.id.chipPrice3k6k) {
                setSliderRange(3000f, 6000f);
            } else if (checkedId == R.id.chipPrice6k10k) {
                setSliderRange(6000f, 10000f);
            } else if (checkedId == R.id.chipPriceAbove10k) {
                setSliderRange(10000f, 20000f);
            } else if (checkedId == R.id.chipPriceAll) {
                setSliderRange(0f, 20000f);
            }
            updateMatchingCount();
        });

        // Range Slider change
        priceRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values != null && values.size() >= 2) {
                float min = values.get(0);
                float max = values.get(1);
                criteria.minPrice = min;
                criteria.maxPrice = max;
                updateSliderDisplay(min, max);
                updateMatchingCount();
            }
        });

        // Apply Button
        btnApplyFilter.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFilterApplied(criteria);
            }
            dismiss();
        });
    }

    private void setSliderRange(float min, float max) {
        criteria.minPrice = min;
        criteria.maxPrice = max;
        priceRangeSlider.setValues(Arrays.asList(min, max));
        updateSliderDisplay(min, max);
    }

    private void updateSliderDisplay(float min, float max) {
        NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
        String minStr = "RM" + fmt.format((int) min);
        String maxStr = "RM" + fmt.format((int) max) + (max >= 20000f ? "+" : "");
        tvSliderRangeDisplay.setText(minStr + " - " + maxStr);
    }

    private void updateMatchingCount() {
        int count = 0;
        if (allPackages != null) {
            for (UmrahPackage pkg : allPackages) {
                if (pkg.matchesCriteria(criteria)) {
                    count++;
                }
            }
        }
        btnApplyFilter.setText(getString(R.string.filter_apply_button, count));
    }

    private void resetFilters() {
        criteria.reset();
        if (scopedCategory != null) {
            criteria.category = scopedCategory;
        }
        bindCriteriaToViews();
        applyCategoryDestinationRules();
        updateMatchingCount();
    }
}
