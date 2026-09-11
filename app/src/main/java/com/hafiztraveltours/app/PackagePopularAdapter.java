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
        this.items = items;
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
        UmrahPackage pkg = items.get(position);
        holder.name.setText(pkg.getDisplayName());

        String cleanPrice = (pkg.price != null) ? pkg.price.replace("RM", "").replace("rm", "").trim() : "";
        holder.durationPrice.setText(context.getString(
                R.string.package_duration_price, pkg.durationDays, pkg.nightsCount, cleanPrice));

        Glide.with(context)
                .load(pkg.imageUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .into(holder.image);

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
        return items.size();
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