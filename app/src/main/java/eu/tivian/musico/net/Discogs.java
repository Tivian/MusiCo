package eu.tivian.musico.net;

import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import eu.tivian.musico.BuildConfig;
import eu.tivian.musico.data.Album;
import eu.tivian.musico.data.Duration;
import eu.tivian.musico.data.Song;
import eu.tivian.musico.utility.Utilities;

/**
 * Provides base information about albums, including the album artworks.
 *
 * @see <a href="https://www.discogs.com/developers">Discogs API reference</a>
 */
public final class Discogs extends Service {
    /**
     * Base URL for the Discogs API.
     */
    private static final String ROOT_URL = "https://api.discogs.com/";

    /**
     * Consumer key used to authenticate the application.
     */
    private static final String API_KEY = "ZqrAgkIyIWdwKGigDpcv";

    /**
     * Consumer secret used to authenticate the application.
     */
    private static final String API_SECRET = "KvaFisRmtOYqdmyayfbGABvbNkddytaK";

    /**
     * The Discord API requires the request to include valid user-agent which reflects the application name.
     */
    private static final String USER_AGENT = "MusiCo/" + BuildConfig.VERSION_NAME + " +https://tivian.github.io/musico";

    /**
     * Header format used for the authorization.
     */
    private static final String AUTH_STRING = String.format("Discogs key=%s, secret=%s", API_KEY, API_SECRET);

    /**
     * Regular expression used to validate if string is a valid EAN-13 or UPC-A barcode.
     */
    private static final String BARCODE_REGEX = "\\d{12,13}";

    /**
     * Available types of entries supported by query function.
     */
    public enum EntryType {
        /**
         * An album release entry.
         */
        release,

        /**
         * An album original release(master) entry.
         */
        master,

        /**
         * An artist entry.
         */
        artist,

        /**
         * An entry for record companies.
         */
        label
    }

    /**
     * Helps to construct query string for the search function.
     */
    public static class Query {
        /**
         * Raw query string.
         */
        public String query;

        /**
         * Type of the entry.
         */
        public EntryType type;

        /**
         * Title of the album.
         */
        public String title;

        /**
         * Title of the original release.
         */
        public String release_title;

        /**
         * Name of the artist.
         */
        public String artist;

        /**
         * Name of the record company.
         */
        public String label;

        /**
         * Genre associated with the requested album.
         */
        public String genre;

        /**
         * Style (it's more specific than genre) of the album.
         */
        public String style;

        /**
         * Country of the release.
         */
        public String country;

        /**
         * Year of the release.
         */
        public String year;

        /**
         * Format of the release (e.g. album or vinyl).
         */
        public String format;

        /**
         * EAN-13 or UPC-A barcode.
         */
        public String barcode;

        /**
         * Name of the track on the requested album.
         */
        public String track;

        /**
         * Parses the album into the query for the search function.
         *
         * @param album album to parse.
         * @return search query object.
         */
        public static Query from(Album album) {
            Query query = new Query();
            if (!TextUtils.isEmpty(album.artist))
                query.artist = album.artist;
            if (!TextUtils.isEmpty(album.title))
                query.title = query.release_title = album.title;
            if (album.year > 1900)
                query.year = Integer.toString(album.year);
            if (!TextUtils.isEmpty(album.genre))
                query.genre = album.genre;

            query.format = "album";
            query.type = EntryType.release;
            return query;
        }

        /**
         * Formats this object into valid search request string.
         *
         * @return search request string.
         */
        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (Field f : Query.class.getFields()) {
                try {
                    Object obj = f.get(this);
                    if (obj != null)
                        sb.append(f.getName()).append("=")
                            .append(Utilities.encodeURL(obj.toString())).append("&");
                } catch (IllegalAccessException ignored) {}
            }

