package eu.tivian.musico.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.tivian.musico.R;
import eu.tivian.musico.SharedViewModel;
import eu.tivian.musico.database.SimpleStatement;

import static eu.tivian.musico.database.DatabaseContract.*;

/**
 * Fragment containing statistics about the collection database.
 */
public class StatisticsFragment extends Fragment {
    /**
     * Adapter for the {@link RecyclerView}.
     */
    private Adapter adapter;

    /**
     * List of all available statistics.
     */
    private List<Pair<Integer, SimpleStatement>> stats;

    /**
     * The {@link RecyclerView} adapter.
     */
    private class StatsAdapter extends Adapter<StatsAdapter.ViewHolder> {
        /**
         * The view holder for the {@link Adapter}.
         */
        private class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * Title of the statistic.
             */
            TextView title;

            /**
             * Description of the statistic.
             */
            TextView info;

            /**
             * Constructs the view holder.
             *
             * @param itemView the view supplied by the {@link Adapter}.
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);

                title = itemView.findViewById(R.id.tv_stat_title);
                info = itemView.findViewById(R.id.tv_stat_info);
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
                    .inflate(R.layout.item_stat, parent, false));
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
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pair<Integer, SimpleStatement> entry = stats.get(position);
            if (entry != null && entry.first != null && entry.second != null) {
                holder.title.setText(getString(entry.first));
                holder.info.setText(entry.second.get());
            }
        }

        /**
         * Returns the total number of available statistics.
         *
         * @return the total number of available statistics.
         */
        @Override
        public int getItemCount() {
            return stats == null ? 0 : stats.size();
        }
    }

