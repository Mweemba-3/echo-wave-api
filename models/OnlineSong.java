package com.example.echo_wave.models;

public class OnlineSong {
    private String id;
    private String title;
    private String artist;
    private String duration;
    private int durationSeconds;
    private String url;
    private String thumbnail;
    private boolean isDownloaded = false;
    
    public OnlineSong() {}
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getDuration() { return duration; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getUrl() { return url; }
    public String getThumbnail() { return thumbnail; }
    public boolean isDownloaded() { return isDownloaded; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public void setUrl(String url) { this.url = url; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    public void setDownloaded(boolean downloaded) { isDownloaded = downloaded; }
}