package com.example.echo_wave.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.echo_wave.R;
import com.example.echo_wave.services.MediaPlayerService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EqualizerActivity extends AppCompatActivity {

    private static final String TAG = "EqualizerActivity";
    private static final String PREFS_NAME = "equalizer_prefs";
    private static final String KEY_EQ_ENABLED = "eq_enabled";
    private static final String KEY_PRESET = "eq_preset";
    private static final String KEY_BASS_BOOST = "bass_boost";
    private static final String KEY_VIRTUALIZER = "virtualizer";
    private static final String KEY_CUSTOM_BANDS = "custom_bands";

    // Views
    private ImageView btnBack;
    private SwitchMaterial switchEqualizer;
    private LinearLayout bandsContainer;
    private MaterialButton btnReset, btnSavePreset;
    private TextView tvStatus, tvCurrentPreset;
    private LinearLayout presetsContainer;
    private SeekBar seekBass, seekVirtualizer, seekReverb, seekWideness;
    private TextView tvBassValue, tvVirtualizerValue, tvReverbValue, tvWidenessValue;

    // Audio Effects
    private MediaPlayerService mediaPlayerService;
    private boolean isServiceBound = false;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;

    private boolean isEqEnabled = true;
    private short numberOfBands = 0;
    private short[] bandLevelRange;
    private List<SeekBar> seekBars = new ArrayList<>();
    private List<TextView> valueTexts = new ArrayList<>();
    private float[] currentBandLevels;
    private int currentPreset = 0;
    private int bassBoostLevel = 0;
    private int virtualizerLevel = 0;
    private int reverbLevel = 0;
    private int widenessLevel = 0;

    // Presets
    private String[] presetNames = {
            "Normal", "Pop", "Rock", "Jazz", "Classical",
            "Dance", "Hip Hop", "R&B", "Electronic", "Acoustic",
            "Bass Boost", "Treble Boost", "Vocal Boost", "Party", "CUSTOM"
    };

    private short[][] presetValues = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},           // Normal
            {6, 5, 3, 0, -3, -4, -2, 2, 5, 7},        // Pop
            {8, 6, 2, -3, -6, -6, -3, 2, 6, 8},       // Rock
            {3, 4, 5, 4, 3, 2, 2, 3, 4, 3},           // Jazz
            {-5, -3, 0, 3, 5, 5, 3, 0, -3, -5},       // Classical
            {8, 6, 4, 2, -2, -4, -2, 2, 4, 6},        // Dance
            {12, 10, 6, 0, -4, -6, -4, 2, 6, 10},     // Hip Hop
            {6, 6, 5, 3, 0, -2, 0, 3, 5, 5},          // R&B
            {9, 7, 4, 0, -4, -5, -3, 2, 6, 9},        // Electronic
            {2, 3, 4, 3, 2, 1, 1, 2, 3, 2},           // Acoustic
            {15, 13, 10, 5, 0, -5, -8, -5, 0, 5},     // Bass Boost
            {-5, -3, 0, 3, 5, 7, 9, 11, 13, 15},      // Treble Boost
            {0, 2, 5, 8, 8, 6, 4, 2, 0, -2},          // Vocal Boost
            {8, 7, 6, 5, 3, 2, 3, 5, 7, 8},           // Party
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}            // CUSTOM
    };

    private SharedPreferences prefs;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            mediaPlayerService = binder.getService();
            isServiceBound = true;
            initEffects();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_equalizer_complete);
        Log.d(TAG, "Layout inflated");

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        loadSettings();
        bindService();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        switchEqualizer = findViewById(R.id.switch_equalizer);
        bandsContainer = findViewById(R.id.bands_container);
        btnReset = findViewById(R.id.btn_reset);
        btnSavePreset = findViewById(R.id.btn_save_preset);
        tvStatus = findViewById(R.id.tv_status);
        tvCurrentPreset = findViewById(R.id.tv_current_preset);
        presetsContainer = findViewById(R.id.presets_container);
        seekBass = findViewById(R.id.seek_bass);
        seekVirtualizer = findViewById(R.id.seek_virtualizer);
        seekReverb = findViewById(R.id.seek_reverb);
        seekWideness = findViewById(R.id.seek_wideness);
        tvBassValue = findViewById(R.id.tv_bass_value);
        tvVirtualizerValue = findViewById(R.id.tv_virtualizer_value);
        tvReverbValue = findViewById(R.id.tv_reverb_value);
        tvWidenessValue = findViewById(R.id.tv_wideness_value);

        setupTabs();

        btnBack.setOnClickListener(v -> finish());

        switchEqualizer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isEqEnabled = isChecked;
            if (equalizer != null) {
                equalizer.setEnabled(isChecked);
            }
            if (bassBoost != null) {
                bassBoost.setEnabled(isChecked);
            }
            if (virtualizer != null) {
                virtualizer.setEnabled(isChecked);
            }
            saveSettings();
        });

        btnReset.setOnClickListener(v -> resetEqualizer());
        btnSavePreset.setOnClickListener(v -> saveCustomPreset());

        // Bass Boost
        seekBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && bassBoost != null) {
                    bassBoost.setStrength((short) progress);
                    tvBassValue.setText(progress + "%");
                    bassBoostLevel = progress;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // Virtualizer
        seekVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && virtualizer != null) {
                    virtualizer.setStrength((short) progress);
                    tvVirtualizerValue.setText(progress + "%");
                    virtualizerLevel = progress;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // Reverb
        seekReverb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvReverbValue.setText(progress + "%");
                reverbLevel = progress;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        // Wideness
        seekWideness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvWidenessValue.setText(progress + "%");
                widenessLevel = progress;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                saveSettings();
            }
        });

        createPresetButtons();
    }

    private void setupTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        View presetsSection = findViewById(R.id.presets_section);
        View frequencySection = findViewById(R.id.frequency_section);
        View effectsSection = findViewById(R.id.effects_section);

        tabLayout.addTab(tabLayout.newTab().setText("PRESETS"));
        tabLayout.addTab(tabLayout.newTab().setText("FREQUENCY"));
        tabLayout.addTab(tabLayout.newTab().setText("EFFECTS"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                presetsSection.setVisibility(View.GONE);
                frequencySection.setVisibility(View.GONE);
                effectsSection.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0:
                        presetsSection.setVisibility(View.VISIBLE);
                        break;
                    case 1:
                        frequencySection.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        effectsSection.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void createPresetButtons() {
        if (presetsContainer == null) return;

        presetsContainer.removeAllViews();

        for (int i = 0; i < presetNames.length; i++) {
            final int index = i;
            MaterialButton button = new MaterialButton(this);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            button.setText(presetNames[i]);
            button.setTextSize(14);
            button.setPadding(16, 12, 16, 12);
            button.setAllCaps(false);
            button.setCornerRadius(8);
            button.setElevation(2f);

            if (i == currentPreset) {
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.electric_cyan));
                button.setTextColor(ContextCompat.getColor(this, R.color.near_black));
            } else if (i == presetNames.length - 1) {
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.card_background));
                button.setTextColor(ContextCompat.getColor(this, R.color.electric_cyan));
                button.setStrokeColor(ContextCompat.getColorStateList(this, R.color.electric_cyan));
                button.setStrokeWidth(2);
            } else {
                button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.card_background));
                button.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                button.setStrokeWidth(0);
            }

            button.setOnClickListener(v -> applyPreset(index));

            presetsContainer.addView(button);

            if (i < presetNames.length - 1) {
                View space = new View(this);
                space.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 4));
                presetsContainer.addView(space);
            }
        }
    }

    private void bindService() {
        try {
            Intent intent = new Intent(this, MediaPlayerService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e(TAG, "Bind error: " + e.getMessage());
        }
    }

    private void loadSettings() {
        isEqEnabled = prefs.getBoolean(KEY_EQ_ENABLED, true);
        currentPreset = prefs.getInt(KEY_PRESET, 0);
        bassBoostLevel = prefs.getInt(KEY_BASS_BOOST, 0);
        virtualizerLevel = prefs.getInt(KEY_VIRTUALIZER, 0);
        reverbLevel = prefs.getInt("reverb", 0);
        widenessLevel = prefs.getInt("wideness", 0);

        switchEqualizer.setChecked(isEqEnabled);

        if (currentPreset < presetNames.length) {
            tvCurrentPreset.setText(presetNames[currentPreset]);
        }

        seekBass.setProgress(bassBoostLevel);
        tvBassValue.setText(bassBoostLevel + "%");

        seekVirtualizer.setProgress(virtualizerLevel);
        tvVirtualizerValue.setText(virtualizerLevel + "%");

        seekReverb.setProgress(reverbLevel);
        tvReverbValue.setText(reverbLevel + "%");

        seekWideness.setProgress(widenessLevel);
        tvWidenessValue.setText(widenessLevel + "%");

        createPresetButtons();
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_EQ_ENABLED, isEqEnabled);
        editor.putInt(KEY_PRESET, currentPreset);
        editor.putInt(KEY_BASS_BOOST, bassBoostLevel);
        editor.putInt(KEY_VIRTUALIZER, virtualizerLevel);
        editor.putInt("reverb", reverbLevel);
        editor.putInt("wideness", widenessLevel);
        editor.apply();

        // Apply to service immediately
        if (isServiceBound && mediaPlayerService != null) {
            mediaPlayerService.applyEqualizerSettings();
        }

        Log.d(TAG, "Settings saved and applied");
    }

    private void saveCustomBands() {
        if (currentBandLevels == null || numberOfBands == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numberOfBands; i++) {
            if (i > 0) sb.append(",");
            sb.append(currentBandLevels[i]);
        }
        prefs.edit().putString(KEY_CUSTOM_BANDS, sb.toString()).apply();
    }

    private void loadCustomBands() {
        String saved = prefs.getString(KEY_CUSTOM_BANDS, null);
        if (saved != null && !saved.isEmpty()) {
            String[] values = saved.split(",");
            for (int i = 0; i < Math.min(values.length, numberOfBands); i++) {
                try {
                    currentBandLevels[i] = Float.parseFloat(values[i]);
                } catch (NumberFormatException e) {
                    currentBandLevels[i] = 0;
                }
            }
        }
    }

    private void initEffects() {
        try {
            if (!isServiceBound || mediaPlayerService == null) {
                tvStatus.setText("Waiting for media player...");
                tvStatus.setVisibility(View.VISIBLE);
                return;
            }

            int audioSessionId = mediaPlayerService.getAudioSessionId();

            if (audioSessionId == 0) {
                tvStatus.setText("Please play a song first");
                tvStatus.setVisibility(View.VISIBLE);
                return;
            }

            // Initialize Equalizer
            try {
                equalizer = new Equalizer(0, audioSessionId);
                equalizer.setEnabled(isEqEnabled);

                numberOfBands = equalizer.getNumberOfBands();
                bandLevelRange = equalizer.getBandLevelRange();
                currentBandLevels = new float[numberOfBands];

                for (short i = 0; i < numberOfBands; i++) {
                    currentBandLevels[i] = equalizer.getBandLevel(i);
                }

                loadCustomBands();

                if (currentPreset == presetNames.length - 1) {
                    for (short i = 0; i < numberOfBands; i++) {
                        equalizer.setBandLevel(i, (short) currentBandLevels[i]);
                    }
                }

                createEqualizerBands();

            } catch (Exception e) {
                Log.e(TAG, "Equalizer error: " + e.getMessage());
                tvStatus.setText("Equalizer not available");
                tvStatus.setVisibility(View.VISIBLE);
                return;
            }

            // Initialize Bass Boost
            try {
                bassBoost = new BassBoost(0, audioSessionId);
                bassBoost.setEnabled(isEqEnabled);
                bassBoost.setStrength((short) bassBoostLevel);
            } catch (Exception e) {
                Log.e(TAG, "Bass Boost not available");
            }

            // Initialize Virtualizer
            try {
                virtualizer = new Virtualizer(0, audioSessionId);
                virtualizer.setEnabled(isEqEnabled);
                virtualizer.setStrength((short) virtualizerLevel);
            } catch (Exception e) {
                Log.e(TAG, "Virtualizer not available");
            }

            tvStatus.setVisibility(View.GONE);

        } catch (Exception e) {
            Log.e(TAG, "Effects error: " + e.getMessage());
            tvStatus.setText("Error: " + e.getMessage());
            tvStatus.setVisibility(View.VISIBLE);
        }
    }

    private void createEqualizerBands() {
        if (bandsContainer == null || equalizer == null || numberOfBands == 0) {
            return;
        }

        bandsContainer.removeAllViews();
        seekBars.clear();
        valueTexts.clear();

        for (short i = 0; i < numberOfBands; i++) {
            final short bandIndex = i;
            final int minLevel = bandLevelRange[0];
            final int maxLevel = bandLevelRange[1];

            LinearLayout bandRow = new LinearLayout(this);
            bandRow.setOrientation(LinearLayout.HORIZONTAL);
            bandRow.setPadding(8, 8, 8, 8);
            bandRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvFreq = new TextView(this);
            int freq = equalizer.getCenterFreq(i) / 1000;
            String freqText = freq >= 1000 ?
                    String.format(Locale.US, "%.1fk", freq/1000f) :
                    freq + "Hz";
            tvFreq.setText(freqText);
            tvFreq.setTextColor(0xFF00FF00);
            tvFreq.setWidth(80);
            tvFreq.setPadding(4, 0, 4, 0);

            SeekBar seekBar = new SeekBar(this);
            seekBar.setMax(maxLevel - minLevel);
            seekBar.setProgress((int) (currentBandLevels[i] - minLevel));
            seekBar.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            seekBar.setPadding(4, 0, 4, 0);

            TextView tvValue = new TextView(this);
            tvValue.setText(formatLevel((short) currentBandLevels[i]));
            tvValue.setTextColor(0xFFFFFFFF);
            tvValue.setWidth(60);
            tvValue.setPadding(4, 0, 4, 0);
            tvValue.setGravity(android.view.Gravity.END);

            bandRow.addView(tvFreq);
            bandRow.addView(seekBar);
            bandRow.addView(tvValue);

            final TextView valueView = tvValue;
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && equalizer != null) {
                        short level = (short) (minLevel + progress);
                        equalizer.setBandLevel(bandIndex, level);
                        currentBandLevels[bandIndex] = level;
                        valueView.setText(formatLevel(level));

                        if (currentPreset != presetNames.length - 1) {
                            currentPreset = presetNames.length - 1;
                            tvCurrentPreset.setText("CUSTOM");
                            createPresetButtons();
                        }
                        saveCustomBands();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            seekBars.add(seekBar);
            valueTexts.add(tvValue);
            bandsContainer.addView(bandRow);
        }
    }

    private void applyPreset(int presetIndex) {
        if (equalizer == null) return;

        currentPreset = presetIndex;
        short[] values = presetValues[presetIndex];

        for (short i = 0; i < numberOfBands && i < values.length; i++) {
            short level = values[i];
            equalizer.setBandLevel(i, level);
            currentBandLevels[i] = level;

            if (i < seekBars.size()) {
                int minLevel = bandLevelRange[0];
                int progress = level - minLevel;
                seekBars.get(i).setProgress(progress);
                valueTexts.get(i).setText(formatLevel(level));
            }
        }

        tvCurrentPreset.setText(presetNames[presetIndex]);
        createPresetButtons();

        if (presetIndex != presetNames.length - 1) {
            saveSettings();
        }

        saveCustomBands();
    }

    private void saveCustomPreset() {
        if (equalizer == null) return;

        currentPreset = presetNames.length - 1;
        tvCurrentPreset.setText("CUSTOM");
        createPresetButtons();
        saveCustomBands();
        saveSettings();
    }

    private void resetEqualizer() {
        applyPreset(0);

        if (bassBoost != null) {
            bassBoost.setStrength((short) 0);
            seekBass.setProgress(0);
            tvBassValue.setText("0%");
        }

        if (virtualizer != null) {
            virtualizer.setStrength((short) 0);
            seekVirtualizer.setProgress(0);
            tvVirtualizerValue.setText("0%");
        }

        seekReverb.setProgress(0);
        tvReverbValue.setText("0%");
        seekWideness.setProgress(0);
        tvWidenessValue.setText("0%");

        bassBoostLevel = 0;
        virtualizerLevel = 0;
        reverbLevel = 0;
        widenessLevel = 0;

        saveSettings();
    }

    private String formatLevel(short level) {
        float dB = level / 100.0f;
        return String.format(Locale.US, "%.0f dB", dB);
    }

    @Override
    protected void onDestroy() {
        if (equalizer != null) equalizer.release();
        if (bassBoost != null) bassBoost.release();
        if (virtualizer != null) virtualizer.release();
        if (isServiceBound) unbindService(serviceConnection);
        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Save settings one more time when leaving
        saveSettings();
        saveCustomBands();

        // Apply to service if bound
        if (isServiceBound && mediaPlayerService != null) {
            mediaPlayerService.applyEqualizerSettings();
        }
    }
}