package com.example.echo_wave.ui.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.MediaPlayerHelper;

import java.util.List;

public class HomeSongAdapter extends RecyclerView.Adapter<HomeSongAdapter.ViewHolder> {
    private List<Song> songs;

    public HomeSongAdapter(List<Song> songs) {
        this.songs = songs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horizontal_song_premium, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (songs == null || position >= songs.size()) return;

        Song song = songs.get(position);
        if (song == null) return;

        holder.tvTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown");
        holder.tvSubtitle.setText(song.getArtist() != null ? song.getArtist() : "Unknown Artist");

        if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(song.getAlbumArt())
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.default_album_art);
        }

        holder.tvBadge.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (song != null) {
                MediaPlayerHelper.getInstance().playSong(v.getContext(), song);
                v.getContext().startActivity(new Intent(v.getContext(), NowPlayingActivity.class));
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void updateData(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle, tvSubtitle, tvBadge;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvBadge = itemView.findViewById(R.id.tv_badge);
        }
    }
}