package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.AlbumDetailsActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlbumsTabFragment extends Fragment {

    private RecyclerView rvAlbums;
    private AlbumsAdapter adapter;
    private List<AlbumItem> albumList = new ArrayList<>();
    private TextView tvEmptyState;
    private View loadingView;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_albums_tab, container, false);

        initViews();
        loadAlbums();

        return view;
    }

    private void initViews() {
        rvAlbums = view.findViewById(R.id.rv_albums);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        loadingView = view.findViewById(R.id.loading_view);

        rvAlbums.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new AlbumsAdapter();
        rvAlbums.setAdapter(adapter);
    }

    private void loadAlbums() {
        showLoading(true);

        new Thread(() -> {
            try {
                List<Song> allSongs = MusicDatabase.getInstance(requireContext())
                        .songDao()
                        .getAllSongs();

                Map<String, AlbumItem> albumMap = new HashMap<>();

                for (Song song : allSongs) {
                    String albumName = song.getAlbum();
                    String artistName = song.getArtist();

                    if (albumName == null || albumName.isEmpty()) {
                        albumName = "Unknown Album";
                    }
                    if (artistName == null || artistName.isEmpty()) {
                        artistName = "Unknown Artist";
                    }

                    String key = albumName + "||" + artistName;

                    if (!albumMap.containsKey(key)) {
                        AlbumItem album = new AlbumItem();
                        album.name = albumName;
                        album.artist = artistName;
                        album.albumArt = song.getAlbumArt();
                        album.songCount = 1;
                        albumMap.put(key, album);
                    } else {
                        AlbumItem album = albumMap.get(key);
                        album.songCount++;
                        if (album.albumArt == null && song.getAlbumArt() != null) {
                            album.albumArt = song.getAlbumArt();
                        }
                    }
                }

                List<AlbumItem> albums = new ArrayList<>(albumMap.values());
                Collections.sort(albums, (a1, a2) -> a1.name.compareToIgnoreCase(a2.name));

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        albumList.clear();
                        albumList.addAll(albums);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        showLoading(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Error loading albums");
                    });
                }
            }
        }).start();
    }

    private void updateEmptyState() {
        if (albumList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvAlbums.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvAlbums.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAlbums();
    }

    private static class AlbumItem {
        String name;
        String artist;
        String albumArt;
        int songCount;
    }

    private class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_album_grid, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AlbumItem album = albumList.get(position);

            holder.tvAlbumName.setText(album.name);
            holder.tvArtistName.setText(album.artist);
            holder.tvSongCount.setText(album.songCount + " " + (album.songCount == 1 ? "song" : "songs"));

            if (album.albumArt != null && !album.albumArt.isEmpty()) {
                Glide.with(requireContext())
                        .load(album.albumArt)
                        .placeholder(R.drawable.default_album_art)
                        .error(R.drawable.default_album_art)
                        .into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art);
            }

            holder.cardAlbum.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), AlbumDetailsActivity.class);
                intent.putExtra("album_name", album.name);
                intent.putExtra("artist_name", album.artist);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return albumList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardAlbum;
            ImageView ivAlbumArt;
            TextView tvAlbumName, tvArtistName, tvSongCount;

            ViewHolder(View itemView) {
                super(itemView);
                cardAlbum = itemView.findViewById(R.id.card_album);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                tvAlbumName = itemView.findViewById(R.id.tv_album_name);
                tvArtistName = itemView.findViewById(R.id.tv_artist_name);
                tvSongCount = itemView.findViewById(R.id.tv_song_count);
            }
        }
    }
}