package eu.tivian.musico.database;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import eu.tivian.musico.utility.SimpleTextWatcher;

/**
 * An autocomplete engine.
 */
public class DatabaseSuggestion implements SimpleTextWatcher {
    /**
     * SQL statement used for getting the suggestions from the database.
     */
    private static final String SQL_SUGGESTION_FORMAT =
        "SELECT DISTINCT %2$s FROM ( " +
            " SELECT %2$s, 1 AS ord FROM %1$s WHERE %2$s LIKE @starts_with " +
            " UNION " +
            " SELECT %2$s, 2 AS ord FROM %1$s WHERE %2$s LIKE @like " +
        ") ORDER BY ord";

    /**
     * Default number of suggestions.
     */
    private static final int DEFAULT_SIZE = 10;

    /**
     * Target view.
     */
    private AutoCompleteTextView textView;

    /**
     * The adapter containing the suggestions from the database.
     */
    private ArrayAdapter<String> adapter;

    /**
     * Object used to communicate with database.
     */
    private DatabaseAdapter databaseAdapter;

    /**
     * Determines which table is targeted by the autocomplete engine.
     */
    private String table;

    /**
     * Determines which column is targeted by the autocomplete engine.
     */
    private String column;

    /**
     * Determines the number of suggestions.
     */
    private int limit;

    /**
     * Used to determine if the value in the {@link #textView} changed.
     */
    private String last;

    /**
     * Creates the autocomplete engine.
     *
     * @param context the current context.
     * @param textView the target {@link AutoCompleteTextView}
     * @param table the target table.
     * @param column the target column in the {@code table}.
     */
    public DatabaseSuggestion(Context context, AutoCompleteTextView textView,
                              String table, String column) {
        this(context, textView, table, column, DEFAULT_SIZE);
    }

    /**
     * Creates the autocomplete engine.
     *
     * @param context the current context.
     * @param textView the target {@link AutoCompleteTextView}
     * @param table the target table.
     * @param column the target column in the {@code table}.
     * @param limit the number of suggestions to be displayed at once.
     */
    public DatabaseSuggestion(Context context, AutoCompleteTextView textView,
                              String table, String column, int limit) {
        this.textView = textView;
        this.table = table;
        this.column = column;
        this.limit = limit;

        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
        databaseAdapter = DatabaseAdapter.get();

        textView.addTextChangedListener(this);
        textView.setAdapter(adapter);

        update();
    }

    /**
     * Updates the suggestion list using the current text of {@link #textView}.
     */
    private void update() {
        update(textView.getText().toString());
    }

    /**
     * Updates the suggestion list using the supplied {@code text}.
     *
     * @param text text on which the suggestions will be based.
     */
    private void update(String text) {
        try (Cursor cursor = query(text)) {
            adapter.clear();

            for (int i = 0; i < limit && cursor.moveToNext(); i++)
                adapter.add(cursor.getString(0));
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Query the database for the list of the suggestions.
     *
     * @param text text on which the suggestions will be based.
     * @return the cursor with the list of the suggestions.
     */
    private Cursor query(String text) {
        return databaseAdapter.query(String.format(SQL_SUGGESTION_FORMAT, table, column), text + "%", "%" + text + "%");
    }

    /**
     * This method is called to notify you that, within {@code s}, the {@code count} characters
     * beginning at {@code start} have just replaced old text that had length {@code before}.
     *
     * @param s the text.
     * @param start the index of first character changed.
     * @param before the length of the text before change occurred.
     * @param count number of changed characters.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String text = s.toString();
        if (!text.equals(last)) {
            last = text;

            if (!TextUtils.isEmpty(text.trim()))
                update(text);
        }
    }
}
