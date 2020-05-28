package eu.tivian.musico;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;

import eu.tivian.musico.database.AlbumCursor;
import eu.tivian.musico.database.DatabaseAdapter;
import eu.tivian.musico.utility.Utilities;

import static eu.tivian.musico.database.DatabaseContract.SettingsEntry;

/**
 * {@link ViewModel} for data which is used between different UI elements.
 */
public class SharedViewModel extends ViewModel {
    /**
     * The Last.fm username.
     */
    private MutableLiveData<String> username;

    /**
     * Current language of the app.
     */
    private MutableLiveData<Integer> language;

    /**
     * The album list cursor.
     */
    private MutableLiveData<AlbumCursor> cursor;

    /**
     * An alpha value of the {@link FloatingActionButton} on the album list screen.
     */
    private MutableLiveData<Float> fabAlpha = new MutableLiveData<>();

    /**
     * The list of the various statistics used in {@link eu.tivian.musico.ui.StatisticsFragment}.
     */
    private MutableLiveData<Map<String, Object>> stats = new MutableLiveData<>();

    /**
     * Sets the album list cursor.
     *
     * @param cursor new album list cursor.
     */
    public void setCursor(AlbumCursor cursor) {
        if (this.cursor.getValue() != null)
            this.cursor.getValue().close();
        this.cursor.setValue(cursor);
    }

    /**
     * Gets the album list cursor.
     *
     * @return the album list cursor.
     */
    public LiveData<AlbumCursor> getCursor() {
        if (cursor == null)
            cursor = new MutableLiveData<>(null);
        return cursor;
    }

    /**
     * Saves the Last.fm username into settings.
     *
     * @param username the Last.fm username.
     */
    public void setUsername(String username) {
        DatabaseAdapter.get().setSetting(SettingsEntry.KEY_USERNAME, username);
        this.username.setValue(username);
    }

    /**
     * Gets the saved Last.fm username from settings.
     *
     * @return the Last.fm username.
     */
    public LiveData<String> getUsername() {
        if (username == null)
            username = new MutableLiveData<>(
                    DatabaseAdapter.get().getSetting(SettingsEntry.KEY_USERNAME));
        return username;
    }

    /**
     * Saves the language into settings.
     *
     * @param language the language ID according to {@link Utilities#LANGUAGES}.
     */
    public void setLanguage(int language) {
        DatabaseAdapter.get().setSetting(SettingsEntry.KEY_LANGUAGE, String.valueOf(language));
        this.language.setValue(language);
    }

    /**
     * Gets the language ID from the settings.
     *
     * @return the language ID.
     */
    public LiveData<Integer> getLanguage() {
        if (language == null)
            language = new MutableLiveData<>(
                    Integer.valueOf(DatabaseAdapter.get().getSetting(SettingsEntry.KEY_LANGUAGE)));
        return language;
    }

    /**
     * Sets the alpha value of the {@link FloatingActionButton}
     *  on the album list screen.
     *
     * @param alpha the alpha value.
     */
    public void setFabAlpha(float alpha) {
        fabAlpha.setValue(alpha);
    }

    /**
     * Gets the alpha value of the {@link FloatingActionButton}
     *  on the album list screen.
     *
     * @return the alpha value.
     */
    public LiveData<Float> getFabAlpha() {
        return fabAlpha;
    }

    /**
     * Sets given statistic with given value.
     *
     * @param key a key of the statistic.
     * @param value a value to be saved at the given key.
     */
    public void setStat(String key, Object value) {
        Map<String, Object> map = (stats.getValue() == null) ? new HashMap<>() : stats.getValue();
        map.put(key, value);
        stats.setValue(map);
    }

    /**
     * Gets the list of the statistics.
     *
     * @return list of the statistics.
     */
    public LiveData<Map<String, Object>> getStats() {
        return stats;
    }
}
