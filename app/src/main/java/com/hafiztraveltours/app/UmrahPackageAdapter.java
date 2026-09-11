package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class UmrahPackageAdapter extends RecyclerView.Adapter<UmrahPackageAdapter.ViewHolder> {

    public interface OnFavoriteToggleListener {
        void onToggled(UmrahPackage pkg, boolean isFavoriteNow, int position, View itemView);
    }

    private final Context context;
    private List<UmrahPackage> fullList;
    private List<UmrahPackage> filteredList;
    private final OnFavoriteToggleListener toggleListener; // nullable

    public UmrahPackageAdapter(Context context, List<UmrahPackage> items, OnFavoriteToggleListener toggleListener) {
        this.context = context;
        this.fullList = new ArrayList<>(items);
        this.filteredList = new ArrayList<>(items);
        this.toggleListener = toggleListener;
    }

    public void setItems(List<UmrahPackage> items) {
        this.fullList = new ArrayList<>(items);
        this.filteredList = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    public void removeItemAt(int position) {
        if (position >= 0 && position < filteredList.size()) {
            UmrahPackage removed = filteredList.remove(position);
            fullList.remove(removed);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, filteredList.size() - position);
        }
    }

    public void insertItemAt(int position, UmrahPackage pkg) {
        if (position >= 0 && position <= filteredList.size()) {
            filteredList.add(position, pkg);
            fullList.add(pkg);
            notifyItemInserted(position);
            notifyItemRangeChanged(position, filteredList.size() - position);
        }
    }

    /** Filters by package name, case-insensitive. Pass "" to reset. */
    public void filter(String query) {
        String q = query.trim().toLowerCase();
        filteredList.clear();
        if (q.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            for (UmrahPackage pkg : fullList) {
                if (pkg.name.toLowerCase().contains(q)) {
                    filteredList.add(pkg);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_umrah_package, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UmrahPackage pkg = filteredList.get(position);

        holder.name.setText(pkg.getDisplayName());
        String cleanPrice = (pkg.price != null) ? pkg.price.replace("RM", "").replace("rm", "").trim() : "";
        holder.durationPrice.setText(context.getString(
                R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));

        Glide.with(context)
                .load(pkg.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .into(holder.image);

        updateFavoriteIcon(holder.favoriteIcon, pkg);

        holder.favoriteIcon.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            FavoritesManager.handleFavoriteToggle(context, pkg, holder.favoriteIcon, isFavoriteNow -> {
                updateFavoriteIcon(holder.favoriteIcon, pkg);
                if (toggleListener != null) {
                    toggleListener.onToggled(pkg, isFavoriteNow, holder.getBindingAdapterPosition(), holder.itemView);
                }
            });
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PackageDetailActivity.class);
            intent.putExtra(PackageDetailActivity.EXTRA_COLLECTION, pkg.collectionName);
            intent.putExtra(PackageDetailActivity.EXTRA_PACKAGE_ID, pkg.id);
            context.startActivity(intent);
        });
    }

    private void updateFavoriteIcon(ImageView icon, UmrahPackage pkg) {
        boolean isFav = FavoritesManager.isFavorite(context, pkg);
        icon.setColorFilter(isFav ? Color.parseColor("#E91E63") : Color.parseColor("#B0B0B0"));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, favoriteIcon;
        TextView name, durationPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.packageImage);
            name = itemView.findViewById(R.id.packageName);
            durationPrice = itemView.findViewById(R.id.packageDurationPrice);
            favoriteIcon = itemView.findViewById(R.id.packageFavoriteIcon);
        }
    }
}