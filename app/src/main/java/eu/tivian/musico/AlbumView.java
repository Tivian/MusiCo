package eu.tivian.musico;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.basgeekball.awesomevalidation.AwesomeValidation;
import com.basgeekball.awesomevalidation.ValidationStyle;
import com.basgeekball.awesomevalidation.utility.RegexTemplate;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;

import eu.tivian.musico.data.Album;
import eu.tivian.musico.data.Song;
import eu.tivian.musico.database.DatabaseContract;
import eu.tivian.musico.database.DatabaseSuggestion;
import eu.tivian.musico.net.Discogs;
import eu.tivian.musico.utility.DateWatcher;
import eu.tivian.musico.utility.SuggestionShower;
import eu.tivian.musico.utility.Utilities;

/**
 * Activity for adding / editing / viewing the {@link Album}.
 */
public class AlbumView extends AppCompatActivity {
    /**
     * Request code used for file picker activity.
     */
    private static final int FILE_PICKER = 1;

    /**
     * Date format used in this activity.
     */
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    /**
     * {@link Bundle} key used for storing reference to the album.
     */
    public static final String ALBUM_KEY = "album";

    /**
     * {@link Bundle} key used for storing currently selected mode.
     */
    public static final String MODE_KEY  =  "mode";

    /**
     * Request code used by this activity.
     */
    public static final int REQUEST_CODE  = 0x8912;

    /**
     * Denotes that the result of this activity will be added into the album database.
     */
    public static final int MODE_ADD  = 0x01;

    /**
     * Denotes that the result of this activity will modify currently present data in the database.
     */
    public static final int MODE_EDIT = 0x02;

    /**
     * Denotes that this activity was started only for viewing purposes.
     */
    public static final int MODE_VIEW = 0x04;

    /**
     * Adapter for the {@link RecyclerView}.
     */
    private Adapter adapter;

    /**
     * Validator for the text fields in this activity.
     */
    private AwesomeValidation validation;

    /**
     * The currently inspecting album.
     */
    private Album album;

    /**
     * The cover of the album.
     */
    private ImageView cover;

    /**
     * The title of the album.
     */
    private AutoCompleteTextView title;

    /**
     * The name of the artist.
     */
    private AutoCompleteTextView artist;

    /**
     * The year of the release of the album.
     */
    private EditText year;

    /**
     * The genre of the album.
     */
    private AutoCompleteTextView genre;

    /**
     * Total duration of the album.
     */
    private TextView length;

    /**
     * The name of the store where album was bought.
     */
    private AutoCompleteTextView store;

    /**
     * The date when album was bought.
     */
    private EditText date;

    /**
     * The price of the album.
     */
    private EditText price;

    /**
     * The currency of the purchase.
     */
    private AutoCompleteTextView currency;

    /**
     * Stores which mode was selected for this activity.
     */
    private int mode;

    /**
     * The {@link RecyclerView} adapter.
     */
    private class AlbumTracksAdapter extends Adapter<AlbumTracksAdapter.ViewHolder> {
        /**
         * The view holder for the {@link Adapter}.
         */
        private class ViewHolder extends RecyclerView.ViewHolder {
            /**
             * The position of the track.
             */
            TextView trackId;

            /**
             * The title of the track.
             */
            TextView trackTitle;

            /**
             * The duration of the track.
             */
            TextView trackTime;

            /**
             * Constructs the view holder.
             *
             * @param itemView the view supplied by the {@link Adapter}.
             */
            ViewHolder(@NonNull  View itemView) {
                super(itemView);

                trackId = itemView.findViewById(R.id.tv_track_id);
                trackTitle = itemView.findViewById(R.id.tv_track_title);
                trackTime = itemView.findViewById(R.id.tv_track_time);
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
        public AlbumTracksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_album_track, parent, false));
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
            if (album == null)
                return;

