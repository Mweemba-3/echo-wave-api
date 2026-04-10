package com.example.echo_wave.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.echo_wave.R;
import com.example.echo_wave.ui.activities.ArtistDetailsActivity;

import java.util.List;

public class AllArtistsAdapter extends RecyclerView.Adapter<AllArtistsAdapter.ViewHolder> {
    private List<String> artists;

    public AllArtistsAdapter(List<String> artists) {
        this.artists = artists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String artist = artists.get(position);
        holder.tvName.setText(artist);
        holder.ivImage.setImageResource(R.drawable.default_artist);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ArtistDetailsActivity.class);
            intent.putExtra("artist_name", artist);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return artists.size();
    }

    public void updateData(List<String> newArtists) {
        this.artists = newArtists;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_artist_image);
            tvName = itemView.findViewById(R.id.tv_artist_name);
        }
    }
}