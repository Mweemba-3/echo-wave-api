package com.example.echo_wave.models;

public class Album {
    private String name;
    private String artist;
    private int songCount;
    private String coverArt;

    public Album(String name, String artist, int songCount, String coverArt) {
        this.name = name;
        this.artist = artist;
        this.songCount = songCount;
        this.coverArt = coverArt;
    }

    public String getName() { return name; }
    public String getArtist() { return artist; }
    public int getSongCount() { return songCount; }
    public String getCoverArt() { return coverArt; }
}