package com.hafiztraveltours.app.adapters;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.PodcastViewHolder> {

    private final List<Podcast> podcasts;

    public PodcastAdapter(List<Podcast> podcasts) {
        this.podcasts = podcasts != null ? podcasts : new java.util.ArrayList<>();
    }

    @NonNull
    @Override
    public PodcastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_podcast, parent, false);
        return new PodcastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PodcastViewHolder holder, int position) {
        if (podcasts == null || position < 0 || position >= podcasts.size()) return;
        Podcast podcast = podcasts.get(position);
        if (podcast == null) return;

        holder.title.setText(podcast.title != null ? podcast.title : "");

        try {
            if (podcast.videoId != null && !podcast.videoId.trim().isEmpty()) {
                Glide.with(holder.itemView)
                        .load("https://img.youtube.com/vi/" + podcast.videoId.trim() + "/hqdefault.jpg")
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .fallback(R.drawable.bg_image_placeholder)
                        .into(holder.thumbnail);
            } else {
                Glide.with(holder.itemView)
                        .load(R.drawable.bg_image_placeholder)
                        .into(holder.thumbnail);
            }
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            Uri appUri = Uri.parse("vnd.youtube:" + podcast.videoId);
            Uri webUri = Uri.parse("https://www.youtube.com/watch?v=" + podcast.videoId);
            try {
                holder.itemView.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW, appUri));
            } catch (android.content.ActivityNotFoundException e) {
                holder.itemView.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW, webUri));
            }
        });
    }

    @Override
    public int getItemCount() {
        return podcasts != null ? podcasts.size() : 0;
    }

    static class PodcastViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;

        PodcastViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.podcastThumbnail);
            title = itemView.findViewById(R.id.podcastTitle);
        }
    }
}