package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.AdManager;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LibrarySongsFragment extends Fragment {

    private static final String TAG = "LibrarySongsFragment";
    private static final String PREF_NAME = "library_prefs";
    private static final String KEY_SORT_CRITERIA = "sort_criteria";
    private static final String KEY_SORT_ASCENDING = "sort_ascending";

    // Native ad configuration - SET TO 0 TO DISABLE ADS
    private static final int AD_POSITION_INTERVAL = 0; // 0 = no ads, 5 = ad every 5 songs
    private static final int TYPE_SONG = 0;
    private static final int TYPE_AD = 1;

    private RecyclerView rvSongs;
    private EditText etSearch;
    private ImageView btnSort, btnClearSearch;
    private TextView tvEmptyState, tvResultCount;
    private View loadingView;

    private SongsAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private List<Song> filteredList = new ArrayList<>();
    private MusicDatabase musicDatabase;

    private String currentSort = "title";
    private boolean sortAscending = true;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;
        try {
            view = inflater.inflate(R.layout.fragment_library_songs, container, false);
            initViews(view);
            musicDatabase = MusicDatabase.getInstance(requireContext());
            sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
            loadSavedSortPreferences();
            setupSearch();
            loadSongs();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView: " + e.getMessage());
            e.printStackTrace();
        }
        return view;
    }

    private void loadSavedSortPreferences() {
        try {
            currentSort = sharedPreferences.getString(KEY_SORT_CRITERIA, "title");
            sortAscending = sharedPreferences.getBoolean(KEY_SORT_ASCENDING, true);
            Log.d(TAG, "Loaded sort preferences: criteria=" + currentSort + ", ascending=" + sortAscending);
        } catch (Exception e) {
            Log.e(TAG, "Error loading sort preferences: " + e.getMessage());
            currentSort = "title";
            sortAscending = true;
        }
    }

    private void saveSortPreferences() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SORT_CRITERIA, currentSort);
            editor.putBoolean(KEY_SORT_ASCENDING, sortAscending);
            editor.apply();
            Log.d(TAG, "Saved sort preferences: criteria=" + currentSort + ", ascending=" + sortAscending);
        } catch (Exception e) {
            Log.e(TAG, "Error saving sort preferences: " + e.getMessage());
        }
    }

    private void initViews(View view) {
        try {
            rvSongs = view.findViewById(R.id.rv_songs);
            etSearch = view.findViewById(R.id.et_search);
            btnSort = view.findViewById(R.id.btn_sort);
            btnClearSearch = view.findViewById(R.id.btn_clear_search);
            tvEmptyState = view.findViewById(R.id.tv_empty_state);
            tvResultCount = view.findViewById(R.id.tv_result_count);
            loadingView = view.findViewById(R.id.loading_view);

            if (rvSongs != null) {
                rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
                adapter = new SongsAdapter();
                rvSongs.setAdapter(adapter);
            }

            if (btnSort != null) {
                btnSort.setOnClickListener(v -> showSortDialog());
            }

            if (btnClearSearch != null) {
                btnClearSearch.setOnClickListener(v -> {
                    if (etSearch != null) {
                        etSearch.setText("");
                    }
                    filterSongs("");
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage());
        }
    }

    private void setupSearch() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null) {
                    filterSongs(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterSongs(String query) {
        if (getActivity() == null) return;

        if (songList == null) {
            songList = new ArrayList<>();
        }

        if (query == null) query = "";

        try {
            if (query.isEmpty()) {
                filteredList.clear();
                filteredList.addAll(songList);
                if (btnClearSearch != null) btnClearSearch.setVisibility(View.GONE);
            } else {
                filteredList.clear();
                String lowerQuery = query.toLowerCase();
                for (Song song : songList) {
                    if (song == null) continue;
                    String title = song.getTitle() != null ? song.getTitle().toLowerCase() : "";
                    String artist = song.getArtist() != null ? song.getArtist().toLowerCase() : "";
                    String album = song.getAlbum() != null ? song.getAlbum().toLowerCase() : "";

                    if (title.contains(lowerQuery) || artist.contains(lowerQuery) || album.contains(lowerQuery)) {
                        filteredList.add(song);
                    }
                }
                if (btnClearSearch != null) btnClearSearch.setVisibility(View.VISIBLE);
            }

            sortSongs(currentSort, sortAscending);

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

            updateEmptyState();

            if (tvResultCount != null) {
                int count = filteredList != null ? filteredList.size() : 0;
                tvResultCount.setText(count + " " + (count == 1 ? "song" : "songs"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in filterSongs: " + e.getMessage());
        }
    }

    private void showSortDialog() {
        if (getContext() == null || getActivity() == null) return;

        try {
            String[] options = {
                    "Title (A-Z)", "Title (Z-A)",
                    "Artist (A-Z)", "Artist (Z-A)",
                    "Album (A-Z)", "Album (Z-A)",
                    "Duration (Shortest First)", "Duration (Longest First)",
                    "Date Added (Newest First)", "Date Added (Oldest First)",
                    "Play Count (Most First)", "Play Count (Least First)"
            };

            int checkedItem = getCurrentSortIndex();

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Sort Songs")
                    .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                        try {
                            applySortFromIndex(which);
                            saveSortPreferences();
                            sortSongs(currentSort, sortAscending);
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error applying sort: " + e.getMessage());
                        }
                        dialog.dismiss();
                    })
                    .setPositiveButton("Reset to Default", (dialog, which) -> {
                        resetToDefaultSort();
                        saveSortPreferences();
                        sortSongs(currentSort, sortAscending);
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing sort dialog: " + e.getMessage());
        }
    }

    private int getCurrentSortIndex() {
        if (currentSort.equals("title")) {
            return sortAscending ? 0 : 1;
        } else if (currentSort.equals("artist")) {
            return sortAscending ? 2 : 3;
        } else if (currentSort.equals("album")) {
            return sortAscending ? 4 : 5;
        } else if (currentSort.equals("duration")) {
            return sortAscending ? 6 : 7;
        } else if (currentSort.equals("date_added")) {
            return sortAscending ? 8 : 9;
        } else if (currentSort.equals("play_count")) {
            return sortAscending ? 10 : 11;
        }
        return 0;
    }

    private void applySortFromIndex(int which) {
        switch (which) {
            case 0: currentSort = "title"; sortAscending = true; break;
            case 1: currentSort = "title"; sortAscending = false; break;
            case 2: currentSort = "artist"; sortAscending = true; break;
            case 3: currentSort = "artist"; sortAscending = false; break;
            case 4: currentSort = "album"; sortAscending = true; break;
            case 5: currentSort = "album"; sortAscending = false; break;
            case 6: currentSort = "duration"; sortAscending = true; break;
            case 7: currentSort = "duration"; sortAscending = false; break;
            case 8: currentSort = "date_added"; sortAscending = false; break;
            case 9: currentSort = "date_added"; sortAscending = true; break;
            case 10: currentSort = "play_count"; sortAscending = false; break;
            case 11: currentSort = "play_count"; sortAscending = true; break;
        }
    }

    private void resetToDefaultSort() {
        currentSort = "title";
        sortAscending = true;
    }

    private void sortSongs(String criteria, boolean ascending) {
        if (filteredList == null || filteredList.isEmpty()) return;

        try {
            Comparator<Song> comparator = null;

            switch (criteria) {
                case "title":
                    comparator = (s1, s2) -> {
                        String title1 = s1.getTitle() != null ? s1.getTitle() : "";
                        String title2 = s2.getTitle() != null ? s2.getTitle() : "";
                        return title1.compareToIgnoreCase(title2);
                    };
                    break;
                case "artist":
                    comparator = (s1, s2) -> {
                        String artist1 = s1.getArtist() != null ? s1.getArtist() : "";
                        String artist2 = s2.getArtist() != null ? s2.getArtist() : "";
                        return artist1.compareToIgnoreCase(artist2);
                    };
                    break;
                case "album":
                    comparator = (s1, s2) -> {
                        String album1 = s1.getAlbum() != null ? s1.getAlbum() : "";
                        String album2 = s2.getAlbum() != null ? s2.getAlbum() : "";
                        return album1.compareToIgnoreCase(album2);
                    };
                    break;
                case "duration":
                    comparator = (s1, s2) -> Long.compare(s1.getDuration(), s2.getDuration());
                    break;
                case "date_added":
                    comparator = (s1, s2) -> Long.compare(s1.getDateAdded(), s2.getDateAdded());
                    break;
                case "play_count":
                    comparator = (s1, s2) -> Integer.compare(s2.getPlayCount(), s1.getPlayCount());
                    break;
            }

            if (comparator != null) {
                if (!ascending && !criteria.equals("play_count")) {
                    comparator = comparator.reversed();
                }
                Collections.sort(filteredList, comparator);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sorting: " + e.getMessage());
        }
    }

    private void loadSongs() {
        if (getActivity() == null) return;

        showLoading(true);

        new Thread(() -> {
            try {
                List<Song> songs = null;
                try {
                    songs = musicDatabase.songDao().getAllSongs();
                } catch (Exception e) {
                    Log.e(TAG, "Database error: " + e.getMessage());
                    songs = new ArrayList<>();
                }

                final List<Song> finalSongs = songs != null ? songs : new ArrayList<>();

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        try {
                            songList.clear();
                            songList.addAll(finalSongs);
                            filteredList.clear();
                            filteredList.addAll(finalSongs);
                            sortSongs(currentSort, sortAscending);

                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                            }

                            updateEmptyState();

                            if (tvResultCount != null) {
                                int count = filteredList.size();
                                tvResultCount.setText(count + " " + (count == 1 ? "song" : "songs"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating UI: " + e.getMessage());
                        } finally {
                            showLoading(false);
                        }
                    });
                } else {
                    showLoading(false);
                }

                Log.d(TAG, "Loaded " + finalSongs.size() + " songs");
            } catch (Exception e) {
                Log.e(TAG, "Error loading songs: " + e.getMessage());
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> showLoading(false));
                }
            }
        }).start();
    }

    private void updateEmptyState() {
        if (getActivity() == null) return;
        if (tvEmptyState == null || rvSongs == null) return;

        try {
            boolean isEmpty = filteredList == null || filteredList.isEmpty();

            requireActivity().runOnUiThread(() -> {
                if (isEmpty) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvSongs.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvSongs.setVisibility(View.VISIBLE);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating empty state: " + e.getMessage());
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() == null) return;

        try {
            if (loadingView != null) {
                loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSongs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter = null;
        }
        if (rvSongs != null) {
            rvSongs.setAdapter(null);
        }
    }

    // ========== ADAPTER CLASS (NO NATIVE ADS - FIXED) ==========

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SongViewHolder> {

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_card, parent, false);
            return new SongViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            if (filteredList == null || position >= filteredList.size()) return;

            Song song = filteredList.get(position);
            if (song == null) return;

            holder.bind(song, position);
        }

        @Override
        public int getItemCount() {
            return filteredList != null ? filteredList.size() : 0;
        }

        class SongViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            ImageView ivAlbumArt;
            TextView tvTitle, tvArtist, tvDuration;

            SongViewHolder(View itemView) {
                super(itemView);
                try {
                    cardView = (MaterialCardView) itemView;
                    ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                    tvTitle = itemView.findViewById(R.id.tv_song_title);
                    tvArtist = itemView.findViewById(R.id.tv_song_artist);
                    tvDuration = itemView.findViewById(R.id.tv_duration);
                } catch (Exception e) {
                    Log.e(TAG, "Error initializing ViewHolder: " + e.getMessage());
                }
            }

            void bind(Song song, int position) {
                if (song == null) return;

                try {
                    tvTitle.setText(song.getTitle() != null ? song.getTitle() : "Unknown Title");
                    tvArtist.setText(song.getArtist() != null ? song.getArtist() : "Unknown Artist");
                    tvDuration.setText(song.getDurationFormatted());

                    if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                        try {
                            Glide.with(itemView.getContext())
                                    .load(song.getAlbumArt())
                                    .placeholder(R.drawable.default_album_art)
                                    .error(R.drawable.default_album_art)
                                    .into(ivAlbumArt);
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading album art: " + e.getMessage());
                            ivAlbumArt.setImageResource(R.drawable.default_album_art);
                        }
                    } else {
                        ivAlbumArt.setImageResource(R.drawable.default_album_art);
                    }

                    cardView.setOnClickListener(v -> {
                        if (getContext() != null && filteredList != null && !filteredList.isEmpty()) {
                            try {
                                MediaPlayerHelper.getInstance().playSong(requireContext(), filteredList, position);
                                Intent intent = new Intent(requireContext(), NowPlayingActivity.class);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Error playing song: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error binding song: " + e.getMessage());
                }
            }
        }
    }
}