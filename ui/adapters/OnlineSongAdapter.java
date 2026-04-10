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
import com.example.echo_wave.models.OnlineSong;

import java.util.List;

public class OnlineSongAdapter extends RecyclerView.Adapter<OnlineSongAdapter.ViewHolder> {

    private List<OnlineSong> songs;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onPlayClick(OnlineSong song, int position);
        void onDownloadClick(OnlineSong song, int position);
    }

    public OnlineSongAdapter(List<OnlineSong> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    public void updateSongs(List<OnlineSong> newSongs) {
        this.songs.clear();
        this.songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    public void updateDownloadState(String songId, boolean downloaded) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId().equals(songId)) {
                songs.get(i).setDownloaded(downloaded);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_online_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OnlineSong song = songs.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(song.getDuration());

        if (song.getThumbnail() != null && !song.getThumbnail().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(song.getThumbnail())
                    .placeholder(R.drawable.default_album_art_small)
                    .error(R.drawable.default_album_art_small)
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.default_album_art_small);
        }

        // Show download indicator if already downloaded
        if (song.isDownloaded()) {
            holder.btnDownload.setVisibility(View.GONE);
            holder.ivDownloaded.setVisibility(View.VISIBLE);
        } else {
            holder.btnDownload.setVisibility(View.VISIBLE);
            holder.ivDownloaded.setVisibility(View.GONE);
        }

        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayClick(song, position);
            }
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownloadClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, btnPlay, btnDownload, ivDownloaded;
        TextView tvTitle, tvArtist, tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnDownload = itemView.findViewById(R.id.btn_download);
            ivDownloaded = itemView.findViewById(R.id.iv_downloaded);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtist = itemView.findViewById(R.id.tv_artist);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }
}