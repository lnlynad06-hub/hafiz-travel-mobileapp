package com.example.hafiztraveltours;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PackagePopularAdapter extends RecyclerView.Adapter<PackagePopularAdapter.ViewHolder> {

    private final List<Package> items;

    public PackagePopularAdapter(List<Package> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package_popular, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Package item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);

        // Bind gambar drawable ke ImageView
        if (item.imageResId != 0) {
            holder.image.setImageResource(item.imageResId);
        }

        holder.itemView.setOnClickListener(v -> {
            if (item.url == null || item.url.isEmpty()) {
                return;
            }
            Intent intent = new Intent(v.getContext(), WebViewActivity.class);
            intent.putExtra(WebViewActivity.EXTRA_TITLE, item.title);
            intent.putExtra(WebViewActivity.EXTRA_URL, item.url);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView image;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.packageTitle);
            subtitle = itemView.findViewById(R.id.packageSubtitle);
            image = itemView.findViewById(R.id.packageImage);
        }
    }
}