package com.example.echo_wave.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.echo_wave.models.EqualizerSettings;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;

@Database(entities = {
        Song.class,
        Playlist.class,
        PlaylistSong.class,
        EqualizerSettings.class
}, version = 5, exportSchema = false)
public abstract class MusicDatabase extends RoomDatabase {

    private static MusicDatabase instance;
    private static final String DATABASE_NAME = "echo_wave_db";

    public abstract SongDao songDao();
    public abstract PlaylistDao playlistDao();
    public abstract PlaylistSongDao playlistSongDao();
    public abstract EqualizerSettingsDao equalizerSettingsDao();

    // Migration from version 4 to 5 (adding new equalizer fields)
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add new columns for equalizer settings
            try {
                database.execSQL("ALTER TABLE equalizer_settings ADD COLUMN reverbLevel INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }

            try {
                database.execSQL("ALTER TABLE equalizer_settings ADD COLUMN widenessLevel INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }

            try {
                database.execSQL("ALTER TABLE equalizer_settings ADD COLUMN currentPreset INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }

            try {
                database.execSQL("ALTER TABLE equalizer_settings ADD COLUMN bassBoostLevel INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }

            try {
                database.execSQL("ALTER TABLE equalizer_settings ADD COLUMN virtualizerLevel INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column might already exist
            }
        }
    };

    // Migration from version 3 to 4 (if you had previous schema)
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Create equalizer settings table with new schema
            database.execSQL("CREATE TABLE IF NOT EXISTS equalizer_settings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "isEnabled INTEGER DEFAULT 1, " +
                    "currentPreset INTEGER DEFAULT 0, " +
                    "bassBoostLevel INTEGER DEFAULT 0, " +
                    "virtualizerLevel INTEGER DEFAULT 0, " +
                    "reverbLevel INTEGER DEFAULT 0, " +
                    "widenessLevel INTEGER DEFAULT 0, " +
                    "customBands TEXT DEFAULT '', " +
                    "lastUpdated INTEGER DEFAULT 0)");

            // Insert default settings
            database.execSQL("INSERT INTO equalizer_settings " +
                    "(isEnabled, currentPreset, bassBoostLevel, virtualizerLevel, reverbLevel, widenessLevel, customBands, lastUpdated) " +
                    "VALUES (1, 0, 0, 0, 0, 0, '', " + System.currentTimeMillis() + ")");
        }
    };

    public static synchronized MusicDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            MusicDatabase.class, DATABASE_NAME)
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void destroyInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}