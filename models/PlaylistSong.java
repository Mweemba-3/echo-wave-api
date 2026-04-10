package com.example.echo_wave.models;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs",
        indices = {@Index(value = {"playlistId", "songId"}, unique = true)})
public class PlaylistSong {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private int playlistId;
    private String songId;
    private long dateAdded;

    public PlaylistSong(int playlistId, String songId) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.dateAdded = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPlaylistId() { return playlistId; }
    public void setPlaylistId(int playlistId) { this.playlistId = playlistId; }
    public String getSongId() { return songId; }
    public void setSongId(String songId) { this.songId = songId; }
    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }
}