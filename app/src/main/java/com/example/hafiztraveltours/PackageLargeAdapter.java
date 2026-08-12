package com.example.hafiztraveltours;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PackageLargeAdapter extends RecyclerView.Adapter<PackageLargeAdapter.ViewHolder> {

    private final List<Package> items;

    public PackageLargeAdapter(List<Package> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_package_large, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Package item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        // TODO: when this item is clicked, open the Package Detail screen
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.packageTitle);
            subtitle = itemView.findViewById(R.id.packageSubtitle);
        }
    }
}