    /**
     * Constructs the fragment and initializes the list of statistics.
     */
    private StatisticsFragment() {
        List<Pair<Integer, SimpleStatement>> list = new ArrayList<>();

        list.add(Pair.create(R.string.stat_album_count, new SimpleStatement(
            "SELECT COUNT(*) FROM " + AlbumEntry.TABLE_NAME)));
        list.add(Pair.create(R.string.stat_artist_count, new SimpleStatement(
            "SELECT COUNT(*) FROM " + ArtistEntry.TABLE_NAME)));
        list.add(Pair.create(R.string.stat_song_count, new SimpleStatement(
            "SELECT COUNT(*) FROM " + SongEntry.TABLE_NAME)));
        list.add(Pair.create(R.string.stat_total_length, new SimpleStatement(
            "SELECT CAST(t / 3600 AS 'INT') || ':' || "
                 + "CAST((t % 3600) / 60 AS 'INT') || ':' || "
                 + "CAST(t % 60 AS 'INT') " +
            "FROM (SELECT (TOTAL(SUBSTR(" + SongEntry.COLUMN_DURATION + ", 0, 3)) * 60 + "
                        + "TOTAL(SUBSTR(" + SongEntry.COLUMN_DURATION + ", 4))) t " +
                  "FROM " + SongEntry.TABLE_NAME + ")")));
        list.add(Pair.create(R.string.stat_store_count, new SimpleStatement(
            "SELECT COUNT(*) FROM " + StoreEntry.TABLE_NAME)));
        list.add(Pair.create(R.string.stat_money_total, new SimpleStatement(
            "SELECT GROUP_CONCAT(tot, '\n') " +
                "FROM (SELECT (TOTAL(" + PurchaseEntry.COLUMN_PRICE + ") || ' ' || " + CurrencyEntry.COLUMN_NAME + ") tot " +
                    "FROM " + PurchaseEntry.TABLE_NAME + " p, " + CurrencyEntry.TABLE_NAME + " c " +
                    "WHERE p." + PurchaseEntry.COLUMN_CURRENCY_ID + " = c." + CurrencyEntry._ID + " " +
                    "GROUP BY " + PurchaseEntry.COLUMN_CURRENCY_ID + ")")));
        list.add(Pair.create(R.string.stat_album_recent, new SimpleStatement(
            "SELECT (r." + ArtistEntry.COLUMN_NAME + " || ' - ' || a." + AlbumEntry.COLUMN_TITLE + " || '\n' || dat) " +
            "FROM (SELECT DATE(p." + PurchaseEntry.COLUMN_DATE + " / 1000, 'UNIXEPOCH') dat, "
                        + "p." + PurchaseEntry.COLUMN_ALBUM_ID + " " +
                    "FROM " + PurchaseEntry.TABLE_NAME + " p " +
                    "ORDER BY dat DESC " +
                    "LIMIT 1), "
                + AlbumEntry.TABLE_NAME + " a, "
                + ArtistEntry.TABLE_NAME + " r " +
            "WHERE a." + AlbumEntry._ID + " = " + PurchaseEntry.COLUMN_ALBUM_ID + " " +
                "AND r." + ArtistEntry._ID + " = a." + AlbumEntry.COLUMN_ARTIST_ID)));
        list.add(Pair.create(R.string.stat_store_total, new SimpleStatement(
            "SELECT (" + StoreEntry.COLUMN_NAME + " || '\n' || tot || ' PLN') " +
            "FROM (SELECT s." + StoreEntry.COLUMN_NAME + ", "
                       + "ROUND(TOTAL(" + PurchaseEntry.COLUMN_PRICE + " / " + CurrencyEntry.COLUMN_RATE + "), 2) tot " +
                    "FROM " + PurchaseEntry.TABLE_NAME + " p, "
                        + CurrencyEntry.TABLE_NAME + " c, "
                        + StoreEntry.TABLE_NAME + " s " +
                    "WHERE c." + CurrencyEntry._ID + " = p." + PurchaseEntry.COLUMN_CURRENCY_ID + " " +
                        "AND s." + StoreEntry._ID + " = p." + PurchaseEntry.COLUMN_STORE_ID + " " +
                    "GROUP BY s." + StoreEntry.COLUMN_NAME + ") " +
            "ORDER BY tot DESC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_avg_price, new SimpleStatement(
            "SELECT (ROUND(AVG(" + PurchaseEntry.COLUMN_PRICE + " / " + CurrencyEntry.COLUMN_RATE + "), 2) || ' PLN') " +
            "FROM " + PurchaseEntry.TABLE_NAME + " p, "
                    + CurrencyEntry.TABLE_NAME + " c " +
            "WHERE c." + CurrencyEntry._ID + " = p." + PurchaseEntry.COLUMN_CURRENCY_ID)));
        list.add(Pair.create(R.string.stat_store_economical, new SimpleStatement(
            "SELECT " + StoreEntry.COLUMN_NAME + " || '\n' || av || ' PLN' " +
            "FROM (SELECT s." + StoreEntry.COLUMN_NAME + ", "
                       + "ROUND(AVG(" + PurchaseEntry.COLUMN_PRICE + " / " + CurrencyEntry.COLUMN_RATE + "), 2) av " +
                    "FROM " + PurchaseEntry.TABLE_NAME + " p, "
                            + CurrencyEntry.TABLE_NAME + " c, "
                            + StoreEntry.TABLE_NAME + " s " +
                    "WHERE c." + CurrencyEntry._ID + " = p." + PurchaseEntry.COLUMN_CURRENCY_ID + " " +
                        "AND s." + StoreEntry._ID + " = p." + PurchaseEntry.COLUMN_STORE_ID + " " +
                    "GROUP BY p." + PurchaseEntry.COLUMN_STORE_ID + " " +
                    "ORDER BY 2) " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_popular_artist, new SimpleStatement(
            "SELECT (" + ArtistEntry.COLUMN_NAME + " || '\n' || COUNT(" + ArtistEntry.COLUMN_NAME + ")) " +
            "FROM " + AlbumEntry.TABLE_NAME + " a, " + ArtistEntry.TABLE_NAME + " r " +
            "WHERE a." + AlbumEntry.COLUMN_ARTIST_ID + " = r." + ArtistEntry._ID + " " +
            "GROUP BY r." + ArtistEntry._ID + " " +
            "ORDER BY COUNT(" + ArtistEntry.COLUMN_NAME + ") DESC, " + ArtistEntry.COLUMN_NAME + " ASC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_popular_genre, new SimpleStatement(
            "SELECT (" + GenreEntry.COLUMN_NAME + " || '\n' || COUNT(" + GenreEntry.COLUMN_NAME + ")) " +
            "FROM " + AlbumEntry.TABLE_NAME + " a, " + GenreEntry.TABLE_NAME + " r " +
            "WHERE a." + AlbumEntry.COLUMN_GENRE_ID + " = r." + GenreEntry._ID + " " +
            "GROUP BY r." + GenreEntry._ID + " " +
            "ORDER BY COUNT(" + GenreEntry.COLUMN_NAME + ") DESC, " + GenreEntry.COLUMN_NAME + " ASC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_one_year_count, new SimpleStatement(
            "SELECT (COUNT(*) || '\n' || y) " +
            "FROM (SELECT STRFTIME('%Y', " + PurchaseEntry.COLUMN_DATE + " / 1000, 'UNIXEPOCH') y " +
                  "FROM " + PurchaseEntry.TABLE_NAME + ") " +
                  "GROUP BY y " +
                  "ORDER BY COUNT(*) DESC " +
                  "LIMIT 1")));
        list.add(Pair.create(R.string.stat_album_newest, new SimpleStatement(
            "SELECT (" + ArtistEntry.COLUMN_NAME + " || ' - ' || "
                       + AlbumEntry.COLUMN_TITLE + " || '\n' || "
                       + AlbumEntry.COLUMN_YEAR  + ") " +
            "FROM " + AlbumEntry.TABLE_NAME + ", " + ArtistEntry.TABLE_NAME + " " +
            "WHERE " + AlbumEntry.COLUMN_ARTIST_ID + " = " + ArtistEntry.TABLE_NAME + "." + ArtistEntry._ID + " " +
            "ORDER BY " + AlbumEntry.COLUMN_YEAR + " DESC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_album_oldest, new SimpleStatement(
            "SELECT (" + ArtistEntry.COLUMN_NAME + " || ' - ' || "
                       + AlbumEntry.COLUMN_TITLE + " || '\n' || "
                       + AlbumEntry.COLUMN_YEAR  + ") " +
            "FROM " + AlbumEntry.TABLE_NAME + ", " + ArtistEntry.TABLE_NAME + " " +
            "WHERE " + AlbumEntry.COLUMN_ARTIST_ID + " = " + ArtistEntry.TABLE_NAME + "." + ArtistEntry._ID + " " +
            "ORDER BY " + AlbumEntry.COLUMN_YEAR + " " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_album_expensive, new SimpleStatement(
            "SELECT (r." + ArtistEntry.COLUMN_NAME + " || ' - ' || "
                  + "a." + AlbumEntry.COLUMN_TITLE + " || '\n' || "
                  + "p." + PurchaseEntry.COLUMN_PRICE + " || ' ' || "
                  + "c." + CurrencyEntry.COLUMN_NAME + ") " +
            "FROM " + AlbumEntry.TABLE_NAME + " a, "
                    + ArtistEntry.TABLE_NAME + " r, "
                    + PurchaseEntry.TABLE_NAME + " p, "
                    + CurrencyEntry.TABLE_NAME + " c " +
            "WHERE a." + AlbumEntry._ID + " = p." + PurchaseEntry.COLUMN_ALBUM_ID + " " +
                "AND r." + ArtistEntry._ID + " = a." + AlbumEntry.COLUMN_ARTIST_ID + " " +
                "AND p." + PurchaseEntry.COLUMN_CURRENCY_ID + " = c." + CurrencyEntry._ID + " " +
            "ORDER BY (p." + PurchaseEntry.COLUMN_PRICE + " / c." + CurrencyEntry.COLUMN_RATE + ") DESC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_album_cheapest, new SimpleStatement(
            "SELECT (r." + ArtistEntry.COLUMN_NAME + " || ' - ' || "
                  + "a." + AlbumEntry.COLUMN_TITLE + " || '\n' || "
                  + "p." + PurchaseEntry.COLUMN_PRICE + " || ' ' || "
                  + "c." + CurrencyEntry.COLUMN_NAME + ") " +
            "FROM " + AlbumEntry.TABLE_NAME + " a, "
                    + ArtistEntry.TABLE_NAME + " r, "
                    + PurchaseEntry.TABLE_NAME + " p, "
                    + CurrencyEntry.TABLE_NAME + " c " +
            "WHERE a." + AlbumEntry._ID + " = p." + PurchaseEntry.COLUMN_ALBUM_ID + " " +
                "AND r." + ArtistEntry._ID + " = a." + AlbumEntry.COLUMN_ARTIST_ID + " " +
                "AND p." + PurchaseEntry.COLUMN_CURRENCY_ID + " = c." + CurrencyEntry._ID + " " +
            "ORDER BY (p." + PurchaseEntry.COLUMN_PRICE + " / c." + CurrencyEntry.COLUMN_RATE + ") ASC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_artist_expensive, new SimpleStatement(
            "SELECT (art || '\n' || tot || ' PLN') " +
            "FROM (SELECT r." + ArtistEntry.COLUMN_NAME + " art, "
                       + "ROUND(TOTAL(p." + PurchaseEntry.COLUMN_PRICE + " / " + CurrencyEntry.COLUMN_RATE + "), 2) tot " +
                "FROM " + AlbumEntry.TABLE_NAME + " a, "
                        + ArtistEntry.TABLE_NAME + " r, "
                        + PurchaseEntry.TABLE_NAME + " p, "
                        + CurrencyEntry.TABLE_NAME + " c " +
                "WHERE a." + AlbumEntry._ID + " = p." + PurchaseEntry.COLUMN_ALBUM_ID + " "
                    + "AND r." + ArtistEntry._ID + " = a." + AlbumEntry.COLUMN_ARTIST_ID + " "
                    + "AND c." + CurrencyEntry._ID + " = p." + PurchaseEntry.COLUMN_CURRENCY_ID + " " +
                "GROUP BY r." + ArtistEntry.COLUMN_NAME + ") " +
            "ORDER BY tot DESC " +
            "LIMIT 1")));
        list.add(Pair.create(R.string.stat_artist_cheapest, new SimpleStatement(
            "SELECT (art || '\n' || tot || ' PLN') " +
            "FROM (SELECT r." + ArtistEntry.COLUMN_NAME + " art, "
                      + "ROUND(TOTAL(p." + PurchaseEntry.COLUMN_PRICE + " / " + CurrencyEntry.COLUMN_RATE + "), 2) tot " +
                "FROM " + AlbumEntry.TABLE_NAME + " a, "
                        + ArtistEntry.TABLE_NAME + " r, "
                        + PurchaseEntry.TABLE_NAME + " p, "
                        + CurrencyEntry.TABLE_NAME + " c " +
                "WHERE a." + AlbumEntry._ID + " = p." + PurchaseEntry.COLUMN_ALBUM_ID + " "
                    + "AND r." + ArtistEntry._ID + " = a." + AlbumEntry.COLUMN_ARTIST_ID + " "
                    + "AND c." + CurrencyEntry._ID + " = p." + PurchaseEntry.COLUMN_CURRENCY_ID + " " +
                "GROUP BY r." + ArtistEntry.COLUMN_NAME + ") " +
            "ORDER BY tot ASC " +
            "LIMIT 1")));

        stats = Collections.unmodifiableList(list);
    }

    /**
     * Creates new instance of this fragment.
     *
     * @return the new instance.
     */
    static StatisticsFragment newInstance() {
        return new StatisticsFragment();
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
     * Initializes the user interface of the statistics list.
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);
        Context context = getActivity();

        RecyclerView recyclerView = view.findViewById(R.id.rv_stats);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new StatsAdapter();
        recyclerView.setAdapter(adapter);

        new ViewModelProvider(requireActivity()).get(SharedViewModel.class)
                .getLanguage().observe(getViewLifecycleOwner(), lang ->
            adapter.notifyDataSetChanged()
        );

        return view;
    }
}
