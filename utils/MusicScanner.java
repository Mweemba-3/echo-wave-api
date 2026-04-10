package com.example.echo_wave.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.example.echo_wave.models.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicScanner {
    private static final String TAG = "MusicScanner";
    private final Context context;
    private final ExecutorService executor;

    public interface ScanCallback {
        void onScanStarted();
        void onScanProgress(int progress, int max);
        void onScanComplete(List<Song> songs);
    }

    public MusicScanner(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void scanMusic(ScanCallback callback) {
        executor.execute(() -> {
            if (callback != null) callback.onScanStarted();

            List<Song> songList = new ArrayList<>();
            Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DATE_ADDED  // ADD THIS LINE
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

            try (Cursor cursor = context.getContentResolver().query(
                    collection, projection, selection, null, null)) {

                if (cursor != null) {
                    int total = cursor.getCount();
                    int current = 0;

                    while (cursor.moveToNext()) {
                        current++;
                        if (callback != null) {
                            int finalCurrent = current;
                            new android.os.Handler(context.getMainLooper()).post(() ->
                                    callback.onScanProgress(finalCurrent, total));
                        }

                        Song song = new Song();
                        String path = cursor.getString(5);

                        if (path == null || !new File(path).exists()) continue;

                        song.setId(cursor.getString(0));
                        song.setTitle(cursor.getString(1));
                        song.setArtist(cursor.getString(2));
                        song.setAlbum(cursor.getString(3));
                        song.setDuration(cursor.getLong(4));
                        song.setPath(path);

                        // FIX: Set date_added properly
                        int dateAddedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
                        if (dateAddedIndex >= 0) {
                            long dateAdded = cursor.getLong(dateAddedIndex) * 1000; // Convert seconds to milliseconds
                            song.setDateAdded(dateAdded);
                        } else {
                            song.setDateAdded(System.currentTimeMillis());
                        }

                        long albumId = cursor.getLong(6);
                        song.setAlbumArt(getAlbumArtUri(albumId));

                        songList.add(song);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scanning", e);
            }

            if (callback != null) {
                List<Song> finalSongs = songList;
                new android.os.Handler(context.getMainLooper()).post(() ->
                        callback.onScanComplete(finalSongs));
            }
        });
    }

    private String getAlbumArtUri(long albumId) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId).toString();
    }

    public void cancel() {
        executor.shutdownNow();
    }
}