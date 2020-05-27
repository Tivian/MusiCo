package eu.tivian.musico.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Locale;

import eu.tivian.musico.R;
import eu.tivian.musico.SharedViewModel;
import eu.tivian.musico.net.LastFm;
import eu.tivian.musico.utility.PeriodicTask;
import eu.tivian.musico.utility.Utilities;

/**
 * Fragment displaying information about the Last.fm user.
 */
public class LastFmFragment extends Fragment {
    /**
     * The number of top artist that will be displayed in this fragment.
     */
    private static final int TOP_ARTIST_COUNT = 20;

    /**
     * The string IDs of the titles for sub-fragments.
     */
    @IdRes
    private static final int[] FRAGMENTS = {
        R.string.label_top_artists, R.string.label_recent_tracks
    };

    /**
     * The view model which provides cross-activity access to data.
     */
    private SharedViewModel viewModel;

    /**
     * Encapsulation of Last.fm user.
     */
    private LastFm.User user;

    /**
     * The periodic task which updates info about the user every minute.
     */
    private PeriodicTask updater;

    /**
     * The avatar of the user.
     */
    private ImageView avatar;

    /**
     * The username of the given Last.fm user.
     */
    private TextView userName;

    /**
     * The real name of the Last.fm user, if provided.
     */
    private TextView realName;

    /**
     * Creation date of the Last.fm account.
     */
    private TextView userSince;

    /**
     * The number of songs played by the given user.
     */
    private TextView userPlayed;

    /**
     * Sets the UI elements according to the data in {@link #user} object.
     */
    private void setUserInfo() {
        if (user == null)
            return;

        userName.setText(user.name);
        realName.setText(user.realName);
        userSince.setText(getString(R.string.user_since,
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(user.since)));
        userPlayed.setText(getString(R.string.user_played, user.playCount));
        userPlayed.setSelected(true);

        if (user.icon.size() > 0) {
            String avatarUrl = user.icon.get("large");
            if (avatarUrl == null)
                avatarUrl = user.icon.values().iterator().next();

            Object tag = avatar.getTag();
            if (tag == null || !tag.equals(avatarUrl)) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.last_fm_icon)
                    .into(avatar);

                avatar.setTag(avatarUrl);
            }
        }
    }

    /**
     * Sets the UI according to the data in given {@link eu.tivian.musico.net.LastFm.User} object.
     * @param user the Last.fm user.
     */
    private void setUserInfo(LastFm.User user) {
        this.user = user;
        setUserInfo();
    }

    /**
     * Opens the external URL for given Last.fm user.
     */
    private void openUserSite() {
        if (user != null && user.url != null) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(user.url)));
        }
    }

    /**
     * Private constructor to prevent instantiating from outside.
     */
    private LastFmFragment() { }

    /**
     * Creates the new instance of this fragment.
     *
     * @return the new instance.
     */
    static LastFmFragment newInstance() {
        return new LastFmFragment();
    }

    /**
     * Called to do initial creation of a fragment.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *        a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Initializes the user interface of the Last.fm user information fragment.
     *
     * @param inflater the {@link LayoutInflater} object that can be used to inflate
     *        any views in the fragment,
     * @param container if non-{@code null}, this is the parent view that the fragment's
     *        UI should be attached to. The fragment should not add the view itself,
     *        but this can be used to generate the {@link LayoutParams} of the view.
     * @param savedInstanceState if non-{@code null}, this fragment is being re-constructed
     *        from a previous saved state as given here.
     *
     * @return return the {@link View} for the fragment's UI, or {@code null}.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_last_fm, container, false);

        userName = view.findViewById(R.id.tv_user_name);
        realName = view.findViewById(R.id.tv_user_real_name);
        userSince = view.findViewById(R.id.tv_user_since);
        userPlayed = view.findViewById(R.id.tv_user_played);
        avatar = view.findViewById(R.id.iv_user_icon);

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getUsername().observe(getViewLifecycleOwner(), login -> {
            if (login != null) {
                LastFm.get().getUser(login, this::setUserInfo);
                update(login);
            }
        });

        updater = new PeriodicTask(this::update, 60000);
        updater.start();

        avatar.setOnClickListener(v -> openUserSite());
        view.findViewById(R.id.ib_settings).setOnClickListener(v ->
            new SettingsDialogFragment().show(getParentFragmentManager(), null)
        );
        ImageButton btn = view.findViewById(R.id.ib_refresh);
        btn.setOnClickListener(v -> {
            long ms = Utilities.getTime();

            if (btn.getTag() == null || ms - (long) btn.getTag() > 10000) {
                update();
                Toast.makeText(getContext(), R.string.msg_refresh, Toast.LENGTH_SHORT).show();
                btn.setTag(ms);
            }
        });

        FragmentManager fragmentManager = getChildFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            for (int id : FRAGMENTS)
                fragmentTransaction.add(R.id.container_last_fm, LastFmStatsFragment.newInstance(id));
            fragmentTransaction.commit();
        }

        return view;
    }

    /**
     * When this fragment is destroyed the {@link #updater} is stopped.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updater.stop();
    }

    /**
     * Updates the top artists and recent tracks lists for current {@link #user}.
     */
    private void update() {
        if (user != null)
            update(user.name);
    }

    /**
     * Updates the top artists and recent tracks lists for given Last.fm user.
     *
     * @param username the Last.fm username.
     */
    private void update(String username) {
        final LastFm lastFm = LastFm.get();
        lastFm.getTopArtists(username, TOP_ARTIST_COUNT, artists ->
                viewModel.setStat(String.valueOf(FRAGMENTS[0]), artists.second));
        lastFm.getRecentTracks(username, tracks ->
                viewModel.setStat(String.valueOf(FRAGMENTS[1]), tracks.second));
    }
}
