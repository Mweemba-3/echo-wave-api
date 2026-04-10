package com.example.echo_wave.models;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song implements Parcelable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "artist")
    private String artist;

    @ColumnInfo(name = "album")
    private String album;

    @ColumnInfo(name = "duration")
    private long duration;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "album_art")
    private String albumArt;

    @ColumnInfo(name = "date_added")
    private long dateAdded;

    @ColumnInfo(name = "date_modified")
    private long dateModified;

    @ColumnInfo(name = "play_count")
    private int playCount = 0;

    @ColumnInfo(name = "last_played")
    private long lastPlayed = 0;

    @ColumnInfo(name = "size")
    private long size;

    @ColumnInfo(name = "format")
    private String format;

    @ColumnInfo(name = "bitrate")
    private int bitrate;

    @ColumnInfo(name = "is_favorite")
    private boolean isFavorite = false;

    @ColumnInfo(name = "genre")
    private String genre;

    @ColumnInfo(name = "year")
    private int year;

    @ColumnInfo(name = "track_number")
    private int trackNumber;

    public Song() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.dateAdded = System.currentTimeMillis();
        this.dateModified = System.currentTimeMillis();
    }

    // Parcelable constructor
    protected Song(Parcel in) {
        id = in.readString();
        title = in.readString();
        artist = in.readString();
        album = in.readString();
        duration = in.readLong();
        path = in.readString();
        albumArt = in.readString();
        dateAdded = in.readLong();
        dateModified = in.readLong();
        playCount = in.readInt();
        lastPlayed = in.readLong();
        size = in.readLong();
        format = in.readString();
        bitrate = in.readInt();
        isFavorite = in.readByte() != 0;
        genre = in.readString();
        year = in.readInt();
        trackNumber = in.readInt();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(album);
        dest.writeLong(duration);
        dest.writeString(path);
        dest.writeString(albumArt);
        dest.writeLong(dateAdded);
        dest.writeLong(dateModified);
        dest.writeInt(playCount);
        dest.writeLong(lastPlayed);
        dest.writeLong(size);
        dest.writeString(format);
        dest.writeInt(bitrate);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeString(genre);
        dest.writeInt(year);
        dest.writeInt(trackNumber);
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title != null ? title : "Unknown Title"; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return (artist == null || artist.isEmpty()) ? "Unknown Artist" : artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return (album == null || album.isEmpty()) ? "Unknown Album" : album; }
    public void setAlbum(String album) { this.album = album; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getDurationFormatted() {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getAlbumArt() { return albumArt; }
    public void setAlbumArt(String albumArt) { this.albumArt = albumArt; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getDateModified() { return dateModified; }
    public void setDateModified(long dateModified) { this.dateModified = dateModified; }

    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public void incrementPlayCount() { this.playCount++; }

    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getTrackNumber() { return trackNumber; }
    public void setTrackNumber(int trackNumber) { this.trackNumber = trackNumber; }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Song) {
            Song other = (Song) obj;
            return id != null && id.equals(other.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}