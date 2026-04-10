package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class LibraryAlbumsFragment extends Fragment {

    private static final String TAG = "LibraryAlbumsFragment";

    private RecyclerView rvAlbums;
    private TextView tvEmptyState;
    private View loadingView;

    private AlbumsAdapter adapter;
    private List<AlbumItem> albumList = new ArrayList<>();
    private MusicDatabase musicDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_albums, container, false);

        try {
            initViews(view);
            musicDatabase = MusicDatabase.getInstance(requireContext());
            loadAlbums();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage());
            e.printStackTrace();
        }

        return view;
    }

    private void initViews(View view) {
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
                List<Song> allSongs = musicDatabase.songDao().getAllSongs();

                // Use a map to group by album name + artist (to handle same album name by different artists)
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

                    // Create unique key by combining album name and artist
                    String key = albumName + "||" + artistName;

                    if (!albumMap.containsKey(key)) {
                        AlbumItem album = new AlbumItem();
                        album.name = albumName;
                        album.artist = artistName;
                        album.coverArt = song.getAlbumArt();
                        album.songCount = 1;
                        albumMap.put(key, album);
                    } else {
                        AlbumItem album = albumMap.get(key);
                        album.songCount++;
                        // Use the first non-null album art we find
                        if (album.coverArt == null && song.getAlbumArt() != null) {
                            album.coverArt = song.getAlbumArt();
                        }
                    }
                }

                List<AlbumItem> albums = new ArrayList<>(albumMap.values());

                // Sort albums by name
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
                Log.e(TAG, "Error loading albums: " + e.getMessage());
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
        String coverArt;
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

            holder.tvName.setText(album.name);
            holder.tvArtist.setText(album.artist);
            holder.tvSongCount.setText(album.songCount + " " + (album.songCount == 1 ? "song" : "songs"));

            if (album.coverArt != null && !album.coverArt.isEmpty()) {
                Glide.with(requireContext())
                        .load(album.coverArt)
                        .placeholder(R.drawable.default_album_art)
                        .error(R.drawable.default_album_art)
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.default_album_art);
            }

            holder.cardView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AlbumDetailsActivity.class);
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
            MaterialCardView cardView;
            ImageView ivCover;
            TextView tvName, tvArtist, tvSongCount;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                ivCover = itemView.findViewById(R.id.iv_album_art);
                tvName = itemView.findViewById(R.id.tv_album_name);
                tvArtist = itemView.findViewById(R.id.tv_artist_name);
                tvSongCount = itemView.findViewById(R.id.tv_song_count);
            }
        }
    }
}