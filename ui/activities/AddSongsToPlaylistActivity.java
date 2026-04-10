package com.example.echo_wave.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddSongsToPlaylistActivity extends AppCompatActivity {

    private static final String TAG = "AddSongsActivity";

    private ImageView btnBack, btnFilter, btnSelectAll;
    private TextView tvTitle, tvSelectedCount;
    private EditText etSearch;
    private RecyclerView rvSongs;
    private MaterialButton btnDone;

    private SongsAdapter adapter;
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private Set<Song> selectedSongs = new HashSet<>();
    private Set<String> existingSongIds = new HashSet<>();
    private Playlist playlist;
    private int playlistId;

    private MusicDatabase database;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private int currentFilter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_songs_to_playlist);

        playlistId = getIntent().getIntExtra("playlist_id", -1);

        if (playlistId == -1) {
            Toast.makeText(this, "Invalid playlist ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        database = MusicDatabase.getInstance(this);

        initViews();
        loadPlaylist();
        loadSongs();
        setupSearch();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnFilter = findViewById(R.id.btn_filter);
        btnSelectAll = findViewById(R.id.btn_select_all);
        tvTitle = findViewById(R.id.tv_title);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        etSearch = findViewById(R.id.et_search);
        rvSongs = findViewById(R.id.rv_songs);
        btnDone = findViewById(R.id.btn_done);

        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnSelectAll.setOnClickListener(v -> toggleSelectAll());
        btnDone.setOnClickListener(v -> addSelectedSongsToPlaylist());

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter();
        rvSongs.setAdapter(adapter);

        updateSelectedCount();
    }

    private void showFilterDialog() {
        String[] options = {"All Songs", "By Artist", "By Album"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Filter Songs")
                .setSingleChoiceItems(options, currentFilter, (dialog, which) -> {
                    currentFilter = which;
                    applyFilter();
                    dialog.dismiss();
                })
                .show();
    }

    private void applyFilter() {
        switch (currentFilter) {
            case 0:
                filteredSongs.clear();
                filteredSongs.addAll(allSongs);
                adapter.notifyDataSetChanged();
                break;
            case 1:
                showArtistFilter();
                break;
            case 2:
                showAlbumFilter();
                break;
        }
    }

    private void showArtistFilter() {
        executor.execute(() -> {
            Set<String> artists = new HashSet<>();
            for (Song song : allSongs) {
                if (song.getArtist() != null && !song.getArtist().isEmpty()) {
                    artists.add(song.getArtist());
                }
            }
            List<String> artistList = new ArrayList<>(artists);
            Collections.sort(artistList);
            String[] artistArray = artistList.toArray(new String[0]);

            mainHandler.post(() -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Select Artist")
                        .setItems(artistArray, (dialog, which) -> {
                            filterByArtist(artistArray[which]);
                        })
                        .show();
            });
        });
    }

    private void showAlbumFilter() {
        executor.execute(() -> {
            Set<String> albums = new HashSet<>();
            for (Song song : allSongs) {
                if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                    albums.add(song.getAlbum());
                }
            }
            List<String> albumList = new ArrayList<>(albums);
            Collections.sort(albumList);
            String[] albumArray = albumList.toArray(new String[0]);

            mainHandler.post(() -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Select Album")
                        .setItems(albumArray, (dialog, which) -> {
                            filterByAlbum(albumArray[which]);
                        })
                        .show();
            });
        });
    }

    private void filterByArtist(String artist) {
        filteredSongs.clear();
        for (Song song : allSongs) {
            if (artist.equals(song.getArtist())) {
                filteredSongs.add(song);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterByAlbum(String album) {
        filteredSongs.clear();
        for (Song song : allSongs) {
            if (album.equals(song.getAlbum())) {
                filteredSongs.add(song);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleSelectAll() {
        if (selectedSongs.size() == getSelectableCount() && !filteredSongs.isEmpty()) {
            selectedSongs.clear();
            btnSelectAll.setImageResource(R.drawable.ic_select_none);
        } else {
            selectedSongs.clear();
            for (Song song : filteredSongs) {
                if (!existingSongIds.contains(song.getId())) {
                    selectedSongs.add(song);
                }
            }
            btnSelectAll.setImageResource(R.drawable.ic_select_all);
        }
        adapter.notifyDataSetChanged();
        updateSelectedCount();
    }

    private int getSelectableCount() {
        int count = 0;
        for (Song song : filteredSongs) {
            if (!existingSongIds.contains(song.getId())) {
                count++;
            }
        }
        return count;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchSongs(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchSongs(String query) {
        filteredSongs.clear();
        if (query.isEmpty()) {
            filteredSongs.addAll(allSongs);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(lowerQuery) ||
                        (song.getArtist() != null && song.getArtist().toLowerCase().contains(lowerQuery)) ||
                        (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(lowerQuery))) {
                    filteredSongs.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadPlaylist() {
        executor.execute(() -> {
            playlist = database.playlistDao().getPlaylistById(playlistId);
            mainHandler.post(() -> {
                if (playlist != null) {
                    tvTitle.setText("Add to " + playlist.getName());
                } else {
                    Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private void loadSongs() {
        executor.execute(() -> {
            List<Song> songs = database.songDao().getAllSongs();
            List<Song> existingSongs = database.playlistSongDao().getSongsForPlaylist(playlistId);

            existingSongIds.clear();
            for (Song song : existingSongs) {
                existingSongIds.add(song.getId());
            }

            Collections.sort(songs, (s1, s2) ->
                    s1.getTitle().compareToIgnoreCase(s2.getTitle()));

            mainHandler.post(() -> {
                allSongs.clear();
                allSongs.addAll(songs);
                filteredSongs.clear();
                filteredSongs.addAll(songs);
                adapter.notifyDataSetChanged();
                updateSelectedCount();
            });
        });
    }

    private void updateSelectedCount() {
        tvSelectedCount.setText(selectedSongs.size() + " selected");
        btnDone.setEnabled(!selectedSongs.isEmpty());
    }

    private void addSelectedSongsToPlaylist() {
        if (selectedSongs.isEmpty()) {
            Toast.makeText(this, "No songs selected", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDone.setEnabled(false);

        executor.execute(() -> {
            try {
                int addedCount = 0;

                // Get fresh list of existing songs
                List<Song> existingSongs = database.playlistSongDao().getSongsForPlaylist(playlistId);
                Set<String> currentExistingIds = new HashSet<>();
                for (Song s : existingSongs) {
                    currentExistingIds.add(s.getId());
                }

                // Add only new songs
                for (Song song : selectedSongs) {
                    if (!currentExistingIds.contains(song.getId())) {
                        PlaylistSong playlistSong = new PlaylistSong(playlistId, song.getId());
                        database.playlistSongDao().insert(playlistSong);
                        addedCount++;
                    }
                }

                // Update playlist count
                if (addedCount > 0) {
                    int newCount = database.playlistSongDao().getSongCount(playlistId);
                    playlist.setSongCount(newCount);
                    database.playlistDao().update(playlist);
                }

                int finalAddedCount = addedCount;
                mainHandler.post(() -> {
                    Toast.makeText(this, finalAddedCount + " songs added", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error adding songs: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnDone.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

        private Set<String> existingIds = new HashSet<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_song_selectable, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Song song = filteredSongs.get(position);

            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());

            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(AddSongsToPlaylistActivity.this)
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.default_album_art_small)
                        .error(R.drawable.default_album_art_small)
                        .into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
            }

            boolean alreadyInPlaylist = existingSongIds.contains(song.getId());
            boolean isSelected = selectedSongs.contains(song);

            if (alreadyInPlaylist) {
                holder.itemView.setAlpha(0.5f);
                holder.ivCheck.setVisibility(View.GONE);
                holder.itemView.setEnabled(false);
                holder.tvTitle.setTextColor(getColor(R.color.text_secondary));
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.itemView.setEnabled(true);
                holder.tvTitle.setTextColor(getColor(R.color.text_primary));
                holder.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

                holder.itemView.setOnClickListener(v -> {
                    if (selectedSongs.contains(song)) {
                        selectedSongs.remove(song);
                        holder.ivCheck.setVisibility(View.GONE);
                    } else {
                        selectedSongs.add(song);
                        holder.ivCheck.setVisibility(View.VISIBLE);
                    }
                    updateSelectedCount();
                    updateSelectAllButton();
                });
            }
        }

        private void updateSelectAllButton() {
            if (filteredSongs.isEmpty()) {
                btnSelectAll.setImageResource(R.drawable.ic_select_none);
            } else if (selectedSongs.size() == getSelectableCount()) {
                btnSelectAll.setImageResource(R.drawable.ic_select_all);
            } else if (selectedSongs.isEmpty()) {
                btnSelectAll.setImageResource(R.drawable.ic_select_none);
            } else {
                btnSelectAll.setImageResource(R.drawable.ic_select_some);
            }
        }

        @Override
        public int getItemCount() {
            return filteredSongs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAlbumArt, ivCheck;
            TextView tvTitle, tvArtist;

            ViewHolder(View itemView) {
                super(itemView);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                ivCheck = itemView.findViewById(R.id.iv_check);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvArtist = itemView.findViewById(R.id.tv_song_artist);
            }
        }
    }
}