            return sb.toString();
        }
    }

    /**
     * Single search entry.
     */
    public static class SearchResult {
        /**
         * Title of the entry.
         */
        public String title;

        /**
         * URL for the thumbnail of the entry.
         */
        public String thumbnail;

        /**
         * URL used to retrieve more detailed information about the entry.
         */
        public String url;

        /**
         * Private constructor to prevent instantiating from outside.
         */
        private SearchResult() {}
    }

    /**
     * Helper class to ensure thread-safe singleton creation.
     */
    private static class InstanceHolder {
        /**
         * Singleton instance.
         */
        private static final Discogs instance = new Discogs();
    }

    /**
     * Private constructor to prevent instantiating from outside.
     */
    private Discogs() {}

    /**
     * Returns the singleton.
     *
     * @return the singleton instance.
     */
    public static Discogs get() {
        return InstanceHolder.instance;
    }

    /**
     * Searches the Discogs database looking for the specified {@code query}.
     *
     * @param query search query.
     * @param action functor determining what should happen with received data.
     */
    public void search(Query query, Consumer<List<SearchResult>> action) {
        search(query, action, null);
    }

    /**
     * Searches the Discogs database looking for the specified {@code query}.
     *
     * @param query search query.
     * @param action functor determining what should happen with received data.
     * @param onError functor called only if any I/O errors occurred while retrieving information from the server.
     */
    public void search(Query query, Consumer<List<SearchResult>> action, Consumer<Exception> onError) {
        execute("database/search?" + query, this::parseSearch, action, onError);
    }

    /**
     * Gets information about the requested album.
     *
     * @param str album title or EAN-13 / UPC-A barcode.
     * @param action functor determining what should happen with received album information.
     */
    public void getAlbum(String str, Consumer<Album> action) {
        getAlbum(str, action, null);
    }

    /**
     * Gets information about the requested album.
     *
     * @param str album title or EAN-13 / UPC-A barcode.
     * @param action functor determining what should happen with received album information.
     * @param onError functor called only if any I/O errors occurred while retrieving information from the server.
     */
    public void getAlbum(String str, Consumer<Album> action, Consumer<Exception> onError) {
        Query query = new Query();
        if (str.matches(BARCODE_REGEX))
            query.barcode = str;
        else
            query.title = str;

        getAlbum(query, action, onError);
    }

    /**
     * Gets information about the requested album.
     *
     * @param query search query.
     * @param action functor determining what should happen with received album information.
     */
    public void getAlbum(Query query, Consumer<Album> action) {
        getAlbum(query, action, null);
    }

    /**
     * Gets information about the requested album.
     *
     * @param query search query.
     * @param action functor determining what should happen with received album information.
     * @param onError functor called only if any I/O errors occurred while retrieving information from the server.
     */
    public void getAlbum(Query query, Consumer<Album> action, Consumer<Exception> onError) {
        search(query, list -> {
            if (list.size() > 0)
                execute(list.get(0).url, this::parseAlbum, action, onError);
            else if (onError != null)
                onError.accept(null);
        }, onError);
    }

    /**
     *  Parses single song entry.
     *
     * @param reader JSON reader.
     * @return pair consisting of position of the song and actual information about the song.
     * @see <a href="https://www.discogs.com/developers#page:database,header:database-release">
     *      API reference</a>
     */
    private Pair<Integer, Song> parseSong(JsonReader reader) {
        Song track = new Song();
        int position = -1;
        boolean discard = false;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "title":
                        track.title = reader.nextString();
                        break;
                    case "duration":
                        track.duration = Duration.from(reader.nextString());
                        break;
                    case "position":
                        try {
                            position = Integer.parseInt(reader.nextString()) - 1;
                        } catch (NumberFormatException ignored) {}
                        break;
                    case "type_":
                        discard = reader.nextString().equals("heading");
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
        } catch (IOException ex) {
            return null;
        }

        return discard ? null : Pair.create(position, track);
    }

    /**
     * Parses the album entry.
     *
     * @param reader JSON reader.
     * @return parsed album.
     * @see <a href="https://www.discogs.com/developers#page:database,header:database-release">
     *      API reference</a>
     */
    private Album parseAlbum(JsonReader reader) {
        Album album = new Album();
        String name;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                name = reader.nextName();

                switch (name) {
                    case "artists":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                name = reader.nextName();

                                if (name.equals("name"))
                                    album.artist = reader.nextString();
                                else
                                    reader.skipValue();
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                        break;
                    case "title":
                        album.title = reader.nextString();
                        break;
                    case "year":
                        album.year = reader.nextInt();
                        break;
                    case "genres":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (TextUtils.isEmpty(album.genre))
                                album.genre = reader.nextString();
                            else
                                reader.skipValue();
                        }
                        reader.endArray();
                        break;
                    case "images":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                name = reader.nextName();

                                if (name.equals("uri") && album.coverUrl == null)
                                    album.coverUrl = reader.nextString();
                                else
                                    reader.skipValue();
                            }
                            reader.endObject();
                        }
                        reader.endArray();
                        break;
                    case "tracklist":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            Pair<Integer, Song> track = parseSong(reader);
                            if (track != null) {
                                album.tracks.add(track.first == null || track.first == -1
                                    ? album.tracks.size() : track.first, track.second);
                            }
                        }
                        reader.endArray();
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
            reader.endObject();
        } catch (IOException ex) {
            return null;
        }

        return album;
    }

    /**
     * Parses search result into the list of entries.
     *
     * @param reader JSON reader.
     * @return list of search result entries.
     * @see <a href="https://www.discogs.com/developers#page:database,header:database-search">
     *      API reference</a>
     */
    private List<SearchResult> parseSearch(JsonReader reader) {
        List<SearchResult> result = new ArrayList<>();
        String name;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                name = reader.nextName();

                if (name.equals("results")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        reader.beginObject();
                        SearchResult record = new SearchResult();

                        while (reader.hasNext()) {
                            name = reader.nextName();

                            switch (name) {
                                case "thumb":
                                    record.thumbnail = reader.nextString();
                                    break;
                                case "title":
                                    record.title = reader.nextString();
                                    break;
                                case "master_url":
                                    if (reader.peek() != JsonToken.NULL)
                                        record.url = reader.nextString();
                                    else
                                        reader.nextNull();
                                    break;
                                case "resource_url":
                                    if (record.url == null) {
                                        record.url = reader.nextString();
                                        break;
                                    }
                                default:
                                    reader.skipValue();
                                    break;
                            }
                        }

                        result.add(record);
                        reader.endObject();
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignored) {}

        return result;
    }

    /**
     * Connects to the server using the appropriate authentication method.
     *
     * @param req the URL to connect to.
     * @return input stream or {@code null}
     *         if server responded with code other than {@link HttpURLConnection#HTTP_OK}.
     * @see <a href="https://www.discogs.com/developers#page:authentication,header:authentication-discogs-auth-flow">
     *      API reference</a>
     */
    @Override
    protected InputStream connect(String req) {
        if (!req.contains(ROOT_URL))
            req = ROOT_URL + req;

        try {
            URL url = new URL(req);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", AUTH_STRING);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.connect();
            return (conn.getResponseCode() != HttpURLConnection.HTTP_OK) ?
                    null : conn.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
