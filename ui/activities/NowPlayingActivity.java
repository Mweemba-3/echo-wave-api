package com.example.echo_wave.ui.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.services.MediaPlayerService;
import com.example.echo_wave.ui.adapters.QueueAdapter;
import com.example.echo_wave.utils.SleepTimerManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NowPlayingActivity extends AppCompatActivity {

    private static final String TAG = "NowPlayingActivity";
    private static final String PREFS_NAME = "NowPlayingPrefs";
    private static final String KEY_LAST_SONG_ID = "last_song_id";
    private static final String KEY_LAST_POSITION = "last_position";
    private static final String PLAYBACK_PREFS = "PlaybackPrefs";
    private static final String KEY_SHUFFLE = "shuffle_mode";
    private AdView adViewNowPlaying;
    private static final String KEY_REPEAT = "repeat_mode";
    static final int FAVORITES_PLAYLIST_ID = 1;
    private static final String FAVORITES_PREFS = "FavoritesPrefs";
    private static final String KEY_FAVORITES = "favorite_songs";
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private Set<String> favoriteSongs = new HashSet<>();

    // Views
    private ImageView btnBack, btnMenu, btnShuffle, btnPrevious, btnPlayPause, btnNext, btnRepeat;
    private ImageView btnEqualizer, btnQueue, btnFavorite, btnSleepTimer;
    private ImageView ivAlbumArt;
    private TextView tvSongTitle, tvArtist, tvCurrentTime, tvTotalDuration;
    private SeekBar seekBar;
    private CardView cvAlbumArt;
    private ProgressBar progressLoader;
    private TextView tvPlaylistName;
    private TextView tvPlaylistSubtitle;

    // Service
    private MediaPlayerService mediaPlayerService;
    private boolean isServiceBound = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private Runnable fastUpdateRunnable;
    private AdView adViewBelowArt;

    // State
    private boolean isPlaying = false;
    private boolean isShuffleOn = false;
    private boolean isFavorite = false;
    private int repeatMode = 0;
    private Song currentSong = null;
    private List<Song> currentQueue = new ArrayList<>();
    private String currentAlbumArtPath = null;

    // Colors for adaptation
    private int vibrantColor = Color.WHITE;
    private int mutedColor = Color.LTGRAY;

    // Gesture Detection
    private GestureDetectorCompat gestureDetector;

    // Sleep Timer
    private SleepTimerManager sleepTimerManager;

    // Database
    private MusicDatabase musicDatabase;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "Service connected");
            updateFromService();
            startProgressUpdates();
            startFastUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            mediaPlayerService = null;
        }
    };

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action != null) {
                runOnUiThread(() -> {
                    switch (action) {
                        case "PLAYBACK_STARTED":
                        case "PLAYBACK_RESUMED":
                            isPlaying = true;
                            updatePlayPauseButton();
                            break;
                        case "PLAYBACK_PAUSED":
                            isPlaying = false;
                            updatePlayPauseButton();
                            break;
                        case "PLAYBACK_STOPPED":
                            finish();
                            break;
                        case "SONG_CHANGED":
                            // Immediately update UI when song changes
                            updateFromService();
                            break;
                        case "SHUFFLE_CHANGED":
                            if (mediaPlayerService != null) {
                                isShuffleOn = mediaPlayerService.isShuffleEnabled();
                                updateShuffleButton();
                                savePlaybackState();
                            }
                            break;
                        case "REPEAT_CHANGED":
                            if (mediaPlayerService != null) {
                                repeatMode = mediaPlayerService.getRepeatMode();
                                updateRepeatButton();
                                savePlaybackState();
                            }
                            break;
                        case "QUEUE_UPDATED":
                            if (mediaPlayerService != null) {
                                currentQueue = mediaPlayerService.getPlaylist();
                            }
                            break;
                    }
                });
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        musicDatabase = MusicDatabase.getInstance(this);

        initViews();
        setupListeners();
        setupGestureDetection();
        loadPlaybackState();
        loadFavorites();

        bindService();
        registerReceivers();
        // Load banner ad
        adViewBelowArt = findViewById(R.id.adView_below_art);
        if (adViewBelowArt != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewBelowArt.loadAd(adRequest);
        }

        sleepTimerManager = new SleepTimerManager(this, handler, this::onSleepTimerComplete);
        createDefaultPlaylists();

        LocalBroadcastManager.getInstance(this).registerReceiver(refreshReceiver,
                new IntentFilter("REFRESH_DATA"));

        // Show loading
        if (progressLoader != null) {
            progressLoader.setVisibility(View.VISIBLE);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnMenu = findViewById(R.id.btn_menu);
        tvPlaylistName = findViewById(R.id.tv_playlist_name);
        tvPlaylistSubtitle = findViewById(R.id.tv_playlist_subtitle);
        cvAlbumArt = findViewById(R.id.cv_album_art);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvArtist = findViewById(R.id.tv_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalDuration = findViewById(R.id.tv_total_duration);
        seekBar = findViewById(R.id.seek_bar);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnPrevious = findViewById(R.id.btn_previous);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnEqualizer = findViewById(R.id.btn_equalizer);
        btnQueue = findViewById(R.id.btn_queue);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnSleepTimer = findViewById(R.id.btn_sleep_timer);
        progressLoader = findViewById(R.id.progress_loader);

        // Set defaults
        tvSongTitle.setText("");
        tvArtist.setText("");
        tvCurrentTime.setText("0:00");
        tvTotalDuration.setText("0:00");
        tvPlaylistName.setText("");
        tvPlaylistSubtitle.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (isServiceBound && mediaPlayerService != null) {
                if (isPlaying) {
                    mediaPlayerService.pauseMedia();
                } else {
                    mediaPlayerService.playMedia();
                }
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (isServiceBound && mediaPlayerService != null) {
                mediaPlayerService.playPrevious();
                animateButton(v);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (isServiceBound && mediaPlayerService != null) {
                mediaPlayerService.playNext();
                animateButton(v);
            }
        });

        btnShuffle.setOnClickListener(v -> {
            if (isServiceBound && mediaPlayerService != null) {
                mediaPlayerService.toggleShuffle();
                animateButton(v);
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (isServiceBound && mediaPlayerService != null) {
                mediaPlayerService.toggleRepeat();
                animateButton(v);
            }
        });

        btnEqualizer.setOnClickListener(v -> {
            Toast.makeText(this, "Equalizer coming soon", Toast.LENGTH_SHORT).show();
        });

        btnQueue.setOnClickListener(v -> openQueue());

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnSleepTimer.setOnClickListener(v -> showSleepTimerDialog());

        if (seekBar != null) {
            // Initially hide thumb
            seekBar.getThumb().setAlpha(0);

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && tvCurrentTime != null) {
                        tvCurrentTime.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBar.getThumb().setAlpha(255);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBar.getThumb().setAlpha(0);
                    if (isServiceBound && mediaPlayerService != null) {
                        mediaPlayerService.seekTo(seekBar.getProgress());
                    }
                }
            });
        }
    }

    private void updateFromService() {
        if (mediaPlayerService == null) return;

        try {
            Song newSong = mediaPlayerService.getCurrentSong();

            // Check if song actually changed
            boolean songChanged = (currentSong == null && newSong != null) ||
                    (currentSong != null && newSong != null &&
                            currentSong.getId() != null && !currentSong.getId().equals(newSong.getId()));

            currentSong = newSong;
            isPlaying = mediaPlayerService.isPlaying();
            isShuffleOn = mediaPlayerService.isShuffleEnabled();
            repeatMode = mediaPlayerService.getRepeatMode();
            currentQueue = mediaPlayerService.getPlaylist();

            if (currentSong != null) {
                // Update UI immediately
                updateUIWithSong(currentSong);
                checkIfFavorite();

                // Force update album art immediately
                loadAlbumArtImmediately(currentSong.getAlbumArt());
            }

            updatePlayPauseButton();
            updateShuffleButton();
            updateRepeatButton();

            // Hide loading
            if (progressLoader != null) {
                progressLoader.setVisibility(View.GONE);
            }

            if (songChanged) {
                Log.d(TAG, "Song changed to: " + currentSong.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateFromService: " + e.getMessage());
        }
    }

    private void updateUIWithSong(Song song) {
        if (song == null) return;

        // Update text immediately
        tvSongTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());

        int duration = (int) song.getDuration();
        seekBar.setMax(duration);
        tvTotalDuration.setText(formatTime(duration));

        // Update top bar with folder/album name
        updateTopBarInfo(song);
    }

    private void updateTopBarInfo(Song song) {
        new Thread(() -> {
            try {
                String playlistName = null;
                String playlistType = null;

                // Check if song is in a playlist
                List<Playlist> playlists = musicDatabase.playlistDao().getAllPlaylists();
                for (Playlist playlist : playlists) {
                    List<Song> playlistSongs = musicDatabase.playlistSongDao()
                            .getSongsForPlaylist(playlist.getId());

                    for (Song s : playlistSongs) {
                        if (s.getId().equals(song.getId())) {
                            playlistName = playlist.getName();
                            if (playlist.getId() == FAVORITES_PLAYLIST_ID) {
                                playlistType = "Favorites";
                            } else {
                                playlistType = "Playlist";
                            }
                            break;
                        }
                    }
                    if (playlistName != null) break;
                }

                // If not in playlist, get folder name
                if (playlistName == null && song.getPath() != null) {
                    File file = new File(song.getPath());
                    File parentDir = file.getParentFile();
                    if (parentDir != null) {
                        playlistName = parentDir.getName();
                        playlistType = "Folder";
                    }
                }

                // If still null, use album
                if (playlistName == null && song.getAlbum() != null && !song.getAlbum().isEmpty()) {
                    playlistName = song.getAlbum();
                    playlistType = "Album";
                }

                final String finalName = playlistName != null ? playlistName : "Now Playing";
                final String finalType = playlistType;

                runOnUiThread(() -> {
                    tvPlaylistName.setText(finalName);
                    if (finalType != null) {
                        tvPlaylistSubtitle.setText(finalType);
                        tvPlaylistSubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvPlaylistSubtitle.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating top bar: " + e.getMessage());
                runOnUiThread(() -> {
                    tvPlaylistName.setText("Now Playing");
                    tvPlaylistSubtitle.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private void loadAlbumArtImmediately(String albumArtPath) {
        // Clear previous album art first to avoid showing old image
        ivAlbumArt.setImageResource(R.drawable.default_album_art);

        if (albumArtPath == null || albumArtPath.isEmpty()) {
            resetColors();
            return;
        }

        Object loadObject = albumArtPath.startsWith("content://") ?
                Uri.parse(albumArtPath) : new File(albumArtPath);

        Glide.with(this)
                .asBitmap()
                .load(loadObject)
                .placeholder(R.drawable.default_album_art)
                .error(R.drawable.default_album_art)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ivAlbumArt.setImageBitmap(resource);

                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                vibrantColor = palette.getVibrantColor(Color.WHITE);
                                mutedColor = palette.getMutedColor(Color.LTGRAY);
                                applyAdaptiveColors();
                            }
                        });
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        ivAlbumArt.setImageResource(R.drawable.default_album_art);
                        resetColors();
                    }
                });
    }

    private void applyAdaptiveColors() {
        tvSongTitle.setTextColor(vibrantColor);
        tvArtist.setTextColor(mutedColor);
        tvCurrentTime.setTextColor(mutedColor);
        tvTotalDuration.setTextColor(mutedColor);

        updateShuffleButton();
        updateRepeatButton();
        updateFavoriteButton();
    }

    private void resetColors() {
        vibrantColor = Color.WHITE;
        mutedColor = Color.LTGRAY;
        applyAdaptiveColors();
    }

    private void updatePlayPauseButton() {
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void updateShuffleButton() {
        btnShuffle.setColorFilter(isShuffleOn ? vibrantColor : mutedColor);
        btnShuffle.setAlpha(isShuffleOn ? 1.0f : 0.6f);
    }

    private void updateRepeatButton() {
        switch (repeatMode) {
            case 0: // Off
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(mutedColor);
                btnRepeat.setAlpha(0.6f);
                break;
            case 1: // All
                btnRepeat.setImageResource(R.drawable.ic_repeat);
                btnRepeat.setColorFilter(vibrantColor);
                btnRepeat.setAlpha(1.0f);
                break;
            case 2: // One
                btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                btnRepeat.setColorFilter(vibrantColor);
                btnRepeat.setAlpha(1.0f);
                break;
        }
    }

    private void updateFavoriteButton() {
        btnFavorite.setColorFilter(isFavorite ? vibrantColor : mutedColor);
    }

    private void checkIfFavorite() {
        if (currentSong == null) return;
        if (favoriteSongs.isEmpty()) loadFavorites();
        isFavorite = favoriteSongs.contains(currentSong.getId());
        updateFavoriteButton();
    }

    private void toggleFavorite() {
        if (currentSong == null) return;

        if (favoriteSongs.contains(currentSong.getId())) {
            favoriteSongs.remove(currentSong.getId());
            isFavorite = false;
            Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
        } else {
            favoriteSongs.add(currentSong.getId());
            isFavorite = true;
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
        }

        saveFavorites();
        updateFavoriteButton();
        updateFavoritesPlaylist();

        // Update database
        new Thread(() -> {
            try {
                currentSong.setFavorite(isFavorite);
                musicDatabase.songDao().update(currentSong);
            } catch (Exception e) {
                Log.e(TAG, "Error updating favorite status: " + e.getMessage());
            }
        }).start();
    }

    private void saveFavorites() {
        SharedPreferences prefs = getSharedPreferences(FAVORITES_PREFS, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String id : favoriteSongs) {
            sb.append(id).append(",");
        }
        prefs.edit().putString(KEY_FAVORITES, sb.toString()).apply();
    }

    private void loadFavorites() {
        SharedPreferences prefs = getSharedPreferences(FAVORITES_PREFS, MODE_PRIVATE);
        String favoritesString = prefs.getString(KEY_FAVORITES, "");
        favoriteSongs.clear();

        if (!favoritesString.isEmpty()) {
            String[] ids = favoritesString.split(",");
            for (String id : ids) {
                if (!id.isEmpty()) favoriteSongs.add(id);
            }
        }
    }

    private void updateFavoritesPlaylist() {
        new Thread(() -> {
            try {
                Playlist favorites = musicDatabase.playlistDao().getPlaylistById(FAVORITES_PLAYLIST_ID);
                if (favorites != null) {
                    musicDatabase.playlistSongDao().clearPlaylist(FAVORITES_PLAYLIST_ID);
                    for (String songId : favoriteSongs) {
                        PlaylistSong playlistSong = new PlaylistSong(FAVORITES_PLAYLIST_ID, songId);
                        musicDatabase.playlistSongDao().insert(playlistSong);
                    }
                    int count = musicDatabase.playlistSongDao().getSongCount(FAVORITES_PLAYLIST_ID);
                    favorites.setSongCount(count);
                    musicDatabase.playlistDao().update(favorites);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating favorites playlist: " + e.getMessage());
            }
        }).start();
    }

    private void saveQueueAsPlaylist() {
        if (currentQueue == null || currentQueue.isEmpty()) {
            Toast.makeText(this, "Queue is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Playlist name");
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_tertiary));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Save Queue as Playlist")
                .setMessage("Save " + currentQueue.size() + " songs to a new playlist")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new Thread(() -> {
                        try {
                            // Create new playlist
                            Playlist playlist = new Playlist();
                            playlist.setName(name);
                            playlist.setDefault(false);
                            playlist.setSongCount(currentQueue.size());

                            // Insert playlist
                            long playlistId = musicDatabase.playlistDao().insert(playlist);
                            playlist.setId((int) playlistId);

                            // Add all songs from queue to playlist
                            for (int i = 0; i < currentQueue.size(); i++) {
                                Song song = currentQueue.get(i);
                                PlaylistSong playlistSong = new PlaylistSong((int) playlistId, song.getId());
                                musicDatabase.playlistSongDao().insert(playlistSong);
                            }

                            runOnUiThread(() -> {
                                Toast.makeText(this, "Playlist \"" + name + "\" created with " +
                                        currentQueue.size() + " songs", Toast.LENGTH_LONG).show();
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error saving queue as playlist: " + e.getMessage());
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Error creating playlist", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openQueue() {
        // Get current queue from service
        if (mediaPlayerService != null) {
            currentQueue = mediaPlayerService.getPlaylist();
        }

        if (currentQueue == null || currentQueue.isEmpty()) {
            Toast.makeText(this, "Queue is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            View view = getLayoutInflater().inflate(R.layout.bottom_sheet_queue, null);

            RecyclerView recyclerView = view.findViewById(R.id.rv_queue);
            TextView tvQueueTitle = view.findViewById(R.id.tv_queue_title);
            MaterialButton btnClearQueue = view.findViewById(R.id.btn_clear_queue);
            MaterialButton btnSaveQueue = view.findViewById(R.id.btn_save_queue);
            ImageView btnClose = view.findViewById(R.id.btn_close_queue);

            tvQueueTitle.setText("Queue (" + currentQueue.size() + ")");

            // Create adapter
            QueueAdapter adapter = new QueueAdapter(
                    currentQueue,
                    currentSong,
                    new QueueAdapter.OnQueueItemClickListener() {
                        @Override
                        public void onItemClick(Song song, int position) {
                            if (isServiceBound && mediaPlayerService != null) {
                                mediaPlayerService.playAtPosition(position);
                                dialog.dismiss();
                            }
                        }

                        @Override
                        public void onRemoveClick(Song song, int position) {
                            if (isServiceBound && mediaPlayerService != null) {
                                // Remove from queue
                                List<Song> newQueue = new ArrayList<>(currentQueue);
                                newQueue.remove(position);
                                mediaPlayerService.setPlaylist(newQueue,
                                        position < newQueue.size() ? position : newQueue.size() - 1);
                                dialog.dismiss();
                                openQueue(); // Refresh queue
                            }
                        }
                    });

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            btnClearQueue.setOnClickListener(v -> {
                if (isServiceBound && mediaPlayerService != null && currentSong != null) {
                    List<Song> newQueue = new ArrayList<>();
                    newQueue.add(currentSong);
                    mediaPlayerService.setPlaylist(newQueue, 0);
                    dialog.dismiss();
                    Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
                }
            });

            btnSaveQueue.setOnClickListener(v -> {
                saveQueueAsPlaylist();
                dialog.dismiss();
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.setContentView(view);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening queue: " + e.getMessage());
            Toast.makeText(this, "Error opening queue", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSleepTimerDialog() {
        String[] options = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "End of song"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    int minutes = 0;
                    switch (which) {
                        case 0: minutes = 5; break;
                        case 1: minutes = 10; break;
                        case 2: minutes = 15; break;
                        case 3: minutes = 30; break;
                        case 4: minutes = 60; break;
                        case 5:
                            if (currentSong != null) {
                                int duration = (int) currentSong.getDuration();
                                int remaining = duration - (mediaPlayerService != null ?
                                        mediaPlayerService.getCurrentPosition() : 0);
                                sleepTimerManager.startTimer(remaining);
                                Toast.makeText(this, "Timer set to end of song", Toast.LENGTH_SHORT).show();
                            }
                            return;
                    }
                    sleepTimerManager.startTimer(minutes * 60 * 1000);
                    Toast.makeText(this, "Sleep timer set for " + options[which], Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void onSleepTimerComplete() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Sleep timer finished", Toast.LENGTH_SHORT).show();
            if (isServiceBound && mediaPlayerService != null) {
                mediaPlayerService.pauseMedia();
            }
        });
    }

    private void animateButton(View button) {
        button.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction(() -> button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void setupGestureDetection() {
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            runOnUiThread(() -> {
                                if (isServiceBound && mediaPlayerService != null) {
                                    mediaPlayerService.playPrevious();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                if (isServiceBound && mediaPlayerService != null) {
                                    mediaPlayerService.playNext();
                                }
                            });
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        findViewById(R.id.root_layout).setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void startProgressUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceBound && mediaPlayerService != null && !isFinishing()) {
                    int position = mediaPlayerService.getCurrentPosition();
                    seekBar.setProgress(position);
                    tvCurrentTime.setText(formatTime(position));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateRunnable);
    }

    // Add this broadcast receiver
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("REFRESH_DATA".equals(intent.getAction())) {
                // Refresh favorites status
                checkIfFavorite();
            }
        }
    };

    private void startFastUpdates() {
        fastUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceBound && mediaPlayerService != null && !isFinishing()) {
                    // Fast updates for seekbar smoothness
                    int position = mediaPlayerService.getCurrentPosition();
                    if (Math.abs(seekBar.getProgress() - position) > 100) {
                        seekBar.setProgress(position);
                    }
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(fastUpdateRunnable);
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void bindService() {
        Intent intent = new Intent(this, MediaPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter("PLAYBACK_STATE_CHANGED");
        LocalBroadcastManager.getInstance(this).registerReceiver(playbackReceiver, filter);
    }

    private void loadPlaybackState() {
        SharedPreferences prefs = getSharedPreferences(PLAYBACK_PREFS, MODE_PRIVATE);
        isShuffleOn = prefs.getBoolean(KEY_SHUFFLE, false);
        repeatMode = prefs.getInt(KEY_REPEAT, 0);
    }

    private void savePlaybackState() {
        SharedPreferences prefs = getSharedPreferences(PLAYBACK_PREFS, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_SHUFFLE, isShuffleOn)
                .putInt(KEY_REPEAT, repeatMode)
                .apply();
    }

    private void createDefaultPlaylists() {
        new Thread(() -> {
            try {
                if (musicDatabase.playlistDao().getPlaylistById(FAVORITES_PLAYLIST_ID) == null) {
                    Playlist favorites = new Playlist("Favorites");
                    favorites.setId(FAVORITES_PLAYLIST_ID);
                    favorites.setDefault(true);
                    musicDatabase.playlistDao().insert(favorites);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating default playlists: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceBound && mediaPlayerService != null) {
            updateFromService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlaybackState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        if (isServiceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service: " + e.getMessage());
            }
        }
    }
}