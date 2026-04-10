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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Artist;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.ArtistDetailsActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryArtistsFragment extends Fragment {

    private static final String TAG = "LibraryArtistsFragment";

    private RecyclerView rvArtists;
    private TextView tvEmptyState;
    private View loadingView;

    private ArtistsAdapter adapter;
    private List<Artist> artistList = new ArrayList<>();
    private MusicDatabase musicDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_artists, container, false);

        try {
            initViews(view);
            musicDatabase = MusicDatabase.getInstance(requireContext());
            loadArtists();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage());
            e.printStackTrace();
        }

        return view;
    }

    private void initViews(View view) {
        rvArtists = view.findViewById(R.id.rv_artists);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        loadingView = view.findViewById(R.id.loading_view);

        rvArtists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArtistsAdapter();
        rvArtists.setAdapter(adapter);
    }

    private void loadArtists() {
        showLoading(true);

        new Thread(() -> {
            try {
                List<Song> allSongs = musicDatabase.songDao().getAllSongs();

                // Get unique artists
                Set<String> uniqueArtists = new HashSet<>();
                for (Song song : allSongs) {
                    if (song.getArtist() != null && !song.getArtist().isEmpty()) {
                        uniqueArtists.add(song.getArtist());
                    }
                }

                List<Artist> artists = new ArrayList<>();
                for (String artistName : uniqueArtists) {
                    List<Song> artistSongs = musicDatabase.songDao().getSongsByArtist(artistName);

                    String coverArt = null;
                    if (!artistSongs.isEmpty() && artistSongs.get(0).getAlbumArt() != null) {
                        coverArt = artistSongs.get(0).getAlbumArt();
                    }

                    // Count distinct albums for this artist
                    Set<String> uniqueAlbums = new HashSet<>();
                    for (Song song : artistSongs) {
                        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                            uniqueAlbums.add(song.getAlbum());
                        }
                    }

                    artists.add(new Artist(artistName, artistSongs.size(), uniqueAlbums.size(), coverArt));
                }

                // Sort artists by name
                Collections.sort(artists, (a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()));

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
                Log.e(TAG, "Error loading artists: " + e.getMessage());
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> showLoading(false));
                }
            }
        }).start();
    }

    private void updateEmptyState() {
        if (artistList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvArtists.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvArtists.setVisibility(View.VISIBLE);
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

    private class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_artist_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Artist artist = artistList.get(position);

            holder.tvName.setText(artist.getName());
            holder.tvDetails.setText(artist.getSongCount() + " songs · " + artist.getAlbumCount() + " albums");

            if (artist.getCoverArt() != null && !artist.getCoverArt().isEmpty()) {
                Glide.with(requireContext())
                        .load(artist.getCoverArt())
                        .placeholder(R.drawable.ic_artist)
                        .error(R.drawable.ic_artist)
                        .circleCrop()
                        .into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.ic_artist);
            }

            holder.cardView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ArtistDetailsActivity.class);
                intent.putExtra("artist_name", artist.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return artistList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            ImageView ivCover;
            TextView tvName, tvDetails;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                ivCover = itemView.findViewById(R.id.iv_artist_image);
                tvName = itemView.findViewById(R.id.tv_artist_name);
                tvDetails = itemView.findViewById(R.id.tv_artist_details);
            }
        }
    }
}