package eu.tivian.musico.utility;

import android.view.View;
import android.widget.AutoCompleteTextView;

/**
 * Helper class which makes autosuggestion popup when the view get the focus.
 */
public class SuggestionShower implements View.OnFocusChangeListener {
    /**
     * Shows or hides the autocomplete suggestions based on the state of the focus for the view.
     *
     * @param v the view whose state has changed.
     * @param hasFocus the new focus state of v.
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof AutoCompleteTextView) {
            if (hasFocus)
                ((AutoCompleteTextView) v).showDropDown();
            else
                ((AutoCompleteTextView) v).dismissDropDown();
        }
    }
}
