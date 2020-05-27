package eu.tivian.musico.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import eu.tivian.musico.data.Album;
import eu.tivian.musico.data.Song;
import eu.tivian.musico.net.Exchange;

import static eu.tivian.musico.database.DatabaseContract.AlbumEntry;
import static eu.tivian.musico.database.DatabaseContract.ArtistEntry;
import static eu.tivian.musico.database.DatabaseContract.CurrencyEntry;
import static eu.tivian.musico.database.DatabaseContract.GenreEntry;
import static eu.tivian.musico.database.DatabaseContract.PurchaseEntry;
import static eu.tivian.musico.database.DatabaseContract.SQL_LIST_ALL;
import static eu.tivian.musico.database.DatabaseContract.SQL_LIST_TRACKS;
import static eu.tivian.musico.database.DatabaseContract.SettingsEntry;
import static eu.tivian.musico.database.DatabaseContract.SongEntry;
import static eu.tivian.musico.database.DatabaseContract.StoreEntry;
import static eu.tivian.musico.database.DatabaseContract.TrackEntry;

/**
 * A helper class which minimizes the direct communication with the database, to prevent corruption of data.
 *
 * @see <a href="https://sqlite.org/">SQLite documentation</a>
 */
public class DatabaseAdapter implements Closeable {
    /**
     * Singleton instance of this class.
     */
    private static DatabaseAdapter instance;

    /**
     * A SQLite database.
     */
    private final SQLiteDatabase db;

    /**
     * Stores last used ORDER BY clause for listing all of the albums.
     */
    private String lastOrderBy;

    /**
     * Determines whether descending or ascending ordering will be used.
     * <br>
     * {@code true} value means descending ordering, while {@code false} value means ascending ordering
     */
    private boolean ordering = false;

    /**
     * Compiled SQL statement used to refresh the currency rates.
     */
    private SimpleStatement ratesUpdated;

    /**
     * Available sorting options for listing all albums.
     */
    public static final class Sort {
        /**
         * Sorts by the artist name.
         */
        public static final String ARTIST = ArtistEntry.TABLE_NAME + "." + ArtistEntry.COLUMN_NAME;

        /**
         * Sorts by the title of the album.
         */
        public static final String TITLE = AlbumEntry.TABLE_NAME + "." + AlbumEntry.COLUMN_TITLE;

        /**
         * Sorts by the year of the original release of the album.
         */
        public static final String YEAR = AlbumEntry.TABLE_NAME + "." + AlbumEntry.COLUMN_YEAR;

        /**
         * Sorts by the genre of the album.
         */
        public static final String GENRE = GenreEntry.TABLE_NAME + "." + GenreEntry.COLUMN_NAME;

        /**
         * Sorts by the date of purchase.
         */
        public static final String DATE = PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_DATE;

        /**
         * Sorts by the store in which album was bought.
         */
        public static final String STORE = StoreEntry.TABLE_NAME + "." + StoreEntry.COLUMN_NAME;

        /**
         * Sorts by the price of the album.
         */
        public static final String PRICE = PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_PRICE;

        /**
         * Sorts by the currency used in the purchase.
         */
        public static final String CURRENCY = CurrencyEntry.TABLE_NAME + "." + CurrencyEntry.COLUMN_NAME;
    }

