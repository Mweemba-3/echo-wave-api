package com.example.echo_wave.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.echo_wave.models.Song;
import com.example.echo_wave.services.MediaPlayerService;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerHelper {

    private static final String TAG = "MediaPlayerHelper";
    private static MediaPlayerHelper instance;
    private MediaPlayerService mediaPlayerService;

    private MediaPlayerHelper() {}

    public static synchronized MediaPlayerHelper getInstance() {
        if (instance == null) {
            instance = new MediaPlayerHelper();
        }
        return instance;
    }

    public void init(Context context) {
        // Initialize any necessary resources
        Log.d(TAG, "MediaPlayerHelper initialized");
    }

    public void setMediaPlayerService(MediaPlayerService service) {
        this.mediaPlayerService = service;
        Log.d(TAG, "MediaPlayerService set");
    }

    // ========== PLAYBACK METHODS ==========

    public void playSong(Context context, Song song) {
        if (song == null) return;

        if (mediaPlayerService != null) {
            mediaPlayerService.playSong(song);
        } else {
            // Start service and play
            Intent intent = new Intent(context, MediaPlayerService.class);
            intent.setAction(MediaPlayerService.ACTION_PLAY);
            intent.putExtra("song_id", song.getId());
            intent.putExtra("song_title", song.getTitle());
            intent.putExtra("song_artist", song.getArtist());
            intent.putExtra("song_album", song.getAlbum());
            intent.putExtra("song_duration", song.getDuration());
            intent.putExtra("song_path", song.getPath());
            intent.putExtra("song_album_art", song.getAlbumArt());

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }
    }

    public void playSong(Context context, List<Song> playlist, int startPosition) {
        if (playlist == null || playlist.isEmpty()) return;

        if (mediaPlayerService != null) {
            mediaPlayerService.setPlaylist(playlist, startPosition);
            mediaPlayerService.playSong(playlist.get(startPosition));
        } else {
            // Start service with playlist
            Intent intent = new Intent(context, MediaPlayerService.class);
            intent.setAction(MediaPlayerService.ACTION_PLAY);
            Song song = playlist.get(startPosition);
            intent.putExtra("song_id", song.getId());
            intent.putExtra("song_title", song.getTitle());
            intent.putExtra("song_artist", song.getArtist());
            intent.putExtra("song_album", song.getAlbum());
            intent.putExtra("song_duration", song.getDuration());
            intent.putExtra("song_path", song.getPath());
            intent.putExtra("song_album_art", song.getAlbumArt());

            // Store playlist in service via broadcast or separate intent
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }

            // Set playlist after service starts (handled in service)
        }
    }

    public void pauseMedia() {
        if (mediaPlayerService != null) {
            mediaPlayerService.pauseMedia();
        }
    }

    public void playMedia() {
        if (mediaPlayerService != null) {
            mediaPlayerService.playMedia();
        }
    }

    public void stopMedia() {
        if (mediaPlayerService != null) {
            mediaPlayerService.stopMedia();
        }
    }

    public void playNext() {
        if (mediaPlayerService != null) {
            mediaPlayerService.playNext();
        }
    }

    public void playPrevious() {
        if (mediaPlayerService != null) {
            mediaPlayerService.playPrevious();
        }
    }

    public void seekTo(int position) {
        if (mediaPlayerService != null) {
            mediaPlayerService.seekTo(position);
        }
    }

    // ========== QUEUE MANAGEMENT METHODS ==========

    public List<Song> getQueue() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getQueue();
        }
        return new ArrayList<>();
    }

    public void clearQueue() {
        if (mediaPlayerService != null) {
            mediaPlayerService.clearQueue();
            Log.d(TAG, "Queue cleared");
        }
    }

    public void removeFromQueue(int position) {
        if (mediaPlayerService != null) {
            mediaPlayerService.removeFromQueue(position);
            Log.d(TAG, "Removed from queue at position: " + position);
        }
    }

    public void addToQueue(Song song) {
        if (mediaPlayerService != null && song != null) {
            mediaPlayerService.addToQueue(song);
            Log.d(TAG, "Added to queue: " + song.getTitle());
        }
    }

    public void addToQueueNext(Song song) {
        if (mediaPlayerService != null && song != null) {
            mediaPlayerService.addToQueueNext(song);
            Log.d(TAG, "Added to queue next: " + song.getTitle());
        }
    }

    public void addAllToQueue(List<Song> songs) {
        if (mediaPlayerService != null && songs != null && !songs.isEmpty()) {
            mediaPlayerService.addAllToQueue(songs);
            Log.d(TAG, "Added " + songs.size() + " songs to queue");
        }
    }

    public void setPlaylist(List<Song> playlist, int startPosition) {
        if (mediaPlayerService != null) {
            mediaPlayerService.setPlaylist(playlist, startPosition);
            Log.d(TAG, "Playlist set with " + playlist.size() + " songs");
        }
    }

    public void playQueue() {
        if (mediaPlayerService != null) {
            mediaPlayerService.playQueue();
            Log.d(TAG, "Playing queue");
        }
    }

    // ========== PLAYLIST MANAGEMENT METHODS ==========

    public List<Song> getPlaylist() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getPlaylist();
        }
        return new ArrayList<>();
    }

    public int getPlaylistPosition() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getPlaylistPosition();
        }
        return -1;
    }

    public void playAtPosition(int position) {
        if (mediaPlayerService != null) {
            mediaPlayerService.playAtPosition(position);
        }
    }

    // ========== SHUFFLE AND REPEAT METHODS ==========

    public void toggleShuffle() {
        if (mediaPlayerService != null) {
            mediaPlayerService.toggleShuffle();
        }
    }

    public void toggleRepeat() {
        if (mediaPlayerService != null) {
            mediaPlayerService.toggleRepeat();
        }
    }

    public boolean isShuffleEnabled() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.isShuffleEnabled();
        }
        return false;
    }

    public int getRepeatMode() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getRepeatMode();
        }
        return 0;
    }

    // ========== CURRENT SONG METHODS ==========

    public Song getCurrentSong() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getCurrentSong();
        }
        return null;
    }

    public boolean isPlaying() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.isPlaying();
        }
        return false;
    }

    public int getCurrentPosition() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getDuration();
        }
        return 0;
    }

    // ========== EQUALIZER METHODS ==========

    public int getAudioSessionId() {
        if (mediaPlayerService != null) {
            return mediaPlayerService.getAudioSessionId();
        }
        return 0;
    }

    public void applyEqualizerSettings() {
        if (mediaPlayerService != null) {
            mediaPlayerService.applyEqualizerSettings();
        }
    }

    // ========== SERVICE METHODS ==========

    public boolean isServiceBound() {
        return mediaPlayerService != null;
    }
}