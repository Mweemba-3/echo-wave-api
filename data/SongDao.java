package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.echo_wave.models.Song;

import java.util.List;

@Dao
public interface SongDao {

    // ========== BASIC CRUD OPERATIONS ==========

    @Insert
    long insert(Song song);

    @Insert
    void insertAll(List<Song> songs);

    @Update
    void update(Song song);

    @Update
    void updateAll(List<Song> songs);

    @Delete
    void delete(Song song);

    @Delete
    void deleteAll(List<Song> songs);

    @Query("DELETE FROM songs")
    void deleteAllSongs();

    // ========== GET ALL SONGS ==========

    @Query("SELECT * FROM songs ORDER BY title ASC")
    List<Song> getAllSongs();


    @Query("SELECT * FROM songs ORDER BY date_added DESC")
    List<Song> getAllSongsByDateAdded();

    @Query("SELECT * FROM songs ORDER BY play_count DESC")
    List<Song> getAllSongsByPlayCount();

    // ========== GET BY ID ==========

    @Query("SELECT * FROM songs WHERE id = :id")
    Song getSongById(String id);

    // ========== SEARCH ==========

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    List<Song> searchSongs(String query);

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    List<Song> searchSongsByTitle(String query);

    @Query("SELECT * FROM songs WHERE artist LIKE '%' || :query || '%' ORDER BY artist ASC")
    List<Song> searchSongsByArtist(String query);

    // ========== GET BY ARTIST ==========

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY title ASC")
    List<Song> getSongsByArtist(String artist);

    @Query("SELECT DISTINCT artist FROM songs WHERE artist IS NOT NULL AND artist != '' ORDER BY artist ASC")
    List<String> getDistinctArtists();

    @Query("SELECT COUNT(*) FROM songs WHERE artist = :artist")
    int getSongCountByArtist(String artist);

    @Query("SELECT COUNT(DISTINCT album) FROM songs WHERE artist = :artist")
    int getDistinctAlbumCountByArtist(String artist);

    // ========== GET BY ALBUM ==========

    @Query("SELECT * FROM songs WHERE album = :album ORDER BY track_number ASC, title ASC")
    List<Song> getSongsByAlbum(String album);

    @Query("SELECT DISTINCT album FROM songs WHERE album IS NOT NULL AND album != '' ORDER BY album ASC")
    List<String> getDistinctAlbums();

    @Query("SELECT COUNT(*) FROM songs WHERE album = :album")
    int getSongCountByAlbum(String album);

    // ========== FAVORITES ==========

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title ASC")
    List<Song> getFavoriteSongs();

    @Query("SELECT * FROM songs WHERE is_favorite = 1 ORDER BY title ASC LIMIT :limit")
    List<Song> getFavoriteSongs(int limit);

    @Query("UPDATE songs SET is_favorite = :isFavorite WHERE id = :id")
    void setFavorite(String id, boolean isFavorite);

    @Query("UPDATE songs SET is_favorite = 0")
    void clearAllFavorites();

    @Query("SELECT COUNT(*) FROM songs WHERE is_favorite = 1")
    int getFavoriteCount();

    // ========== PLAY STATISTICS ==========

    @Query("UPDATE songs SET play_count = play_count + 1, last_played = :timestamp WHERE id = :id")
    void incrementPlayCount(String id, long timestamp);

    @Query("SELECT * FROM songs ORDER BY play_count DESC LIMIT :limit")
    List<Song> getMostPlayedSongs(int limit);

    @Query("SELECT * FROM songs ORDER BY last_played DESC LIMIT :limit")
    List<Song> getRecentlyPlayedSongs(int limit);

    // ========== DATE ADDED ==========

    @Query("SELECT * FROM songs ORDER BY date_added DESC LIMIT :limit")
    List<Song> getRecentlyAddedSongs(int limit);

    @Query("SELECT * FROM songs WHERE date_added > :timestamp ORDER BY date_added DESC")
    List<Song> getSongsAddedAfter(long timestamp);

    // ========== DURATION ==========

    @Query("SELECT * FROM songs ORDER BY duration ASC")
    List<Song> getSongsByDurationAsc();

    @Query("SELECT * FROM songs ORDER BY duration DESC")
    List<Song> getSongsByDurationDesc();

    @Query("SELECT SUM(duration) FROM songs")
    long getTotalDuration();

    // ========== COUNT QUERIES ==========

    @Query("SELECT COUNT(*) FROM songs")
    int getTotalSongCount();

    @Query("SELECT COUNT(*) FROM songs WHERE artist = :artist")
    int getSongCountForArtist(String artist);

    @Query("SELECT COUNT(*) FROM songs WHERE album = :album")
    int getSongCountForAlbum(String album);

    // ========== RANGE QUERIES ==========

    @Query("SELECT * FROM songs WHERE title >= :start AND title <= :end ORDER BY title ASC")
    List<Song> getSongsInRange(String start, String end);

    // ========== DELETE OPERATIONS ==========

    @Query("DELETE FROM songs WHERE id = :id")
    void deleteSongById(String id);

    @Query("DELETE FROM songs WHERE is_favorite = 1")
    void deleteAllFavorites();

    @Query("DELETE FROM songs WHERE path = :path")
    void deleteSongByPath(String path);
}