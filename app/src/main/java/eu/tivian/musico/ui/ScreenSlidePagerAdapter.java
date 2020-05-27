package eu.tivian.musico.ui;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import eu.tivian.musico.R;

/**
 * Adapter used by {@link ViewPager2} to display all 3 fragments.
 */
public class ScreenSlidePagerAdapter extends FragmentStateAdapter {
    /**
     * Array of tab titles.
     */
    @StringRes
    public static final int[] TAB_TITLE = { R.string.tab_albums, R.string.tab_lastfm, R.string.tab_stats };

    /**
     * Creates the adapter.
     *
     * @param fragmentActivity of {@link ViewPager2}'s host
     * @param lifecycle of {@link ViewPager2}'s host
     */
    public ScreenSlidePagerAdapter(FragmentManager fragmentActivity, Lifecycle lifecycle) {
        super(fragmentActivity, lifecycle);
    }

    /**
     * Provide a new {@link Fragment} associated with the specified position.
     *
     * @param position position of the {@link Fragment} in the {@link ViewPager2}.
     * @return a new {@link Fragment} associated with the specified position.
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            default:
            case 0:
                return AlbumsFragment.newInstance();
            case 1:
                return LastFmFragment.newInstance();
            case 2:
                return StatisticsFragment.newInstance();
        }
    }

    /**
     * Returns the total number of tabs.
     *
     * @return The total number of tabs. In case of this app it's always 3.
     */
    @Override
    public int getItemCount() {
        return TAB_TITLE.length;
    }

    
}
