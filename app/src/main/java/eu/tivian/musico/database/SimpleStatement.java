package eu.tivian.musico.database;

import android.database.sqlite.SQLiteStatement;

import androidx.annotation.NonNull;

/**
 * A simplified version of {@link SQLiteStatement} for retrieving single string value from the database.
 */
public class SimpleStatement {
    /**
     * The compiled SQL statement.
     */
    private final SQLiteStatement statement;

    /**
     * Constructs the object using the supplied SQL statement.
     *
     * @param sql SQL statement to compile.
     */
    public SimpleStatement(@NonNull String sql) {
        statement = DatabaseAdapter.get().getDb().compileStatement(sql);
    }

    /**
     * Returns the result of the compiled SQL query.
     *
     * @return result of the SQL query, or empty string if anything failed.
     */
    @NonNull
    public String get() {
        try {
            return statement.simpleQueryForString();
        } catch (Exception ex) {
            return "";
        }
    }
}
