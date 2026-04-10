package com.example.echo_wave.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.echo_wave.R;
import com.example.echo_wave.models.Song;
import com.example.echo_wave.services.MediaPlayerService;
import com.example.echo_wave.ui.adapters.QueueAdapter;
import com.example.echo_wave.utils.MediaPlayerHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class QueueActivity extends AppCompatActivity implements QueueAdapter.OnQueueItemClickListener {
    
    private RecyclerView rvQueue;
    private TextView tvEmptyQueue;
    private MaterialButton btnClearQueue;
    private QueueAdapter adapter;
    private List<Song> queueList = new ArrayList<>();
    private Song currentSong;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        
        // Set toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Queue");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        initViews();
        loadQueue();
        
        btnClearQueue.setOnClickListener(v -> clearQueue());
    }
    
    @SuppressLint("WrongViewCast")
    private void initViews() {
        rvQueue = findViewById(R.id.rv_queue);
        tvEmptyQueue = findViewById(R.id.tv_empty_queue);
        btnClearQueue = findViewById(R.id.btn_clear_queue);
        
        rvQueue.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QueueAdapter(queueList, currentSong, this);
        rvQueue.setAdapter(adapter);
    }
    
    private void loadQueue() {
        // Get queue from MediaPlayerHelper
        MediaPlayerHelper helper = MediaPlayerHelper.getInstance();
        queueList.clear();
        
        List<Song> queue = helper.getQueue();
        if (queue != null && !queue.isEmpty()) {
            queueList.addAll(queue);
        }
        
        currentSong = helper.getCurrentSong();
        
        adapter.updateQueue(queueList);
        adapter.updateCurrentSong(currentSong);
        
        updateEmptyState();
    }
    
    private void updateEmptyState() {
        if (queueList.isEmpty()) {
            tvEmptyQueue.setVisibility(View.VISIBLE);
            rvQueue.setVisibility(View.GONE);
            btnClearQueue.setVisibility(View.GONE);
        } else {
            tvEmptyQueue.setVisibility(View.GONE);
            rvQueue.setVisibility(View.VISIBLE);
            btnClearQueue.setVisibility(View.VISIBLE);
        }
    }
    
    private void clearQueue() {
        MediaPlayerHelper.getInstance().clearQueue();
        loadQueue();
        Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onItemClick(Song song, int position) {
        // Play the selected song from queue
        MediaPlayerHelper helper = MediaPlayerHelper.getInstance();
        
        // Create a new playlist starting from this position
        List<Song> playlist = new ArrayList<>();
        for (int i = position; i < queueList.size(); i++) {
            playlist.add(queueList.get(i));
        }
        
        // Also add the original queue items before current? 
        // Usually we play from selected position
        helper.setPlaylist(playlist, 0);
        helper.playSong(this, playlist.get(0));
        
        // Go to NowPlaying activity
        Intent intent = new Intent(this, NowPlayingActivity.class);
        startActivity(intent);
        
        // Refresh queue
        loadQueue();
    }
    
    @Override
    public void onRemoveClick(Song song, int position) {
        // Remove song from queue
        MediaPlayerHelper.getInstance().removeFromQueue(position);
        loadQueue();
        Toast.makeText(this, "Removed from queue", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadQueue();
    }
}