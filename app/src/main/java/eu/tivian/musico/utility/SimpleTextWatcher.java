package eu.tivian.musico.utility;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * A simplified version of {@link TextWatcher} which requires only
 *  {@link TextWatcher#onTextChanged(CharSequence, int, int, int)} implementation.
 */
public interface SimpleTextWatcher extends TextWatcher {
    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * are about to be replaced by new text with length <code>after</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     */
    @Override
    default void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    /**
     * This method is called to notify you that, within <code>s</code>,
     * the <code>count</code> characters beginning at <code>start</code>
     * have just replaced old text that had length <code>before</code>.
     * It is an error to attempt to make changes to <code>s</code> from
     * this callback.
     */
    @Override
    default void afterTextChanged(Editable s) {}
}
