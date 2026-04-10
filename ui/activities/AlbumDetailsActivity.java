package com.example.echo_wave.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class AlbumDetailsActivity extends AppCompatActivity {

    private ImageView btnBack, ivAlbumArt;
    private TextView tvAlbumName, tvArtistName, tvSongCount;
    private RecyclerView rvSongs;
    private SongsAdapter adapter;
    private List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        String albumName = getIntent().getStringExtra("album_name");
        String artistName = getIntent().getStringExtra("artist_name");

        initViews();
        loadAlbumDetails(albumName, artistName);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvAlbumName = findViewById(R.id.tv_album_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvSongCount = findViewById(R.id.tv_song_count);
        rvSongs = findViewById(R.id.rv_songs);

        btnBack.setOnClickListener(v -> finish());

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongsAdapter();
        rvSongs.setAdapter(adapter);
    }

    private void loadAlbumDetails(String albumName, String artistName) {
        tvAlbumName.setText(albumName);
        tvArtistName.setText(artistName);

        new Thread(() -> {
            List<Song> songs = MusicDatabase.getInstance(this)
                    .songDao()
                    .getSongsByAlbum(albumName);

            runOnUiThread(() -> {
                songList.clear();
                songList.addAll(songs);
                adapter.notifyDataSetChanged();
                tvSongCount.setText(songs.size() + " songs");

                if (!songs.isEmpty() && songs.get(0).getAlbumArt() != null) {
                    Glide.with(this)
                            .load(songs.get(0).getAlbumArt())
                            .placeholder(R.drawable.default_album_art)
                            .error(R.drawable.default_album_art)
                            .into(ivAlbumArt);
                }
            });
        }).start();
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_song_track, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Song song = songList.get(position);

            holder.tvTrackNumber.setText(String.valueOf(position + 1));
            holder.tvTitle.setText(song.getTitle());
            holder.tvDuration.setText(song.getDurationFormatted());

            holder.itemView.setOnClickListener(v -> {
                MediaPlayerHelper.getInstance().playSong(AlbumDetailsActivity.this, songList, position);
                startActivity(new Intent(AlbumDetailsActivity.this, NowPlayingActivity.class));
            });
        }

        @Override
        public int getItemCount() {
            return songList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTrackNumber, tvTitle, tvDuration;

            ViewHolder(android.view.View itemView) {
                super(itemView);
                tvTrackNumber = itemView.findViewById(R.id.tv_track_number);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }
    }
}