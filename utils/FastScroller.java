package com.example.echo_wave.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class FastScroller extends View {
    
    public FastScroller(Context context) {
        super(context);
    }
    
    public FastScroller(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    public FastScroller(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    public void attachToRecyclerView(RecyclerView recyclerView) {
        // Stub implementation
    }
    
    public void setSectionIndexer(SectionIndexer indexer) {
        // Stub implementation
    }
    
    public void updateSections() {
        // Stub implementation
    }
    
    public interface SectionIndexer {
        String getSectionForPosition(int position);
    }
}