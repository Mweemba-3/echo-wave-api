package com.example.echo_wave.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.ui.activities.PlaylistActivity;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistsTabFragment extends Fragment {

    private static final String TAG = "PlaylistsTabFragment";
    private static final String PREFS_NAME = "PlaylistPrefs";
    private static final String KEY_SORT_MODE = "playlist_sort_mode";
    private static final int FAVORITES_PLAYLIST_ID = 1;
    private static final int MOST_PLAYED_PLAYLIST_ID = 2;
    private static final int RECENTLY_ADDED_ID = 3;
    private static final int RECENTLY_PLAYED_ID = 4;

    private RecyclerView rvPlaylists;
    private PlaylistsAdapter adapter;
    private List<Playlist> playlistList = new ArrayList<>();
    private List<Playlist> filteredPlaylistList = new ArrayList<>();
    private FloatingActionButton fabAddPlaylist;
    private View view;
    private TextView tvEmptyState;
    private EditText etSearch;
    private ImageView btnSort;
    private View headerView;

    private MusicDatabase musicDatabase;
    private int currentSortMode = 0;
    private SharedPreferences prefs;
    private Set<String> favoriteSongs = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_playlists_tab, container, false);

        musicDatabase = MusicDatabase.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentSortMode = prefs.getInt(KEY_SORT_MODE, 0);

        initViews();
        loadFavorites();
        loadPlaylists();
        setupSearch();

        return view;
    }

    private void initViews() {
        rvPlaylists = view.findViewById(R.id.rv_playlists);
        fabAddPlaylist = view.findViewById(R.id.fab_add_playlist);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        etSearch = view.findViewById(R.id.et_search);
        btnSort = view.findViewById(R.id.btn_sort);
        headerView = view.findViewById(R.id.header_view);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistsAdapter(filteredPlaylistList);
        rvPlaylists.setAdapter(adapter);

        btnSort.setOnClickListener(v -> showSortDialog());
        fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        loadFavoritesFromPrefs();
    }

    private void loadFavoritesFromPrefs() {
        SharedPreferences favPrefs = requireContext().getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        String favoritesString = favPrefs.getString("favorite_songs", "");
        favoriteSongs.clear();

        if (!favoritesString.isEmpty()) {
            String[] ids = favoritesString.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) {
                    favoriteSongs.add(id);
                }
            }
        }
    }

    private void loadFavorites() {
        new Thread(() -> {
            try {
                // Create Favorites playlist if it doesn't exist
                Playlist favorites = musicDatabase.playlistDao()
                        .getPlaylistById(FAVORITES_PLAYLIST_ID);

                if (favorites == null) {
                    favorites = new Playlist("Favorites");
                    favorites.setDefault(true);
                    favorites.setId(FAVORITES_PLAYLIST_ID);
                    musicDatabase.playlistDao().insert(favorites);
                }

                // Create Most Played playlist if it doesn't exist
                Playlist mostPlayed = musicDatabase.playlistDao()
                        .getPlaylistById(MOST_PLAYED_PLAYLIST_ID);

                if (mostPlayed == null) {
                    mostPlayed = new Playlist("Most Played");
                    mostPlayed.setDefault(true);
                    mostPlayed.setId(MOST_PLAYED_PLAYLIST_ID);
                    musicDatabase.playlistDao().insert(mostPlayed);
                }

                // Create Recently Added playlist if it doesn't exist
                Playlist recentlyAdded = musicDatabase.playlistDao()
                        .getPlaylistById(RECENTLY_ADDED_ID);

                if (recentlyAdded == null) {
                    recentlyAdded = new Playlist("Recently Added");
                    recentlyAdded.setDefault(true);
                    recentlyAdded.setId(RECENTLY_ADDED_ID);
                    musicDatabase.playlistDao().insert(recentlyAdded);
                }

                // Create Recently Played playlist if it doesn't exist
                Playlist recentlyPlayed = musicDatabase.playlistDao()
                        .getPlaylistById(RECENTLY_PLAYED_ID);

                if (recentlyPlayed == null) {
                    recentlyPlayed = new Playlist("Recently Played");
                    recentlyPlayed.setDefault(true);
                    recentlyPlayed.setId(RECENTLY_PLAYED_ID);
                    musicDatabase.playlistDao().insert(recentlyPlayed);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating default playlists: " + e.getMessage());
            }
        }).start();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlaylists(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterPlaylists(String query) {
        filteredPlaylistList.clear();

        if (query.isEmpty()) {
            filteredPlaylistList.addAll(playlistList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Playlist playlist : playlistList) {
                if (playlist.getName().toLowerCase().contains(lowerQuery)) {
                    filteredPlaylistList.add(playlist);
                }
            }
        }

        sortPlaylists();
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showSortDialog() {
        String[] options = {"Name (A-Z)", "Name (Z-A)", "Song Count"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort Playlists")
                .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    prefs.edit().putInt(KEY_SORT_MODE, which).apply();
                    sortPlaylists();
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                })
                .show();
    }

    private void sortPlaylists() {
        switch (currentSortMode) {
            case 0: // Name A-Z
                Collections.sort(filteredPlaylistList, (p1, p2) ->
                        p1.getName().compareToIgnoreCase(p2.getName()));
                break;
            case 1: // Name Z-A
                Collections.sort(filteredPlaylistList, (p1, p2) ->
                        p2.getName().compareToIgnoreCase(p1.getName()));
                break;
            case 2: // Song Count
                Collections.sort(filteredPlaylistList, (p1, p2) ->
                        Integer.compare(p2.getSongCount(), p1.getSongCount()));
                break;
        }
    }

    private void updateEmptyState() {
        if (filteredPlaylistList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            if (etSearch.getText().toString().isEmpty()) {
                tvEmptyState.setText("No playlists yet.\nTap + to create one.");
            } else {
                tvEmptyState.setText("No playlists match your search.");
            }
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void loadPlaylists() {
        new Thread(() -> {
            try {
                List<Playlist> playlists = musicDatabase.playlistDao().getAllPlaylists();

                // Update song counts for each playlist
                for (Playlist playlist : playlists) {
                    int count = musicDatabase.playlistSongDao().getSongCount(playlist.getId());
                    playlist.setSongCount(count);
                }

                // Update dynamic playlists
                updateMostPlayedPlaylist();
                updateRecentlyAddedPlaylist();
                updateRecentlyPlayedPlaylist();

                // Reload playlists after updates
                playlists = musicDatabase.playlistDao().getAllPlaylists();

                // Update song counts again
                for (Playlist playlist : playlists) {
                    int count = musicDatabase.playlistSongDao().getSongCount(playlist.getId());
                    playlist.setSongCount(count);
                }

                List<Playlist> finalPlaylists = playlists;
                requireActivity().runOnUiThread(() -> {
                    playlistList.clear();
                    playlistList.addAll(finalPlaylists);
                    filterPlaylists(etSearch.getText().toString());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading playlists: " + e.getMessage());
            }
        }).start();
    }

    private void updateMostPlayedPlaylist() {
        try {
            List<Song> allSongs = musicDatabase.songDao().getAllSongs();

            // Sort by play count
            Collections.sort(allSongs, (s1, s2) ->
                    Integer.compare(s2.getPlayCount(), s1.getPlayCount()));

            // Clear existing songs
            musicDatabase.playlistSongDao().clearPlaylist(MOST_PLAYED_PLAYLIST_ID);

            // Add top 50 most played
            int limit = Math.min(50, allSongs.size());
            int added = 0;
            for (int i = 0; i < limit; i++) {
                Song song = allSongs.get(i);
                if (song.getPlayCount() > 0) {
                    PlaylistSong playlistSong = new PlaylistSong(MOST_PLAYED_PLAYLIST_ID, song.getId());
                    musicDatabase.playlistSongDao().insert(playlistSong);
                    added++;
                }
            }

            // Update playlist song count
            Playlist mostPlayed = musicDatabase.playlistDao()
                    .getPlaylistById(MOST_PLAYED_PLAYLIST_ID);
            if (mostPlayed != null) {
                mostPlayed.setSongCount(added);
                musicDatabase.playlistDao().update(mostPlayed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating most played: " + e.getMessage());
        }
    }

    private void updateRecentlyAddedPlaylist() {
        try {
            List<Song> allSongs = musicDatabase.songDao().getAllSongs();

            // Sort by date added
            Collections.sort(allSongs, (s1, s2) ->
                    Long.compare(s2.getDateAdded(), s1.getDateAdded()));

            // Clear existing songs
            musicDatabase.playlistSongDao().clearPlaylist(RECENTLY_ADDED_ID);

            // Add last 50
            int limit = Math.min(50, allSongs.size());
            for (int i = 0; i < limit; i++) {
                Song song = allSongs.get(i);
                PlaylistSong playlistSong = new PlaylistSong(RECENTLY_ADDED_ID, song.getId());
                musicDatabase.playlistSongDao().insert(playlistSong);
            }

            // Update playlist song count
            Playlist recentlyAdded = musicDatabase.playlistDao()
                    .getPlaylistById(RECENTLY_ADDED_ID);
            if (recentlyAdded != null) {
                int count = musicDatabase.playlistSongDao().getSongCount(RECENTLY_ADDED_ID);
                recentlyAdded.setSongCount(count);
                musicDatabase.playlistDao().update(recentlyAdded);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating recently added: " + e.getMessage());
        }
    }

    private void updateRecentlyPlayedPlaylist() {
        try {
            List<Song> allSongs = musicDatabase.songDao().getAllSongs();

            // Sort by last played
            Collections.sort(allSongs, (s1, s2) ->
                    Long.compare(s2.getLastPlayed(), s1.getLastPlayed()));

            // Clear existing songs
            musicDatabase.playlistSongDao().clearPlaylist(RECENTLY_PLAYED_ID);

            // Add last 50
            int limit = Math.min(50, allSongs.size());
            int added = 0;
            for (int i = 0; i < limit; i++) {
                Song song = allSongs.get(i);
                if (song.getLastPlayed() > 0) {
                    PlaylistSong playlistSong = new PlaylistSong(RECENTLY_PLAYED_ID, song.getId());
                    musicDatabase.playlistSongDao().insert(playlistSong);
                    added++;
                }
            }

            // Update playlist song count
            Playlist recentlyPlayed = musicDatabase.playlistDao()
                    .getPlaylistById(RECENTLY_PLAYED_ID);
            if (recentlyPlayed != null) {
                recentlyPlayed.setSongCount(added);
                musicDatabase.playlistDao().update(recentlyPlayed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating recently played: " + e.getMessage());
        }
    }

    private void showCreatePlaylistDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);

            TextInputEditText etName = dialogView.findViewById(R.id.et_playlist_name);

            builder.setTitle("Create New Playlist")
                    .setView(dialogView)
                    .setPositiveButton("Create", (dialog, which) -> {
                        String name = etName.getText().toString().trim();
                        if (name.isEmpty()) {
                            Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        new Thread(() -> {
                            try {
                                Playlist playlist = new Playlist(name);
                                long id = musicDatabase.playlistDao().insert(playlist);
                                Log.d(TAG, "Playlist created with ID: " + id);

                                requireActivity().runOnUiThread(() -> {
                                    loadPlaylists();
                                    Toast.makeText(getContext(), "Playlist created", Toast.LENGTH_SHORT).show();
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error creating playlist: " + e.getMessage());
                            }
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + e.getMessage(), e);
        }
    }

    private void showPlaylistOptions(Playlist playlist) {
        if (playlist.isDefault()) {
            // For system playlists, show limited options
            String[] options = {"Play", "View Details"};

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(playlist.getName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                playPlaylist(playlist);
                                break;
                            case 1:
                                showPlaylistDetails(playlist);
                                break;
                        }
                    })
                    .show();
            return;
        }

        // For user-created playlists, show full options
        String[] options = {"Play", "Shuffle", "Rename", "Delete", "Share"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(playlist.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playPlaylist(playlist);
                            break;
                        case 1:
                            shufflePlaylist(playlist);
                            break;
                        case 2:
                            showRenamePlaylistDialog(playlist);
                            break;
                        case 3:
                            showDeletePlaylistDialog(playlist);
                            break;
                        case 4:
                            sharePlaylist(playlist);
                            break;
                    }
                })
                .show();
    }

    private void playPlaylist(Playlist playlist) {
        new Thread(() -> {
            List<Song> songs = musicDatabase.playlistSongDao()
                    .getSongsForPlaylist(playlist.getId());

            if (!songs.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    MediaPlayerHelper.getInstance().playSong(requireContext(), songs, 0);
                    startActivity(new Intent(requireContext(), NowPlayingActivity.class));
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Playlist is empty", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shufflePlaylist(Playlist playlist) {
        new Thread(() -> {
            List<Song> songs = musicDatabase.playlistSongDao()
                    .getSongsForPlaylist(playlist.getId());

            if (!songs.isEmpty()) {
                Collections.shuffle(songs);
                requireActivity().runOnUiThread(() -> {
                    MediaPlayerHelper.getInstance().playSong(requireContext(), songs, 0);
                    startActivity(new Intent(requireContext(), NowPlayingActivity.class));
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Playlist is empty", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showPlaylistDetails(Playlist playlist) {
        new Thread(() -> {
            int songCount = musicDatabase.playlistSongDao().getSongCount(playlist.getId());
            List<Song> songs = musicDatabase.playlistSongDao()
                    .getSongsForPlaylist(playlist.getId());

            long totalDuration = 0;
            for (Song song : songs) {
                totalDuration += song.getDuration();
            }

            long finalTotalDuration = totalDuration;
            int finalSongCount = songCount;

            requireActivity().runOnUiThread(() -> {
                String details = String.format(
                        "Name: %s\nSongs: %d\nTotal Duration: %s",
                        playlist.getName(),
                        finalSongCount,
                        formatDuration(finalTotalDuration)
                );

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Playlist Details")
                        .setMessage(details)
                        .setPositiveButton("OK", null)
                        .show();
            });
        }).start();
    }

    private String formatDuration(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_playlist_name);
        etName.setText(playlist.getName());
        etName.setHint("New playlist name");

        builder.setTitle("Rename Playlist")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    playlist.setName(newName);

                    new Thread(() -> {
                        musicDatabase.playlistDao().update(playlist);
                        requireActivity().runOnUiThread(() -> {
                            loadPlaylists();
                            Toast.makeText(getContext(), "Playlist renamed", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeletePlaylistDialog(Playlist playlist) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Delete '" + playlist.getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        // Delete all songs from playlist first
                        musicDatabase.playlistSongDao().clearPlaylist(playlist.getId());
                        // Then delete the playlist
                        musicDatabase.playlistDao().delete(playlist);

                        requireActivity().runOnUiThread(() -> {
                            loadPlaylists();
                            Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sharePlaylist(Playlist playlist) {
        new Thread(() -> {
            List<Song> songs = musicDatabase.playlistSongDao()
                    .getSongsForPlaylist(playlist.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("🎵 Playlist: ").append(playlist.getName()).append("\n\n");
            sb.append("Songs:\n");

            for (int i = 0; i < songs.size(); i++) {
                Song song = songs.get(i);
                sb.append(i + 1).append(". ").append(song.getTitle())
                        .append(" - ").append(song.getArtist()).append("\n");
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Playlist: " + playlist.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

            requireActivity().startActivity(Intent.createChooser(shareIntent, "Share Playlist"));
        }).start();
    }

    // Adapter class
    private class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.ViewHolder> {
        private List<Playlist> playlists;

        PlaylistsAdapter(List<Playlist> playlists) {
            this.playlists = playlists;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Playlist playlist = playlists.get(position);

            holder.tvPlaylistName.setText(playlist.getName());

            int songCount = playlist.getSongCount();
            holder.tvSongCount.setText(songCount + " " + (songCount == 1 ? "song" : "songs"));

            // Set icon based on playlist type
            if (playlist.isDefault()) {
                if (playlist.getId() == FAVORITES_PLAYLIST_ID) {
                    holder.ivPlaylistArt.setImageResource(R.drawable.ic_favorite_filled);
                } else if (playlist.getId() == MOST_PLAYED_PLAYLIST_ID) {
                    holder.ivPlaylistArt.setImageResource(R.drawable.ic_most_played);
                } else if (playlist.getId() == RECENTLY_ADDED_ID) {
                    holder.ivPlaylistArt.setImageResource(R.drawable.ic_recent);
                } else if (playlist.getId() == RECENTLY_PLAYED_ID) {
                    holder.ivPlaylistArt.setImageResource(R.drawable.ic_history);
                } else {
                    holder.ivPlaylistArt.setImageResource(R.drawable.default_playlist_art);
                }
            } else {
                holder.ivPlaylistArt.setImageResource(R.drawable.default_playlist_art);
                // Optionally load first song's album art
                loadPlaylistArt(holder, playlist);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), PlaylistActivity.class);
                intent.putExtra("playlist_id", playlist.getId());
                intent.putExtra("playlist_name", playlist.getName());
                startActivity(intent);
            });

            holder.itemView.setOnLongClickListener(v -> {
                showPlaylistOptions(playlist);
                return true;
            });
        }

        private void loadPlaylistArt(ViewHolder holder, Playlist playlist) {
            new Thread(() -> {
                List<Song> songs = musicDatabase.playlistSongDao()
                        .getSongsForPlaylist(playlist.getId());

                if (!songs.isEmpty()) {
                    Song firstSong = songs.get(0);
                    requireActivity().runOnUiThread(() -> {
                        if (firstSong.getAlbumArt() != null && !firstSong.getAlbumArt().isEmpty()) {
                            Glide.with(requireContext())
                                    .load(firstSong.getAlbumArt())
                                    .placeholder(R.drawable.default_playlist_art)
                                    .error(R.drawable.default_playlist_art)
                                    .circleCrop()
                                    .into(holder.ivPlaylistArt);
                        }
                    });
                }
            }).start();
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPlaylistArt;
            TextView tvPlaylistName, tvSongCount;

            ViewHolder(View itemView) {
                super(itemView);
                ivPlaylistArt = itemView.findViewById(R.id.iv_playlist_art);
                tvPlaylistName = itemView.findViewById(R.id.tv_playlist_name);
                tvSongCount = itemView.findViewById(R.id.tv_song_count);
            }
        }
    }
}