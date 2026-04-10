package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.PlaylistSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.AlbumDetailsActivity;
import com.example.echo_wave.ui.activities.ArtistDetailsActivity;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SongsTabFragment extends Fragment {

    private static final String TAG = "SongsTabFragment";

    private RecyclerView rvSongs;
    private SongsAdapter adapter;
    private List<Song> songList = new ArrayList<>();
    private TextView tvEmptyState;
    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_songs_tab, container, false);
        initViews();
        loadSongs();
        return view;
    }

    private void initViews() {
        rvSongs = view.findViewById(R.id.rv_songs);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongsAdapter(songList);
        rvSongs.setAdapter(adapter);
    }

    private void loadSongs() {
        new Thread(() -> {
            try {
                List<Song> songs = MusicDatabase.getInstance(requireContext())
                        .songDao().getAllSongs();

                requireActivity().runOnUiThread(() -> {
                    songList.clear();
                    songList.addAll(songs);
                    adapter.notifyDataSetChanged();

                    // Show empty state if no songs
                    if (songList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        rvSongs.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        rvSongs.setVisibility(View.VISIBLE);
                    }

                    Log.d(TAG, "Loaded " + songs.size() + " songs");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading songs: " + e.getMessage());
            }
        }).start();
    }

    private void showSongOptions(Song song) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_song_options, null);

        TextView tvTitle = sheetView.findViewById(R.id.tv_song_title);
        TextView tvArtist = sheetView.findViewById(R.id.tv_artist_name);
        ImageView ivSongArt = sheetView.findViewById(R.id.iv_song_art);
        MaterialButton btnPlayNext = sheetView.findViewById(R.id.btn_play_next);
        MaterialButton btnAddToQueue = sheetView.findViewById(R.id.btn_add_to_queue);
        MaterialButton btnAddToPlaylist = sheetView.findViewById(R.id.btn_add_to_playlist);
        MaterialButton btnGoToArtist = sheetView.findViewById(R.id.btn_go_to_artist);
        MaterialButton btnGoToAlbum = sheetView.findViewById(R.id.btn_go_to_album);
        MaterialButton btnShare = sheetView.findViewById(R.id.btn_share);

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());

        if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
            Glide.with(requireContext())
                    .load(song.getAlbumArt())
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art)
                    .into(ivSongArt);
        } else {
            ivSongArt.setImageResource(R.drawable.default_album_art);
        }

        btnPlayNext.setOnClickListener(v -> {
            MediaPlayerHelper.getInstance().addToQueueNext(song);
            Toast.makeText(getContext(), "Added to play next", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnAddToQueue.setOnClickListener(v -> {
            MediaPlayerHelper.getInstance().addToQueue(song);
            Toast.makeText(getContext(), "Added to queue", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnAddToPlaylist.setOnClickListener(v -> {
            showPlaylistDialog(song);
            dialog.dismiss();
        });

        btnGoToArtist.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ArtistDetailsActivity.class);
            intent.putExtra("artist_name", song.getArtist());
            startActivity(intent);
            dialog.dismiss();
        });

        btnGoToAlbum.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AlbumDetailsActivity.class);
            intent.putExtra("album_name", song.getAlbum());
            intent.putExtra("artist_name", song.getArtist());
            startActivity(intent);
            dialog.dismiss();
        });

        btnShare.setOnClickListener(v -> {
            shareSong(song);
            dialog.dismiss();
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void showPlaylistDialog(Song song) {
        new Thread(() -> {
            try {
                List<Playlist> playlists = MusicDatabase.getInstance(requireContext())
                        .playlistDao().getAllPlaylists();

                requireActivity().runOnUiThread(() -> {
                    if (playlists == null || playlists.isEmpty()) {
                        Toast.makeText(getContext(), "No playlists found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] playlistNames = new String[playlists.size()];
                    for (int i = 0; i < playlists.size(); i++) {
                        playlistNames[i] = playlists.get(i).getName();
                    }

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Add to Playlist")
                            .setItems(playlistNames, (dialog, which) -> {
                                Playlist playlist = playlists.get(which);
                                addSongToPlaylist(playlist, song);
                            })
                            .show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading playlists: " + e.getMessage());
            }
        }).start();
    }

    private void addSongToPlaylist(Playlist playlist, Song song) {
        new Thread(() -> {
            try {
                // Check if song already exists in playlist
                int count = MusicDatabase.getInstance(requireContext())
                        .playlistSongDao().isSongInPlaylist(playlist.getId(), song.getId());

                if (count == 0) {
                    // Add song to playlist using PlaylistSong junction table
                    PlaylistSong playlistSong = new PlaylistSong(playlist.getId(), song.getId());
                    MusicDatabase.getInstance(requireContext())
                            .playlistSongDao().insert(playlistSong);

                    // Update playlist song count
                    int newCount = MusicDatabase.getInstance(requireContext())
                            .playlistSongDao().getSongCount(playlist.getId());
                    playlist.setSongCount(newCount);
                    MusicDatabase.getInstance(requireContext())
                            .playlistDao().update(playlist);

                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Added to " + playlist.getName(),
                                    Toast.LENGTH_SHORT).show());
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Song already in playlist",
                                    Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding to playlist: " + e.getMessage());
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Error adding to playlist",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void shareSong(Song song) {
        try {
            File file = new File(song.getPath());
            android.net.Uri uri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                uri = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", file);
            } else {
                uri = android.net.Uri.fromFile(file);
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing song: " + e.getMessage());
            Toast.makeText(getContext(), "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }

    private class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.ViewHolder> {
        private List<Song> songs;

        SongsAdapter(List<Song> songs) {
            this.songs = songs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_song_with_art, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Song song = songs.get(position);

            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getDurationFormatted());

            if (song.getAlbumArt() != null && !song.getAlbumArt().isEmpty()) {
                Glide.with(requireContext())
                        .load(song.getAlbumArt())
                        .placeholder(R.drawable.default_album_art_small)
                        .error(R.drawable.default_album_art_small)
                        .into(holder.ivAlbumArt);
            } else {
                holder.ivAlbumArt.setImageResource(R.drawable.default_album_art_small);
            }

            holder.itemView.setOnClickListener(v -> {
                MediaPlayerHelper.getInstance().playSong(requireContext(), song);
                startActivity(new Intent(requireContext(), NowPlayingActivity.class));
            });

            holder.itemView.setOnLongClickListener(v -> {
                showSongOptions(song);
                return true;
            });

            if (holder.btnMore != null) {
                holder.btnMore.setOnClickListener(v -> {
                    showSongOptions(song);
                });
            }
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAlbumArt, btnMore;
            TextView tvTitle, tvArtist, tvDuration;

            ViewHolder(View itemView) {
                super(itemView);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvArtist = itemView.findViewById(R.id.tv_song_artist);
                tvDuration = itemView.findViewById(R.id.tv_song_duration);
                btnMore = itemView.findViewById(R.id.btn_more);
            }
        }
    }
}