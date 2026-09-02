package com.hafiztraveltours.app;

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
        this.podcasts = podcasts;
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
        Podcast podcast = podcasts.get(position);

        holder.title.setText(podcast.title);

        String thumbnailUrl = "https://img.youtube.com/vi/" + podcast.videoId + "/hqdefault.jpg";
        Glide.with(holder.itemView.getContext())
                .load(thumbnailUrl)
                .into(holder.thumbnail);

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
        return podcasts.size();
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