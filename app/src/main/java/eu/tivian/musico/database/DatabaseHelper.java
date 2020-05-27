package eu.tivian.musico.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * A helper class to manage database creation and version management.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    /**
     * Current schema version for the database.
     */
    private static final int DATABASE_VERSION = 3;

    /**
     * The database file name.
     */
    private static final String DATABASE_NAME = "AlbumLibrary.db";


    /**
     * SQL statement used to drop tables.
     */
    private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS ";

    /**
     * The name of the static field in contract class {@link DatabaseContract}
     * used to find all table names present in the database.
     */
    private static final String SQL_TABLE_NAME = "TABLE_NAME";

    /**
     * The name of static fields in contract class {@link DatabaseContract}
     * used to initialize properly the database on creation.
     */
    private static final String[] SQL_FIELDS = { "SQL_SCHEMA", "SQL_DEFAULT" };

    /**
     * Create a helper object to create, open, and/or manage a database. This method always returns very quickly.
     * The database is not actually created or opened until one of {@link #getWritableDatabase()} is called.
     *
     * @param context to use for locating paths to the the database.
     */
    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * This is where the creation of tables and the initial population of the tables should happen.
     *
     * @param db the database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        for (Class c : DatabaseContract.class.getDeclaredClasses()) {
            for (String field : SQL_FIELDS) {
                try {
                    String sql = (String) c.getDeclaredField(field).get(null);
                    db.execSQL(sql);
                } catch (Exception ignored) {}
            }
        }

        for (String sql : DatabaseContract.SQL_STATEMENTS)
            db.execSQL(sql);
    }

    /**
     * Called when the database needs to be upgraded. The implementation should use this method to drop tables,
     * add tables, or do anything else it needs to upgrade to the new schema version.
     *
     * @param db the database.
     * @param oldVersion the old database version.
     * @param newVersion the new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (Class c : DatabaseContract.class.getDeclaredClasses()) {
            try {
                db.execSQL(SQL_DROP_TABLE + c.getDeclaredField(SQL_TABLE_NAME).get(null));
            } catch (Exception ignored) { }
        }

        onCreate(db);
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and {@link #onCreate(SQLiteDatabase)},
     * {@link #onUpgrade(SQLiteDatabase, int, int)} and/or {@link SQLiteOpenHelper#onOpen(SQLiteDatabase)} will be called.
     * <br>
     * This function enables the foreign keys check.
     *
     * @return a read/write database object valid until {@link SQLiteDatabase#close()} is called.
     */
    @Override
    public SQLiteDatabase getWritableDatabase() {
        SQLiteDatabase db = super.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys=ON;");
        return db;
    }
}
