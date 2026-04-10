package com.example.echo_wave;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.example.echo_wave.utils.AdManager;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class EchoWaveApplication extends Application {

    public static final String CHANNEL_ID = "echo_wave_playback_channel";
    public static final String CHANNEL_NAME = "Music Playback";
    private static final String TAG = "EchoWaveApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializeAdMob();

        // Initialize Ad Manager (loads app open ad)
        AdManager.getInstance(this);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls music playback");
            channel.setSound(null, null);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initializeAdMob() {
        try {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    Log.d(TAG, "AdMob initialized successfully");

                    // Pre-load interstitial ad
                    AdManager.loadInterstitialAd(EchoWaveApplication.this);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "AdMob initialization failed: " + e.getMessage());
        }
    }
}