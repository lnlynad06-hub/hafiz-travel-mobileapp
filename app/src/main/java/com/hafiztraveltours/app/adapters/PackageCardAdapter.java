package com.hafiztraveltours.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.UmrahPackage;
import com.hafiztraveltours.app.ui.PackageDetailActivity;
import com.hafiztraveltours.app.utils.BottomNavHelper;
import com.hafiztraveltours.app.utils.FavoritesManager;
import com.hafiztraveltours.app.utils.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Single package-card adapter (M1). Replaces the near-duplicate
 * PackagePopularAdapter + UmrahPackageAdapter with one binding path.
 *
 * <p>Visual variants stay as separate layouts (same view IDs) selected by
 * {@link CardStyle}; per-style differences are preserved exactly:
 * favorite-off tint (#FFFFFF on popular/featured, #B0B0B0 on list) and
 * favorite-toggle follow-up (listener when present, else bottom-nav badge refresh).
 */
public class PackageCardAdapter extends RecyclerView.Adapter<PackageCardAdapter.ViewHolder> {

    public interface OnFavoriteToggleListener {
        void onToggled(UmrahPackage pkg, boolean isFavoriteNow, int position, View itemView);
    }

    public enum CardStyle {
        POPULAR(R.layout.item_package_popular, "#FFFFFF"),
        FEATURED(R.layout.item_package_featured, "#FFFFFF"),
        LIST(R.layout.item_umrah_package, "#B0B0B0");

        final int layoutResId;
        final String favoriteOffColor;

        CardStyle(int layoutResId, String favoriteOffColor) {
            this.layoutResId = layoutResId;
            this.favoriteOffColor = favoriteOffColor;
        }
    }

    private final Context context;
    private final List<UmrahPackage> items = new ArrayList<>();
    private final CardStyle style;
    private final OnFavoriteToggleListener toggleListener; // nullable

    public PackageCardAdapter(Context context, List<UmrahPackage> items) {
        this(context, items, CardStyle.POPULAR, null);
    }

    public PackageCardAdapter(Context context, List<UmrahPackage> items, boolean featured) {
        this(context, items, featured ? CardStyle.FEATURED : CardStyle.POPULAR, null);
    }

    public PackageCardAdapter(Context context, List<UmrahPackage> items,
                              CardStyle style, OnFavoriteToggleListener toggleListener) {
        this.context = context;
        if (items != null) this.items.addAll(items);
        this.style = style != null ? style : CardStyle.POPULAR;
        this.toggleListener = toggleListener;
    }

    public void setItems(List<UmrahPackage> newItems) {
        this.items.clear();
        if (newItems != null) this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void removeItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size() - position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(style.layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= items.size()) return;
        UmrahPackage pkg = items.get(position);
        if (pkg == null) return;

        if (holder.category != null) {
            if (pkg.category != null && !pkg.category.trim().isEmpty()) {
                holder.category.setVisibility(View.VISIBLE);
                holder.category.setText(pkg.category.trim());
            } else {
                holder.category.setVisibility(View.GONE);
            }
        }

        holder.name.setText(pkg.getDisplayName());
        holder.durationPrice.setText(context.getString(
                R.string.package_duration_price, pkg.durationDays, pkg.nightsCount,
                MoneyFormat.numericString(pkg.price)));

        if (holder.hotelDistance != null) {
            String hotelDist = pkg.getRawHotelDistance();
            if (hotelDist != null && !hotelDist.trim().isEmpty()) {
                if (!hotelDist.toLowerCase().contains("masjid")) {
                    hotelDist = context.getString(R.string.hotel_distance_to, hotelDist);
                }
                holder.hotelDistance.setVisibility(View.VISIBLE);
                holder.hotelDistance.setText(hotelDist);
            } else {
                holder.hotelDistance.setVisibility(View.GONE);
            }
        }

        if (context instanceof android.app.Activity) {
            android.app.Activity act = (android.app.Activity) context;
            if (act.isFinishing() || act.isDestroyed()) return;
        }

        try {
            Glide.with(holder.itemView)
                    .load(pkg.imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .fallback(R.drawable.bg_image_placeholder)
                    .into(holder.image);
        } catch (Exception ignored) {}

        updateFavoriteIcon(holder.favoriteIcon, pkg);

        holder.favoriteIcon.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.HapticUtil.tap(v);
            FavoritesManager.handleFavoriteToggle(context, pkg, holder.favoriteIcon, isFavoriteNow -> {
                updateFavoriteIcon(holder.favoriteIcon, pkg);
                if (toggleListener != null) {
                    toggleListener.onToggled(pkg, isFavoriteNow, holder.getBindingAdapterPosition(), holder.itemView);
                } else if (context instanceof android.app.Activity) {
                    BottomNavHelper.updateFavoriteBadge((android.app.Activity) context);
                }
            });
        });

        holder.itemView.setOnClickListener(v -> {
            com.hafiztraveltours.app.utils.Navigator.openPackage(
                    context, pkg.collectionName, pkg.id);
        });
    }

    private void updateFavoriteIcon(ImageView icon, UmrahPackage pkg) {
        boolean isFav = FavoritesManager.isFavorite(context, pkg);
        icon.setColorFilter(isFav ? Color.parseColor("#E91E63") : Color.parseColor(style.favoriteOffColor));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, favoriteIcon;
        TextView category, name, durationPrice, hotelDistance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.packageImage);
            category = itemView.findViewById(R.id.packageCategory);
            name = itemView.findViewById(R.id.packageName);
            durationPrice = itemView.findViewById(R.id.packageDurationPrice);
            hotelDistance = itemView.findViewById(R.id.packageHotelDistance);
            favoriteIcon = itemView.findViewById(R.id.packageFavoriteIcon);
        }
    }
}
