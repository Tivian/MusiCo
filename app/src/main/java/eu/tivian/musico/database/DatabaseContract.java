package eu.tivian.musico.database;

import android.provider.BaseColumns;

/**
 * Contract class which describes the schema of the database used for this app.
 */
public final class DatabaseContract {
    /**
     * To prevent someone from accidentally instantiating the contract class,
     * make the constructor private.
     */
    private DatabaseContract() { }

    /**
     * SQL statement used to list all albums present in the database.
     */
    public static String SQL_LIST_ALL =
        "SELECT " +
            AlbumEntry.TABLE_NAME    + "." + AlbumEntry._ID              + ", " +
            ArtistEntry.TABLE_NAME   + "." + ArtistEntry.COLUMN_NAME     + ", " +
            AlbumEntry.TABLE_NAME    + "." + AlbumEntry.COLUMN_TITLE     + ", " +
            AlbumEntry.TABLE_NAME    + "." + AlbumEntry.COLUMN_YEAR      + ", " +
            GenreEntry.TABLE_NAME    + "." + GenreEntry.COLUMN_NAME      + ", " +
            AlbumEntry.TABLE_NAME    + "." + AlbumEntry.COLUMN_COVER     + ", " +
            PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_DATE   + ", " +
            StoreEntry.TABLE_NAME    + "." + StoreEntry.COLUMN_NAME      + ", " +
            PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_PRICE  + ", " +
            CurrencyEntry.TABLE_NAME + "." + CurrencyEntry.COLUMN_NAME   + " "  +
        "FROM " + AlbumEntry.TABLE_NAME  + " " +
        "JOIN " + ArtistEntry.TABLE_NAME + " " +
          "ON " + ArtistEntry.TABLE_NAME + "." + ArtistEntry._ID + " = "
                + AlbumEntry.TABLE_NAME  + "." + AlbumEntry.COLUMN_ARTIST_ID + " " +
        "JOIN " + GenreEntry.TABLE_NAME  + " " +
          "ON " + GenreEntry.TABLE_NAME  + "." + GenreEntry._ID + " = "
                + AlbumEntry.TABLE_NAME  + "." + AlbumEntry.COLUMN_GENRE_ID + " " +
        "JOIN " + PurchaseEntry.TABLE_NAME + " " +
          "ON " + PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_ALBUM_ID + " = "
                + AlbumEntry.TABLE_NAME    + "." + AlbumEntry._ID + " " +
        "JOIN " + CurrencyEntry.TABLE_NAME + " " +
          "ON " + CurrencyEntry.TABLE_NAME + "." + CurrencyEntry._ID + " = "
                + PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_CURRENCY_ID + " " +
        "LEFT OUTER JOIN " + StoreEntry.TABLE_NAME + " " +
          "ON " + StoreEntry.TABLE_NAME    + "." + StoreEntry._ID + " = "
                + PurchaseEntry.TABLE_NAME + "." + PurchaseEntry.COLUMN_STORE_ID;

    /**
     * SQL statement used to list all tracks for chosen album.
     */
    public static String SQL_LIST_TRACKS =
        "SELECT " +
            SongEntry.TABLE_NAME + "." + SongEntry.COLUMN_TITLE    + ", " +
            SongEntry.TABLE_NAME + "." + SongEntry.COLUMN_DURATION + " " +
        "FROM " +
            TrackEntry.TABLE_NAME + ", " +
            SongEntry.TABLE_NAME  + " " +
        "WHERE " + TrackEntry.TABLE_NAME + "." + TrackEntry.COLUMN_ALBUM_ID + " = ? " +
            "AND " + TrackEntry.TABLE_NAME + "." + TrackEntry.COLUMN_SONG_ID + " = "
                + SongEntry.TABLE_NAME + "." + SongEntry._ID;

