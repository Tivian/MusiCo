package eu.tivian.musico.utility;

import android.view.View;

/**
 * Custom {@link android.view.View.OnClickListener} for any view lacking this feature.
 * @param <T> the type of the item which should be passed to the event handler.
 */
public interface ItemClickListener<T> {
    /**
     * Called when a view has been clicked.
     *
     * @param view the view that was clicked.
     * @param item item to be passed to the event handler.
     */
    void onItemClick(View view, T item);
}
