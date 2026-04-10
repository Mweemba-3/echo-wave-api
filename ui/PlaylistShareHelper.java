package com.example.echo_wave.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.Song;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class PlaylistShareHelper {

    public static void shareAsText(Context context, Playlist playlist, List<Song> songs) {
        if (playlist == null) {
            Toast.makeText(context, "Playlist is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(context, "No songs in playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🎵 Playlist: ").append(playlist.getName()).append("\n\n");

        // Note: getDescription() doesn't exist in your Playlist class
        // If you want to include description, you need to add it to Playlist model
        // For now, we'll skip it

        sb.append("Songs:\n");
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            String title = song.getTitle() != null ? song.getTitle() : "Unknown";
            String artist = song.getArtist() != null ? song.getArtist() : "Unknown Artist";

            sb.append(i + 1).append(". ").append(title)
                    .append(" - ").append(artist).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Playlist: " + playlist.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Playlist via"));
        } catch (Exception e) {
            Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    public static void exportAsM3U(Context context, Playlist playlist, List<Song> songs) {
        if (playlist == null) {
            Toast.makeText(context, "Playlist is null", Toast.LENGTH_SHORT).show();
            return;
        }

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(context, "No songs in playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("#EXTM3U\n");
            sb.append("#PLAYLIST:").append(playlist.getName()).append("\n");

            // Skip description if not available

            for (Song song : songs) {
                String title = song.getTitle() != null ? song.getTitle() : "Unknown";
                String artist = song.getArtist() != null ? song.getArtist() : "Unknown Artist";
                String path = song.getPath() != null ? song.getPath() : "";
                long duration = song.getDuration();

                sb.append("#EXTINF:").append(duration / 1000) // Convert to seconds for M3U
                        .append(",").append(artist).append(" - ").append(title).append("\n");
                sb.append(path).append("\n");
            }

            String filename = playlist.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".m3u";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Use MediaStore
                ContentResolver resolver = context.getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "audio/x-mpegurl");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Playlists");

                Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try (OutputStream os = resolver.openOutputStream(uri)) {
                        if (os != null) {
                            os.write(sb.toString().getBytes());
                            Toast.makeText(context, "Playlist exported to Music/Playlists",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android 9 and below - Use raw file
                File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                if (musicDir != null) {
                    File exportDir = new File(musicDir, "Playlists");
                    if (!exportDir.exists()) {
                        exportDir.mkdirs();
                    }

                    File exportFile = new File(exportDir, filename);
                    try (FileOutputStream fos = new FileOutputStream(exportFile)) {
                        fos.write(sb.toString().getBytes());
                        Toast.makeText(context, "Playlist exported to " + exportFile.getPath(),
                                Toast.LENGTH_LONG).show();

                        // Notify media scanner
                        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(exportFile)));
                    }
                } else {
                    Toast.makeText(context, "Cannot access external storage", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Storage permission required", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}