    /**
     * Array of SQL statements executed when the database is created for the first time.
     * This field is used for things like database trigger creation, etc.
     */
    public static String[] SQL_STATEMENTS = {
        "CREATE TRIGGER " + StoreEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + PurchaseEntry.TABLE_NAME + " " +
        "WHEN NOT EXISTS (SELECT * FROM " + PurchaseEntry.TABLE_NAME +
                 " WHERE " + PurchaseEntry.COLUMN_STORE_ID + " = OLD." + PurchaseEntry.COLUMN_STORE_ID + ") " +
        "BEGIN " +
            "DELETE FROM " + StoreEntry.TABLE_NAME +
                 " WHERE " + StoreEntry._ID + " = OLD." + PurchaseEntry.COLUMN_STORE_ID + "; " +
        "END",

        "CREATE TRIGGER " + CurrencyEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + PurchaseEntry.TABLE_NAME + " " +
        "WHEN NOT EXISTS (SELECT * FROM " + PurchaseEntry.TABLE_NAME +
                 " WHERE " + PurchaseEntry.COLUMN_CURRENCY_ID + " = OLD." + PurchaseEntry.COLUMN_CURRENCY_ID + ") " +
        "BEGIN " +
            "DELETE FROM " + CurrencyEntry.TABLE_NAME +
                 " WHERE " + CurrencyEntry._ID + " = OLD." + PurchaseEntry.COLUMN_CURRENCY_ID + "; " +
        "END",

        "CREATE TRIGGER " + AlbumEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + PurchaseEntry.TABLE_NAME + " " +
        "BEGIN " +
            "DELETE FROM " + AlbumEntry.TABLE_NAME +
                 " WHERE " + AlbumEntry._ID + " = OLD." + PurchaseEntry.COLUMN_ALBUM_ID + "; " +
        "END",

        "CREATE TRIGGER " + TrackEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + AlbumEntry.TABLE_NAME + " " +
        "BEGIN " +
            "DELETE FROM " + TrackEntry.TABLE_NAME +
                 " WHERE " + TrackEntry.COLUMN_ALBUM_ID + " = OLD." + AlbumEntry._ID + "; " +
        "END",

        "CREATE TRIGGER " + SongEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + TrackEntry.TABLE_NAME + " " +
        "WHEN NOT EXISTS (SELECT * FROM " + TrackEntry.TABLE_NAME +
                 " WHERE " + TrackEntry.COLUMN_SONG_ID + " = OLD." + TrackEntry.COLUMN_SONG_ID + ") " +
        "BEGIN " +
            "DELETE FROM " + SongEntry.TABLE_NAME +
                 " WHERE " + SongEntry._ID + " = OLD." + TrackEntry.COLUMN_SONG_ID + "; " +
        "END",

        "CREATE TRIGGER " + ArtistEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + AlbumEntry.TABLE_NAME + " " +
        "WHEN NOT EXISTS (SELECT * FROM " + AlbumEntry.TABLE_NAME +
                 " WHERE " + AlbumEntry.COLUMN_ARTIST_ID + " = OLD." + AlbumEntry.COLUMN_ARTIST_ID + ") " +
        "BEGIN " +
            "DELETE FROM " + ArtistEntry.TABLE_NAME +
                 " WHERE " + ArtistEntry._ID + " = OLD." + AlbumEntry.COLUMN_ARTIST_ID + "; " +
        "END",

        "CREATE TRIGGER " + GenreEntry.TABLE_NAME + "_cascade " +
            "AFTER DELETE ON " + AlbumEntry.TABLE_NAME + " " +
        "WHEN NOT EXISTS (SELECT * FROM " + AlbumEntry.TABLE_NAME +
                 " WHERE " + AlbumEntry.COLUMN_GENRE_ID + " = OLD." + AlbumEntry.COLUMN_GENRE_ID + ") " +
        "BEGIN " +
            "DELETE FROM " + GenreEntry.TABLE_NAME +
                 " WHERE " + GenreEntry._ID + " = OLD." + AlbumEntry.COLUMN_GENRE_ID + "; " +
        "END"
    };

    /**
     * A representation of the schema for table containing songs.
     */
    public static class SongEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "song";

