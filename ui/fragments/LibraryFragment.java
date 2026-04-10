package com.example.echo_wave.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.echo_wave.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    private static final String TAG = "LibraryFragment";
    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView started");

        try {
            View view = inflater.inflate(R.layout.fragment_library, container, false);
            Log.d(TAG, "Layout inflated");

            // Find views
            viewPager = view.findViewById(R.id.view_pager);
            tabLayout = view.findViewById(R.id.tab_layout);

            Log.d(TAG, "viewPager: " + (viewPager != null));
            Log.d(TAG, "tabLayout: " + (tabLayout != null));

            if (viewPager == null || tabLayout == null) {
                Log.e(TAG, "Views not found!");
                return view;
            }

            // Setup ViewPager
            setupViewPager();

            return view;

        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            return null;
        }
    }

    private void setupViewPager() {
        // Create adapter
        LibraryPagerAdapter adapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Songs");
                    tab.setIcon(R.drawable.ic_music);
                    break;
                case 1:
                    tab.setText("Albums");
                    tab.setIcon(R.drawable.ic_album);
                    break;
                case 2:
                    tab.setText("Artists");
                    tab.setIcon(R.drawable.ic_artist);
                    break;
                case 3:
                    tab.setText("Playlists");
                    tab.setIcon(R.drawable.ic_playlist);
                    break;
            }
        }).attach();

        Log.d(TAG, "ViewPager setup complete");
    }

    private static class LibraryPagerAdapter extends FragmentStateAdapter {

        public LibraryPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new LibrarySongsFragment();
                case 1:
                    return new LibraryAlbumsFragment();
                case 2:
                    return new LibraryArtistsFragment();
                case 3:
                    return new LibraryPlaylistsFragment();
                default:
                    return new LibrarySongsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}