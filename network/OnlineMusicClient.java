package com.example.echo_wave.network;

import android.content.Context;
import android.util.Log;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.example.echo_wave.models.OnlineSong;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineMusicClient {
    private static final String TAG = "OnlineMusicClient";
    private static OnlineMusicClient instance;
    private Context context;
    private PyObject musicFetcher;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean pythonInitialized = false;
    private Gson gson = new Gson();

    private OnlineMusicClient(Context context) {
        this.context = context.getApplicationContext();
        initPython();
    }

    public static synchronized OnlineMusicClient getInstance(Context context) {
        if (instance == null) {
            instance = new OnlineMusicClient(context);
        }
        return instance;
    }

    private void initPython() {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(context));
            }

            Python py = Python.getInstance();
            PyObject module = py.getModule("music_fetcher");
            musicFetcher = module.callAttr("MusicFetcher", context);
            pythonInitialized = true;
            Log.d(TAG, "✅ Python initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Python init failed: " + e.getMessage(), e);
            pythonInitialized = false;
        }
    }

    // ---------------------------
    // SAFE JSON HELPERS
    // ---------------------------
    private String safeString(JsonObject obj, String key, String fallback) {
        try {
            JsonElement el = obj.get(key);
            if (el != null && !el.isJsonNull()) return el.getAsString();
        } catch (Exception ignored) {}
        return fallback;
    }

    private int safeInt(JsonObject obj, String key, int fallback) {
        try {
            JsonElement el = obj.get(key);
            if (el != null && !el.isJsonNull()) return el.getAsInt();
        } catch (Exception ignored) {}
        return fallback;
    }

    // ---------------------------
    // SEARCH
    // ---------------------------
    public void searchMusic(String query, int limit, SearchCallback callback) {
        if (!pythonInitialized) {
            Log.e(TAG, "Python not initialized");
            if (callback != null) callback.onError("Python not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "🔍 Searching for: " + query);
                PyObject resultJson = musicFetcher.callAttr("search_youtube", query, limit);

                if (resultJson == null) {
                    postError(callback, "Search returned null");
                    return;
                }

                String jsonString = resultJson.toString();
                Log.d(TAG, "Search JSON length: " + jsonString.length());

                JsonArray jsonArray = gson.fromJson(jsonString, JsonArray.class);
                List<OnlineSong> songs = new ArrayList<>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    try {
                        JsonObject obj = jsonArray.get(i).getAsJsonObject();

                        OnlineSong song = new OnlineSong();
                        song.setId(safeString(obj, "video_id", "id_" + i));
                        song.setTitle(safeString(obj, "title", "Unknown Title"));
                        song.setArtist(safeString(obj, "artist", "Unknown Artist"));
                        song.setDuration(safeString(obj, "duration", "00:00"));
                        song.setDurationSeconds(safeInt(obj, "duration_seconds", 0));
                        song.setUrl(safeString(obj, "url", ""));
                        song.setThumbnail(safeString(obj, "thumbnail", ""));

                        if (!song.getUrl().isEmpty()) {
                            songs.add(song);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Skipping malformed song entry: " + e.getMessage());
                    }
                }

                Log.d(TAG, "✅ Found " + songs.size() + " songs");
                postSuccess(callback, songs);

            } catch (Exception e) {
                Log.e(TAG, "❌ Search error: " + e.getMessage(), e);
                postError(callback, e.getMessage());
            }
        });
    }

    // ---------------------------
    // STREAM (real implementation)
    // ---------------------------
    public void streamSong(OnlineSong song, StreamCallback callback) {
        if (!pythonInitialized) {
            if (callback != null) callback.onError("Python not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "🎵 Getting stream URL for: " + song.getTitle());
                PyObject resultJson = musicFetcher.callAttr("get_stream_url", song.getUrl());

                if (resultJson == null || resultJson.toString().equals("null")) {
                    postStreamError(callback, "Stream extraction returned null");
                    return;
                }

                String jsonString = resultJson.toString();
                Log.d(TAG, "Stream result: " + jsonString);

                JsonObject obj = gson.fromJson(jsonString, JsonObject.class);

                // Check for error field
                if (obj.has("error") && !obj.get("error").isJsonNull()) {
                    String error = obj.get("error").getAsString();
                    postStreamError(callback, error);
                    return;
                }

                String streamUrl = safeString(obj, "stream_url", "");
                if (streamUrl.isEmpty()) {
                    postStreamError(callback, "No stream URL in response");
                    return;
                }

                Log.d(TAG, "✅ Got stream URL");

                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(streamUrl);
                });

            } catch (Exception e) {
                Log.e(TAG, "❌ Stream error: " + e.getMessage(), e);
                postStreamError(callback, e.getMessage());
            }
        });
    }

    // ---------------------------
    // DOWNLOAD
    // ---------------------------
    public void downloadSong(OnlineSong song, DownloadCallback callback) {
        if (!pythonInitialized) {
            if (callback != null) callback.onError("Python not initialized");
            return;
        }

        executor.execute(() -> {
            try {
                PyObject resultJson = musicFetcher.callAttr("download_audio", song.getUrl());
                if (resultJson == null) {
                    postDownloadError(callback, "Python returned null");
                    return;
                }

                JsonObject obj = gson.fromJson(resultJson.toString(), JsonObject.class);

                // CRITICAL: Check for error first
                if (obj.has("error") && !obj.get("error").isJsonNull()) {
                    String errorMsg = obj.get("error").getAsString();
                    Log.e(TAG, "Python Download Error: " + errorMsg);
                    postDownloadError(callback, errorMsg);
                    return;
                }

                String filepath = safeString(obj, "filepath", "");
                if (filepath.isEmpty()) {
                    postDownloadError(callback, "Filepath empty in JSON");
                    return;
                }

                // Success logic remains the same...
                postDownloadSuccess(callback, obj, filepath, song);

            } catch (Exception e) {
                postDownloadError(callback, e.getMessage());
            }
        });
    }

    // Helper to keep the main method clean
    private void postDownloadSuccess(DownloadCallback callback, JsonObject obj, String path, OnlineSong original) {
        new android.os.Handler(context.getMainLooper()).post(() -> {
            if (callback != null) callback.onSuccess(original, path);
        });
    }

    // ---------------------------
    // POST HELPERS
    // ---------------------------
    private void postSuccess(SearchCallback callback, List<OnlineSong> songs) {
        android.os.Handler h = new android.os.Handler(context.getMainLooper());
        h.post(() -> { if (callback != null) callback.onSuccess(songs); });
    }

    private void postError(SearchCallback callback, String error) {
        android.os.Handler h = new android.os.Handler(context.getMainLooper());
        h.post(() -> { if (callback != null) callback.onError(error); });
    }

    private void postDownloadError(DownloadCallback callback, String error) {
        android.os.Handler h = new android.os.Handler(context.getMainLooper());
        h.post(() -> { if (callback != null) callback.onError(error); });
    }

    private void postStreamError(StreamCallback callback, String error) {
        android.os.Handler h = new android.os.Handler(context.getMainLooper());
        h.post(() -> { if (callback != null) callback.onError(error); });
    }

    // ---------------------------
    // INTERFACES
    // ---------------------------
    public interface SearchCallback {
        void onSuccess(List<OnlineSong> songs);
        void onError(String error);
    }

    public interface DownloadCallback {
        void onSuccess(OnlineSong song, String filePath);
        void onProgress(int progress);
        void onError(String error);
    }

    public interface StreamCallback {
        void onSuccess(String streamUrl);
        void onError(String error);
    }
}