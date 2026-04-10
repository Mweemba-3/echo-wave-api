package com.example.echo_wave.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.utils.MediaPlayerHelper;

import java.util.ArrayList;
import java.util.List;

public class ArtistDetailsActivity extends AppCompatActivity {

    private ImageView btnBack, ivArtistImage;
    private TextView tvArtistName, tvSongCount;
    private RecyclerView rvSongs;
    private SongsAdapter adapter;
    private List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_details);

        String artistName = getIntent().getStringExtra("artist_name");

        initViews();
        loadArtistDetails(artistName);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivArtistImage = findViewById(R.id.iv_artist_image);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvSongCount = findViewById(R.id.tv_song_count);
        rvSongs = findViewById(R.id.rv_songs);

        btnBack.setOnClickListener(v -> finish());

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter();
        rvSongs.setAdapter(adapter);
    }

    private void loadArtistDetails(String artistName) {
        tvArtistName.setText(artistName);

        new Thread(() -> {
            List<Song> songs = MusicDatabase.getInstance(this)
                    .songDao()
                    .getSongsByArtist(artistName);

            runOnUiThread(() -> {
                songList.clear();
                songList.addAll(songs);
                adapter.notifyDataSetChanged();
                tvSongCount.setText(songs.size() + " songs");

                if (!songs.isEmpty() && songs.get(0).getAlbumArt() != null) {
                    Glide.with(this)
                            .load(songs.get(0).getAlbumArt())
                            .placeholder(R.drawable.ic_artist)
                            .error(R.drawable.ic_artist)
                            .circleCrop()
                            .into(ivArtistImage);
                }
            });
        }).start();
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_song_with_art, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Song song = songList.get(position);

            holder.tvTitle.setText(song.getTitle());
            holder.tvAlbum.setText(song.getAlbum());
            holder.tvDuration.setText(song.getDurationFormatted());

            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(ArtistDetailsActivity.this)
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.default_album_art_small)
                        .error(R.drawable.default_album_art_small)
                        .into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
            }

            holder.itemView.setOnClickListener(v -> {
                MediaPlayerHelper.getInstance().playSong(ArtistDetailsActivity.this, songList, position);
                startActivity(new Intent(ArtistDetailsActivity.this, NowPlayingActivity.class));
            });
        }

        @Override
        public int getItemCount() {
            return songList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAlbumArt;
            TextView tvTitle, tvAlbum, tvDuration;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvAlbum = itemView.findViewById(R.id.tv_song_artist);
                tvDuration = itemView.findViewById(R.id.tv_song_duration);
            }
        }
    }
}