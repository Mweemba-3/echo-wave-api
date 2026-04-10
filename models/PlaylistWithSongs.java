package com.example.echo_wave.models;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist;
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = @Junction(
            value = PlaylistSongCrossRef.class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    public List<Song> songs;
}