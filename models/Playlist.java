package com.example.echo_wave.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlists")
public class Playlist {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private boolean isDefault;
    private int songCount;

    public Playlist(String name) {
        this.name = name;
        this.isDefault = false;
        this.songCount = 0;
    }

    // Required empty constructor for Room
    public Playlist() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }
}