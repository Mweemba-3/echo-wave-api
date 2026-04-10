package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.echo_wave.models.PlaylistSongCrossRef;

@Dao
public interface PlaylistSongCrossRefDao {
    
    @Insert
    void insert(PlaylistSongCrossRef crossRef);
    
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId)")
    boolean exists(int playlistId, String songId);
    
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    void delete(int playlistId, String songId);
}