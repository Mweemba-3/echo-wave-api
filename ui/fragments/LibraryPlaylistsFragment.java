package com.example.echo_wave.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.Playlist;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class LibraryPlaylistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private PlaylistAdapter adapter;
    private List<Playlist> playlistList = new ArrayList<>();
    private MusicDatabase database;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_playlists, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        fabAdd = view.findViewById(R.id.fabAdd);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter();
        recyclerView.setAdapter(adapter);

        database = MusicDatabase.getInstance(requireContext());

        createDefaultPlaylists();
        loadPlaylists();

        fabAdd.setOnClickListener(v -> showCreateDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void createDefaultPlaylists() {
        new Thread(() -> {
            try {
                if (database.playlistDao().getAllPlaylists().isEmpty()) {
                    Playlist fav = new Playlist("Favorites");
                    fav.setDefault(true);
                    database.playlistDao().insert(fav);

                    Playlist recent = new Playlist("Recently Added");
                    recent.setDefault(true);
                    database.playlistDao().insert(recent);

                    Playlist most = new Playlist("Most Played");
                    most.setDefault(true);
                    database.playlistDao().insert(most);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadPlaylists() {
        new Thread(() -> {
            try {
                List<Playlist> playlists = database.playlistDao().getAllPlaylists();
                for (Playlist p : playlists) {
                    int count = database.playlistSongDao().getSongCount(p.getId());
                    p.setSongCount(count);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        playlistList.clear();
                        playlistList.addAll(playlists);
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showCreateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("New Playlist");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        TextView input = dialogView.findViewById(R.id.et_playlist_name);
        builder.setView(dialogView);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Playlist playlist = new Playlist(name);
                new Thread(() -> {
                    try {
                        database.playlistDao().insert(playlist);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadPlaylists();
                                Toast.makeText(getContext(), "Playlist created", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void playPlaylist(Playlist playlist) {
        new Thread(() -> {
            try {
                List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlist.getId());

                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (songs.isEmpty()) {
                            Toast.makeText(getContext(), "No songs in playlist", Toast.LENGTH_SHORT).show();
                        } else {
                            MediaPlayerHelper.getInstance().playSong(requireContext(), songs, 0);
                            Intent intent = new Intent(getContext(), NowPlayingActivity.class);
                            startActivity(intent);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deletePlaylist(Playlist playlist) {
        if (playlist.isDefault()) {
            Toast.makeText(getContext(), "Cannot delete default playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Playlist")
                .setMessage("Delete " + playlist.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            database.playlistSongDao().clearPlaylist(playlist.getId());
                            database.playlistDao().delete(playlist);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    loadPlaylists();
                                    Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPlaylistOptions(Playlist playlist) {
        if (playlist.isDefault()) {
            // Default playlists - only play option
            String[] options = {"Play", "View Details"};

            new AlertDialog.Builder(requireContext())
                    .setTitle(playlist.getName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                playPlaylist(playlist);
                                break;
                            case 1:
                                showPlaylistDetails(playlist);
                                break;
                        }
                    })
                    .show();
        } else {
            // User playlists - full options
            String[] options = {"Play", "Shuffle", "Rename", "Delete", "Share"};

            new AlertDialog.Builder(requireContext())
                    .setTitle(playlist.getName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0:
                                playPlaylist(playlist);
                                break;
                            case 1:
                                shufflePlaylist(playlist);
                                break;
                            case 2:
                                showRenameDialog(playlist);
                                break;
                            case 3:
                                deletePlaylist(playlist);
                                break;
                            case 4:
                                sharePlaylist(playlist);
                                break;
                        }
                    })
                    .show();
        }
    }

    private void shufflePlaylist(Playlist playlist) {
        new Thread(() -> {
            try {
                List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlist.getId());

                if (!songs.isEmpty()) {
                    // Shuffle the list
                    java.util.Collections.shuffle(songs);
                    requireActivity().runOnUiThread(() -> {
                        MediaPlayerHelper.getInstance().playSong(requireContext(), songs, 0);
                        Intent intent = new Intent(getContext(), NowPlayingActivity.class);
                        startActivity(intent);
                    });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "No songs in playlist", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPlaylistDetails(Playlist playlist) {
        new Thread(() -> {
            try {
                int songCount = database.playlistSongDao().getSongCount(playlist.getId());
                List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlist.getId());

                long totalDuration = 0;
                for (Song song : songs) {
                    totalDuration += song.getDuration();
                }

                long finalTotalDuration = totalDuration;
                int finalSongCount = songCount;

                requireActivity().runOnUiThread(() -> {
                    String details = String.format(
                            "Name: %s\nSongs: %d\nTotal Duration: %s",
                            playlist.getName(),
                            finalSongCount,
                            formatDuration(finalTotalDuration)
                    );

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Playlist Details")
                            .setMessage(details)
                            .setPositiveButton("OK", null)
                            .show();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatDuration(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private void showRenameDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Rename Playlist");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_playlist, null);
        TextView input = dialogView.findViewById(R.id.et_playlist_name);
        input.setText(playlist.getName());
        input.setHint("New playlist name");
        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                playlist.setName(newName);
                new Thread(() -> {
                    try {
                        database.playlistDao().update(playlist);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadPlaylists();
                                Toast.makeText(getContext(), "Playlist renamed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                Toast.makeText(getContext(), "Please enter a name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void sharePlaylist(Playlist playlist) {
        new Thread(() -> {
            try {
                List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlist.getId());

                StringBuilder sb = new StringBuilder();
                sb.append("🎵 Playlist: ").append(playlist.getName()).append("\n\n");
                sb.append("Songs:\n");

                for (int i = 0; i < songs.size(); i++) {
                    Song song = songs.get(i);
                    sb.append(i + 1).append(". ").append(song.getTitle())
                            .append(" - ").append(song.getArtist()).append("\n");
                }

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Playlist: " + playlist.getName());
                shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

                startActivity(Intent.createChooser(shareIntent, "Share Playlist"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Playlist playlist = playlistList.get(position);

            holder.tvName.setText(playlist.getName());
            holder.tvCount.setText(playlist.getSongCount() + " songs");

            // Set icon based on playlist type
            if (playlist.isDefault()) {
                String name = playlist.getName().toLowerCase();
                if (name.contains("favorite")) {
                    holder.ivIcon.setImageResource(R.drawable.ic_favorite_filled);
                } else if (name.contains("recent")) {
                    holder.ivIcon.setImageResource(R.drawable.ic_history);
                } else if (name.contains("most played")) {
                    holder.ivIcon.setImageResource(R.drawable.ic_most_played);
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_playlist);
                }
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_playlist);
                // Load first song's album art if available
                loadPlaylistArt(holder, playlist);
            }

            // Click on item - play playlist
            holder.itemView.setOnClickListener(v -> playPlaylist(playlist));

            // Long press - show options
            holder.itemView.setOnLongClickListener(v -> {
                showPlaylistOptions(playlist);
                return true;
            });
        }

        private void loadPlaylistArt(ViewHolder holder, Playlist playlist) {
            new Thread(() -> {
                try {
                    List<Song> songs = database.playlistSongDao().getSongsForPlaylist(playlist.getId());
                    if (!songs.isEmpty() && getActivity() != null) {
                        Song firstSong = songs.get(0);
                        getActivity().runOnUiThread(() -> {
                            if (firstSong.getAlbumArt() != null && !firstSong.getAlbumArt().isEmpty()) {
                                Glide.with(requireContext())
                                        .load(firstSong.getAlbumArt())
                                        .placeholder(R.drawable.default_playlist_art)
                                        .error(R.drawable.default_playlist_art)
                                        .circleCrop()
                                        .into(holder.ivIcon);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        @Override
        public int getItemCount() {
            return playlistList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvCount;

            ViewHolder(View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_icon);
                tvName = itemView.findViewById(R.id.tv_name);
                tvCount = itemView.findViewById(R.id.tv_count);
            }
        }
    }
}