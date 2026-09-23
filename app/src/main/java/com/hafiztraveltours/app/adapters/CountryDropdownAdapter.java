package com.hafiztraveltours.app.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hafiztraveltours.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reusable dropdown adapter for Southeast Asian country selection across Edit Profile.
 * Displays country flag, readable country name, and a clear selected state.
 */
public class CountryDropdownAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> allCountries;
    private String selectedCountry = "";

    public CountryDropdownAdapter(@NonNull Context context, @NonNull String[] countries) {
        super(context, R.layout.item_country_dropdown, new ArrayList<>(Arrays.asList(countries)));
        this.context = context;
        this.allCountries = new ArrayList<>(Arrays.asList(countries));
    }

    public void setSelectedCountry(String country) {
        this.selectedCountry = country != null ? country.trim() : "";
        notifyDataSetChanged();
    }

    public String getSelectedCountry() {
        return selectedCountry;
    }

    /**
     * Resolves the Southeast Asian country flag for both English and Bahasa Melayu names.
     */
    public static String getFlagForCountry(String countryName) {
        if (countryName == null) return "🏳️";
        String lower = countryName.toLowerCase().trim();
        if (lower.contains("malaysia")) return "🇲🇾";
        if (lower.contains("singapore") || lower.contains("singapura")) return "🇸🇬";
        if (lower.contains("indonesia")) return "🇮🇩";
        if (lower.contains("thailand")) return "🇹🇭";
        if (lower.contains("brunei")) return "🇧🇳";
        if (lower.contains("philippines") || lower.contains("filipina")) return "🇵🇭";
        if (lower.contains("vietnam")) return "🇻🇳";
        if (lower.contains("cambodia") || lower.contains("kemboja")) return "🇰🇭";
        if (lower.contains("laos")) return "🇱🇦";
        if (lower.contains("myanmar")) return "🇲🇲";
        if (lower.contains("timor")) return "🇹🇱";
        return "🏳️";
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = LayoutInflater.from(context).inflate(R.layout.item_country_dropdown, parent, false);
        }

        String country = getItem(position);
        if (country == null) country = "";

        TextView tvFlag = row.findViewById(R.id.tvCountryFlag);
        TextView tvName = row.findViewById(R.id.tvCountryName);
        ImageView ivCheck = row.findViewById(R.id.ivCountrySelected);

        if (tvFlag != null) {
            tvFlag.setText(getFlagForCountry(country));
        }

        boolean isSelected = !selectedCountry.isEmpty() &&
                (country.equalsIgnoreCase(selectedCountry) ||
                 selectedCountry.toLowerCase().contains(country.toLowerCase()) ||
                 country.toLowerCase().contains(selectedCountry.toLowerCase()));

        if (tvName != null) {
            tvName.setText(country);
            if (isSelected) {
                tvName.setTextColor(ContextCompat.getColor(context, R.color.pink_dark));
                tvName.setTypeface(null, Typeface.BOLD);
            } else {
                tvName.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
                tvName.setTypeface(null, Typeface.NORMAL);
            }
        }

        if (ivCheck != null) {
            ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }

        if (isSelected) {
            row.setBackgroundResource(R.drawable.bg_country_item_selected);
        } else {
            row.setBackgroundResource(android.R.color.transparent);
        }

        return row;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = allCountries;
                results.count = allCountries.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}
