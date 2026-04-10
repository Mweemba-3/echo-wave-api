package com.example.echo_wave.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.echo_wave.ui.fragments.SongsTabFragment;
import com.example.echo_wave.ui.fragments.AlbumsTabFragment;
import com.example.echo_wave.ui.fragments.ArtistsTabFragment;
import com.example.echo_wave.ui.fragments.PlaylistsTabFragment;

public class LibraryPagerAdapter extends FragmentStateAdapter {

    public LibraryPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SongsTabFragment();
            case 1:
                return new AlbumsTabFragment();
            case 2:
                return new ArtistsTabFragment();
            case 3:
                return new PlaylistsTabFragment();
            default:
                return new SongsTabFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Songs, Albums, Artists, Playlists
    }
}