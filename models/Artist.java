package com.example.echo_wave.models;

public class Artist {
    private String name;
    private int songCount;
    private int albumCount;
    private String coverArt;

    public Artist(String name, int songCount, int albumCount, String coverArt) {
        this.name = name;
        this.songCount = songCount;
        this.albumCount = albumCount;
        this.coverArt = coverArt;
    }

    public String getName() { return name; }
    public int getSongCount() { return songCount; }
    public int getAlbumCount() { return albumCount; }
    public String getCoverArt() { return coverArt; }
}