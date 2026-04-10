package com.example.echo_wave.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.echo_wave.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdManager implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "AdManager";
    private static AdManager instance;
    private Context context;
    private Activity currentActivity;

    // Ad Unit IDs
    private static final String BANNER_AD_UNIT = "ca-app-pub-9223230945598669/7942318824";
    private static final String INTERSTITIAL_AD_UNIT = "ca-app-pub-9223230945598669/7559175449";
    private static final String APP_OPEN_AD_UNIT = "ca-app-pub-9223230945598669/3199024965";
    private static final String NATIVE_AD_UNIT = "ca-app-pub-9223230945598669/5783059344";
    private static final String ADAPTIVE_BANNER_UNIT = "ca-app-pub-9223230945598669/5783059344";

    // Ads instances
    private static InterstitialAd interstitialAd;
    private static AppOpenAd appOpenAd;
    private static RewardedAd rewardedAd;
    private static boolean isLoadingInterstitial = false;
    private static boolean isLoadingAppOpen = false;
    private static boolean isShowingAd = false;

    private AdManager(Context context) {
        this.context = context.getApplicationContext();
        ((Application) context.getApplicationContext()).registerActivityLifecycleCallbacks(this);
    }

    public static synchronized AdManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdManager(context);
        }
        return instance;
    }

    // ==================== BANNER AD ====================
    public static void loadBannerAd(AdView adView) {
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "Banner ad loading");
        }
    }

    // ==================== ADAPTIVE BANNER ====================
    public static void loadAdaptiveBanner(AdView adView, Activity activity) {
        if (adView != null) {
            AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, 320);
            adView.setAdSize(adSize);
            adView.setAdUnitId(ADAPTIVE_BANNER_UNIT);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "Adaptive banner loading");
        }
    }

    // ==================== INTERSTITIAL AD ====================
    public static void loadInterstitialAd(Context context) {
        if (isLoadingInterstitial || interstitialAd != null) {
            return;
        }

        isLoadingInterstitial = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isLoadingInterstitial = false;
                        Log.d(TAG, "Interstitial ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isLoadingInterstitial = false;
                        Log.e(TAG, "Interstitial ad failed: " + loadAdError.getMessage());
                    }
                });
    }

    public static void showInterstitialAd(Activity activity, Runnable onAdClosed) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null;
                    if (onAdClosed != null) onAdClosed.run();
                    loadInterstitialAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    interstitialAd = null;
                    if (onAdClosed != null) onAdClosed.run();
                    loadInterstitialAd(activity);
                }
            });
            interstitialAd.show(activity);
        } else {
            if (onAdClosed != null) onAdClosed.run();
            loadInterstitialAd(activity);
        }
    }

    // ==================== APP OPEN AD ====================
    public void loadAppOpenAd() {
        if (isLoadingAppOpen || appOpenAd != null || isShowingAd) {
            return;
        }

        isLoadingAppOpen = true;
        AdRequest request = new AdRequest.Builder().build();

        AppOpenAd.load(context, APP_OPEN_AD_UNIT, request,
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AppOpenAd ad) {
                        appOpenAd = ad;
                        isLoadingAppOpen = false;
                        Log.d(TAG, "App open ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        appOpenAd = null;
                        isLoadingAppOpen = false;
                        Log.e(TAG, "App open ad failed: " + loadAdError.getMessage());
                    }
                });
    }

    public void showAppOpenAd() {
        if (isShowingAd || appOpenAd == null) {
            loadAppOpenAd();
            return;
        }

        isShowingAd = true;
        appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                appOpenAd = null;
                isShowingAd = false;
                loadAppOpenAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                appOpenAd = null;
                isShowingAd = false;
                loadAppOpenAd();
            }
        });

        appOpenAd.show(currentActivity);
    }

    // ==================== NATIVE AD ====================
    public static void loadNativeAd(Context context, NativeAdCallback callback) {
        AdLoader.Builder builder = new AdLoader.Builder(context, NATIVE_AD_UNIT);

        builder.forNativeAd(nativeAd -> {
            if (callback != null) {
                callback.onAdLoaded(nativeAd);
            }
        });

        builder.withAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Native ad failed: " + loadAdError.getMessage());
                if (callback != null) {
                    callback.onAdFailed();
                }
            }
        });

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .build();
        builder.withNativeAdOptions(adOptions);

        AdLoader adLoader = builder.build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void populateNativeAdView(NativeAd nativeAd, NativeAdView adView) {
        // Get the views
        View headlineView = adView.findViewById(R.id.ad_headline);
        View bodyView = adView.findViewById(R.id.ad_body);
        View callToActionView = adView.findViewById(R.id.ad_call_to_action);
        View iconView = adView.findViewById(R.id.ad_icon);

        // Set the headline
        if (headlineView instanceof TextView) {
            ((TextView) headlineView).setText(nativeAd.getHeadline());
            adView.setHeadlineView(headlineView);
        }

        // Set the body
        if (bodyView instanceof TextView) {
            if (nativeAd.getBody() == null) {
                bodyView.setVisibility(View.INVISIBLE);
            } else {
                bodyView.setVisibility(View.VISIBLE);
                ((TextView) bodyView).setText(nativeAd.getBody());
                adView.setBodyView(bodyView);
            }
        }

        // Set the call to action button
        if (callToActionView instanceof Button) {
            if (nativeAd.getCallToAction() == null) {
                callToActionView.setVisibility(View.INVISIBLE);
            } else {
                callToActionView.setVisibility(View.VISIBLE);
                ((Button) callToActionView).setText(nativeAd.getCallToAction());
                adView.setCallToActionView(callToActionView);
            }
        }

        // Set the icon
        if (iconView instanceof ImageView) {
            if (nativeAd.getIcon() == null) {
                iconView.setVisibility(View.GONE);
            } else {
                ImageView iconImageView = (ImageView) iconView;
                iconImageView.setImageDrawable(nativeAd.getIcon().getDrawable());
                iconView.setVisibility(View.VISIBLE);
                adView.setIconView(iconView);
            }
        }

        // Set the native ad
        adView.setNativeAd(nativeAd);
    }

    // ==================== REWARDED AD (Optional) ====================
    public static void loadRewardedAd(Context context) {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, "ca-app-pub-9223230945598669/xxxxxxxxxx", adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        Log.e(TAG, "Rewarded ad failed: " + loadAdError.getMessage());
                    }
                });
    }

    // ==================== LIFECYCLE CALLBACKS ====================
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        showAppOpenAd();
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}
    @Override
    public void onActivityPaused(@NonNull Activity activity) {}
    @Override
    public void onActivityStopped(@NonNull Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}

    public interface NativeAdCallback {
        void onAdLoaded(NativeAd nativeAd);
        void onAdFailed();
    }
}