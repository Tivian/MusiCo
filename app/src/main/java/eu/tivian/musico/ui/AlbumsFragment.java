package eu.tivian.musico.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.bumptech.glide.Glide;
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.Currency;
import java.util.Locale;

import eu.tivian.musico.AlbumView;
import eu.tivian.musico.R;
import eu.tivian.musico.SharedViewModel;
import eu.tivian.musico.data.Album;
import eu.tivian.musico.database.AlbumCursor;
import eu.tivian.musico.database.DatabaseAdapter;
import eu.tivian.musico.utility.ItemClickListener;
import eu.tivian.musico.utility.SuccessListener;
import eu.tivian.musico.utility.Utilities;

/**
 * Displays the albums list.
 */
public class AlbumsFragment extends Fragment {
    /**
     * The current context.
     */
    private Context context;

    /**
     * Adapter for the {@link RecyclerView}.
     */
    private Adapter adapter;

    /**
     * Cursor containing the albums list.
     */
    private AlbumCursor cursor;

    /**
     * Adapter used to communicate with the collection database.
     */
    private DatabaseAdapter databaseAdapter;

    /**
     * The current target of context menu.
     */
    private AlbumListAdapter.ViewHolder contextTarget;

    /**
     * The view model containing data shared between fragments and activities.
     */
    private SharedViewModel viewModel;

