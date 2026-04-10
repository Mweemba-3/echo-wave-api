package com.example.echo_wave.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "equalizer_settings")
public class EqualizerSettings {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private boolean isEnabled;
    private int currentPreset;
    private int bassBoostLevel;
    private int virtualizerLevel;
    private int reverbLevel;
    private int widenessLevel;
    private String customBands;
    private long lastUpdated;

    public EqualizerSettings() {
        this.isEnabled = true;
        this.currentPreset = 0;
        this.bassBoostLevel = 0;
        this.virtualizerLevel = 0;
        this.reverbLevel = 0;
        this.widenessLevel = 0;
        this.customBands = "";
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(int currentPreset) {
        this.currentPreset = currentPreset;
    }

    public int getBassBoostLevel() {
        return bassBoostLevel;
    }

    public void setBassBoostLevel(int bassBoostLevel) {
        this.bassBoostLevel = bassBoostLevel;
    }

    public int getVirtualizerLevel() {
        return virtualizerLevel;
    }

    public void setVirtualizerLevel(int virtualizerLevel) {
        this.virtualizerLevel = virtualizerLevel;
    }

    public int getReverbLevel() {
        return reverbLevel;
    }

    public void setReverbLevel(int reverbLevel) {
        this.reverbLevel = reverbLevel;
    }

    public int getWidenessLevel() {
        return widenessLevel;
    }

    public void setWidenessLevel(int widenessLevel) {
        this.widenessLevel = widenessLevel;
    }

    public String getCustomBands() {
        return customBands;
    }

    public void setCustomBands(String customBands) {
        this.customBands = customBands;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}