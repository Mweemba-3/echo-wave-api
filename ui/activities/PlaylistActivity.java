package com.example.echo_wave.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlaylistActivity extends AppCompatActivity {

    private static final String TAG = "PlaylistActivity";

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MaterialButton btnPlayAll;
    private SongsAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private MusicDatabase database;
    private Playlist playlist;
    private int playlistId;
    private String playlistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        // Get intent extras
        playlistId = getIntent().getIntExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");

        if (playlistId == -1) {
            Toast.makeText(this, "Invalid playlist ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(playlistName != null ? playlistName : "Playlist");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnPlayAll = findViewById(R.id.btnPlayAll);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter();
        recyclerView.setAdapter(adapter);

        // Get database instance
        database = MusicDatabase.getInstance(this);

        // Load data
        loadPlaylist();
        loadSongs();

        // Play all button click
        btnPlayAll.setOnClickListener(v -> playAllSongs());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadPlaylist() {
        new Thread(() -> {
            playlist = database.playlistDao().getPlaylistById(playlistId);
            if (playlist == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(playlist.getName());
                    }
                });
            }
        }).start();
    }

    private void loadSongs() {
        new Thread(() -> {
            List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlistId);

            // Log for debugging
            Log.d(TAG, "Loaded " + songs.size() + " songs from database");

            // Remove any duplicates
            List<Song> uniqueSongs = removeDuplicates(songs);

            if (uniqueSongs.size() != songs.size()) {
                Log.d(TAG, "Removed " + (songs.size() - uniqueSongs.size()) + " duplicates");
                // Fix database if duplicates found
                fixDuplicates(uniqueSongs);
            }

            List<Song> finalSongs = uniqueSongs;
            runOnUiThread(() -> {
                songList.clear();
                songList.addAll(finalSongs);
                adapter.notifyDataSetChanged();

                // Update empty state
                if (songList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    btnPlayAll.setEnabled(false);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    btnPlayAll.setEnabled(true);
                }
            });
        }).start();
    }

    private List<Song> removeDuplicates(List<Song> songs) {
        Set<String> seenIds = new HashSet<>();
        List<Song> unique = new ArrayList<>();

        for (Song song : songs) {
            if (song != null && seenIds.add(song.getId())) {
                unique.add(song);
            }
        }
        return unique;
    }

    private void fixDuplicates(List<Song> uniqueSongs) {
        new Thread(() -> {
            // Clear and re-add unique songs
            database.playlistSongDao().clearPlaylist(playlistId);
            for (Song song : uniqueSongs) {
                PlaylistSong ps = new PlaylistSong(playlistId, song.getId());
                database.playlistSongDao().insert(ps);
            }
            // Update playlist count
            if (playlist != null) {
                playlist.setSongCount(uniqueSongs.size());
                database.playlistDao().update(playlist);
            }
        }).start();
    }

    private void playSong(Song song) {
        if (song == null) return;

        // Create a playlist with just this song
        List<Song> singleSongList = new ArrayList<>();
        singleSongList.add(song);

        MediaPlayerHelper.getInstance().playSong(this, singleSongList, 0);
        Intent intent = new Intent(this, NowPlayingActivity.class);
        startActivity(intent);
    }

    private void playAllSongs() {
        if (songList.isEmpty()) {
            Toast.makeText(this, "No songs in playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaPlayerHelper.getInstance().playSong(this, songList, 0);
        Intent intent = new Intent(this, NowPlayingActivity.class);
        startActivity(intent);
    }

    // Adapter class
    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songList.get(position);

            holder.tvTitle.setText(song.getTitle());

            String artistText = song.getArtist() != null && !song.getArtist().isEmpty()
                    ? song.getArtist() : "Unknown Artist";
            holder.tvArtist.setText(artistText);

            // Set album art if available
            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(PlaylistActivity.this)
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.default_album_art_small)
                        .error(R.drawable.default_album_art_small)
                        .into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
            }

            // Click to play the song
            holder.itemView.setOnClickListener(v -> playSong(song));

            // Optional: Add long click for options
            holder.itemView.setOnLongClickListener(v -> {
                showSongOptions(song);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return songList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvArtist;
            ImageView ivAlbumArt;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvArtist = itemView.findViewById(R.id.tv_artist);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            }
        }
    }

    private void showSongOptions(Song song) {
        String[] options = {"Play", "Remove from Playlist", "Go to Album", "Share"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(song.getTitle())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playSong(song);
                            break;
                        case 1:
                            removeFromPlaylist(song);
                            break;
                        case 2:
                            // Go to album
                            if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                                Intent intent = new Intent(this, AlbumDetailsActivity.class);
                                intent.putExtra("album_name", song.getAlbum());
                                intent.putExtra("artist_name", song.getArtist());
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "Album info not available", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 3:
                            shareSong(song);
                            break;
                    }
                })
                .show();
    }

    private void removeFromPlaylist(Song song) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Remove from Playlist")
                .setMessage("Remove \"" + song.getTitle() + "\" from " + playlistName + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    new Thread(() -> {
                        // Remove from playlist
                        database.playlistSongDao().delete(playlistId, song.getId());

                        // Update playlist count
                        if (playlist != null) {
                            int newCount = database.playlistSongDao().getSongCount(playlistId);
                            playlist.setSongCount(newCount);
                            database.playlistDao().update(playlist);
                        }

                        runOnUiThread(() -> {
                            // Refresh list
                            loadSongs();
                            Toast.makeText(this, "Removed from playlist", Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareSong(Song song) {
        try {
            java.io.File file = new java.io.File(song.getPath());
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Song"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }
}