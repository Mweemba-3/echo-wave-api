package com.example.echo_wave.models;

public class AlbumItem {
    private String name;
    private String artist;
    private String albumArt;

    public AlbumItem() {}

    public AlbumItem(String name, String artist, String albumArt) {
        this.name = name;
        this.artist = artist;
        this.albumArt = albumArt;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbumArt() { return albumArt; }
    public void setAlbumArt(String albumArt) { this.albumArt = albumArt; }
}