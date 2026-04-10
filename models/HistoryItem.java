package com.example.echo_wave.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.echo_wave.utils.Converters;

@Entity(tableName = "history")
@TypeConverters(Converters.class)
public class HistoryItem {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String songId;

    private long playedAt;

    private int playCount = 1;

    private String source;

    public HistoryItem() {
        this.playedAt = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public String getSongId() { return songId; }
    public long getPlayedAt() { return playedAt; }
    public int getPlayCount() { return playCount; }
    public String getSource() { return source; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSongId(String songId) { this.songId = songId; }
    public void setPlayedAt(long playedAt) { this.playedAt = playedAt; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public void setSource(String source) { this.source = source; }

    // Helper
    public void incrementPlayCount() { this.playCount++; }
}