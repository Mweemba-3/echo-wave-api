package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.echo_wave.models.HistoryItem;
import com.example.echo_wave.models.Song;

import java.util.List;

@Dao
public interface HistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryItem historyItem);
    
    @Update
    void update(HistoryItem historyItem);
    
    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT 50")
    List<HistoryItem> getRecentHistory();
    
    @Query("SELECT s.* FROM songs s INNER JOIN history h ON s.id = h.songId ORDER BY h.playedAt DESC LIMIT :limit")
    List<Song> getRecentSongs(int limit);
    
    @Query("SELECT * FROM history WHERE songId = :songId ORDER BY playedAt DESC LIMIT 1")
    HistoryItem getLastPlayed(String songId);
}