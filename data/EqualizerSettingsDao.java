package com.example.echo_wave.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.echo_wave.models.EqualizerSettings;

@Dao
public interface EqualizerSettingsDao {

    @Insert
    long insert(EqualizerSettings settings);

    @Update
    void update(EqualizerSettings settings);

    @Query("SELECT * FROM equalizer_settings WHERE id = :id")
    EqualizerSettings getSettingsById(int id);

    @Query("SELECT * FROM equalizer_settings LIMIT 1")
    EqualizerSettings getSettings();

    @Query("SELECT * FROM equalizer_settings ORDER BY id DESC LIMIT 1")
    EqualizerSettings getLatestSettings();

    @Query("UPDATE equalizer_settings SET isEnabled = :isEnabled WHERE id = :id")
    void updateEnabled(int id, boolean isEnabled);

    @Query("UPDATE equalizer_settings SET currentPreset = :preset WHERE id = :id")
    void updatePreset(int id, int preset);

    @Query("UPDATE equalizer_settings SET bassBoostLevel = :bassLevel WHERE id = :id")
    void updateBassLevel(int id, int bassLevel);

    @Query("UPDATE equalizer_settings SET virtualizerLevel = :virtualizerLevel WHERE id = :id")
    void updateVirtualizerLevel(int id, int virtualizerLevel);

    @Query("UPDATE equalizer_settings SET customBands = :customBands WHERE id = :id")
    void updateCustomBands(int id, String customBands);

    @Query("DELETE FROM equalizer_settings")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM equalizer_settings")
    int getCount();
}