package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.echo_wave.models.Playlist;

import java.util.List;

@Dao
public interface PlaylistDao {

    @Insert
    long insert(Playlist playlist);

    @Insert
    void insertAll(List<Playlist> playlists);

    @Update
    void update(Playlist playlist);

    @Update
    void updateAll(List<Playlist> playlists);

    @Delete
    void delete(Playlist playlist);

    @Delete
    void deleteAll(List<Playlist> playlists);

    @Query("SELECT * FROM playlists ORDER BY CASE WHEN isDefault = 1 THEN 0 ELSE 1 END, name ASC")
    List<Playlist> getAllPlaylists();

    @Query("SELECT * FROM playlists WHERE id = :id")
    Playlist getPlaylistById(int id);

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    Playlist getPlaylistByName(String name);

    @Query("DELETE FROM playlists WHERE id = :id")
    void deletePlaylistById(int id);

    @Query("UPDATE playlists SET songCount = :count WHERE id = :playlistId")
    void updateSongCount(int playlistId, int count);

    @Query("SELECT COUNT(*) FROM playlists")
    int getPlaylistCount();

    @Query("SELECT * FROM playlists WHERE isDefault = 0 ORDER BY name ASC")
    List<Playlist> getUserPlaylists();

    @Query("SELECT * FROM playlists WHERE isDefault = 1 ORDER BY name ASC")
    List<Playlist> getDefaultPlaylists();

    @Query("SELECT COUNT(*) FROM playlists WHERE isDefault = 0")
    int getUserPlaylistCount();

    @Query("UPDATE playlists SET songCount = (SELECT COUNT(*) FROM playlist_songs WHERE playlistId = id)")
    void updateAllSongCounts();
}