            Song song = album.tracks.get(position);
            holder.trackId.setText(getString(R.string.track_id, position + 1));
            holder.trackTitle.setText(song.title);
            holder.trackTime.setText(song.duration.toString());
        }

        /**
         * Returns the total number of tracks on the album.
         *
         * @return the total number of tracks on the album.
         */
        @Override
        public int getItemCount() {
            return album == null ? 0 : album.tracks.size();
        }
    }

    /**
     * Converts the {@link ImageView} containing the album artwork into the raw {@code byte[]} array.
     *
     * @return the raw bitmap contained in the {@link #cover} image.
     */
    private byte[] getRawCover() {
        return Utilities.getBytes(Utilities.getBitmap(cover));
    }

    /**
     * Sets the album artwork given by the {@link Uri}.
     *
     * @param uri the content path.
     */
    private void setCover(Uri uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.album)
            .into(cover);
    }

    /**
     * Sets the album artwork given by the {@link String}.
     *
     * @param uri the content path.
     */
    private void setCover(String uri) {
        setCover(uri == null ? null : Uri.parse(uri));
    }

    /**
     * Sets the album artwork given by the raw {@code byte[]} array.
     *
     * @param raw the raw {@code byte[]} array containing the image.
     */
    private void setCover(byte[] raw) {
        Glide.with(this)
            .load(raw)
            .placeholder(R.drawable.album)
            .into(cover);
    }

    /**
     * Sets the UI elements according to the info in the {@link #album} object.
     * Clears all of the UI elements if {@link #album} is {@code null}.
     */
    private void setData() {
        if (album == null) {
            clear();
        } else {
            title.setText(album.title);
            artist.setText(album.artist);
            if (album.year > 1900)
                year.setText(String.format(Locale.getDefault(), "%4d", album.year));
            genre.setText(album.genre);
            length.setText(album.getDuration().toString());

            store.setText(album.purchase.store);
            date.setText(Utilities.toString(album.purchase.date, DATE_FORMAT));

            if (album.purchase.price > 0) {
                price.setText(String.format(Locale.getDefault(), "%.2f", album.purchase.price));
                currency.setText(album.purchase.currency);
            }

            if (album.cover == null)
                setCover(album.coverUrl);
            else
                setCover(album.cover);
        }

        adapter.notifyDataSetChanged();
    }

    /**
     * Sets the UI elements according to the info in given album.
     *
     * @param album the album to set.
     */
    private void setData(Album album) {
        setData(album, true);
    }

    /**
     *
     *
     * @param album the album to set.
     * @param preserve if {@code true} then {@link Album#purchase}
     *        part of {@link Album} object is preserved.
     */
    private void setData(Album album, boolean preserve) {
        if (preserve) {
            album.purchase = getAlbum().purchase;

            if (album.id == -1 && this.album != null)
                album.id = this.album.id;
        }

        this.album = album;
        setData();
        validation.clear();
    }

    /**
     * Creates new {@link Album} object from information in the UI elements.
     *
     * @return the new album.
     */
    private Album getAlbum() {
        Album album = new Album();
        album.id = this.album == null ? -1 : this.album.id;
        album.title = title.getText().toString().trim();
        album.artist = artist.getText().toString().trim();
        album.genre = genre.getText().toString().trim();

        try {
            album.year = Integer.parseInt(year.getText().toString());
        } catch (NumberFormatException ex) {
            album.year = 0;
        }

        if (this.album != null)
            album.tracks = new ArrayList<>(this.album.tracks);

        album.purchase.store = store.getText().toString().trim();
        album.purchase.date = Utilities.parseDate(date.getText().toString(), DATE_FORMAT);

        try {
            album.purchase.price = Double.parseDouble(price.getText().toString());
        } catch (NumberFormatException ex) {
            album.purchase.price = 0;
        }

        album.purchase.currency = currency.getText().toString().trim().toUpperCase();
        album.cover = getRawCover();

        return album;
    }

    /**
     * Clears all editable {@link TextView}s.
     */
    private void clear() {
        final TextView[] fields = {
            title, artist, year, genre, length, store, date, price, currency
        };

        setCover((Uri) null);
        for (TextView tv : fields)
            tv.setText(null);

        validation.clear();
    }

    /**
     * Makes all {@link TextView}s in this activity uneditable.
     */
    private void disable() {
        final TextView[] fields = {
            title, artist, year, genre, store, date, price, currency
        };

        for (TextView tv : fields) {
            tv.setInputType(InputType.TYPE_NULL);
            tv.setFocusable(false);
            tv.setFocusableInTouchMode(false);
        }
    }

    /**
     * Enlarges the album artwork and centers it on the screen.
     *
     * @param v the view containing the album artwork.
     */
    private void exhibitCover(View v) {
        Point screen = Utilities.getScreenDim(this);
        Point rel = Utilities.getRelativePoint((View) cover.getParent());
        int height = v.getHeight();
        int width = v.getWidth();

        if (height > 0 && width > 0) {
            int x = (screen.x - width) / 2 - rel.x;
            int y = (screen.y - height) / 2 - rel.y;

            v.setTag(new PointF(v.getX(), v.getY()));
            View albumMask = findViewById(R.id.album_mask);
            albumMask.animate().alpha(0.5f);

            screen.x = albumMask.getWidth();
            screen.y = albumMask.getHeight();

            float scale;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                scale = screen.y / (float) height;
            else
                scale = screen.x / (float) width;
            scale *= 0.99f;

            v.animate().x(x).y(y).scaleX(scale).scaleY(scale);
        }

        v.setOnClickListener(this::concealCover);
    }

    /**
     * Reduces the album artwork and retracts it into the default position.
     *
     * @param v the view containing the album artwork.
     */
    private void concealCover(View v) {
        PointF pos = (PointF) v.getTag();

        v.animate().scaleY(1).scaleX(1).y(pos.y).x(pos.x);
        findViewById(R.id.album_mask).animate().alpha(0);

        v.setOnClickListener(this::exhibitCover);
    }

    /**
     * Validates all text inputs in this activity.
     *
     * @return {@code true} if all text inputs have valid values.
     */
    private boolean validate() {
        return validation.validate();
    }

    /**
     * Creates and initializes the UI for the activity.
     *
     * @param savedInstanceState if the activity is being re-initialized after previously being
     *        shut down then this Bundle contains the data it most recently supplied in
     *        {@link #onSaveInstanceState(Bundle)}. This value may be {@code null}.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_view);

        setSupportActionBar(findViewById(R.id.toolbar_album));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.edit_album));

        cover = findViewById(R.id.iv_album_cover);
        title = findViewById(R.id.et_album_title);
        artist = findViewById(R.id.et_album_artist);
        year = findViewById(R.id.et_album_year);
        genre = findViewById(R.id.et_album_genre);
        length = findViewById(R.id.tv_album_length);
        store = findViewById(R.id.et_album_store);
        date = findViewById(R.id.et_album_date);
        price = findViewById(R.id.et_album_price);
        currency = findViewById(R.id.et_album_currency);

        validation = new AwesomeValidation(ValidationStyle.BASIC);
        validation.addValidation(title, RegexTemplate.NOT_EMPTY, getString(R.string.error_empty));
        validation.addValidation(artist, RegexTemplate.NOT_EMPTY, getString(R.string.error_empty));
        validation.addValidation(genre, RegexTemplate.NOT_EMPTY, getString(R.string.error_empty));
        validation.addValidation(price, RegexTemplate.NOT_EMPTY, getString(R.string.error_empty));
        validation.addValidation(currency, input -> {
            try {
                Currency.getInstance(input);
                return true;
            } catch (IllegalArgumentException | NullPointerException ex) {
                return false;
            }
        }, getString(R.string.error_currency));

        validation.addValidation(year, input -> {
            if (TextUtils.isEmpty(input))
                return true;

            try {
                int year = Integer.parseInt(input);
                return year >= 1900 && year <= Calendar.getInstance().get(Calendar.YEAR);
            } catch (NumberFormatException ex) {
                return false;
            }
        }, getString(R.string.error_year_range, Calendar.getInstance().get(Calendar.YEAR)));

        Bundle bundle = getIntent().getExtras();
        if (savedInstanceState != null)
            bundle = savedInstanceState;

        if (bundle != null) {
            album = bundle.getParcelable(ALBUM_KEY);
            mode = bundle.getInt(MODE_KEY);

            switch (mode) {
                case MODE_ADD:
                    getSupportActionBar().setTitle(R.string.add_album);
                    break;
                case MODE_VIEW:
                    getSupportActionBar().setTitle(R.string.view_album);
                    disable();
                    break;
            }
        }

        RecyclerView recyclerView = findViewById(R.id.rv_album_tracks);
        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new AlbumTracksAdapter();
        recyclerView.setAdapter(adapter);

        setData();

        if (mode != MODE_VIEW) {
            date.addTextChangedListener(new DateWatcher(date));
            currency.setOnFocusChangeListener(new SuggestionShower());

            new DatabaseSuggestion(this, title,
                    DatabaseContract.AlbumEntry.TABLE_NAME,
                    DatabaseContract.AlbumEntry.COLUMN_TITLE);
            new DatabaseSuggestion(this, artist,
                    DatabaseContract.ArtistEntry.TABLE_NAME,
                    DatabaseContract.ArtistEntry.COLUMN_NAME);
            new DatabaseSuggestion(this, genre,
                    DatabaseContract.GenreEntry.TABLE_NAME,
                    DatabaseContract.GenreEntry.COLUMN_NAME);
            new DatabaseSuggestion(this, store,
                    DatabaseContract.StoreEntry.TABLE_NAME,
                    DatabaseContract.StoreEntry.COLUMN_NAME);
            new DatabaseSuggestion(this, currency,
                    DatabaseContract.CurrencyEntry.TABLE_NAME,
                    DatabaseContract.CurrencyEntry.COLUMN_NAME);

            cover.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, FILE_PICKER);
            });
        } else {
            cover.setOnClickListener(this::exhibitCover);
        }
    }

    /**
     * Saves the current activity configuration.
     *
     * @param outState {@link Bundle} in which to place your saved state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (album != null)
            album.cover = getRawCover();

        outState.putParcelable(ALBUM_KEY, album);
        outState.putInt(MODE_KEY, mode);
        super.onSaveInstanceState(outState);
    }

    /**
     * Creates menu for album manipulation options.
     *
     * @param menu the options menu in which you place your items.
     * @return always {@code true}, so the menu will be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mode != MODE_VIEW)
            getMenuInflater().inflate(R.menu.menu_album, menu);

        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     *
     * @param item the menu item that was selected.
     * @return {@code true} if any action was taken by this function,
     *         {@code false} otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.button_album_clear:
                setData(null, false);
                return true;
            case R.id.button_album_parse:
                Discogs.get().getAlbum(Discogs.Query.from(getAlbum()), this::setData);
                return true;
            case R.id.button_album_ok:
                if (validate()) {
                    Intent intent = new Intent();
                    intent.putExtra(ALBUM_KEY, getAlbum());
                    intent.putExtra(MODE_KEY, mode);
                    setResult(RESULT_OK, intent);
                    finish();
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Handles data returned from the file picker activity.
     *
     * @param requestCode the integer request code originally supplied to
     *        {@link #startActivityForResult(Intent, int)} allowing to identify the caller.
     * @param resultCode the integer result code returned by the child activity through its {@link #setResult(int)}.
     * @param data an {@link Intent}, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER && resultCode == Activity.RESULT_OK && data != null)
            setCover(data.getData());
    }
}
