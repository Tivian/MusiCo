package eu.tivian.musico.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import eu.tivian.musico.R;
import eu.tivian.musico.SharedViewModel;
import eu.tivian.musico.net.LastFm;
import eu.tivian.musico.net.Spotify;

/**
 * Fragment used to display recent tracks or top artists of given Last.fm user.
 */
public class LastFmStatsFragment extends Fragment {
    /**
     * The key for the saved label for this fragment.
     */
    private static final String ARG_LABEL_ID = "label";

    /**
     * The current context.
     */
    private Context context;

    /**
     * The string ID of this fragment label.
     */
    private int labelId;

    /**
     * The view representing this fragment.
     */
    private View view;

    /**
     * This fragment label.
     */
    private TextView textView;

    /**
     * Adapter for the {@link RecyclerView}.
     */
    private Adapter adapter;

    /**
     * List of the objects for the adapter.
     */
    private List list;

    /**
     * The {@link RecyclerView} adapter.
     */
    private class StatListAdapter extends Adapter<StatListAdapter.ViewHolder> {
        /**
         * The view holder for the {@link Adapter}.
         */
        private class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * Image associated with this item.
             */
            ImageView image;

            /**
             * Title for this item.
             */
            TextView title;

            /**
             * Subtitle for this item.
             */
            TextView subtitle;

            /**
             * Constructs the view holder.
             *
             * @param itemView the view supplied by the {@link Adapter}.
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);

                image = itemView.findViewById(R.id.iv_stat);
                title = itemView.findViewById(R.id.tv_stat_name);
                subtitle = itemView.findViewById(R.id.tv_stat_amount);
            }
        }

        /**
         * Called when {@link RecyclerView} needs a new {@link ViewHolder} of
         *  the given type to represent an item.
         *
         * @param parent the {@link ViewGroup} into which the new {@link View} will be added
         *        after it is bound to an adapter position.
         * @param viewType the view type of the new {@link View}.
         * @return a new {@link ViewHolder} that holds a {@link View} of the given view type.
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_last_fm_stat, parent, false));
        }

        /**
         * Called by {@link RecyclerView} to display the data at the specified position.
         * This method updates the contents of the {@link ViewHolder#itemView}
         *  to reflect the item at the given position.
         *
         * @param holder the {@link ViewHolder} which should be updated to represent
         *        the contents of the item at the given position in the data set.
         * @param position the position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            Object obj = list.get(position);
            if (obj instanceof LastFm.Artist) {
                LastFm.Artist artist = (LastFm.Artist) obj;
                holder.title.setText(artist.name);
                holder.subtitle.setText(getString(R.string.artist_played, artist.playCount));

                Glide.with(context)
                    .load(artist.icon.values().iterator().next())
                    .placeholder(R.drawable.last_fm_icon)
                    .into(holder.image);
            } else if (obj instanceof LastFm.Track) {
                LastFm.Track track = (LastFm.Track) obj;
                holder.title.setText(track.title);

                if (track.date != null) {
                    holder.subtitle.setText(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                    .format(track.date));
                } else if (track.nowPlaying) {
                    holder.subtitle.setText(R.string.now_playing);
                }

                Glide.with(context)
                    .load(track.icon.values().iterator().next())
                    .placeholder(R.drawable.last_fm_icon)
                    .into(holder.image);
            }
        }

        /**
         * Returns the total number of entries in the {@link #list}.
         *
         * @return the total number of entries in the {@link #list}.
         */
        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }
    }

    /**
     * Private constructor to prevent instantiating from outside.
     */
    private LastFmStatsFragment() {}

    /**
     * Creates the new instance of this fragment.
     *
     * @param id the string ID of the label for the fragment.
     * @return the new instance.
     */
    static LastFmStatsFragment newInstance(@StringRes int id) {
        LastFmStatsFragment fragment = new LastFmStatsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_LABEL_ID, id);
        fragment.setArguments(args);
        return fragment;
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
        if (getArguments() != null)
            labelId = getArguments().getInt(ARG_LABEL_ID);
    }

    /**
     * Initializes the user interface of the Last.fm statistics fragment.
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
        view = inflater.inflate(R.layout.fragment_last_fm_stats, container, false);
        context = getActivity();

        view.setVisibility(View.GONE);

        textView = view.findViewById(R.id.tv_lastfm_stats_label);
        RecyclerView recyclerView = view.findViewById(R.id.rv_lastfm_stats);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new StatListAdapter();
        recyclerView.setAdapter(adapter);

        textView.setText(getString(labelId));
        textView.setSelected(true);

        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getStats().observe(requireActivity(), map -> {
            String key = String.valueOf(labelId);
            if (map.containsKey(key)) {
                list = (List) map.get(key);

                // kind of dirty fix, because LastFm API doesn't return proper artist arts
                if (list != null && list.get(0) instanceof LastFm.Artist) {
                    final int size = list.size();
                    final AtomicInteger count = new AtomicInteger(0);
                    for (Object obj : list) {
                        final LastFm.Artist artist = (LastFm.Artist) obj;
                        if (artist.icon == null || artist.icon instanceof TreeMap) {
                            Spotify.get().getArtistImage(artist.name, url -> {
                                artist.icon = new HashMap<>();
                                artist.icon.put(null, url);

                                if (count.addAndGet(1) == size)
                                    adapter.notifyDataSetChanged();
                            });
                        }
                    }
                } else {
                    adapter.notifyDataSetChanged();
                }

                if (view.getVisibility() == View.GONE) {
                    view.setTranslationY(view.getHeight());
                    view.setAlpha(0);
                    view.setVisibility(View.VISIBLE);
                    view.animate().alpha(1).translationY(0).setDuration(2000);
                }
            }
        });
        viewModel.getLanguage().observe(requireActivity(), lang -> {
            textView.setText(getString(labelId));
            adapter.notifyDataSetChanged();
        });

        return view;
    }
}
