package com.example.echo_wave.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.echo_wave.R;
import com.example.echo_wave.ui.activities.EqualizerActivity;
import com.example.echo_wave.utils.SettingsManager;
import com.example.echo_wave.utils.SleepTimerManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private static final String WEBSITE_URL = "https://www.google.com";

    // UI Components
    private SwitchMaterial switchGapless, switchCrossfade, switchHeadsetAutoPlay, switchDarkMode, switchNotifications;
    private MaterialCardView cardEqualizer, cardSleepTimer, cardAudioQuality, cardStorage, cardAbout, cardRate, cardShare, cardFeedback;
    private TextView tvVersion, tvCacheSize, tvSleepTimerStatus, tvAudioQuality;

    // Managers
    private SettingsManager settingsManager;
    private SleepTimerManager sleepTimerManager;
    private Handler handler = new Handler(Looper.getMainLooper());
    private AppCompatActivity parentActivity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AppCompatActivity) {
            parentActivity = (AppCompatActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize managers
        settingsManager = SettingsManager.getInstance(requireContext());
        sleepTimerManager = new SleepTimerManager(requireContext(), handler, this::updateSleepTimerStatus);

        // Find views
        findViews(view);

        // Setup listeners
        setupListeners();

        // Load saved settings
        loadSettings();

        // Update UI
        updateCacheSize();
        updateSleepTimerStatus();
        updateAudioQualityText();

        return view;
    }

    private void findViews(View view) {
        // Switches
        switchGapless = view.findViewById(R.id.switch_gapless);
        switchCrossfade = view.findViewById(R.id.switch_crossfade);
        switchHeadsetAutoPlay = view.findViewById(R.id.switch_headset_auto_play);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        switchNotifications = view.findViewById(R.id.switch_notifications);

        // Cards
        cardEqualizer = view.findViewById(R.id.card_equalizer);
        cardSleepTimer = view.findViewById(R.id.card_sleep_timer);
        cardAudioQuality = view.findViewById(R.id.card_audio_quality);
        cardStorage = view.findViewById(R.id.card_storage);
        cardAbout = view.findViewById(R.id.card_about);
        cardRate = view.findViewById(R.id.card_rate);
        cardShare = view.findViewById(R.id.card_share);
        cardFeedback = view.findViewById(R.id.card_feedback);

        // Text views
        tvVersion = view.findViewById(R.id.tv_version);
        tvCacheSize = view.findViewById(R.id.tv_cache_size);
        tvSleepTimerStatus = view.findViewById(R.id.tv_sleep_timer_status);
        tvAudioQuality = view.findViewById(R.id.tv_audio_quality);

        // Set version
        tvVersion.setText("Version " + getVersionName());
    }

    private void setupListeners() {
        // Audio Settings Switches
        switchGapless.setOnCheckedChangeListener((button, isChecked) -> settingsManager.setGapless(isChecked));
        switchCrossfade.setOnCheckedChangeListener((button, isChecked) -> settingsManager.setCrossfade(isChecked));
        switchHeadsetAutoPlay.setOnCheckedChangeListener((button, isChecked) -> settingsManager.setHeadsetAutoPlay(isChecked));
        switchDarkMode.setOnCheckedChangeListener((button, isChecked) -> settingsManager.setDarkMode(isChecked));
        switchNotifications.setOnCheckedChangeListener((button, isChecked) -> settingsManager.setNotificationsEnabled(isChecked));

        // Equalizer
        cardEqualizer.setOnClickListener(v -> {
            if (parentActivity != null) {
                startActivity(new Intent(parentActivity, EqualizerActivity.class));
            }
        });

        // Sleep Timer
        cardSleepTimer.setOnClickListener(v -> showSleepTimerDialog());

        // Audio Quality
        cardAudioQuality.setOnClickListener(v -> showAudioQualityDialog());

        // Storage
        cardStorage.setOnClickListener(v -> showStorageDialog());

        // About
        cardAbout.setOnClickListener(v -> showAboutDialog());

        // Rate App
        cardRate.setOnClickListener(v -> openPlayStore());

        // Share App
        cardShare.setOnClickListener(v -> shareApp());

        // Feedback
        cardFeedback.setOnClickListener(v -> sendFeedback());
    }

    private void loadSettings() {
        switchGapless.setChecked(settingsManager.isGapless());
        switchCrossfade.setChecked(settingsManager.isCrossfade());
        switchHeadsetAutoPlay.setChecked(settingsManager.isHeadsetAutoPlay());
        switchDarkMode.setChecked(settingsManager.isDarkMode());
        switchNotifications.setChecked(settingsManager.isNotificationsEnabled());
    }

    private String getVersionName() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(
                    requireContext().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private void updateCacheSize() {
        long size = getDirSize(requireContext().getCacheDir());
        tvCacheSize.setText(formatFileSize(size));
    }

    private long getDirSize(File dir) {
        long size = 0;
        if (dir != null && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirSize(file);
                    }
                }
            }
        }
        return size;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void updateAudioQualityText() {
        tvAudioQuality.setText(settingsManager.getAudioQualityText());
    }

    private void updateSleepTimerStatus() {
        long timeLeft = sleepTimerManager.getTimeLeft();
        if (timeLeft > 0) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60;
            tvSleepTimerStatus.setText(String.format(Locale.US, "%02d:%02d remaining", minutes, seconds));
            tvSleepTimerStatus.setVisibility(View.VISIBLE);
        } else {
            tvSleepTimerStatus.setVisibility(View.GONE);
        }
    }

    private void showSleepTimerDialog() {
        String[] options = {"5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour", "Cancel Timer"};
        int[] times = {5, 10, 15, 30, 60, 0};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sleep Timer")
                .setItems(options, (dialog, which) -> {
                    if (which == 5) {
                        sleepTimerManager.cancelTimer();
                    } else {
                        sleepTimerManager.startTimer(times[which] * 60 * 1000);
                    }
                    updateSleepTimerStatus();
                })
                .show();
    }

    private void showAudioQualityDialog() {
        String[] options = {"Standard", "High", "Very High", "Lossless"};
        int current = settingsManager.getAudioQuality();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Audio Quality")
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    settingsManager.setAudioQuality(which);
                    updateAudioQualityText();
                    dialog.dismiss();
                })
                .show();
    }

    private void showStorageDialog() {
        long cacheSize = getDirSize(requireContext().getCacheDir());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Storage")
                .setMessage("Cache size: " + formatFileSize(cacheSize) + "\n\nClear cache to free up space?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    deleteDir(requireContext().getCacheDir());
                    updateCacheSize();
                    Toast.makeText(getContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean deleteDir(File dir) {
        if (dir == null) return false;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }
        return dir.delete();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Echo-Wave")
                .setMessage("Version " + getVersionName() + "\n\n" +
                        "A Search Engine powered app")
                .setPositiveButton("OK", null)
                .setNeutralButton("Visit Website", (dialog, which) -> {
                    // Open your website
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(WEBSITE_URL));
                    startActivity(intent);
                })
                .show();
    }

    private void sendFeedback() {
        String[] options = {"Send Email", "Visit Website"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Feedback")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Send email
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:"));
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Echo-Wave Feedback");
                        intent.putExtra(Intent.EXTRA_TEXT,
                                "App Version: " + getVersionName() +
                                        "\n\nDevice: " + Build.MANUFACTURER + " " + Build.MODEL +
                                        "\nAndroid: " + Build.VERSION.RELEASE +
                                        "\n\nFeedback:\n");

                        try {
                            startActivity(Intent.createChooser(intent, "Send Feedback"));
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "No email app found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Visit website
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(WEBSITE_URL + "/feedback"));
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void openPlayStore() {
        try {
            String packageName = requireContext().getPackageName();
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            String packageName = requireContext().getPackageName();
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void shareApp() {
        String packageName = requireContext().getPackageName();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Echo-Wave Music Player");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "🎵 Check out Echo-Wave!\n\n" +
                        "Download now: https://play.google.com/store/apps/details?id=" + packageName + "\n\n" +
                        "Visit us: " + WEBSITE_URL);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (sleepTimerManager != null) {
            sleepTimerManager.onDestroy();
        }
    }
}