package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.ArtistDetailsActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistsTabFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArtistsAdapter adapter;
    private List<ArtistItem> artistList = new ArrayList<>();
    private MusicDatabase database;
    private View loadingView;
    private TextView tvEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artists_tab, container, false);

        database = MusicDatabase.getInstance(requireContext());

        initViews(view);
        loadArtists();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_artists);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        loadingView = view.findViewById(R.id.loading_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArtistsAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadArtists() {
        showLoading(true);

        new Thread(() -> {
            try {
                List<Song> allSongs = database.songDao().getAllSongs();

                // Extract unique artist names
                Set<String> artistSet = new HashSet<>();
                for (Song song : allSongs) {
                    if (song.getArtist() != null && !song.getArtist().isEmpty()) {
                        artistSet.add(song.getArtist());
                    }
                }

                List<ArtistItem> artists = new ArrayList<>();
                for (String artistName : artistSet) {
                    List<Song> artistSongs = database.songDao().getSongsByArtist(artistName);

                    String coverArt = null;
                    int songCount = artistSongs.size();
                    int albumCount = 0;

                    if (!artistSongs.isEmpty()) {
                        coverArt = artistSongs.get(0).getAlbumArt();

                        // Count unique albums
                        Set<String> uniqueAlbums = new HashSet<>();
                        for (Song song : artistSongs) {
                            if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                                uniqueAlbums.add(song.getAlbum());
                            }
                        }
                        albumCount = uniqueAlbums.size();
                    }

                    artists.add(new ArtistItem(artistName, songCount, albumCount, coverArt));
                }

                Collections.sort(artists, (a1, a2) -> a1.name.compareToIgnoreCase(a2.name));

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        artistList.clear();
                        artistList.addAll(artists);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        showLoading(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> showLoading(false));
                }
            }
        }).start();
    }

    private void updateEmptyState() {
        if (artistList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
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
        loadArtists();
    }

    private static class ArtistItem {
        String name;
        int songCount;
        int albumCount;
        String coverArt;

        ArtistItem(String name, int songCount, int albumCount, String coverArt) {
            this.name = name;
            this.songCount = songCount;
            this.albumCount = albumCount;
            this.coverArt = coverArt;
        }
    }

    private class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_artist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ArtistItem artist = artistList.get(position);

            holder.tvArtistName.setText(artist.name);
            holder.tvDetails.setText(artist.songCount + " songs · " + artist.albumCount + " albums");

            if (artist.coverArt != null && !artist.coverArt.isEmpty()) {
                Glide.with(requireContext())
                        .load(artist.coverArt)
                        .placeholder(R.drawable.default_artist)
                        .error(R.drawable.default_artist)
                        .circleCrop()
                        .into(holder.ivArtistImage);
            } else {
                holder.ivArtistImage.setImageResource(R.drawable.default_artist);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), ArtistDetailsActivity.class);
                intent.putExtra("artist_name", artist.name);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return artistList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CircleImageView ivArtistImage;
            TextView tvArtistName, tvDetails;

            ViewHolder(View itemView) {
                super(itemView);
                ivArtistImage = itemView.findViewById(R.id.iv_artist_image);
                tvArtistName = itemView.findViewById(R.id.tv_artist_name);
                tvDetails = itemView.findViewById(R.id.tv_artist_details);
            }
        }
    }
}