        /**
         * The name of the column for song title.
         */
        public static final String COLUMN_TITLE = "title";

        /**
         * The name of the column for the duration of the song.
         */
        public static final String COLUMN_DURATION = "duration";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
               COLUMN_TITLE + " TEXT NOT NULL, " +
            COLUMN_DURATION + " TEXT, " +
                 "UNIQUE (" + COLUMN_TITLE + ", " + COLUMN_DURATION + ") " + ")";
    }

    /**
     * A representation of the schema for table describing artists.
     */
    public static class ArtistEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "artist";

        /**
         * The name of the column for the name of the artist.
         */
        public static final String COLUMN_NAME = "name";

        /**
         * SQL statement used to create this table.
         */
        static  final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_NAME + " TEXT UNIQUE NOT NULL " + ")";
    }

    /**
     * A representation of the schema for table describing albums.
     */
    public static class AlbumEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "album";

        /**
         * The name of the column for the foreign key associated with the {@link ArtistEntry}{@code ._ID}.
         */
        public static final String COLUMN_ARTIST_ID = "artist_id";

        /**
         * The name of the column for he title of the album.
         */
        public static final String COLUMN_TITLE = "title";

        /**
         * The name of the column for the year of the original release of the album.
         */
        public static final String COLUMN_YEAR = "year";

        /**
         * The name of the column for the genre of the album.
         */
        public static final String COLUMN_GENRE_ID = "genre_id";

        /**
         * The name of the column for a blob representation of the JPEG for the album cover.
         */
        public static final String COLUMN_COVER = "cover";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
           COLUMN_ARTIST_ID + " INTEGER NOT NULL, " +
               COLUMN_TITLE + " TEXT NOT NULL, " +
                COLUMN_YEAR + " INTEGER NOT NULL, " +
            COLUMN_GENRE_ID + " INTEGER, " +
               COLUMN_COVER + " BLOB, " +
            "FOREIGN KEY (" + COLUMN_ARTIST_ID + ") REFERENCES " + ArtistEntry.TABLE_NAME + "(" + ArtistEntry._ID + "), " +
            "FOREIGN KEY (" + COLUMN_GENRE_ID + ") REFERENCES " + GenreEntry.TABLE_NAME + "(" + GenreEntry._ID + "), "+
                 "UNIQUE (" + COLUMN_ARTIST_ID + ", " + COLUMN_TITLE + ", " + COLUMN_YEAR + ") " + ")";
    }

    /**
     * A representation of the schema for table of tracks.
     */
    public static class TrackEntry {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "track";

        /**
         * The name of the column for the foreign key associated with the {@link SongEntry}{@code ._ID}.
         */
        public static final String COLUMN_SONG_ID = "song_id";

        /**
         * The name of the column for the foreign key associated with the {@link AlbumEntry}{@code ._ID}.
         */
        public static final String COLUMN_ALBUM_ID = "album_id";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
             COLUMN_SONG_ID + " INTEGER NOT NULL, " +
            COLUMN_ALBUM_ID + " INTEGER NOT NULL, " +
            "PRIMARY KEY (" + COLUMN_SONG_ID  + ", " + COLUMN_ALBUM_ID + "), " +
            "FOREIGN KEY (" + COLUMN_SONG_ID  + ") REFERENCES " + SongEntry.TABLE_NAME + "(" + SongEntry._ID + "), " +
            "FOREIGN KEY (" + COLUMN_ALBUM_ID + ") REFERENCES " + AlbumEntry.TABLE_NAME + "(" + AlbumEntry._ID + ") " + ")";
    }

    /**
     * A representation of the schema for table of genres.
     */
    public static class GenreEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "genre";

        /**
         * The name of the column for the name of the genre.
         */
        public static final String COLUMN_NAME = "name";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_NAME + " TEXT UNIQUE NOT NULL " + ")";
    }

    /**
     * A representation of the schema for table of stores.
     */
    public static class StoreEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "store";

        /**
         * The name of the column for the name of the store.
         */
        public static final String COLUMN_NAME = "name";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_NAME + " TEXT UNIQUE NOT NULL " + ")";
    }

    /**
     * A representation of the schema for the table of currencies.
     */
    public static class CurrencyEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "currency";

        /**
         * The name of the column for the
         *  {@link <a href="https://www.iban.com/currency-codes">ISO 4217</a>} encoded currency code.
         */
        public static final String COLUMN_NAME = "name";

        /**
         * The name of the column for the currency rate of the specific currency.
         * By default they're PLN based.
         */
        public static final String COLUMN_RATE = "rate";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                COLUMN_NAME + " TEXT UNIQUE NOT NULL, " +
                COLUMN_RATE + " REAL " + ")";
    }

    /**
     * A representation of the schema for the table of purchases.
     */
    public static class PurchaseEntry implements BaseColumns {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "purchase";

        /**
         * The name of the column for the foreign key associated with the {@link AlbumEntry}{@code ._ID}.
         */
        public static final String COLUMN_ALBUM_ID = "album_id";

        /**
         * The name of the column for the foreign key associated with the {@link StoreEntry}{@code ._ID}.
         */
        public static final String COLUMN_STORE_ID = "store_id";

        /**
         * The name of the column for the price of the album.
         */
        public static final String COLUMN_PRICE = "price";

        /**
         * The name of the column for the foreign key associated with the {@link CurrencyEntry}{@code ._ID}.
         */
        public static final String COLUMN_CURRENCY_ID = "currency_id";

        /**
         * The name of the column for the date of the purchase.
         */
        public static final String COLUMN_DATE = "date";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            COLUMN_ALBUM_ID + " INTEGER NOT NULL, " +
            COLUMN_STORE_ID + " INTEGER, " +
               COLUMN_PRICE + " REAL NOT NULL, " +
         COLUMN_CURRENCY_ID + " INTEGER NOT NULL, " +
                COLUMN_DATE + " INTEGER, " +
            "FOREIGN KEY (" + COLUMN_ALBUM_ID + ") REFERENCES " + AlbumEntry.TABLE_NAME + "(" + AlbumEntry._ID + "), " +
            "FOREIGN KEY (" + COLUMN_STORE_ID + ") REFERENCES " + StoreEntry.TABLE_NAME + "(" + StoreEntry._ID + "), " +
            "FOREIGN KEY (" + COLUMN_CURRENCY_ID + ") REFERENCES " + CurrencyEntry.TABLE_NAME + "(" + CurrencyEntry._ID + "), " +
                 "UNIQUE (" + COLUMN_ALBUM_ID + ", " + COLUMN_STORE_ID + ", " + COLUMN_PRICE + ", " + COLUMN_CURRENCY_ID + ", " + COLUMN_DATE + ")" + ")";
    }

    /**
     * A representation of the schema for table of settings.
     */
    public static class SettingsEntry {
        /**
         * The name of the table.
         */
        public static final String TABLE_NAME = "settings";

        /**
         * The name of the column for the name of the setting key.
         */
        public static final String COLUMN_KEY = "key";

        /**
         * The name of the column for the name of the setting value.
         */
        public static final String COLUMN_VALUE = "value";

        /**
         * The setting key for the username.
         */
        public static final String KEY_USERNAME = "username";

        /**
         * The setting key for the chosen application language.
         */
        public static final String KEY_LANGUAGE = "language";

        /**
         * The setting key for the last date when the currency rates were updated.
         */
        public static final String KEY_RATE_UPDATE = "rates_update";

        /**
         * SQL statement used to create this table.
         */
        static final String SQL_SCHEMA =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                 COLUMN_KEY + " TEXT PRIMARY KEY NOT NULL, " +
               COLUMN_VALUE + " TEXT " + ")";

        /**
         * Default values for this table, supplied when the database is created for the first time.
         */
        static final String SQL_DEFAULT =
            "INSERT INTO " + TABLE_NAME + " " +
                "VALUES ('" + KEY_LANGUAGE + "', 1)";
    }
}
