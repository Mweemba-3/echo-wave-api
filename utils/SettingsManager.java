package com.example.echo_wave.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "echo_wave_prefs";
    private static final String KEY_SORT_CRITERIA = "sort_criteria";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_SHUFFLE_MODE = "shuffle_mode";
    private static final String KEY_REPEAT_MODE = "repeat_mode";
    private static final String KEY_EQUALIZER_ENABLED = "equalizer_enabled";
    private static final String KEY_EQUALIZER_PRESET = "equalizer_preset";
    private static final String KEY_GAPLESS = "gapless";
    private static final String KEY_CROSSFADE = "crossfade";
    private static final String KEY_HEADSET_AUTO_PLAY = "headset_auto_play";
    private static final String KEY_LAST_PLAYED_SONG_ID = "last_played_song_id";
    private static final String KEY_LAST_PLAYED_POSITION = "last_played_position";
    private static final String KEY_EQUALIZER_BANDS = "equalizer_bands";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_AUDIO_QUALITY = "audio_quality";
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_AUTO_DOWNLOAD_ART = "auto_download_art";
    private static final String KEY_LYRICS_ENABLED = "lyrics_enabled";
    private static final String KEY_VISUALIZER_ENABLED = "visualizer_enabled";
    private static final String KEY_VISUALIZER_TYPE = "visualizer_type";

    private static SettingsManager instance;
    private SharedPreferences prefs;

    private SettingsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsManager(context);
        }
        return instance;
    }

    // Sort Settings
    public void saveSortSettings(String criteria, boolean ascending) {
        prefs.edit()
                .putString(KEY_SORT_CRITERIA, criteria)
                .putBoolean(KEY_SORT_ORDER, ascending)
                .apply();
    }

    public String getSortCriteria() {
        return prefs.getString(KEY_SORT_CRITERIA, "title");
    }

    public boolean isSortAscending() {
        return prefs.getBoolean(KEY_SORT_ORDER, true);
    }

    // Playback Settings
    public void savePlaybackSettings(boolean shuffle, int repeat) {
        prefs.edit()
                .putBoolean(KEY_SHUFFLE_MODE, shuffle)
                .putInt(KEY_REPEAT_MODE, repeat)
                .apply();
    }

    public boolean getShuffleMode() {
        return prefs.getBoolean(KEY_SHUFFLE_MODE, false);
    }

    public int getRepeatMode() {
        return prefs.getInt(KEY_REPEAT_MODE, 0);
    }

    // Last Played Song
    public void saveLastPlayed(String songId, int position) {
        prefs.edit()
                .putString(KEY_LAST_PLAYED_SONG_ID, songId)
                .putInt(KEY_LAST_PLAYED_POSITION, position)
                .apply();
    }

    public String getLastPlayedSongId() {
        return prefs.getString(KEY_LAST_PLAYED_SONG_ID, null);
    }

    public int getLastPlayedPosition() {
        return prefs.getInt(KEY_LAST_PLAYED_POSITION, 0);
    }

    // ============= EQUALIZER SETTINGS =============
    public void setEqualizerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_EQUALIZER_ENABLED, enabled).apply();
    }

    public boolean isEqualizerEnabled() {
        return prefs.getBoolean(KEY_EQUALIZER_ENABLED, true);
    }

    public void setEqualizerPreset(int preset) {
        prefs.edit().putInt(KEY_EQUALIZER_PRESET, preset).apply();
    }

    public int getEqualizerPreset() {
        return prefs.getInt(KEY_EQUALIZER_PRESET, 0);
    }

    public void setEqualizerBands(String bandsJson) {
        prefs.edit().putString(KEY_EQUALIZER_BANDS, bandsJson).apply();
    }

    public String getEqualizerBands() {
        return prefs.getString(KEY_EQUALIZER_BANDS, null);
    }

    public String getEqualizerStatusText() {
        if (!isEqualizerEnabled()) {
            return "Off";
        }
        int preset = getEqualizerPreset();
        switch (preset) {
            case 0: return "Normal";
            case 1: return "Classical";
            case 2: return "Dance";
            case 3: return "Flat";
            case 4: return "Folk";
            case 5: return "Heavy Metal";
            case 6: return "Hip Hop";
            case 7: return "Jazz";
            case 8: return "Pop";
            case 9: return "Rock";
            default: return "Custom";
        }
    }
    // ==============================================

    // Audio Settings
    public void setGapless(boolean enabled) {
        prefs.edit().putBoolean(KEY_GAPLESS, enabled).apply();
    }

    public boolean isGapless() {
        return prefs.getBoolean(KEY_GAPLESS, true);
    }

    public void setCrossfade(boolean enabled) {
        prefs.edit().putBoolean(KEY_CROSSFADE, enabled).apply();
    }

    public boolean isCrossfade() {
        return prefs.getBoolean(KEY_CROSSFADE, false);
    }

    public void setHeadsetAutoPlay(boolean enabled) {
        prefs.edit().putBoolean(KEY_HEADSET_AUTO_PLAY, enabled).apply();
    }

    public boolean isHeadsetAutoPlay() {
        return prefs.getBoolean(KEY_HEADSET_AUTO_PLAY, true);
    }

    // Dark Mode
    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, true);
    }

    // Notifications
    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    // Audio Quality
    public void setAudioQuality(int quality) {
        prefs.edit().putInt(KEY_AUDIO_QUALITY, quality).apply();
    }

    public int getAudioQuality() {
        return prefs.getInt(KEY_AUDIO_QUALITY, 0);
    }

    public String getAudioQualityText() {
        int quality = getAudioQuality();
        switch (quality) {
            case 0: return "Standard";
            case 1: return "High";
            case 2: return "Very High";
            case 3: return "Lossless";
            default: return "Standard";
        }
    }

    // Cache Size
    public void setCacheSize(long size) {
        prefs.edit().putLong(KEY_CACHE_SIZE, size).apply();
    }

    public long getCacheSize() {
        return prefs.getLong(KEY_CACHE_SIZE, 0);
    }

    // First Run
    public boolean isFirstRun() {
        boolean firstRun = prefs.getBoolean(KEY_FIRST_RUN, true);
        if (firstRun) {
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        }
        return firstRun;
    }

    // Auto Download Album Art
    public void setAutoDownloadArt(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD_ART, enabled).apply();
    }

    public boolean isAutoDownloadArt() {
        return prefs.getBoolean(KEY_AUTO_DOWNLOAD_ART, true);
    }

    // Lyrics
    public void setLyricsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LYRICS_ENABLED, enabled).apply();
    }

    public boolean isLyricsEnabled() {
        return prefs.getBoolean(KEY_LYRICS_ENABLED, false);
    }

    // Visualizer
    public void setVisualizerEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VISUALIZER_ENABLED, enabled).apply();
    }

    public boolean isVisualizerEnabled() {
        return prefs.getBoolean(KEY_VISUALIZER_ENABLED, true);
    }

    public void setVisualizerType(int type) {
        prefs.edit().putInt(KEY_VISUALIZER_TYPE, type).apply();
    }

    public int getVisualizerType() {
        return prefs.getInt(KEY_VISUALIZER_TYPE, 0);
    }

    // Sleep Timer
    public void saveSleepTimerStatus(long timeLeft) {
        prefs.edit().putLong("sleep_timer_time", timeLeft).apply();
    }

    public long getSleepTimerStatus() {
        return prefs.getLong("sleep_timer_time", 0);
    }

    // Reset all settings
    public void resetToDefaults() {
        prefs.edit().clear().apply();
        saveSortSettings("title", true);
        savePlaybackSettings(false, 0);
        setGapless(true);
        setCrossfade(false);
        setHeadsetAutoPlay(true);
        setDarkMode(true);
        setNotificationsEnabled(true);
        setAutoDownloadArt(true);
        setVisualizerEnabled(true);
        setEqualizerEnabled(true);
        setEqualizerPreset(0);
    }

    // Get all settings as formatted string
    public String getAllSettings() {
        return "Settings:\n" +
                "Sort: " + getSortCriteria() + " (" + (isSortAscending() ? "ASC" : "DESC") + ")\n" +
                "Shuffle: " + getShuffleMode() + "\n" +
                "Repeat: " + getRepeatMode() + "\n" +
                "Equalizer: " + (isEqualizerEnabled() ? "ON" : "OFF") + " - " + getEqualizerStatusText() + "\n" +
                "Gapless: " + isGapless() + "\n" +
                "Crossfade: " + isCrossfade() + "\n" +
                "Headset Auto: " + isHeadsetAutoPlay() + "\n" +
                "Dark Mode: " + isDarkMode() + "\n" +
                "Notifications: " + isNotificationsEnabled() + "\n" +
                "Audio Quality: " + getAudioQualityText() + "\n" +
                "Auto Download Art: " + isAutoDownloadArt() + "\n" +
                "Lyrics: " + isLyricsEnabled() + "\n" +
                "Visualizer: " + isVisualizerEnabled();
    }

    // Clear all settings
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}