package com.example.echo_wave.ui.adapters;

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

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private List<Song> queue;
    private Song currentSong;
    private OnQueueItemClickListener listener;

    public interface OnQueueItemClickListener {
        void onItemClick(Song song, int position);
        void onRemoveClick(Song song, int position);
    }

    public QueueAdapter(List<Song> queue, Song currentSong, OnQueueItemClickListener listener) {
        this.queue = queue != null ? queue : new java.util.ArrayList<>();
        this.currentSong = currentSong;
        this.listener = listener;
    }

    public void updateQueue(List<Song> newQueue) {
        this.queue = newQueue != null ? newQueue : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateCurrentSong(Song song) {
        this.currentSong = song;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue_song, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        if (queue == null || position >= queue.size()) return;

        Song song = queue.get(position);
        if (song == null) return;

        // Set title and artist
        holder.tvTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown");
        holder.tvArtist.setText(song.getArtist() != null ? song.getArtist() : "Unknown");

        // Show position number
        holder.tvPosition.setText(String.valueOf(position + 1));

        // Highlight current playing song
        boolean isCurrent = currentSong != null && song.getId() != null &&
                song.getId().equals(currentSong.getId());

        if (isCurrent) {
            holder.tvTitle.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.electric_cyan));
            holder.ivNowPlaying.setVisibility(View.VISIBLE);
            holder.tvPosition.setVisibility(View.GONE);
        } else {
            holder.tvTitle.setTextColor(holder.itemView.getContext()
                    .getColor(R.color.text_primary));
            holder.ivNowPlaying.setVisibility(View.GONE);
            holder.tvPosition.setVisibility(View.VISIBLE);
        }

        // Load album art
        if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(song.getAlbumArt())
                    .placeholder(R.drawable.default_album_art_small)
                    .error(R.drawable.default_album_art_small)
                    .into(holder.ivAlbumArt);
        } else {
            holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
        }

        final int pos = position;
        final Song finalSong = song;

        // Item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && finalSong != null) {
                listener.onItemClick(finalSong, pos);
            }
        });

        // Remove button click listener
        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null && finalSong != null) {
                listener.onRemoveClick(finalSong, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return queue != null ? queue.size() : 0;
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt, ivNowPlaying, btnRemove;
        TextView tvTitle, tvArtist, tvPosition;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            ivNowPlaying = itemView.findViewById(R.id.iv_now_playing);
            btnRemove = itemView.findViewById(R.id.btn_remove);
            tvTitle = itemView.findViewById(R.id.tv_song_title);
            tvArtist = itemView.findViewById(R.id.tv_song_artist);
            tvPosition = itemView.findViewById(R.id.tv_position);
        }
    }
}