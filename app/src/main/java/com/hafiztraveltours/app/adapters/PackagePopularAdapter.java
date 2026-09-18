package com.hafiztraveltours.app.adapters;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;

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

import java.util.List;

public class PackagePopularAdapter extends RecyclerView.Adapter<PackagePopularAdapter.ViewHolder> {

    private final Context context;
    private final List<UmrahPackage> items;

    private final int layoutResId;

    public PackagePopularAdapter(Context context, List<UmrahPackage> items) {
        this(context, items, false);
    }

    public PackagePopularAdapter(Context context, List<UmrahPackage> items, boolean featured) {
        this.context = context;
        this.items = items != null ? items : new java.util.ArrayList<>();
        this.layoutResId = featured ? R.layout.item_package_featured : R.layout.item_package_popular;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (items == null || position < 0 || position >= items.size()) return;
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

        String cleanPrice = (pkg.price != null) ? pkg.price.replace("RM", "").replace("rm", "").trim() : "";
        holder.durationPrice.setText(context.getString(
                R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));

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

        // Heart PUNYA listener sendiri - tap sini TIDAK akan propagate ke itemView di bawah
        holder.favoriteIcon.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            FavoritesManager.handleFavoriteToggle(context, pkg, holder.favoriteIcon, isFavoriteNow -> {
                updateFavoriteIcon(holder.favoriteIcon, pkg);
                if (context instanceof android.app.Activity) {
                    BottomNavHelper.updateFavoriteBadge((android.app.Activity) context);
                }
            });
        });

        // itemView punya listener berasingan - buka WebView pakej
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PackageDetailActivity.class);
            intent.putExtra(PackageDetailActivity.EXTRA_COLLECTION, pkg.collectionName);
            intent.putExtra(PackageDetailActivity.EXTRA_PACKAGE_ID, pkg.id);
            context.startActivity(intent);
        });
    }

    private void updateFavoriteIcon(ImageView icon, UmrahPackage pkg) {
        boolean isFav = FavoritesManager.isFavorite(context, pkg);
        icon.setColorFilter(isFav ? Color.parseColor("#E91E63") : Color.parseColor("#FFFFFF"));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
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