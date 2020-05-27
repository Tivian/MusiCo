package eu.tivian.musico.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import androidx.arch.core.util.Function;

import java.util.Date;
import java.util.Locale;

import eu.tivian.musico.data.Album;
import eu.tivian.musico.data.Duration;
import eu.tivian.musico.data.Song;
import eu.tivian.musico.utility.Utilities;

/**
 * A database cursor focused on providing the interface for retrieving {@link Album} object from database.
 */
public class AlbumCursor extends CursorWrapper {
    /**
     * Function used to get the track list of the album at the current position of the cursor.
     */
    private Function<Long, Cursor> tracks;

    /**
     * Denotes which ordering is currently used.
     * This information is used to determine the scroll bar label text.
     */
    private String ordering;

    /**
     * Default constructor used to wrap the supplied cursor in this object.
     *
     * @param cursor database cursor to be wrapped by this class.
     * @param ordering currently used ordering of the album list.
     * @param tracks functor used to get the track list.
     */
    AlbumCursor(Cursor cursor, String ordering, Function<Long, Cursor> tracks) {
        super(cursor);
        this.ordering = ordering;
        this.tracks = tracks;
    }

    /**
     * Gets the album at the current position of the cursor.
     *
     * @return the album at the current position of the cursor.
     *         The returned album doesn't have the track list.
     *         It can be supplied later by calling the {@link #loadTracks(Album)} function.
     */
    public Album getAlbum() {
        Album album = new Album();
        Cursor c = getWrappedCursor();

        album.id = c.getLong(0);
        album.artist = c.getString(1);
        album.title = c.getString(2);
        album.year = c.getInt(3);
        album.genre = c.getString(4);
        album.cover = c.getBlob(5);
        album.purchase.date = new Date(c.getLong(6));
        album.purchase.store = c.getString(7);
        album.purchase.price = c.getDouble(8);
        album.purchase.currency = c.getString(9);

        return album;
    }

    /**
     * Loads the track list into the album.
     *
     * @param album album into which the track list will be loaded.
     */
    public void loadTracks(Album album) {
        if (album.tracks != null && album.tracks.size() != 0)
            return;

        try (Cursor c = tracks.apply(album.id)) {
            while (c.moveToNext())
                album.tracks.add(new Song(c.getString(0), Duration.from(c.getString(1))));
        }
    }

    /**
     * Gets the label according to the supplied {@link #ordering}.
     *
     * @return the scroll bar label text.
     */
    public String getLabel() {
        int column = 2;
        boolean trim = ordering != null && ordering.equals(DatabaseAdapter.Sort.TITLE);
        final Cursor c = getWrappedCursor();
        String out = null;

        if (ordering != null) {
            switch (ordering) {
                case DatabaseAdapter.Sort.ARTIST:
                    column = 1;
                    break;
                case DatabaseAdapter.Sort.CURRENCY:
                    column = 9;
                    break;
                case DatabaseAdapter.Sort.DATE:
                    column = 6;
                    out = Utilities.toString(new Date(c.getLong(column)));
                    break;
                case DatabaseAdapter.Sort.GENRE:
                    column = 4;
                    break;
                case DatabaseAdapter.Sort.PRICE:
                    column = 8;
                    out = String.format(Locale.getDefault(), "%.2f", c.getDouble(column));
                    break;
                case DatabaseAdapter.Sort.STORE:
                    column = 7;
                    break;
                case DatabaseAdapter.Sort.YEAR:
                    column = 3;
                    break;
            }
        }

        if (out == null)
            out = getWrappedCursor().getString(column);

        return trim ? "" + out.charAt(0) : out;
    }
}
