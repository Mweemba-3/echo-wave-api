package com.example.echo_wave.ui.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.echo_wave.R;
import com.example.echo_wave.utils.ArtistData;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtistSelectionActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ArtistSelectionPrefs";
    private static final String KEY_SELECTED_ARTISTS = "selected_artists";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ARTISTS_SELECTED = "artists_selected";
    private RecyclerView rvArtists;
    private ChipGroup chipGroupSelected;
    private LinearLayout layoutSearch;
    private EditText etSearch;
    private ImageView btnSearch, btnClearSearch;
    private Button btnDone;
    private TextView tvSelectedCount;

    private ArtistAdapter artistAdapter;
    private List<String> allArtists;
    private List<String> filteredArtists;
    private Set<String> selectedArtists = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_selection);

        // Check if artists have already been selected
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean hasSelectedArtists = prefs.getBoolean(KEY_ARTISTS_SELECTED, false);

        if (hasSelectedArtists) {
            // Artists already selected, go to main activity
            startMainActivity();
            return;
        }

        initViews();
        setupArtists();
        setupListeners();
    }

    private void initViews() {
        rvArtists = findViewById(R.id.rv_artists);
        chipGroupSelected = findViewById(R.id.chip_group_selected);
        layoutSearch = findViewById(R.id.layout_search);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        btnDone = findViewById(R.id.btn_done);
        tvSelectedCount = findViewById(R.id.tv_selected_count);

        rvArtists.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void setupArtists() {
        allArtists = ArtistData.getArtists();
        filteredArtists = new ArrayList<>(allArtists);
        artistAdapter = new ArtistAdapter(filteredArtists);
        rvArtists.setAdapter(artistAdapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> performSearch());
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            clearSearch();
        });

        btnDone.setOnClickListener(v -> {
            if (selectedArtists.isEmpty()) {
                Toast.makeText(this, "Please select at least one artist", Toast.LENGTH_SHORT).show();
                return;
            }

            saveSelectedArtists();
            startMainActivity();
        });
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            clearSearch();
            return;
        }

        filteredArtists.clear();
        for (String artist : allArtists) {
            if (artist.toLowerCase().contains(query)) {
                filteredArtists.add(artist);
            }
        }
        artistAdapter.notifyDataSetChanged();
        btnClearSearch.setVisibility(View.VISIBLE);
    }

    private void clearSearch() {
        filteredArtists.clear();
        filteredArtists.addAll(allArtists);
        artistAdapter.notifyDataSetChanged();
        btnClearSearch.setVisibility(View.GONE);
    }

    private void toggleArtist(String artist) {
        if (selectedArtists.contains(artist)) {
            selectedArtists.remove(artist);
        } else {
            selectedArtists.add(artist);
        }
        updateSelectedChips();
        artistAdapter.notifyDataSetChanged();
    }

    private void updateSelectedChips() {
        chipGroupSelected.removeAllViews();
        tvSelectedCount.setText(selectedArtists.size() + " artists selected");

        for (String artist : selectedArtists) {
            Chip chip = new Chip(this);
            chip.setText(artist);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedArtists.remove(artist);
                updateSelectedChips();
                artistAdapter.notifyDataSetChanged();
            });
            chipGroupSelected.addView(chip);
        }
    }

    private void saveSelectedArtists() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (String artist : selectedArtists) {
            sb.append(artist).append(",");
        }
        prefs.edit()
                .putString("selected_artists", sb.toString())
                .putBoolean(KEY_ARTISTS_SELECTED, true)
                .apply();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {
        private List<String> artists;

        ArtistAdapter(List<String> artists) {
            this.artists = artists;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_artist_select, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String artist = artists.get(position);
            holder.tvName.setText(artist);
            holder.ivCheck.setVisibility(selectedArtists.contains(artist) ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                toggleArtist(artist);
                holder.ivCheck.setVisibility(selectedArtists.contains(artist) ? View.VISIBLE : View.GONE);
            });
        }

        @Override
        public int getItemCount() {
            return artists.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView ivCheck;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_artist_name);
                ivCheck = itemView.findViewById(R.id.iv_check);
            }
        }
    }
}