package com.example.echo_wave.ui.activities;

import android.Manifest;

import com.example.echo_wave.utils.AdManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.OnlineSong;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.network.OnlineMusicClient;
import com.example.echo_wave.services.MediaPlayerService;
import com.example.echo_wave.ui.adapters.OnlineSongAdapter;
import com.example.echo_wave.ui.fragments.LibraryFragment;
import com.example.echo_wave.ui.fragments.SearchFragment;
import com.example.echo_wave.ui.fragments.SettingsFragment;
import com.example.echo_wave.utils.FastScroller;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.example.echo_wave.utils.MusicScanner;
import com.example.echo_wave.utils.SettingsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int FAVORITES_PLAYLIST_ID = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MAIN_ACTIVITY";
    private AdView adView;

    // Views
    private SwipeRefreshLayout swipeRefresh;
    private NestedScrollView mainContent;
    private RecyclerView rvFeatured, rvRecent, rvMostPlayed, rvFavorites, rvNewlyAdded, rvOnlineResults;
    private TextView tvViewAllRecent, tvViewAllMostPlayed, tvViewAllFavorites, tvViewAllNew, tvRefreshFeatured;
    private HomeSongAdapter featuredAdapter, recentAdapter, mostPlayedAdapter, favoritesAdapter, newlyAddedAdapter;
    private List<Song> featuredSongs = new ArrayList<>();
    private List<Song> recentSongs = new ArrayList<>();
    private List<Song> mostPlayedSongs = new ArrayList<>();
    private List<Song> favoriteSongs = new ArrayList<>();
    private List<Song> newlyAddedSongs = new ArrayList<>();

    // Online Section
    private LinearLayout onlineSection;
    private ProgressBar onlineProgress;
    private TextView tvOnlineStatus;
    private ImageView ivNetworkIcon;
    private List<OnlineSong> onlineTrendingSongs = new ArrayList<>();
    private OnlineSongAdapter onlineTrendingAdapter;
    private boolean onlineContentLoaded = false;

    // All Songs View
    private FrameLayout songsContainer;
    private RecyclerView rvSongs;
    private FastScroller fastScroller;
    private SongAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();
    private TextView tvSongCount;

    // Favorites View
    private FrameLayout favoritesContainer;
    private RecyclerView rvFavoritesList;
    private SongAdapter favoritesListAdapter;
    private List<Song> favoritesList = new ArrayList<>();

    // All Artists View
    private FrameLayout artistsContainer;
    private RecyclerView rvAllArtists;
    private AllArtistsAdapter allArtistsAdapter;
    private List<String> artists = new ArrayList<>();

    // All Playlists View
    private FrameLayout playlistsContainer;
    private RecyclerView rvAllPlaylists;
    private AllPlaylistsAdapter allPlaylistsAdapter;

    // Quick Actions
    private MaterialCardView quickSongs, quickFavorites, quickPlaylists, quickArtists;
    private TextView tvFavoriteCount;

    // Mini Player
    private MaterialCardView miniPlayerContainer;
    private ImageView ivMiniPlayerArt;
    private TextView tvMiniPlayerTitle, tvMiniPlayerArtist;
    private ImageView btnMiniPlayerPlayPause, btnMiniPlayerNext, btnMiniPlayerClose;
    private View miniPlayerPlayingIndicator;

    // Bottom Navigation
    private BottomNavigationView bottomNavigation;

    // Fragment Container
    private FrameLayout fragmentContainer;
    private LinearLayout header;

    // Network
    private boolean isOnline = false;
    private TextView tvNetworkStatus;
    private MaterialCardView onlineCard;
    private OnlineMusicClient onlineMusicClient;

    // Service
    private MediaPlayerService mediaPlayerService;
    private boolean isServiceBound = false;
    private boolean isPlaying = false;
    private Song currentPlayingSong = null;

    private ActionMode actionMode;
    private List<Song> selectedSongs = new ArrayList<>();

    private String currentSort = "date_added";
    private boolean sortAscending = false;
    private SettingsManager settingsManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateMiniPlayerProgress;
    private Runnable onlineRefreshRunnable;

    private MediaPlayer mediaPlayer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Random random = new Random();
    private MusicDatabase musicDatabase;

    // ========== SERVICE CONNECTION ==========
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isServiceBound = true;
            MediaPlayerHelper.getInstance().setMediaPlayerService(mediaPlayerService);
            if (mediaPlayerService.getCurrentSong() != null) {
                currentPlayingSong = mediaPlayerService.getCurrentSong();
                isPlaying = mediaPlayerService.isPlaying();
                showMiniPlayer(currentPlayingSong);
                updatePlayPauseButton(isPlaying);
            }
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
            if (action == null) return;

            switch (action) {
                case "PLAYBACK_STARTED":
                case "PLAYBACK_RESUMED":
                    if (mediaPlayerService != null && mediaPlayerService.getCurrentSong() != null) {
                        currentPlayingSong = mediaPlayerService.getCurrentSong();
                        isPlaying = true;
                        showMiniPlayer(currentPlayingSong);
                        updatePlayPauseButton(true);
                        startMiniPlayerProgress();
                    }
                    break;
                case "PLAYBACK_PAUSED":
                    isPlaying = false;
                    updatePlayPauseButton(false);
                    stopMiniPlayerProgress();
                    break;
                case "PLAYBACK_STOPPED":
                    if (miniPlayerContainer != null) miniPlayerContainer.setVisibility(View.GONE);
                    isPlaying = false;
                    currentPlayingSong = null;
                    stopMiniPlayerProgress();
                    break;
                case "SONG_CHANGED":
                    if (mediaPlayerService != null && mediaPlayerService.getCurrentSong() != null) {
                        currentPlayingSong = mediaPlayerService.getCurrentSong();
                        showMiniPlayer(currentPlayingSong);
                    }
                    break;
            }
        }
    };

    // ========== LIFECYCLE ==========
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        musicDatabase = MusicDatabase.getInstance(this);
        onlineMusicClient = OnlineMusicClient.getInstance(this);
        settingsManager = SettingsManager.getInstance(this);

        initViews();
        setupListeners();
        loadBannerAd();
        setupBottomNav();
        setupMiniPlayer();
        setupPlayAllButton();
        setupNetworkMonitoring();

        MediaPlayerHelper.getInstance().init(this);
        checkAllPermissions();
        bindService();
        registerReceivers();
        setupMiniPlayerProgress();

        startOnlineRefreshTimer();
    }

    private void initViews() {
        // Swipe Refresh
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> {
            loadHomeData();
            refreshOnlineSection();
            swipeRefresh.setRefreshing(false);
        });

        // Home Screen
        mainContent = findViewById(R.id.main_content);
        rvFeatured = findViewById(R.id.rv_featured);
        rvRecent = findViewById(R.id.rv_recent);
        rvMostPlayed = findViewById(R.id.rv_most_played);
        rvFavorites = findViewById(R.id.rv_favorites);
        rvNewlyAdded = findViewById(R.id.rv_newly_added);
        rvOnlineResults = findViewById(R.id.rv_online_results);

        tvViewAllRecent = findViewById(R.id.tv_view_all_recent);
        tvViewAllMostPlayed = findViewById(R.id.tv_view_all_most_played);
        tvViewAllFavorites = findViewById(R.id.tv_view_all_favorites);
        tvViewAllNew = findViewById(R.id.tv_view_all_new);
        tvRefreshFeatured = findViewById(R.id.tv_refresh_featured);

        // Online Section
        onlineSection = findViewById(R.id.online_section);
        onlineProgress = findViewById(R.id.online_progress);
        tvOnlineStatus = findViewById(R.id.tv_online_status);
        ivNetworkIcon = findViewById(R.id.iv_network_icon);
        tvNetworkStatus = findViewById(R.id.tv_network_status);
        onlineCard = findViewById(R.id.online_card);

        // Songs View
        songsContainer = findViewById(R.id.songs_container);
        rvSongs = findViewById(R.id.rv_songs);
        fastScroller = findViewById(R.id.fast_scroller);
        tvSongCount = findViewById(R.id.tv_song_count);

        // Favorites View
        favoritesContainer = findViewById(R.id.favorites_container);
        rvFavoritesList = findViewById(R.id.rv_favorites_list);

        // Artists View
        artistsContainer = findViewById(R.id.artists_container);
        rvAllArtists = findViewById(R.id.rv_all_artists);

        // Playlists View
        playlistsContainer = findViewById(R.id.playlists_container);
        rvAllPlaylists = findViewById(R.id.rv_all_playlists);

        // Quick Actions
        quickSongs = findViewById(R.id.quick_songs);
        quickFavorites = findViewById(R.id.quick_favorites);
        quickPlaylists = findViewById(R.id.quick_playlists);
        quickArtists = findViewById(R.id.quick_artists);
        tvFavoriteCount = findViewById(R.id.tv_favorite_count);

        // Mini Player
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        ivMiniPlayerArt = findViewById(R.id.iv_mini_player_art);
        tvMiniPlayerTitle = findViewById(R.id.tv_mini_player_title);
        tvMiniPlayerArtist = findViewById(R.id.tv_mini_player_artist);
        btnMiniPlayerPlayPause = findViewById(R.id.btn_mini_player_play_pause);
        btnMiniPlayerNext = findViewById(R.id.btn_mini_player_next);
        btnMiniPlayerClose = findViewById(R.id.btn_mini_player_close);
        miniPlayerPlayingIndicator = findViewById(R.id.mini_player_playing_indicator);

        // Bottom Navigation
        bottomNavigation = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);
        header = findViewById(R.id.header);

        // Setup Adapters
        featuredAdapter = new HomeSongAdapter(featuredSongs);
        recentAdapter = new HomeSongAdapter(recentSongs);
        mostPlayedAdapter = new HomeSongAdapter(mostPlayedSongs);
        favoritesAdapter = new HomeSongAdapter(favoriteSongs);
        newlyAddedAdapter = new HomeSongAdapter(newlyAddedSongs);

        // Single initialization of onlineTrendingAdapter
        onlineTrendingAdapter = new OnlineSongAdapter(onlineTrendingSongs, new OnlineSongAdapter.OnSongClickListener() {
            @Override
            public void onPlayClick(OnlineSong song, int position) {
                streamSong(song);
            }

            @Override
            public void onDownloadClick(OnlineSong song, int position) {
                downloadSong(song, position);
            }
        });

        songAdapter = new SongAdapter(songList);
        favoritesListAdapter = new SongAdapter(favoritesList);
        allArtistsAdapter = new AllArtistsAdapter(artists, songList);

        // Setup LayoutManagers
        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecent.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMostPlayed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFavorites.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNewlyAdded.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvOnlineResults.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        rvFavoritesList.setLayoutManager(new LinearLayoutManager(this));
        rvAllArtists.setLayoutManager(new LinearLayoutManager(this));
        rvAllPlaylists.setLayoutManager(new LinearLayoutManager(this));

        // Set Adapters
        rvFeatured.setAdapter(featuredAdapter);
        rvRecent.setAdapter(recentAdapter);
        rvMostPlayed.setAdapter(mostPlayedAdapter);
        rvFavorites.setAdapter(favoritesAdapter);
        rvNewlyAdded.setAdapter(newlyAddedAdapter);
        rvOnlineResults.setAdapter(onlineTrendingAdapter);

        rvSongs.setAdapter(songAdapter);
        rvFavoritesList.setAdapter(favoritesListAdapter);
        rvAllArtists.setAdapter(allArtistsAdapter);
        rvAllPlaylists.setAdapter(allPlaylistsAdapter);
    }

    private void setupNetworkMonitoring() {
        checkNetworkStatus();
        onlineCard.setOnClickListener(v -> {
            if (isOnline) {
                showSearchFragment();
            } else {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            isOnline = capabilities != null &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isOnline = activeNetwork != null && activeNetwork.isConnected();
        }
        updateNetworkUI();

        if (isOnline && !onlineContentLoaded && onlineTrendingSongs.isEmpty()) {
            refreshOnlineSection();
        } else if (!isOnline && onlineTrendingSongs.isEmpty()) {
            tvOnlineStatus.setText("Offline - connect to discover new music");
        }
    }
    private void loadBannerAd() {
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void updateNetworkUI() {
        if (isOnline) {
            tvNetworkStatus.setText("Online");
            tvNetworkStatus.setTextColor(getColorCompat(R.color.electric_cyan));
            ivNetworkIcon.setImageResource(R.drawable.ic_online);
            onlineSection.setVisibility(View.VISIBLE);
            onlineCard.setVisibility(View.VISIBLE);
            tvOnlineStatus.setText("You're online! Discover new music");
        } else {
            tvNetworkStatus.setText("Offline");
            tvNetworkStatus.setTextColor(getColorCompat(R.color.text_tertiary));
            ivNetworkIcon.setImageResource(R.drawable.ic_offline);
            onlineSection.setVisibility(View.GONE);
            onlineCard.setVisibility(View.GONE);
            tvOnlineStatus.setText("You're offline. Playing local music");
        }
    }

    private void refreshOnlineSection() {
        if (!isOnline) return;
        if (onlineProgress.getVisibility() == View.VISIBLE) return;

        showOnlineLoading(true);
        String[] trendingQueries = {"top hits", "latest songs", "afrobeat", "gospel", "Zambian music"};
        String randomQuery = trendingQueries[random.nextInt(trendingQueries.length)];

        onlineMusicClient.searchMusic(randomQuery, 10, new OnlineMusicClient.SearchCallback() {
            @Override
            public void onSuccess(List<OnlineSong> songs) {
                showOnlineLoading(false);
                onlineTrendingSongs.clear();
                onlineTrendingSongs.addAll(songs);
                onlineTrendingAdapter.updateSongs(onlineTrendingSongs);
                onlineContentLoaded = true;
                tvOnlineStatus.setText("Trending: " + randomQuery);
            }

            @Override
            public void onError(String error) {
                showOnlineLoading(false);
                tvOnlineStatus.setText("Failed to load: " + error);
            }
        });
    }

    private void showOnlineLoading(boolean show) {
        if (onlineProgress != null) {
            onlineProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void startOnlineRefreshTimer() {
        onlineRefreshRunnable = () -> {
            if (isOnline) {
                if (onlineContentLoaded && onlineTrendingSongs.size() > 0) {
                    refreshOnlineSection();
                } else if (onlineTrendingSongs.isEmpty()) {
                    refreshOnlineSection();
                }
            }
            handler.postDelayed(onlineRefreshRunnable, 30 * 60 * 1000);
        };
        handler.post(onlineRefreshRunnable);
    }

    // Add streaming method
    private void streamSong(OnlineSong song) {
        if (!isOnline) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Loading: " + song.getTitle() + "...", Toast.LENGTH_SHORT).show();

        onlineMusicClient.streamSong(song, new OnlineMusicClient.StreamCallback() {
            @Override
            public void onSuccess(String streamUrl) {
                runOnUiThread(() -> {
                    // Build a temporary Song object pointing at the stream URL
                    Song tempSong = new Song();
                    tempSong.setId(song.getId());
                    tempSong.setTitle(song.getTitle());
                    tempSong.setArtist(song.getArtist());
                    tempSong.setDuration(song.getDurationSeconds());
                    tempSong.setPath(streamUrl);          // ExoPlayer handles http:// URLs
                    tempSong.setAlbumArt(song.getThumbnail());
                    tempSong.setDateAdded(System.currentTimeMillis());
                    tempSong.setPlayCount(0);
                    tempSong.setLastPlayed(0);
                    tempSong.setFavorite(false);

                    playSong(tempSong);

                    Intent intent = new Intent(MainActivity.this, NowPlayingActivity.class);
                    intent.putExtra("song_title", song.getTitle());
                    intent.putExtra("song_artist", song.getArtist());
                    intent.putExtra("song_album_art", song.getThumbnail());
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("MainActivity", "Stream failed: " + error);
                    Toast.makeText(MainActivity.this,
                            "Stream failed — try downloading instead", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void downloadSong(OnlineSong song, int position) {
        if (!isOnline) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Downloading: " + song.getTitle(), Toast.LENGTH_SHORT).show();

        onlineMusicClient.downloadSong(song, new OnlineMusicClient.DownloadCallback() {
            @Override
            public void onSuccess(OnlineSong downloadedSong, String filePath) {
                runOnUiThread(() -> {
                    addToLocalLibrary(downloadedSong, filePath);
                    song.setDownloaded(true);
                    if (onlineTrendingAdapter != null) {
                        onlineTrendingAdapter.notifyItemChanged(position);
                    }
                    Toast.makeText(MainActivity.this,
                            "Downloaded: " + downloadedSong.getTitle(), Toast.LENGTH_SHORT).show();
                    loadHomeData();
                });
            }

            @Override
            public void onProgress(int progress) {
                // Optional: show progress notification
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("MainActivity", "Download error: " + error);
                    Toast.makeText(MainActivity.this,
                            "Download failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void addToLocalLibrary(OnlineSong song, String filePath) {
        Song localSong = convertToLocalSong(song, filePath);
        new Thread(() -> {
            try {
                Song existing = musicDatabase.songDao().getSongById(song.getId());
                if (existing == null) {
                    musicDatabase.songDao().insert(localSong);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving song: " + e.getMessage());
            }
        }).start();
    }

    private Song convertToLocalSong(OnlineSong song, String filePath) {
        Song localSong = new Song();
        localSong.setId(song.getId());
        localSong.setTitle(song.getTitle());
        localSong.setArtist(song.getArtist());
        localSong.setDuration(song.getDurationSeconds());
        localSong.setPath(filePath);
        localSong.setAlbumArt(song.getThumbnail());
        localSong.setDateAdded(System.currentTimeMillis());
        localSong.setPlayCount(0);
        localSong.setLastPlayed(0);
        localSong.setFavorite(false);
        return localSong;
    }

    private void setupListeners() {
        quickSongs.setOnClickListener(v -> showSongsView());
        quickFavorites.setOnClickListener(v -> showFavoritesView());
        quickPlaylists.setOnClickListener(v -> showPlaylistsView());
        quickArtists.setOnClickListener(v -> showArtistsView());

        tvViewAllRecent.setOnClickListener(v -> showSongsView());
        tvViewAllMostPlayed.setOnClickListener(v -> showSongsView());
        tvViewAllFavorites.setOnClickListener(v -> showFavoritesView());
        tvViewAllNew.setOnClickListener(v -> showSongsView());
        tvRefreshFeatured.setOnClickListener(v -> refreshFeaturedSection());
    }

    private void setupBottomNav() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHomeView();
                return true;
            } else if (id == R.id.nav_search) {
                showSearchFragment();
                return true;
            } else if (id == R.id.nav_library) {
                showLibraryFragment();
                return true;
            } else if (id == R.id.nav_settings) {
                showSettingsFragment();
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    // ========== VIEW MANAGEMENT ==========
    private void showHomeView() {
        if (songsContainer != null) songsContainer.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (mainContent != null) mainContent.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);

        if (isOnline && !onlineContentLoaded && onlineTrendingSongs.isEmpty()) {
            refreshOnlineSection();
        } else if (onlineTrendingSongs.isEmpty() && !isOnline) {
            tvOnlineStatus.setText("Offline - playing local music");
        }
    }

    private void showSongsView() {
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (songsContainer != null) songsContainer.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);
        sortSongs(currentSort, sortAscending);
    }

    private void showFavoritesView() {
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (songsContainer != null) songsContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);
        loadFavoritesList();
    }

    private void showArtistsView() {
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (songsContainer != null) songsContainer.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);
    }

    private void showPlaylistsView() {
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (songsContainer != null) songsContainer.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.VISIBLE);
        loadPlaylists();
    }

    private void showSearchFragment() {
        AdManager.showInterstitialAd(this, () -> {
            // Navigate after ad closes
            if (mainContent != null) mainContent.setVisibility(View.GONE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new SearchFragment(), "Search");
        });
    }

    private void showLibraryFragment() {
        AdManager.showInterstitialAd(this, () -> {
            if (mainContent != null) mainContent.setVisibility(View.GONE);
            if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
            loadFragment(new LibraryFragment(), "Library");
        });
    }

    private void showSettingsFragment() {
        if (mainContent != null) mainContent.setVisibility(View.GONE);
        if (songsContainer != null) songsContainer.setVisibility(View.GONE);
        if (favoritesContainer != null) favoritesContainer.setVisibility(View.GONE);
        if (artistsContainer != null) artistsContainer.setVisibility(View.GONE);
        if (playlistsContainer != null) playlistsContainer.setVisibility(View.GONE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (header != null) header.setVisibility(View.GONE);
        loadFragment(new SettingsFragment(), "Settings");
    }

    private void loadFragment(Fragment fragment, String tag) {
        if (fragment != null && !isFinishing()) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment, tag);
            transaction.commitAllowingStateLoss();
        }
    }

    // ========== DATA LOADING ==========
    private void loadHomeData() {
        new Thread(() -> {
            try {
                List<Song> allSongs = musicDatabase.songDao().getAllSongs();

                runOnUiThread(() -> {
                    if (tvSongCount != null) tvSongCount.setText(String.valueOf(allSongs.size()));
                });

                if (!allSongs.isEmpty()) {
                    List<Song> recent = new ArrayList<>(allSongs);
                    Collections.sort(recent, (a, b) -> Long.compare(b.getLastPlayed(), a.getLastPlayed()));
                    recentSongs.clear();
                    recentSongs.addAll(recent.subList(0, Math.min(10, recent.size())));

                    List<Song> mostPlayed = new ArrayList<>(allSongs);
                    Collections.sort(mostPlayed, (a, b) -> Integer.compare(b.getPlayCount(), a.getPlayCount()));
                    mostPlayedSongs.clear();
                    mostPlayedSongs.addAll(mostPlayed.subList(0, Math.min(10, mostPlayed.size())));

                    List<Song> allFavorites = new ArrayList<>();
                    for (Song song : allSongs) {
                        if (song.isFavorite()) allFavorites.add(song);
                    }
                    favoriteSongs.clear();
                    if (!allFavorites.isEmpty()) {
                        favoriteSongs.addAll(allFavorites.subList(0, Math.min(10, allFavorites.size())));
                    }

                    List<Song> newlyAdded = new ArrayList<>(allSongs);
                    Collections.sort(newlyAdded, (a, b) -> Long.compare(b.getDateAdded(), a.getDateAdded()));
                    newlyAddedSongs.clear();
                    newlyAddedSongs.addAll(newlyAdded.subList(0, Math.min(10, newlyAdded.size())));

                    refreshFeaturedSectionInternal(allSongs);

                    Set<String> artistSet = new HashSet<>();
                    for (Song song : allSongs) {
                        if (song.getArtist() != null && !song.getArtist().isEmpty()) {
                            artistSet.add(song.getArtist());
                        }
                    }
                    artists.clear();
                    artists.addAll(artistSet);
                    Collections.sort(artists);

                    runOnUiThread(() -> {
                        if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(allFavorites.size()));

                        if (recentAdapter != null) recentAdapter.notifyDataSetChanged();
                        if (mostPlayedAdapter != null) mostPlayedAdapter.notifyDataSetChanged();
                        if (favoritesAdapter != null) favoritesAdapter.notifyDataSetChanged();
                        if (newlyAddedAdapter != null) newlyAddedAdapter.notifyDataSetChanged();
                        if (featuredAdapter != null) featuredAdapter.notifyDataSetChanged();

                        if (allArtistsAdapter != null) {
                            allArtistsAdapter.updateArtists(artists);
                            try {
                                allArtistsAdapter.updateSongs(allSongs);
                            } catch (Exception e) {
                                Log.e(TAG, "updateSongs error: " + e.getMessage());
                            }
                        }

                        songList.clear();
                        songList.addAll(allSongs);
                        if (songAdapter != null) songAdapter.notifyDataSetChanged();
                        if (fastScroller != null) fastScroller.updateSections();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading home data: " + e.getMessage());
            }
        }).start();
    }

    private void refreshFeaturedSection() {
        new Thread(() -> {
            List<Song> allSongs = musicDatabase.songDao().getAllSongs();
            refreshFeaturedSectionInternal(allSongs);
            runOnUiThread(() -> featuredAdapter.notifyDataSetChanged());
        }).start();
    }

    private void refreshFeaturedSectionInternal(List<Song> allSongs) {
        featuredSongs.clear();
        if (!allSongs.isEmpty()) {
            List<Song> shuffled = new ArrayList<>(allSongs);
            Collections.shuffle(shuffled, random);
            featuredSongs.addAll(shuffled.subList(0, Math.min(10, shuffled.size())));
        }
    }

    private void loadPlaylists() {
        new Thread(() -> {
            try {
                List<Playlist> loadedPlaylists = musicDatabase.playlistDao().getAllPlaylists();
                runOnUiThread(() -> {
                    if (allPlaylistsAdapter != null) {
                        TextView tvPlaylistCount = findViewById(R.id.tv_playlist_count);
                        if (tvPlaylistCount != null) tvPlaylistCount.setText(String.valueOf(loadedPlaylists.size()));
                        allPlaylistsAdapter.updatePlaylists(loadedPlaylists);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading playlists: " + e.getMessage());
            }
        }).start();
    }

    private void loadFavoritesList() {
        new Thread(() -> {
            try {
                List<Song> allSongs = musicDatabase.songDao().getAllSongs();
                List<Song> favorites = new ArrayList<>();
                for (Song song : allSongs) {
                    if (song.isFavorite()) favorites.add(song);
                }
                runOnUiThread(() -> {
                    favoritesList.clear();
                    favoritesList.addAll(favorites);
                    if (favoritesListAdapter != null) favoritesListAdapter.notifyDataSetChanged();
                    if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(favorites.size()));
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorites list: " + e.getMessage());
            }
        }).start();
    }

    // ========== PLAYER METHODS ==========
    private void playSong(Song song) {
        if (song == null) return;
        if (isServiceBound && mediaPlayerService != null) {
            mediaPlayerService.playSong(song);
        } else {
            Intent intent = new Intent(this, MediaPlayerService.class);
            intent.setAction(MediaPlayerService.ACTION_PLAY);
            intent.putExtra("song_id", song.getId());
            intent.putExtra("song_title", song.getTitle());
            intent.putExtra("song_artist", song.getArtist());
            intent.putExtra("song_album", song.getAlbum());
            intent.putExtra("song_duration", song.getDuration());
            intent.putExtra("song_path", song.getPath());
            intent.putExtra("song_album_art", song.getAlbumArt());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
        updateSongStats(song);
    }

    private void updateSongStats(Song song) {
        new Thread(() -> {
            try {
                song.setPlayCount(song.getPlayCount() + 1);
                song.setLastPlayed(System.currentTimeMillis());
                musicDatabase.songDao().update(song);
            } catch (Exception e) {
                Log.e(TAG, "Error updating song stats: " + e.getMessage());
            }
        }).start();
    }

    private void updateFavoritesPlaylist() {
        new Thread(() -> {
            try {
                Playlist favoritesPlaylist = musicDatabase.playlistDao().getPlaylistById(FAVORITES_PLAYLIST_ID);
                if (favoritesPlaylist != null) {
                    musicDatabase.playlistSongDao().clearPlaylist(FAVORITES_PLAYLIST_ID);
                    List<Song> allSongs = musicDatabase.songDao().getAllSongs();
                    List<Song> favoriteSongsList = new ArrayList<>();
                    for (Song song : allSongs) {
                        if (song.isFavorite()) favoriteSongsList.add(song);
                    }
                    for (Song song : favoriteSongsList) {
                        PlaylistSong playlistSong = new PlaylistSong(FAVORITES_PLAYLIST_ID, song.getId());
                        musicDatabase.playlistSongDao().insert(playlistSong);
                    }
                    favoritesPlaylist.setSongCount(favoriteSongsList.size());
                    musicDatabase.playlistDao().update(favoritesPlaylist);
                    runOnUiThread(() -> {
                        if (tvFavoriteCount != null) tvFavoriteCount.setText(String.valueOf(favoriteSongsList.size()));
                        loadHomeData();
                        if (favoritesContainer != null && favoritesContainer.getVisibility() == View.VISIBLE) {
                            loadFavoritesList();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating favorites playlist: " + e.getMessage());
            }
        }).start();
    }

    private void toggleFavorite(Song song, int position) {
        if (song == null) return;
        new Thread(() -> {
            try {
                boolean newFavoriteStatus = !song.isFavorite();
                song.setFavorite(newFavoriteStatus);
                musicDatabase.songDao().update(song);
                updateFavoritesPlaylist();
                runOnUiThread(() -> {
                    if (position >= 0 && position < songList.size()) {
                        songList.set(position, song);
                        if (songAdapter != null) songAdapter.notifyItemChanged(position);
                    }
                    loadHomeData();
                    if (favoritesContainer != null && favoritesContainer.getVisibility() == View.VISIBLE) {
                        loadFavoritesList();
                    }
                    Toast.makeText(MainActivity.this,
                            newFavoriteStatus ? "Added to Favorites" : "Removed from Favorites",
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error toggling favorite: " + e.getMessage());
            }
        }).start();
    }

    // ========== MINI PLAYER ==========
    private void openNowPlaying() {
        Intent intent = new Intent(this, NowPlayingActivity.class);
        if (currentPlayingSong != null) {
            intent.putExtra("song_title", currentPlayingSong.getTitle());
            intent.putExtra("song_artist", currentPlayingSong.getArtist());
        }
        startActivity(intent);
    }

    private void setupPlayAllButton() {
        ImageView btnPlayAll = findViewById(R.id.btn_play_all);
        if (btnPlayAll != null) {
            btnPlayAll.setOnClickListener(v -> playAllSongs());
        }
    }

    private void playAllSongs() {
        if (songList.isEmpty()) {
            Toast.makeText(this, "No songs to play", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isServiceBound && mediaPlayerService != null) {
            mediaPlayerService.setPlaylist(new ArrayList<>(songList), 0);
            mediaPlayerService.playSong(songList.get(0));
        } else {
            Intent intent = new Intent(this, MediaPlayerService.class);
            intent.setAction(MediaPlayerService.ACTION_PLAY);
            intent.putExtra("song_id", songList.get(0).getId());
            intent.putExtra("song_title", songList.get(0).getTitle());
            intent.putExtra("song_artist", songList.get(0).getArtist());
            intent.putExtra("song_album", songList.get(0).getAlbum());
            intent.putExtra("song_duration", songList.get(0).getDuration());
            intent.putExtra("song_path", songList.get(0).getPath());
            intent.putExtra("song_album_art", songList.get(0).getAlbumArt());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
        Toast.makeText(this, "Playing all " + songList.size() + " songs", Toast.LENGTH_SHORT).show();
    }

    private void showMiniPlayer(Song song) {
        if (miniPlayerContainer == null || song == null) return;
        miniPlayerContainer.setVisibility(View.VISIBLE);
        tvMiniPlayerTitle.setText(song.getTitle());
        tvMiniPlayerArtist.setText(song.getArtist());
        if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            Glide.with(this).load(song.getAlbumArt()).into(ivMiniPlayerArt);
        } else {
            ivMiniPlayerArt.setImageResource(R.drawable.default_album_art_small);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (btnMiniPlayerPlayPause == null) return;
        btnMiniPlayerPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_small : R.drawable.ic_play_small);
    }

    private void setupMiniPlayer() {
        miniPlayerContainer.setOnClickListener(v -> openNowPlaying());
        btnMiniPlayerPlayPause.setOnClickListener(v -> {
            if (mediaPlayerService != null) {
                if (isPlaying) mediaPlayerService.pauseMedia();
                else mediaPlayerService.playMedia();
            }
        });
        btnMiniPlayerNext.setOnClickListener(v -> {
            if (mediaPlayerService != null) mediaPlayerService.playNext();
        });
        btnMiniPlayerClose.setOnClickListener(v -> {
            if (mediaPlayerService != null) {
                mediaPlayerService.stopMedia();
                miniPlayerContainer.setVisibility(View.GONE);
            }
        });
    }

    private void setupMiniPlayerProgress() {
        updateMiniPlayerProgress = () -> {
            if (isPlaying && miniPlayerContainer != null && miniPlayerContainer.getVisibility() == View.VISIBLE) {
                handler.postDelayed(updateMiniPlayerProgress, 500);
            }
        };
    }

    private void startMiniPlayerProgress() {
        if (miniPlayerPlayingIndicator != null) miniPlayerPlayingIndicator.setVisibility(View.VISIBLE);
        handler.removeCallbacks(updateMiniPlayerProgress);
        handler.post(updateMiniPlayerProgress);
    }

    private void stopMiniPlayerProgress() {
        if (miniPlayerPlayingIndicator != null) miniPlayerPlayingIndicator.setVisibility(View.GONE);
        handler.removeCallbacks(updateMiniPlayerProgress);
    }

    // ========== SORTING ==========
    private void sortSongs(String criteria, boolean ascending) {
        Comparator<Song> comparator = null;
        switch (criteria) {
            case "title": comparator = (s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()); break;
            case "artist": comparator = (s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()); break;
            case "album": comparator = (s1, s2) -> s1.getAlbum().compareToIgnoreCase(s2.getAlbum()); break;
            case "duration": comparator = (s1, s2) -> Long.compare(s1.getDuration(), s2.getDuration()); break;
            case "date_added": comparator = (s1, s2) -> Long.compare(s1.getDateAdded(), s2.getDateAdded()); break;
            case "play_count": comparator = (s1, s2) -> Integer.compare(s1.getPlayCount(), s2.getPlayCount()); break;
        }
        if (comparator != null) {
            if (!ascending && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                comparator = comparator.reversed();
            }
            Collections.sort(songList, comparator);
            songAdapter.notifyDataSetChanged();
            if (fastScroller != null) fastScroller.updateSections();
        }
    }

    // ========== PERMISSIONS ==========
    private void checkAllPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            startMusicScan();
        }
    }

    private void startMusicScan() {
        showLoading(true);
        try {
            MusicScanner scanner = new MusicScanner(this);
            scanner.scanMusic(new MusicScanner.ScanCallback() {
                @Override public void onScanStarted() {}
                @Override public void onScanProgress(int progress, int max) {}
                @Override
                public void onScanComplete(List<Song> songs) {
                    runOnUiThread(() -> {
                        if (songs != null && !songs.isEmpty()) {
                            for (Song song : songs) {
                                if (song.getDateAdded() == 0) song.setDateAdded(System.currentTimeMillis());
                            }
                            songList.clear();
                            songList.addAll(songs);
                            if (tvSongCount != null) tvSongCount.setText(String.valueOf(songs.size()));
                            sortSongs(currentSort, sortAscending);
                            songAdapter.notifyDataSetChanged();
                            if (fastScroller != null) fastScroller.updateSections();
                            new Thread(() -> {
                                try {
                                    musicDatabase.songDao().insertAll(songs);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving to database: " + e.getMessage());
                                }
                            }).start();
                            loadHomeData();
                            loadPlaylists();
                        }
                        showLoading(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting scan: " + e.getMessage());
            showLoading(false);
        }
    }

    private void showLoading(boolean show) {
        View loadingView = findViewById(R.id.loading_view);
        View loadingOverlay = findViewById(R.id.loading_overlay);
        if (loadingView != null) loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (loadingOverlay != null) loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // ========== SERVICE METHODS ==========
    private void bindService() {
        try {
            startService(new Intent(this, MediaPlayerService.class));
            bindService(new Intent(this, MediaPlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Error binding service: " + e.getMessage());
        }
    }

    private void registerReceivers() {
        try {
            LocalBroadcastManager.getInstance(this).registerReceiver(playbackReceiver, new IntentFilter("PLAYBACK_STATE_CHANGED"));
        } catch (Exception e) {
            Log.e(TAG, "Error registering receivers: " + e.getMessage());
        }
    }

    // ========== PLAYLIST METHODS ==========
    private void addSelectedToPlaylist() { /* Keep existing implementation */ }
    private void showCreatePlaylistDialog() { /* Keep existing implementation */ }
    private void addSelectedToQueue() { /* Keep existing implementation */ }
    private void deleteSelectedSongs() { /* Keep existing implementation */ }
    private void deleteSongFromDevice(Song song) { /* Keep existing implementation */ }
    private void selectAllSongs() { /* Keep existing implementation */ }
    private void showSongOptions(Song song, int position) { /* Keep existing implementation */ }
    private void shareSong(Song song) { /* Keep existing implementation */ }

    // ========== ADAPTER CLASSES ==========
    private class HomeSongAdapter extends RecyclerView.Adapter<HomeSongAdapter.ViewHolder> {
        private List<Song> songs;
        HomeSongAdapter(List<Song> songs) { this.songs = songs != null ? songs : new ArrayList<>(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horizontal_song_small, parent, false);
            return new ViewHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(MainActivity.this).load(song.getAlbumArt()).placeholder(R.drawable.default_album_art_small).into(holder.ivImage);
            } else {
                holder.ivImage.setImageResource(R.drawable.default_album_art_small);
            }
            holder.itemView.setOnClickListener(v -> playSong(song));
            holder.btnFavorite.setOnClickListener(v -> toggleFavorite(song, -1));
            holder.btnFavorite.setImageResource(song.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
        }
        @Override public int getItemCount() { return songs.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage, btnFavorite;
            TextView tvTitle, tvArtist;
            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_album_art);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvArtist = itemView.findViewById(R.id.tv_artist);
                btnFavorite = itemView.findViewById(R.id.btn_favorite);
            }
        }
    }

    private class AllArtistsAdapter extends RecyclerView.Adapter<AllArtistsAdapter.ViewHolder> {
        private List<String> artists;
        private List<Song> allSongs;
        private Map<String, Integer> artistSongCounts = new HashMap<>();
        AllArtistsAdapter(List<String> artists, List<Song> songs) {
            this.artists = artists != null ? artists : new ArrayList<>();
            this.allSongs = songs != null ? songs : new ArrayList<>();
            calculateArtistCounts();
        }
        public void updateArtists(List<String> newArtists) {
            this.artists = newArtists != null ? newArtists : new ArrayList<>();
            calculateArtistCounts();
            notifyDataSetChanged();
        }
        public void updateSongs(List<Song> songs) {
            this.allSongs = songs != null ? songs : new ArrayList<>();
            calculateArtistCounts();
        }
        private void calculateArtistCounts() {
            artistSongCounts.clear();
            for (Song song : allSongs) {
                String artist = song.getArtist();
                if (artist != null && !artist.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        artistSongCounts.put(artist, artistSongCounts.getOrDefault(artist, 0) + 1);
                    }
                }
            }
        }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
            return new ViewHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (artists == null || position >= artists.size()) return;
            String artist = artists.get(position);
            if (artist == null) return;
            holder.tvName.setText(artist);
            int songCount = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                songCount = artistSongCounts.getOrDefault(artist, 0);
            }
            holder.tvCount.setText(songCount + " " + (songCount == 1 ? "song" : "songs"));
            holder.itemView.setOnClickListener(v -> {
                List<Song> artistSongs = new ArrayList<>();
                for (Song song : allSongs) {
                    if (artist.equals(song.getArtist())) artistSongs.add(song);
                }
                if (!artistSongs.isEmpty()) {
                    if (isServiceBound && mediaPlayerService != null) {
                        mediaPlayerService.setPlaylist(artistSongs, 0);
                        mediaPlayerService.playSong(artistSongs.get(0));
                    } else {
                        playSong(artistSongs.get(0));
                    }
                    Toast.makeText(MainActivity.this, "Playing " + artist, Toast.LENGTH_SHORT).show();
                }
            });
        }
        @Override public int getItemCount() { return artists != null ? artists.size() : 0; }
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName, tvCount;
            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_artist_image);
                tvName = itemView.findViewById(R.id.tv_artist_name);
                tvCount = itemView.findViewById(R.id.tv_song_count);
            }
        }
    }

    private class AllPlaylistsAdapter extends RecyclerView.Adapter<AllPlaylistsAdapter.ViewHolder> {
        private List<Playlist> playlists;

        AllPlaylistsAdapter(List<Playlist> playlists) {
            this.playlists = playlists != null ? playlists : new ArrayList<>();
        }

        public void updatePlaylists(List<Playlist> newPlaylists) {
            this.playlists = newPlaylists != null ? newPlaylists : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (playlists == null || position >= playlists.size()) return;
            Playlist playlist = playlists.get(position);
            if (playlist == null) return;
            holder.tvName.setText(playlist.getName());
            holder.tvCount.setText(playlist.getSongCount() + " songs");
            if (holder.ivImage != null) {
                holder.ivImage.setImageResource(R.drawable.default_playlist);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, PlaylistActivity.class);
                intent.putExtra("playlist_id", playlist.getId());
                intent.putExtra("playlist_name", playlist.getName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return playlists != null ? playlists.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName, tvCount;

            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_playlist_image);
                tvName = itemView.findViewById(R.id.tv_playlist_name);
                tvCount = itemView.findViewById(R.id.tv_playlist_count);
            }
        }
    }

    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
        private List<Song> songs;
        SongAdapter(List<Song> songs) { this.songs = songs != null ? songs : new ArrayList<>(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_song_small, parent, false);
            return new ViewHolder(view);
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songs.get(position);
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getDurationFormatted());
            if (selectedSongs.contains(song)) {
                holder.itemView.setBackgroundColor(getColorCompat(R.color.electric_cyan_alpha));
                holder.ivSelected.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setBackgroundColor(getColorCompat(android.R.color.transparent));
                holder.ivSelected.setVisibility(View.GONE);
            }
            if (currentPlayingSong != null && currentPlayingSong.getId().equals(song.getId())) {
                holder.tvTitle.setTextColor(getColorCompat(R.color.electric_cyan));
            } else {
                holder.tvTitle.setTextColor(getColorCompat(R.color.text_primary));
            }
            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(MainActivity.this).load(song.getAlbumArt()).placeholder(R.drawable.default_album_art_small).into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
            }
            holder.btnFavorite.setImageResource(song.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
            holder.btnFavorite.setOnClickListener(v -> toggleFavorite(song, position));
            holder.itemView.setOnClickListener(v -> {
                if (actionMode != null) toggleSelection(song, position);
                else playSong(song);
            });
            holder.itemView.setOnLongClickListener(v -> {
                if (actionMode == null) {
                    actionMode = startSupportActionMode(actionModeCallback);
                    toggleSelection(song, position);
                }
                return true;
            });
            holder.btnMore.setOnClickListener(v -> {
                if (actionMode == null) showSongOptions(song, position);
            });
        }
        private void toggleSelection(Song song, int position) {
            if (selectedSongs.contains(song)) selectedSongs.remove(song);
            else selectedSongs.add(song);
            if (selectedSongs.isEmpty()) {
                if (actionMode != null) actionMode.finish();
                actionMode = null;
            } else if (actionMode != null) {
                actionMode.setTitle(selectedSongs.size() + " selected");
            }
            notifyItemChanged(position);
        }
        @Override public int getItemCount() { return songs.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAlbumArt, ivSelected, btnMore, btnFavorite;
            TextView tvTitle, tvArtist, tvDuration;
            ViewHolder(View itemView) {
                super(itemView);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                ivSelected = itemView.findViewById(R.id.iv_selected);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvArtist = itemView.findViewById(R.id.tv_song_artist);
                tvDuration = itemView.findViewById(R.id.tv_song_duration);
                btnMore = itemView.findViewById(R.id.btn_more);
                btnFavorite = itemView.findViewById(R.id.btn_favorite);
            }
        }
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.action_mode_menu, menu);
            return true;
        }
        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }
        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_add_to_playlist) { addSelectedToPlaylist(); mode.finish(); return true; }
            else if (id == R.id.action_add_to_queue) { addSelectedToQueue(); mode.finish(); return true; }
            else if (id == R.id.action_delete) { deleteSelectedSongs(); mode.finish(); return true; }
            else if (id == R.id.action_select_all) { selectAllSongs(); return true; }
            return false;
        }
        @Override public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedSongs.clear();
            songAdapter.notifyDataSetChanged();
        }
    };

    private int getColorCompat(int colorRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getColor(colorRes);
        } else {
            return getResources().getColor(colorRes);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        checkNetworkStatus();
        if (isServiceBound && mediaPlayerService != null) updateFromService();
    }

    private void updateFromService() {
        if (mediaPlayerService != null && mediaPlayerService.getCurrentSong() != null) {
            currentPlayingSong = mediaPlayerService.getCurrentSong();
            isPlaying = mediaPlayerService.isPlaying();
            showMiniPlayer(currentPlayingSong);
            updatePlayPauseButton(isPlaying);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        savePlaybackState();
    }

    private void savePlaybackState() {
        SharedPreferences prefs = getSharedPreferences("PlaybackPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("shuffle_mode", false).putInt("repeat_mode", 0).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(playbackReceiver);
            if (isServiceBound) unbindService(serviceConnection);
            handler.removeCallbacks(updateMiniPlayerProgress);
            if (onlineRefreshRunnable != null) handler.removeCallbacks(onlineRefreshRunnable);
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            executor.shutdown();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }

    @Override public void onBackPressed() {
        if (songsContainer != null && songsContainer.getVisibility() == View.VISIBLE) { showHomeView(); return; }
        if (favoritesContainer != null && favoritesContainer.getVisibility() == View.VISIBLE) { showHomeView(); return; }
        if (artistsContainer != null && artistsContainer.getVisibility() == View.VISIBLE) { showHomeView(); return; }
        if (playlistsContainer != null && playlistsContainer.getVisibility() == View.VISIBLE) { showHomeView(); return; }
        if (fragmentContainer != null && fragmentContainer.getVisibility() == View.VISIBLE) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                showHomeView();
            } else { showHomeView(); }
            return;
        }
        super.onBackPressed();
    }
}