    /**
     * Private constructor used to initialize the singleton.
     *
     * @param context to use for locating paths to the the database.
     * @see #instance
     */
    private DatabaseAdapter(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    /**
     * Initializes the singleton in thread-safe manner.
     *
     * @param context to use for locating paths to the the database.
     * @return the singleton instance.
     */
    public synchronized static DatabaseAdapter init(Context context) {
        if (instance == null)
            instance = new DatabaseAdapter(context);

        return instance;
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance.
     */
    public static DatabaseAdapter get() {
        return instance;
    }

    /**
     * Inserts the {@code value} into the {@code table} at the specified {@code column}
     *  if it doesn't already exists in the database and returns the row {@link BaseColumns#_ID}.
     * If the {@code value} is already present then it only returns the
     *  appropriate {@link BaseColumns#_ID} value.
     *
     * @param table target table.
     * @param column target column.
     * @param value a value which we're looking for, or are inserting if it's not present in the database.
     * @return the {@link BaseColumns#_ID} value.
     */
    private long getIdOrInsert(String table, String column, String value) {
        if (TextUtils.isEmpty(table) || TextUtils.isEmpty(column) || TextUtils.isEmpty(value))
            return -1;

        ContentValues values = new ContentValues();
        values.put(column, value);
        long _id = db.insert(table, null, values);

        if (_id == -1) {
            String[] columns = { BaseColumns._ID };
            String selection = column + " = ?";
            String[] selectionArgs = { value };
            Cursor cursor = db.query(table, columns, selection,
                    selectionArgs, null, null, null);
            if (cursor.moveToNext())
                _id = cursor.getLong(0);
            cursor.close();
        }

        return _id;
    }

    /**
     * Adds the album into the database.
     *
     * @param album album to be added into the database.
     */
    public void add(Album album) {
        boolean new_album = true;

        long artist_id = getIdOrInsert(ArtistEntry.TABLE_NAME,
            ArtistEntry.COLUMN_NAME, album.artist);

        long genre_id = getIdOrInsert(GenreEntry.TABLE_NAME,
            GenreEntry.COLUMN_NAME, album.genre);

        long store_id = TextUtils.isEmpty(album.purchase.store) ? -1 :
            getIdOrInsert(StoreEntry.TABLE_NAME, StoreEntry.COLUMN_NAME, album.purchase.store);

        long currency_id = getIdOrInsert(CurrencyEntry.TABLE_NAME,
            CurrencyEntry.COLUMN_NAME, album.purchase.currency);

        // add album
        ContentValues values = new ContentValues();
        values.put(AlbumEntry.COLUMN_ARTIST_ID, artist_id);
        values.put(AlbumEntry.COLUMN_TITLE, album.title);
        values.put(AlbumEntry.COLUMN_YEAR, album.year);
        values.put(AlbumEntry.COLUMN_GENRE_ID, genre_id);
        values.put(AlbumEntry.COLUMN_COVER, album.cover);
        long album_id = db.insert(AlbumEntry.TABLE_NAME, null, values);

        // get id, if album exists
        if (album_id == -1) {
            new_album = false;

            String[] columns = {AlbumEntry._ID};
            String selection = AlbumEntry.COLUMN_ARTIST_ID + " = ? " +
                    "AND " + AlbumEntry.COLUMN_TITLE + " = ? " +
                    "AND " + AlbumEntry.COLUMN_YEAR + " = ?";
            String[] selectionArgs = {
                    String.valueOf(artist_id),
                    album.title,
                    String.valueOf(album.year)
            };

            Cursor cursor = db.query(AlbumEntry.TABLE_NAME, columns, selection, selectionArgs,
                    null, null, null);
            if (cursor.moveToFirst())
                album_id = cursor.getLong(0);
            cursor.close();
        }

        // add purchase
        values = new ContentValues();
        values.put(PurchaseEntry.COLUMN_ALBUM_ID, album_id);

        if (store_id == -1)
            values.putNull(PurchaseEntry.COLUMN_STORE_ID);
        else
            values.put(PurchaseEntry.COLUMN_STORE_ID, store_id);

        if (album.purchase.date == null)
            values.putNull(PurchaseEntry.COLUMN_DATE);
        else
            values.put(PurchaseEntry.COLUMN_DATE, album.purchase.date.getTime());

        values.put(PurchaseEntry.COLUMN_PRICE, album.purchase.price);
        values.put(PurchaseEntry.COLUMN_CURRENCY_ID, currency_id);
        db.insert(PurchaseEntry.TABLE_NAME, null, values);

        if (new_album) {
            // add songs or get their ids
            List<Long> song_ids = new ArrayList<>();
            for (Song s : album.tracks) {
                values = new ContentValues();
                values.put(SongEntry.COLUMN_TITLE, s.title);
                values.put(SongEntry.COLUMN_DURATION, s.duration.toString());
                long song_id = db.insert(SongEntry.TABLE_NAME, null, values);

                if (song_id == -1) {
                    String[] columns = {SongEntry._ID};
                    String selection = SongEntry.COLUMN_TITLE + " = ? " +
                            "AND " + SongEntry.COLUMN_DURATION + " = ?";
                    String[] selectionArgs = {s.title, s.duration.toString()};
                    Cursor cursor = db.query(SongEntry.TABLE_NAME, columns, selection, selectionArgs,
                            null, null, null);
                    if (cursor.moveToFirst())
                        song_id = cursor.getLong(0);
                    cursor.close();
                }

                song_ids.add(song_id);
            }

            // add songs to tracks table
            for (long song_id : song_ids) {
                values = new ContentValues();
                values.put(TrackEntry.COLUMN_ALBUM_ID, album_id);
                values.put(TrackEntry.COLUMN_SONG_ID, song_id);
                db.insert(TrackEntry.TABLE_NAME, null, values);
            }
        }

        album.id = album_id;
    }

    /**
     * Updates the database based on the {@link Album#id} value.
     *
     * @param album album to be updated.
     * @return {@code true} if the update was successful.
     */
    public boolean update(Album album) {
        if (album.id == -1)
            return false;

        // simple but effective way, at least for small databases
        if (delete(album)) {
            add(album);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Deletes the album from the database based on the {@link Album#id} value.
     *
     * @param album album to be deleted.
     * @return {@code true} if the deletion was successful.
     */
    public boolean delete(Album album) {
        String whereClause = PurchaseEntry.COLUMN_ALBUM_ID + " = ?";
        String[] whereArgs = { String.valueOf(album.id) };

        // rest of deletion logic is handled by triggers inside the database
        return db.delete(PurchaseEntry.TABLE_NAME, whereClause, whereArgs) == 1;
    }

    /**
     * Sets the {@code value} into the settings table at the supplied {@code key}.
     *
     * @param key settings key.
     * @param value value to be added or change (whether the key is present).
     */
    public void setSetting(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(SettingsEntry.COLUMN_KEY, key);
        values.put(SettingsEntry.COLUMN_VALUE, value);

        db.replace(SettingsEntry.TABLE_NAME, null, values);
    }

    /**
     * Returns the value of the chosen setting.
     *
     * @param key the chosen setting.
     * @return value corresponding to the {@code key} in the settings table.
     */
    public String getSetting(String key) {
        ContentValues values = new ContentValues();
        values.put(SettingsEntry.COLUMN_KEY, key);

        String[] columns = { SettingsEntry.COLUMN_VALUE };
        String selection = SettingsEntry.COLUMN_KEY + " = ?";
        String[] selectionArgs = { key };

        Cursor cursor = db.query(SettingsEntry.TABLE_NAME, columns, selection,
            selectionArgs, null, null, null);

        String result = null;
        if (cursor.moveToFirst())
            result = cursor.getString(0);
        cursor.close();

        return result;
    }

    /**
     * Default cursor for listing all albums.
     * Default ordering is first title of the album, then the year of the release.
     *
     * @return the cursor with list of the albums.
     */
    public AlbumCursor getCursor() {
        return getCursor(true, DatabaseAdapter.Sort.TITLE, DatabaseAdapter.Sort.YEAR);
    }

    /**
     * Gets the cursor with list of all the albums.
     *
     * @param orderBy determines the ordering of the list.
     * @return the cursor with list of the albums.
     */
    public AlbumCursor getCursor(String... orderBy) {
        return getCursor(false, orderBy);
    }

    /**
     * Gets the cursor with list of all the albums.
     *
     * @param useLastOrdering {@code true} if you want the ordering to be the same,
     *        but the results will be flipped.
     * @param orderBy determines the ordering of the list.
     * @return the cursor with list of the albums.
     */
    public AlbumCursor getCursor(boolean useLastOrdering, String... orderBy) {
        StringBuilder sb = new StringBuilder(SQL_LIST_ALL).append(" ORDER BY ");
        String majorOrder = null;

        if (orderBy != null && orderBy.length > 0) {
            String joined = TextUtils.join(", ", orderBy);
            sb.append(joined);
            majorOrder = orderBy[0];

            if (lastOrderBy != null && lastOrderBy.equals(joined)) {
                sb.insert(sb.indexOf(",", sb.lastIndexOf("ORDER BY")), ordering ? " DESC" : " ASC");
                ordering = !ordering;
            } else {
                ordering = true;
            }

            lastOrderBy = joined;
        } else if (useLastOrdering && !TextUtils.isEmpty(lastOrderBy)) {
            sb.append(lastOrderBy);
            majorOrder = lastOrderBy.split(", ")[0];
        } else {
            sb.append(Sort.TITLE);
            majorOrder = Sort.TITLE;
        }

        return new AlbumCursor(db.rawQuery(sb.toString(), null),
                majorOrder, this::getTracks);
    }

    /**
     * Updates the currency rates. Can only be called once a day. Any other calls will be ignored.
     */
    public void updateRates() {
        if (ratesUpdated == null) {
            ratesUpdated = new SimpleStatement(
                "SELECT " + SettingsEntry.COLUMN_VALUE + " " +
                "FROM " + SettingsEntry.TABLE_NAME + " " +
                "WHERE " + SettingsEntry.COLUMN_KEY + " = '" + SettingsEntry.KEY_RATE_UPDATE + "'");
        }

        final String today = Exchange.DATE_FORMAT.format(new Date());
        if (ratesUpdated.get().equals(today))
            return;

        Exchange.get().getRates(rates -> {
            String whereClause = CurrencyEntry.COLUMN_NAME + " = ?";
            for (Map.Entry<String, Double> entry : rates.rate.entrySet()) {
                ContentValues values = new ContentValues();
                values.put(CurrencyEntry.COLUMN_RATE, entry.getValue());
                String[] whereArgs = { entry.getKey() };

                db.update(CurrencyEntry.TABLE_NAME, values, whereClause, whereArgs);
            }
            setSetting(SettingsEntry.KEY_RATE_UPDATE, today);
        });
    }

    /**
     * Gets cursor from raw SQL query.
     *
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param params you may include ?s in where clause in the query,
     *        which will be replaced by the values from {@code params}.
     *        The values will be bound as Strings.
     * @return the cursor, which is positioned before the first entry.
     */
    Cursor query(String sql, String... params) {
        return db.rawQuery(sql, params);
    }

    /**
     * Returns the underlying SQLite database.
     *
     * @return the database object.
     */
    SQLiteDatabase getDb() {
        return db;
    }

    /**
     * Gets the list of tracks for chosen {@link Album#id}.
     *
     * @param id album ID.
     * @return the cursor with track list for chosen album.
     */
    private Cursor getTracks(long id) {
        return db.rawQuery(SQL_LIST_TRACKS, new String[] { String.valueOf(id) });
    }

    /**
     * Closes the database.
     */
    @Override
    public void close() {
        if (db != null)
            db.close();
    }
}
