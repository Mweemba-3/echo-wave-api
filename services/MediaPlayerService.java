package com.example.echo_wave.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.example.echo_wave.R;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.SettingsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "MediaPlayerService";
    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "echo_wave_playback_channel";
    private static final String CHANNEL_NAME = "Music Playback";

    // Actions
    public static final String ACTION_PLAY = "com.example.echo_wave.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.echo_wave.ACTION_PAUSE";
    public static final String ACTION_PLAY_PAUSE = "com.example.echo_wave.ACTION_PLAY_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.echo_wave.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.echo_wave.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.echo_wave.ACTION_STOP";
    public static final String ACTION_SEEK = "com.example.echo_wave.ACTION_SEEK";
    public static final String ACTION_EXIT = "com.example.echo_wave.ACTION_EXIT";
    public static final String ACTION_SHUFFLE = "com.example.echo_wave.ACTION_SHUFFLE";
    public static final String ACTION_REPEAT = "com.example.echo_wave.ACTION_REPEAT";

    // Broadcast Actions
    public static final String ACTION_SONG_CHANGED = "SONG_CHANGED";
    public static final String ACTION_SHUFFLE_CHANGED = "SHUFFLE_CHANGED";
    public static final String ACTION_REPEAT_CHANGED = "REPEAT_CHANGED";
    public static final String ACTION_QUEUE_UPDATED = "QUEUE_UPDATED";
    public static final String PLAYBACK_STATE_CHANGED = "PLAYBACK_STATE_CHANGED";

    // Audio Effects
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private boolean isEqEnabled = true;

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;

    private Song currentSong;
    private List<Song> playlist = new ArrayList<>();
    private List<Song> originalPlaylist = new ArrayList<>();
    private List<Song> queue = new ArrayList<>();
    private int currentPosition = -1;

    private boolean isForeground = false;
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0=none, 1=all, 2=one
    private Random random = new Random();

    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private SettingsManager settingsManager;
    private Bitmap cachedAlbumArt;

    private final IBinder iBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        settingsManager = SettingsManager.getInstance(this);

        isShuffle = settingsManager.getShuffleMode();
        repeatMode = settingsManager.getRepeatMode();

        createNotificationChannel();
        initMediaSession();
        registerReceivers();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls music playback");
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "EchoWaveSession");
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                playMedia();
            }

            @Override
            public void onPause() {
                pauseMedia();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSkipToPrevious() {
                playPrevious();
            }

            @Override
            public void onStop() {
                stopMedia();
                stopSelf();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo((int) pos);
            }
        });

        updatePlaybackState();
    }

    private void updatePlaybackState() {
        int state = (mediaPlayer != null && mediaPlayer.isPlaying()) ?
                PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, getCurrentPosition(), 1.0f);

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            Log.d(TAG, "Action received: " + intent.getAction());

            switch (intent.getAction()) {
                case ACTION_PLAY:
                    handlePlayIntent(intent);
                    break;
                case ACTION_PAUSE:
                    pauseMedia();
                    break;
                case ACTION_PLAY_PAUSE:
                    if (isPlaying()) pauseMedia(); else playMedia();
                    break;
                case ACTION_PREVIOUS:
                    playPrevious();
                    break;
                case ACTION_NEXT:
                    playNext();
                    break;
                case ACTION_STOP:
                    stopMedia();
                    break;
                case ACTION_EXIT:
                    stopMedia();
                    stopForeground(true);
                    stopSelf();
                    break;
                case ACTION_SEEK:
                    int position = intent.getIntExtra("position", 0);
                    seekTo(position);
                    break;
                case ACTION_SHUFFLE:
                    toggleShuffle();
                    break;
                case ACTION_REPEAT:
                    toggleRepeat();
                    break;
            }
        }
        return START_STICKY;
    }

    private void handlePlayIntent(Intent intent) {
        try {
            String songId = intent.getStringExtra("song_id");
            String songTitle = intent.getStringExtra("song_title");
            String songArtist = intent.getStringExtra("song_artist");
            String songAlbum = intent.getStringExtra("song_album");
            long songDuration = intent.getLongExtra("song_duration", 0);
            String songPath = intent.getStringExtra("song_path");
            String songAlbumArt = intent.getStringExtra("song_album_art");

            if (songPath == null || songPath.isEmpty()) {
                Log.e(TAG, "Song path is null or empty");
                return;
            }

            Song song = new Song();
            song.setId(songId);
            song.setTitle(songTitle);
            song.setArtist(songArtist);
            song.setAlbum(songAlbum);
            song.setDuration(songDuration);
            song.setPath(songPath);
            song.setAlbumArt(songAlbumArt);

            // Check if song already in playlist
            int existingPosition = -1;
            for (int i = 0; i < playlist.size(); i++) {
                if (playlist.get(i).getId() != null && playlist.get(i).getId().equals(songId)) {
                    existingPosition = i;
                    break;
                }
            }

            if (existingPosition != -1) {
                currentPosition = existingPosition;
            } else {
                playlist.add(song);
                originalPlaylist.add(song);
                currentPosition = playlist.size() - 1;
            }

            playSong(song);

        } catch (Exception e) {
            Log.e(TAG, "Error handling play intent: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public void playSong(Song song) {
        try {
            Log.d(TAG, "Playing: " + (song != null ? song.getTitle() : "null"));

            if (!requestAudioFocus()) {
                Log.e(TAG, "Could not get audio focus");
                return;
            }

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                mediaPlayer.setOnPreparedListener(this);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
            } else {
                mediaPlayer.reset();
            }

            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();

            currentSong = song;
            updateMediaSessionMetadata();
            loadAlbumArtAsync(song);
            startForeground();
            sendBroadcastToUI("PLAYBACK_STARTED", song);
            updatePlaybackState();

        } catch (IOException e) {
            Log.e(TAG, "Error playing song: " + e.getMessage());
        }
    }

    private void loadAlbumArtAsync(Song song) {
        executor.execute(() -> {
            try {
                if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                    cachedAlbumArt = Glide.with(MediaPlayerService.this)
                            .asBitmap()
                            .load(song.getAlbumArt())
                            .submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();
                } else {
                    cachedAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
                }
            } catch (Exception e) {
                cachedAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
            }

            handler.post(() -> {
                updateNotification();
                updateMediaSessionMetadata();
            });
        });
    }

    private void updateMediaSessionMetadata() {
        if (currentSong == null) return;

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                        currentSong.getTitle() != null ? currentSong.getTitle() : "Unknown")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                        currentSong.getArtist() != null ? currentSong.getArtist() : "Unknown")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                        currentSong.getAlbum() != null ? currentSong.getAlbum() : "Unknown")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.getDuration())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cachedAlbumArt);

        mediaSession.setMetadata(metadataBuilder.build());
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        applyEqualizerSettings();
        playMedia();
        sendBroadcastToUI("PLAYBACK_STARTED", currentSong);
    }

    public void playMedia() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateNotification();
            sendBroadcastToUI("PLAYBACK_RESUMED", currentSong);
            updatePlaybackState();
        }
    }

    public void pauseMedia() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateNotification();
            sendBroadcastToUI("PLAYBACK_PAUSED", currentSong);
            updatePlaybackState();
        }
    }

    public void stopMedia() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        abandonAudioFocus();
        stopForeground(true);
        isForeground = false;
        currentSong = null;
        sendBroadcastToUI("PLAYBACK_STOPPED", null);
        updatePlaybackState();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return;

        if (repeatMode == 2) {
            seekTo(0);
            playMedia();
            sendBroadcastToUI("SONG_CHANGED", currentSong);
            return;
        }

        int nextPosition = currentPosition + 1;

        if (nextPosition >= playlist.size()) {
            if (repeatMode == 1) {
                nextPosition = 0;
            } else {
                pauseMedia();
                seekTo(0);
                return;
            }
        }

        currentPosition = nextPosition;
        playSong(playlist.get(currentPosition));
        sendBroadcastToUI("SONG_CHANGED", currentSong);
    }

    public void playPrevious() {
        if (playlist.isEmpty()) return;

        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            seekTo(0);
            return;
        }

        int prevPosition = currentPosition - 1;

        if (prevPosition < 0) {
            if (repeatMode == 1) {
                prevPosition = playlist.size() - 1;
            } else {
                seekTo(0);
                return;
            }
        }

        currentPosition = prevPosition;
        playSong(playlist.get(currentPosition));
        sendBroadcastToUI("SONG_CHANGED", currentSong);
    }

    public void playAtPosition(int position) {
        if (position >= 0 && position < playlist.size()) {
            currentPosition = position;
            playSong(playlist.get(currentPosition));
            sendBroadcastToUI("SONG_CHANGED", currentSong);
        }
    }

    // Add this method to MediaPlayerService class
    public void toggleShuffle() {
        try {
            isShuffle = !isShuffle;
            settingsManager.savePlaybackSettings(isShuffle, repeatMode);

            if (isShuffle) {
                // Create shuffled playlist
                if (playlist != null && !playlist.isEmpty() && currentSong != null) {
                    List<Song> shuffled = new ArrayList<>();
                    // Add current song first
                    shuffled.add(currentSong);

                    // Add remaining songs
                    List<Song> remaining = new ArrayList<>();
                    for (Song song : playlist) {
                        if (song != null && song.getId() != null && currentSong.getId() != null) {
                            if (!song.getId().equals(currentSong.getId())) {
                                remaining.add(song);
                            }
                        }
                    }

                    // Fisher-Yates shuffle
                    for (int i = remaining.size() - 1; i > 0; i--) {
                        int j = random.nextInt(i + 1);
                        Song temp = remaining.get(i);
                        remaining.set(i, remaining.get(j));
                        remaining.set(j, temp);
                    }

                    shuffled.addAll(remaining);
                    playlist = shuffled;
                    currentPosition = 0;
                }
            } else {
                // Restore original playlist
                if (originalPlaylist != null && !originalPlaylist.isEmpty()) {
                    playlist = new ArrayList<>(originalPlaylist);
                    // Find current song position
                    if (currentSong != null && currentSong.getId() != null) {
                        for (int i = 0; i < playlist.size(); i++) {
                            Song song = playlist.get(i);
                            if (song != null && song.getId() != null &&
                                    song.getId().equals(currentSong.getId())) {
                                currentPosition = i;
                                break;
                            }
                        }
                    }
                }
            }

            // Broadcast shuffle change
            Intent intent = new Intent(PLAYBACK_STATE_CHANGED);
            intent.putExtra("action", "SHUFFLE_CHANGED");
            intent.putExtra("is_shuffle", isShuffle);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error toggling shuffle: " + e.getMessage());
        }
    }

    public void toggleRepeat() {
        repeatMode = (repeatMode + 1) % 3;
        settingsManager.savePlaybackSettings(isShuffle, repeatMode);
        sendBroadcastToUI("REPEAT_CHANGED", currentSong);
    }

    public void setPlaylist(List<Song> songs, int position) {
        this.playlist = new ArrayList<>(songs);
        this.originalPlaylist = new ArrayList<>(songs);
        this.currentPosition = position;
        sendBroadcastToUI("QUEUE_UPDATED", currentSong);
    }

    // Queue Management Methods
    public List<Song> getQueue() {
        return queue;
    }

    public void clearQueue() {
        queue.clear();
        sendBroadcastToUI("QUEUE_UPDATED", currentSong);
    }

    public void removeFromQueue(int position) {
        if (position >= 0 && position < queue.size()) {
            queue.remove(position);
            sendBroadcastToUI("QUEUE_UPDATED", currentSong);
        }
    }

    public void addToQueue(Song song) {
        if (song != null) {
            queue.add(song);
            sendBroadcastToUI("QUEUE_UPDATED", currentSong);
        }
    }

    public void addToQueueNext(Song song) {
        if (song != null) {
            if (queue.isEmpty()) {
                queue.add(song);
            } else {
                queue.add(0, song);
            }
            sendBroadcastToUI("QUEUE_UPDATED", currentSong);
        }
    }

    public void addAllToQueue(List<Song> songs) {
        if (songs != null && !songs.isEmpty()) {
            queue.addAll(songs);
            sendBroadcastToUI("QUEUE_UPDATED", currentSong);
        }
    }

    public void playQueue() {
        if (!queue.isEmpty()) {
            List<Song> fullPlaylist = new ArrayList<>(queue);
            setPlaylist(fullPlaylist, 0);
            playSong(queue.get(0));
        }
    }

    public void applyEqualizerSettings() {
        if (mediaPlayer == null) return;

        try {
            int audioSessionId = mediaPlayer.getAudioSessionId();
            if (audioSessionId == 0) return;

            // Release old effects
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }
            if (bassBoost != null) {
                bassBoost.release();
                bassBoost = null;
            }
            if (virtualizer != null) {
                virtualizer.release();
                virtualizer = null;
            }

            // Initialize Equalizer if enabled
            if (isEqEnabled) {
                try {
                    equalizer = new Equalizer(0, audioSessionId);
                    equalizer.setEnabled(true);

                    // Set flat equalizer by default
                    short numberOfBands = equalizer.getNumberOfBands();
                    for (short i = 0; i < numberOfBands; i++) {
                        equalizer.setBandLevel(i, (short) 0);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Equalizer error: " + e.getMessage());
                }
            }

            Log.d(TAG, "Equalizer settings applied");
        } catch (Exception e) {
            Log.e(TAG, "Error applying settings: " + e.getMessage());
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        return true;
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                playMedia();
                break;
        }
    }

    private void startForeground() {
        if (!isForeground) {
            startForeground(NOTIFICATION_ID, createNotification());
            isForeground = true;
        }
    }

    private Notification createNotification() {
        Intent prevIntent = new Intent(this, MediaPlayerService.class);
        prevIntent.setAction(ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, MediaPlayerService.class);
        playPauseIntent.setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MediaPlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MediaPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 3, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle =
                new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopPendingIntent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "Echo-Wave")
                .setContentText(currentSong != null ? currentSong.getArtist() : "No song playing")
                .setLargeIcon(cachedAlbumArt)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, NowPlayingActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setShowWhen(false)
                .addAction(R.drawable.ic_skip_previous, "Previous", prevPendingIntent)
                .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying() ? "Pause" : "Play", playPausePendingIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextPendingIntent)
                .setStyle(mediaStyle)
                .setColor(getResources().getColor(R.color.electric_cyan));

        return builder.build();
    }

    @SuppressLint("MissingPermission")
    private void updateNotification() {
        if (isForeground) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification());
        }
    }

    private void sendBroadcastToUI(String action, Song song) {
        Intent intent = new Intent(PLAYBACK_STATE_CHANGED);
        intent.putExtra("action", action);
        if (song != null) {
            intent.putExtra("song_id", song.getId());
            intent.putExtra("song_title", song.getTitle());
            intent.putExtra("song_artist", song.getArtist());
            intent.putExtra("song_album", song.getAlbum());
            intent.putExtra("song_duration", song.getDuration());
            intent.putExtra("song_album_art", song.getAlbumArt());
        }
        intent.putExtra("is_shuffle", isShuffle);
        intent.putExtra("repeat_mode", repeatMode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void registerReceivers() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(headsetReceiver, filter);
    }

    private final BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                if (state == 0 && isPlaying()) {
                    pauseMedia();
                }
            } else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                if (isPlaying()) {
                    pauseMedia();
                }
            }
        }
    };

    // Getters
    public Song getCurrentSong() { return currentSong; }
    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
    public int getCurrentPosition() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
    public List<Song> getPlaylist() { return playlist; }
    public int getPlaylistPosition() { return currentPosition; }
    public boolean isShuffleEnabled() { return isShuffle; }
    public int getRepeatMode() { return repeatMode; }
    public MediaPlayer getMediaPlayer() { return mediaPlayer; }
// Add this method to MediaPlayerService class

    public int getAudioSessionId() {
        if (mediaPlayer != null) {
            return mediaPlayer.getAudioSessionId();
        }
        return 0;
    }
    public void resumeMedia() {
        // Alias for playMedia() for compatibility
        playMedia();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (equalizer != null) {
            equalizer.release();
            equalizer = null;
        }
        if (bassBoost != null) {
            bassBoost.release();
            bassBoost = null;
        }
        if (virtualizer != null) {
            virtualizer.release();
            virtualizer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        try {
            unregisterReceiver(headsetReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        executor.shutdown();
        handler.removeCallbacksAndMessages(null);
    }
}