package eu.tivian.musico.net;

import android.util.JsonReader;
import android.util.Size;

import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import eu.tivian.musico.utility.SizeComparator;
import eu.tivian.musico.utility.Utilities;

/**
 * Provides information about artists and albums. Currently used only for artist icons and album covers.
 *
 * @see <a href="https://developer.spotify.com/documentation/web-api/reference/">Spotify API reference</a>
 */
public class Spotify extends Service {
    /**
     * Base URL for the Spotify API.
     */
    private static final String ROOT_URL = "https://api.spotify.com/v1/";

    /**
     * URL used to acquire the authorization token.
     */
    private static final String AUTH_URL = "https://accounts.spotify.com/api/token";

    /**
     * API key used to authenticate the application.
     */
    private static final String API_KEY =
        "MjVjZjM3ZTJiOTNiNDA5YWJiODgwOTkxYmIzN2U4MGI6ZmU3YzA2YzhjM2JkNGE3NTk0ZTFjZDBmNjI1YjYzMDE=";

    /**
     * Type of the authorization when acquired the authorization token.
     */
    private static final String AUTH_TYPE = "Basic";

    /**
     * Type of the authorization for the acquired token.
     */
    private static final String TOKEN_TYPE = "Bearer";

    /**
     * Available query types.
     */
    public enum QueryType {
        /**
         * An album query.
         */
        album,

        /**
         * An artist info query.
         */
        artist
    }

    /**
     * Cached authorization token.
     */
    private String token;

    /**
     * Expiration date of the cached token.
     * It is save in Unix epoch format.
     */
    private long expire;

    /**
     * A dictionary with cached urls to minimize number of request to the actual server.
     */
    private Map<String, String> cache;

    /**
     * Helper class to ensure thread-safe singleton creation.
     */
    private static class InstanceHolder {
        /**
         * Singleton instance.
         */
        private static final Spotify instance = new Spotify();
    }

    /**
     * Private constructor, which initializes fields with default values.
     */
    private Spotify() {
        token = null;
        expire = -1;
        cache = new HashMap<>();
    }

    /**
     * Returns the singleton.
     *
     * @return the singleton instance.
     */
    public static Spotify get() {
        return InstanceHolder.instance;
    }

    /**
     * Gets the URL of the album cover, which matches given query.
     * Uses the cache if the identical request was made in the pas.
     *
     * @param query search query.
     * @param action functor which determines what should happen with received cover art URL.
     */
    public void getCover(String query, Consumer<String> action) {
        String url = cache.get(query);
        if (url == null)
            getImage(query, QueryType.album, map -> action.accept(cacheQuery(query, map)));
        else
            action.accept(url);
    }

    /**
     * Gets the URL of the artist icon, which matches given query.
     * Uses the cache if the identical request was made in the pas.
     *
     * @param query search query.
     * @param action functor which determines what should happen with received artist icon URL.
     */
    public void getArtistImage(String query, Consumer<String> action) {
        String url = cache.get(query);
        if (url == null)
            getImage(query, QueryType.artist, map -> action.accept(cacheQuery(query, map)));
        else
            action.accept(url);
    }

    /**
     * Gets the URL of the requested image, which matches given query.
     *
     * @param query search query.
     * @param type type of the search request.
     * @param action functor which determines what should happen with received image URL.
     */
    public void getImage(String query, QueryType type, Consumer<Map<Size, String>> action) {
        execute("search?q=" + query + "&type=" + type.toString(), this::parseSearch, action);
    }

    /**
     * Parses search result.
     *
     * @param reader JSON reader.
     * @return a dictionary of images and their sizes.
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/search/search/">
     *      API reference</a>
     */
    private Map<Size, String> parseSearch(JsonReader reader) {
        Map<Size, String> map = new TreeMap<>(new SizeComparator());

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("artists") || name.equals("albums")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if (reader.nextName().equals("items")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                if (map.size() == 0) {
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        if (reader.nextName().equals("images")) {
                                            reader.beginArray();
                                            while (reader.hasNext()) {
                                                Pair<Size, String> pair = parseImage(reader);
                                                map.put(pair.first, pair.second);
                                            }
                                            reader.endArray();
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                } else {
                                    reader.skipValue();
                                }
                            }
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignored) {}

        return map;
    }

    /**
     * Parses single image.
     *
     * @param reader JSON reader.
     * @return a pair consisting of size of the image and the URL to this image.
     * @see <a href="https://developer.spotify.com/documentation/web-api/reference/object-model/#image-object">
     *      API reference</a>
     */
    private Pair<Size, String> parseImage(JsonReader reader) {
        String url = null;
        int width = 0;
        int height = 0;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "url":
                        url = reader.nextString();
                        break;
                    case "width":
                        width = reader.nextInt();
                        break;
                    case "height":
                        height = reader.nextInt();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignored) {}

        return Pair.create(new Size(width, height), url);
    }

    /**
     * Caches the query with its result to minimize the number of outgoing requests.
     *
     * @param query search query.
     * @param result search result.
     * @return most relevant search result.
     */
    private String cacheQuery(String query, Map<Size, String> result) {
        String url = (result != null && result.size() > 0) ?
                result.values().iterator().next() : null;
        cache.put(query, url);
        return url;
    }

    /**
     * Gets the authorization token.
     *
     * @see <a href="https://developer.spotify.com/documentation/general/guides/authorization-guide/">
     *      API reference</a>
     */
    private void getToken() {
        try {
            URL url = new URL(AUTH_URL + "?grant_type=client_credentials");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", AUTH_TYPE + " " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();

            JsonReader reader = Utilities.getReader(conn);
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "access_token":
                        token = reader.nextString();
                        break;
                    case "token_type":
                        if (!reader.nextString().equals(TOKEN_TYPE))
                            throw new IOException("Invalid token");
                        break;
                    case "expires_in":
                        expire = Utilities.getTime() + reader.nextLong();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();

            conn.disconnect();
        } catch (IOException ex) {
            token = null;
            expire = -1;
        }
    }

    /**
     * Connects to the server using the authorization token.
     * If token was never acquired or expired then this function also gets the token.
     * It does so in thread-safe manner.
     *
     * @param req the URL to connect to.
     * @return input stream or {@code null} if response code
     *         was other than {@link HttpURLConnection#HTTP_OK}.
     */
    @Override
    protected InputStream connect(String req) {
        try {
            synchronized (this) {
                if (token == null || expire < Utilities.getTime())
                    getToken();
            }

            URL url = new URL(ROOT_URL + req.replace(' ', '+'));
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", TOKEN_TYPE + " " + token);
            conn.connect();

            return (conn.getResponseCode() != HttpURLConnection.HTTP_OK) ?
                    null : conn.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
