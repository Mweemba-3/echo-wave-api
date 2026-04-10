package com.example.echo_wave.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.echo_wave.R;
import com.example.echo_wave.data.MusicDatabase;
import com.example.echo_wave.models.OnlineSong;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.network.OnlineMusicClient;
import com.example.echo_wave.ui.activities.NowPlayingActivity;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private ImageView btnClearSearch, btnSearchAction;
    private RecyclerView rvResults;
    private AdView adViewSearch;
    private TextView tvNoResults;
    private TextView tvRecentSearches, tvClearRecent;
    private View recentSearchesContainer;
    private ViewGroup chipGroupRecent;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    private SearchAdapter adapter;
    private OnlineSearchAdapter onlineAdapter;
    private List<Song> searchResults = new ArrayList<>();
    private List<OnlineSong> onlineResults = new ArrayList<>();
    private List<String> recentSearches = new ArrayList<>();
    private View view;

    private OnlineMusicClient onlineMusicClient;
    private int currentTab = 0; // 0 = Local, 1 = Online

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_search, container, false);

        onlineMusicClient = OnlineMusicClient.getInstance(requireContext());

        initViews();
        setupTabs();
        setupSearchListeners();
        loadRecentSearches();

        return view;
    }

    private void initViews() {
        etSearch = view.findViewById(R.id.et_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        btnSearchAction = view.findViewById(R.id.btn_search_action);
        rvResults = view.findViewById(R.id.rv_results);
        tvNoResults = view.findViewById(R.id.tv_no_results);
        tvRecentSearches = view.findViewById(R.id.tv_recent_searches);
        tvClearRecent = view.findViewById(R.id.tv_clear_recent);
        recentSearchesContainer = view.findViewById(R.id.recent_searches_container);
        chipGroupRecent = view.findViewById(R.id.chip_group_recent);
        tabLayout = view.findViewById(R.id.tab_layout);
        progressBar = view.findViewById(R.id.progress_bar);

        adapter = new SearchAdapter(searchResults);
        onlineAdapter = new OnlineSearchAdapter(onlineResults);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);
        // Load banner ad
        adViewSearch = view.findViewById(R.id.adView_search);
        if (adViewSearch != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewSearch.loadAd(adRequest);
        }

        tvNoResults.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btnClearSearch.setVisibility(View.GONE);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Local"));
        tabLayout.addTab(tabLayout.newTab().setText("Online"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                clearResults();
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    showRecentSearches();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearchListeners() {
        tvClearRecent.setOnClickListener(v -> clearRecentSearches());

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            clearResults();
            btnClearSearch.setVisibility(View.GONE);
            showRecentSearches();
        });

        btnSearchAction.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
                hideRecentSearches();
            } else {
                showToast("Enter a search term");
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                    hideRecentSearches();
                }
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (query.isEmpty()) {
                    clearResults();
                    showRecentSearches();
                } else if (query.length() >= 2 && currentTab == 0) {
                    performLocalSearch(query);
                    hideRecentSearches();
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        etSearch.requestFocus();
    }

    private void performSearch(String query) {
        if (currentTab == 0) {
            performLocalSearch(query);
        } else {
            performOnlineSearch(query);
        }
    }

    private void performLocalSearch(String query) {
        if (rvResults.getAdapter() != adapter) {
            rvResults.setAdapter(adapter);
        }

        new Thread(() -> {
            List<Song> results = MusicDatabase.getInstance(requireContext())
                    .songDao()
                    .searchSongs("%" + query + "%");

            if (getActivity() != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    searchResults.clear();
                    searchResults.addAll(results);
                    adapter.notifyDataSetChanged();

                    if (results.isEmpty()) {
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("No local results found");
                        rvResults.setVisibility(View.GONE);
                    } else {
                        tvNoResults.setVisibility(View.GONE);
                        rvResults.setVisibility(View.VISIBLE);
                        saveRecentSearch(query);
                    }
                });
            }
        }).start();
    }

    private void performOnlineSearch(String query) {
        if (rvResults.getAdapter() != onlineAdapter) {
            rvResults.setAdapter(onlineAdapter);
        }

        showLoading(true);
        clearResults();

        onlineMusicClient.searchMusic(query, 50, new OnlineMusicClient.SearchCallback() {
            @Override
            public void onSuccess(List<OnlineSong> songs) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        onlineResults.clear();
                        onlineResults.addAll(songs);
                        onlineAdapter.notifyDataSetChanged();

                        if (songs.isEmpty()) {
                            tvNoResults.setVisibility(View.VISIBLE);
                            tvNoResults.setText("No online results found");
                            rvResults.setVisibility(View.GONE);
                        } else {
                            tvNoResults.setVisibility(View.GONE);
                            rvResults.setVisibility(View.VISIBLE);
                            saveRecentSearch(query);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        tvNoResults.setVisibility(View.VISIBLE);
                        tvNoResults.setText("Search failed: " + error);
                        rvResults.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void downloadAndPlay(OnlineSong song) {
        showToast("Downloading: " + song.getTitle());

        onlineMusicClient.downloadSong(song, new OnlineMusicClient.DownloadCallback() {
            @Override
            public void onSuccess(OnlineSong downloadedSong, String filePath) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        addToLocalLibrary(downloadedSong, filePath);

                        Song localSong = new Song();
                        localSong.setId(downloadedSong.getId());
                        localSong.setTitle(downloadedSong.getTitle());
                        localSong.setArtist(downloadedSong.getArtist());
                        localSong.setDuration(downloadedSong.getDurationSeconds());
                        localSong.setPath(filePath);
                        localSong.setAlbumArt(downloadedSong.getThumbnail());

                        MediaPlayerHelper.getInstance().playSong(requireContext(), localSong);
                        startActivity(new Intent(requireContext(), NowPlayingActivity.class));
                    });
                }
            }

            @Override public void onProgress(int progress) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> showToast("Download failed: " + error));
                }
            }
        });
    }

    private void downloadSong(OnlineSong song, int position) {
        showToast("Downloading: " + song.getTitle());

        onlineMusicClient.downloadSong(song, new OnlineMusicClient.DownloadCallback() {
            @Override
            public void onSuccess(OnlineSong downloadedSong, String filePath) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        showToast("Downloaded: " + downloadedSong.getTitle());
                        addToLocalLibrary(downloadedSong, filePath);
                        song.setDownloaded(true);
                        onlineAdapter.notifyItemChanged(position);
                    });
                }
            }

            @Override public void onProgress(int progress) {}

            @Override
            public void onError(String error) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> showToast("Download failed: " + error));
                }
            }
        });
    }

    private void addToLocalLibrary(OnlineSong song, String filePath) {
        Song localSong = new Song();
        localSong.setId(song.getId());
        localSong.setTitle(song.getTitle());
        localSong.setArtist(song.getArtist());
        localSong.setDuration(song.getDurationSeconds());
        localSong.setPath(filePath);
        localSong.setAlbumArt(song.getThumbnail());
        localSong.setDateAdded(System.currentTimeMillis());
        localSong.setPlayCount(0);
        localSong.setLastPlayed(0);
        localSong.setFavorite(false);

        new Thread(() -> {
            try {
                MusicDatabase.getInstance(requireContext()).songDao().insert(localSong);
            } catch (Exception e) {
                Log.e("SearchFragment", "Error saving song: " + e.getMessage());
            }
        }).start();
    }

    private void clearResults() {
        searchResults.clear();
        onlineResults.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (onlineAdapter != null) onlineAdapter.notifyDataSetChanged();
        rvResults.setVisibility(View.GONE);
        tvNoResults.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void loadRecentSearches() {
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("search_prefs", Context.MODE_PRIVATE);
        String recent = prefs.getString("recent_searches", "");
        recentSearches.clear();

        if (!recent.isEmpty()) {
            for (String item : recent.split(";;")) {
                if (!item.isEmpty()) recentSearches.add(item);
            }
        }
        updateRecentSearchesUI();
    }

    private void saveRecentSearch(String query) {
        if (query.length() < 2) return;
        recentSearches.remove(query);
        recentSearches.add(0, query);
        while (recentSearches.size() > 10) recentSearches.remove(recentSearches.size() - 1);

        StringBuilder sb = new StringBuilder();
        for (String s : recentSearches) sb.append(s).append(";;");

        requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
                .edit().putString("recent_searches", sb.toString()).apply();

        updateRecentSearchesUI();
    }

    private void clearRecentSearches() {
        recentSearches.clear();
        requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
                .edit().putString("recent_searches", "").apply();
        updateRecentSearchesUI();
    }

    private void updateRecentSearchesUI() {
        if (recentSearches.isEmpty()) {
            recentSearchesContainer.setVisibility(View.GONE);
            return;
        }

        recentSearchesContainer.setVisibility(View.VISIBLE);
        chipGroupRecent.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (String search : recentSearches) {
            MaterialCardView chip = (MaterialCardView) inflater.inflate(R.layout.item_search_chip, chipGroupRecent, false);
            TextView tvChip = chip.findViewById(R.id.tv_chip_text);
            tvChip.setText(search);
            chip.setOnClickListener(v -> {
                etSearch.setText(search);
                etSearch.setSelection(search.length());
                performSearch(search);
                hideRecentSearches();
            });
            chipGroupRecent.addView(chip);
        }
    }

    private void showRecentSearches() {
        if (!recentSearches.isEmpty()) recentSearchesContainer.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);
    }

    private void hideRecentSearches() {
        recentSearchesContainer.setVisibility(View.GONE);
    }

    private void showToast(String message) {
        if (getActivity() != null && isAdded()) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    // ========== LOCAL SEARCH ADAPTER ==========
    private class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<Song> songs;

        SearchAdapter(List<Song> songs) { this.songs = songs; }

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
        }

        @Override
        public int getItemCount() { return songs.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAlbumArt;
            TextView tvTitle, tvArtist, tvDuration;

            ViewHolder(View itemView) {
                super(itemView);
                ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
                tvTitle = itemView.findViewById(R.id.tv_song_title);
                tvArtist = itemView.findViewById(R.id.tv_song_artist);
                tvDuration = itemView.findViewById(R.id.tv_song_duration);
            }
        }
    }

    // ========== ONLINE SEARCH ADAPTER ==========
    private class OnlineSearchAdapter extends RecyclerView.Adapter<OnlineSearchAdapter.ViewHolder> {
        private List<OnlineSong> songs;

        OnlineSearchAdapter(List<OnlineSong> songs) { this.songs = songs; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_online_song, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnlineSong song = songs.get(position);
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getDuration());

            if (song.getThumbnail() != null && !song.getThumbnail().isEmpty()) {
                Glide.with(requireContext())
                        .load(song.getThumbnail())
                        .placeholder(R.drawable.default_album_art_small)
                        .error(R.drawable.default_album_art_small)
                        .into(holder.ivThumbnail);
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.default_album_art_small);
            }

            holder.btnPlay.setOnClickListener(v -> downloadAndPlay(song));
            holder.btnDownload.setOnClickListener(v -> downloadSong(song, position));
        }

        @Override
        public int getItemCount() { return songs.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail, btnPlay, btnDownload;
            TextView tvTitle, tvArtist, tvDuration;

            ViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                btnPlay = itemView.findViewById(R.id.btn_play);
                btnDownload = itemView.findViewById(R.id.btn_download);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvArtist = itemView.findViewById(R.id.tv_artist);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }
        }
    }
}