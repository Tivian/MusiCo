package eu.tivian.musico.utility;

import android.text.TextUtils;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Locale;

/**
 * {@link android.text.TextWatcher} which is tailored to handle validation of date text input.
 *
 * @see <a href="https://stackoverflow.com/a/16889503">Source</a>
 */
public class DateWatcher implements SimpleTextWatcher {
    /**
     * {@link EditText} which should be watched.
     */
    private EditText date;
    /**
     * Cached last value of the watched view.
     */
    private String current = "";
    /**
     * Expected format of the text input.
     */
    private String format = "DDMMYYYY";

    /**
     * Creates watcher over the given {@link EditText}.
     *
     * @param date view to be watched.
     */
    public DateWatcher(EditText date) {
        this.date = date;

        if (TextUtils.isEmpty(date.getText()))
            onTextChanged(" ", 0, 0, 1);
    }

    /**
     * Watches the text changes and validates the input whether is a valid date.
     *
     * @param s the text.
     * @param start the index of first character changed.
     * @param before the length of the text before change occurred.
     * @param count number of changed characters.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (!s.toString().equals(current)) {
            String clean = s.toString().replaceAll("[^\\d.]|\\.", "");
            String cleanC = current.replaceAll("[^\\d.]|\\.", "");
            Calendar cal = Calendar.getInstance();

            int cl = clean.length();
            int sel = cl;
            for (int i = 2; i <= cl && i < 6; i += 2)
                sel++;

            //Fix for pressing delete next to a forward slash
            if (clean.equals(cleanC))
                sel--;

            if (clean.length() < 8) {
                clean = clean + format.substring(clean.length());
            } else {
                //This part makes sure that when we finish entering numbers
                //the date is correct, fixing it otherwise
                int day  = Integer.parseInt(clean.substring(0, 2));
                int mon  = Integer.parseInt(clean.substring(2, 4));
                int year = Integer.parseInt(clean.substring(4, 8));

                mon = mon < 1 ? 1 : Math.min(mon, 12);
                cal.set(Calendar.MONTH, mon - 1);
                year = (year < 1900) ? 1900: Math.min(year, 2100);
                cal.set(Calendar.YEAR, year);
                // ^ first set year for the line below to work correctly
                //with leap years - otherwise, date e.g. 29/02/2012
                //would be automatically corrected to 28/02/2012

                day = Math.min(day, cal.getActualMaximum(Calendar.DATE));
                clean = String.format(Locale.getDefault(), "%02d%02d%02d",day, mon, year);
            }

            clean = String.format("%s/%s/%s", clean.substring(0, 2),
                    clean.substring(2, 4),
                    clean.substring(4, 8));

            sel = Math.max(sel, 0);
            current = clean;
            date.setText(current);
            date.setSelection(Math.min(sel, current.length()));
        }
    }
}
