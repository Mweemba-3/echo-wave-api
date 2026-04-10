package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;

import java.util.List;

@Dao
public interface PlaylistSongDao {

    @Insert
    void insert(PlaylistSong playlistSong);

    @Insert
    void insertAll(List<PlaylistSong> playlistSongs);

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId")
    List<PlaylistSong> getPlaylistSongs(int playlistId);

    @Query("SELECT s.* FROM songs s " +
            "INNER JOIN playlist_songs ps ON s.id = ps.songId " +
            "WHERE ps.playlistId = :playlistId " +
            "ORDER BY ps.dateAdded ASC")
    List<Song> getSongsForPlaylist(int playlistId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void delete(int playlistId, String songId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void clearPlaylist(int playlistId);

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    void deleteBySongId(String songId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCount(int playlistId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    int isSongInPlaylist(int playlistId, String songId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE songId = :songId")
    int getPlaylistCountForSong(String songId);

    @Query("DELETE FROM playlist_songs")
    void deleteAll();

    @Query("SELECT * FROM playlist_songs WHERE dateAdded > :timestamp")
    List<PlaylistSong> getPlaylistSongsAddedAfter(long timestamp);
}