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
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.ui.activities.PlaylistActivity;

import java.util.List;

public class AllPlaylistsAdapter extends RecyclerView.Adapter<AllPlaylistsAdapter.ViewHolder> {
    private List<Playlist> playlists;

    public AllPlaylistsAdapter(List<Playlist> playlists) {
        this.playlists = playlists;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);

        holder.tvName.setText(playlist.getName());
        holder.tvCount.setText(playlist.getSongCount() + " songs");
        holder.ivImage.setImageResource(R.drawable.default_playlist_art);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PlaylistActivity.class);
            intent.putExtra("playlist_id", playlist.getId());
            intent.putExtra("playlist_name", playlist.getName());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void updateData(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvCount;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_playlist_image);
            tvName = itemView.findViewById(R.id.tv_playlist_name);
            tvCount = itemView.findViewById(R.id.tv_playlist_count);
        }
    }
}