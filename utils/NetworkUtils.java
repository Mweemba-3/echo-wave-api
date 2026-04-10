package com.example.echo_wave.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtils {
    
    public interface NetworkListener {
        void onNetworkConnected();
        void onNetworkDisconnected();
    }
    
    private static NetworkUtils instance;
    private Context context;
    private NetworkListener listener;
    private ConnectivityManager connectivityManager;
    private boolean isOnline = false;
    
    private NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public static synchronized NetworkUtils getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkUtils(context);
        }
        return instance;
    }
    
    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }
    
    public boolean isOnline() {
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }
    
    public String getNetworkType() {
        if (connectivityManager == null) return "unknown";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return "none";
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) return "unknown";
            
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "wifi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "cellular";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ethernet";
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) return "none";
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return "wifi";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "cellular";
            }
        }
        return "unknown";
    }
    
    public void startMonitoring() {
        // For simplicity, just check once
        checkAndNotify();
    }
    
    private void checkAndNotify() {
        boolean currentState = isOnline();
        if (currentState != isOnline) {
            isOnline = currentState;
            if (listener != null) {
                if (isOnline) {
                    listener.onNetworkConnected();
                } else {
                    listener.onNetworkDisconnected();
                }
            }
        }
    }
    
    public void stopMonitoring() {
        listener = null;
    }
}