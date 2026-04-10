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
import com.example.echo_wave.models.AlbumItem;
import com.example.echo_wave.ui.activities.AlbumDetailsActivity;

import java.util.List;

public class AllAlbumsAdapter extends RecyclerView.Adapter<AllAlbumsAdapter.ViewHolder> {
    private List<AlbumItem> albums;

    public AllAlbumsAdapter(List<AlbumItem> albums) {
        this.albums = albums;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlbumItem album = albums.get(position);

        holder.tvName.setText(album.getName());
        holder.tvArtist.setText(album.getArtist());

        if (album.getAlbumArt() != null && !album.getAlbumArt().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(album.getAlbumArt())
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.default_album_art);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AlbumDetailsActivity.class);
            intent.putExtra("album_name", album.getName());
            intent.putExtra("artist_name", album.getArtist());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public void updateData(List<AlbumItem> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvArtist;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_album_image);
            tvName = itemView.findViewById(R.id.tv_album_name);
            tvArtist = itemView.findViewById(R.id.tv_album_artist);
        }
    }
}