    /**
     * The {@link RecyclerView} adapter.
     */
    private class AlbumListAdapter extends Adapter<AlbumListAdapter.ViewHolder>
            implements FastScrollRecyclerView.SectionedAdapter {
        /**
         * Handler which processes the clicks on {@link ViewHolder}s.
         */
        private ItemClickListener<ViewHolder> itemClickListener;

        /**
         * The view holder for the {@link Adapter}.
         */
        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            /**
             * The album artwork.
             */
            ImageView cover;

            /**
             * Layout of the whole album item. Used to change its background color.
             */
            View layout;

            /**
             * The name of the artist.
             */
            TextView artist;

            /**
             * The title of the album.
             */
            TextView title;

            /**
             * The date of purchase of the album.
             */
            TextView date;

            /**
             * The price of the album.
             */
            TextView price;

            /**
             * The currency of purchase.
             */
            TextView currency;

            /**
             * Reference to the actual album.
             */
            Album album;

            /**
             * Constructs the view holder.
             *
             * @param itemView the view supplied by the {@link Adapter}.
             */
            ViewHolder(@NonNull View itemView) {
                super(itemView);

                cover = itemView.findViewById(R.id.siv_album);
                layout = itemView.findViewById(R.id.layout_album_item);
                artist = itemView.findViewById(R.id.et_album_title);
                title = itemView.findViewById(R.id.et_album_artist);
                date = itemView.findViewById(R.id.et_album_date);
                price = itemView.findViewById(R.id.et_album_price);
                currency = itemView.findViewById(R.id.et_album_currency);

                itemView.setOnClickListener(this);
                AlbumsFragment.this.registerForContextMenu(itemView);
            }

            /**
             * Sets the {@link ViewHolder} UI according to the supplied {@code album}.
             *
             * @param album the album.
             */
            void setAlbum(Album album) {
                this.album = album;

                artist.setText(album.artist);
                title.setText(album.title);
                date.setText(Utilities.toString(album.purchase.date));
                price.setText(String.format(Locale.getDefault(), "%.2f", album.purchase.price));
                currency.setText(Currency.getInstance(album.purchase.currency).getSymbol());

                itemView.setTag(this);
                itemView.setContentDescription(album.artist + " - " + album.title);

                Glide.with(context)
                    .load(album.cover)
                    .placeholder(R.drawable.album)
                    .listener((SuccessListener<Drawable>) (res) -> {
                        int background = Utilities.getCenterPixel(((BitmapDrawable) res).getBitmap());
                        int foreground = Utilities.getContrastColor(background);
                        cover.setBackgroundResource(0);
                        setColor(background, foreground);
                    })
                    .into(cover);
            }

            /**
             * Sets the UI background color and the font color of the text fields.
             *
             * @param background the color of the background.
             * @param foreground the color of the font.
             */
            void setColor(int background, int foreground) {
                layout.setBackgroundColor(background);
                artist.setTextColor(foreground);
                title.setTextColor(foreground);
                date.setTextColor(foreground);
                price.setTextColor(foreground);
                currency.setTextColor(foreground);
            }

            /**
             * Clears the {@link ViewHolder} to its default state.
             */
            void clear() {
                cover.setImageDrawable(null);
                cover.setBackgroundResource(R.drawable.album);
                setColor(Color.WHITE, Color.BLACK);
            }

            /**
             * Called when a {@link ViewHolder} has been clicked.
             *
             * @param v the view that was clicked.
             */
            @Override
            public void onClick(View v) {
                if (itemClickListener != null)
                    itemClickListener.onItemClick(v, this);
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
        public AlbumListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_album, parent, false));
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
            if (cursor.moveToPosition(position)) {
                holder.clear();
                holder.setAlbum(cursor.getAlbum());
            }
        }

        /**
         * Returns the total number of albums in the {@link #cursor}.
         *
         * @return the total number of albums in the {@link #cursor}.
         */
        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        /**
         * Returns the appropriate scroll bar label for given position.
         *
         * @param position the position of the item.
         * @return the scroll bar label.
         */
        @NonNull
        @Override
        public String getSectionName(int position) {
            return cursor.moveToPosition(position) ? cursor.getLabel() : "";
        }

        /**
         * Register a callback to be invoked when this view is clicked.
         *
         * @param l the callback that will run.
         */
        public void setItemClickListener(ItemClickListener<ViewHolder> l) {
            this.itemClickListener = l;
        }
    }

    /**
     * Private constructor to prevent instantiating from outside.
     */
    private AlbumsFragment() {}

    /**
     * Creates the new instance of this fragment.
     *
     * @return the new instance.
     */
    static AlbumsFragment newInstance() {
        return new AlbumsFragment();
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
     * Initializes the user interface of the albums list.
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
        View view = inflater.inflate(R.layout.fragment_albums, container, false);
        context = getActivity();

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.getCursor().observe(getViewLifecycleOwner(), cursor -> {
            if (cursor != null) {
                this.cursor = cursor;
                adapter.notifyDataSetChanged();
            }
        });
        viewModel.getLanguage().observe(getViewLifecycleOwner(), lang ->
            adapter.notifyDataSetChanged()
        );

        databaseAdapter = DatabaseAdapter.get();
        viewModel.setCursor(databaseAdapter.getCursor());

        RecyclerView recyclerView = view.findViewById(R.id.rv_albums);
        // should be replaced by more sophisticated way of deciding how many columns are displayed
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(context,
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 5 : 3);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new AlbumListAdapter();
        recyclerView.setAdapter(adapter);

        SharedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        ((FastScrollRecyclerView) recyclerView).setOnFastScrollStateChangeListener(new OnFastScrollStateChangeListener() {
            @Override
            public void onFastScrollStart() { viewModel.setFabAlpha(0); }

            @Override
            public void onFastScrollStop()  { viewModel.setFabAlpha(1); }
        });

        ((AlbumListAdapter) adapter).setItemClickListener((v, item) -> {
            cursor.loadTracks(item.album);
            startActivityForResult(new Intent(context, AlbumView.class)
                    .putExtra(AlbumView.ALBUM_KEY, item.album)
                    .putExtra(AlbumView.MODE_KEY, AlbumView.MODE_VIEW), AlbumView.REQUEST_CODE);
        });

        return view;
    }

    /**
     * Creates context menu used to choose between edition and deletion of the album.
     *
     * @param menu the context menu that is being built.
     * @param v the view for which the context menu is being built.
     * @param menuInfo always {@code null}.
     */
    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
                                    @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextTarget = (AlbumListAdapter.ViewHolder) v.getTag();

        new MenuInflater(context).inflate(R.menu.context_menu_album, menu);
        menu.setHeaderTitle(contextTarget.itemView.getContentDescription());
    }

    /**
     * Depending on option chosen in the context menu, this function either
     *  opens the deletion dialog or opens the album editor.
     *
     * @param item the context menu item that was selected. This value cannot be null.
     * @return return false to allow normal context menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit:
                cursor.loadTracks(contextTarget.album);
                startActivityForResult(new Intent(context, AlbumView.class)
                        .putExtra(AlbumView.ALBUM_KEY, contextTarget.album)
                        .putExtra(AlbumView.MODE_KEY, AlbumView.MODE_EDIT), AlbumView.REQUEST_CODE);
                return true;
            case R.id.menu_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.dialog_deletion_msg,
                        contextTarget.itemView.getContentDescription()));
                builder.setPositiveButton(R.string.dialog_deletion_ok, (dialog, which) -> {
                    boolean result = databaseAdapter.delete(contextTarget.album);
                    viewModel.setCursor(databaseAdapter.getCursor(true));
                    Toast.makeText(getActivity(), result ? R.string.msg_album_delete
                            : R.string.error_generic, Toast.LENGTH_LONG).show();
                });
                builder.setNegativeButton(R.string.dialog_cancel, null);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * If the editing of the album was successful then this function
     *  handles saving changes into the database.
     *
     * @param requestCode the integer request code originally supplied to
     *        {@link #startActivityForResult(Intent, int)} allowing to identify the caller.
     * @param resultCode the integer result code returned by the child activity
     *        through its {@link Activity#setResult(int)}.
     * @param data an {@link Intent}, which can return result data to the caller.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data != null && requestCode == AlbumView.REQUEST_CODE) {
            Album album = data.getParcelableExtra(AlbumView.ALBUM_KEY);
            if (album != null && data.getIntExtra(AlbumView.MODE_KEY, -1) == AlbumView.MODE_EDIT) {
                boolean result = databaseAdapter.update(album);
                viewModel.setCursor(databaseAdapter.getCursor(true));
                Toast.makeText(getActivity(), result ? R.string.msg_album_edit
                        : R.string.error_generic, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
