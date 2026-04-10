package com.example.echo_wave.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = {"playlistId", "songId"})
public class PlaylistSongCrossRef {

    @NonNull
    private int playlistId;

    @NonNull
    private String songId;

    private long addedAt;

    public PlaylistSongCrossRef(int playlistId, @NonNull String songId) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.addedAt = System.currentTimeMillis();
    }

    @NonNull
    public int getPlaylistId() { return playlistId; }
    public void setPlaylistId(@NonNull int playlistId) { this.playlistId = playlistId; }

    @NonNull
    public String getSongId() { return songId; }
    public void setSongId(@NonNull String songId) { this.songId = songId; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}