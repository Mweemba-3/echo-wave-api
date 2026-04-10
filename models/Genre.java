package com.example.echo_wave.models;

public class Genre {
    private String id;
    private String name;
    private String imageUrl;
    private String color;
    private int songCount;

    public Genre(String id, String name, String imageUrl, String color) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.color = color;
        this.songCount = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }
}