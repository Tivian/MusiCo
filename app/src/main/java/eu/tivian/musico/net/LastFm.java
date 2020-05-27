package eu.tivian.musico.net;

import android.util.JsonReader;

import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;

import eu.tivian.musico.utility.IconComparator;

/**
 * Provides information about users music listening habits.
 *
 * @see <a href="https://www.last.fm/api/">Last.fm API reference</a>
 */
public final class LastFm extends Service {
    /**
     * Base URl for the Last.fm API.
     */
    private static final String ROOT_URL = "https://ws.audioscrobbler.com/2.0/";

    /**
     * API key used for the request authorization.
     */
    private static final String API_KEY = "6af32b424749772ed38ec6013175da6a";

    /**
     * Sets the output format of API to JSON.
     */
    private static final String FORMAT = "&format=json";

    /**
     * Last.fm user encapsulation.
     */
    public static class User {
        /**
         * Number of played songs.
         */
        public long playCount;

        /**
         * Name of the user.
         */
        public String name;

        /**
         * Country, if given, of the user.
         */
        public String country;

        /**
         * Real name, if given, of the user.
         */
        public String realName;

        /**
         * Creation date of Last.fm user.
         */
        public Date since;

        /**
         * User icons, in couple size variations.
         */
        public Map<String, String> icon;

        /**
         * External URL for the given user.
         */
        public String url;

        /**
         * Default constructor, which initializes the fields with default values.
         */
        private User() {
            icon = new TreeMap<>(new IconComparator());
        }
    }

    /**
     * An artist encapsulation.
     */
    public static class Artist {
        /**
         * Name of the artist.
         */
        public String name;

        /**
         * Icon art for the given artist, in different sizes.
         */
        public Map<String, String> icon;

        /**
         * If received from ranking query then this value denotes the order of artist in the ranking.
         */
        public int rank;

        /**
         * Number of plays.
         */
        public int playCount;

        /**
         * External URL to the artist page.
         */
        public String url;

        /**
         * Default constructor, which initializes the fields with default values.
         */
        private Artist() {
            icon = new TreeMap<>(new IconComparator());
            rank = -1;
        }
    }

    /**
     * Encapsulation for single play of a song.
     */
    public static class Track {
        /**
         * Title of the song.
         */
        public String title;

        /**
         * Name of the artist who performs this song.
         */
        public String artist;

        /**
         * Name of the album which contains this song.
         */
        public String album;

        /**
         * Cover art for the containing album, in different sizes.
         */
        public Map<String, String> icon;

        /**
         * The time when the song was played.
         * {@code null} if {@link #nowPlaying} is {@code true}.
         */
        public Date date;

        /**
         * External URL to the song page.
         */
        public String url;

        /**
         * Determines if this song is played by the user right now.
         * If {@code true} then {@link #date} is {@code null}.
         */
        public boolean nowPlaying;

        /**
         * Default constructor, which initializes the fields with default values.
         */
        private Track() {
            icon = new TreeMap<>(new IconComparator());
            nowPlaying = false;
        }
    }

    /**
     * Information about performed query.
     */
    public static class QueryInfo {
        /**
         * Current page.
         */
        public int page;

        /**
         * Total number of entries.
         */
        public int total;

        /**
         * Total number of pages.
         */
        public int totalPages;

        /**
         * Private constructor to prevent instantiating from outside.
         */
        private QueryInfo() {}
    }

    /**
     * Helper class to ensure thread-safe singleton creation.
     */
    private static class InstanceHolder {
        /**
         * Singleton instance.
         */
        private static final LastFm instance = new LastFm();
    }

    /**
     * Private constructor to prevent instantiating from outside.
     */
    private LastFm() {}

    /**
     * Returns the singleton.
     *
     * @return the singleton instance.
     */
    public static LastFm get() {
        return InstanceHolder.instance;
    }

    /**
     * Gets information about given Last.fm user.
     *
     * @param username name of the Last.fm account.
     * @param action functor which determines what should happen with received user info.
     * @see <a href="https://www.last.fm/api/show/user.getInfo">
     *      API reference</a>
     */
    public void getUser(String username, Consumer<User> action) {
        execute("method=user.getinfo&user=" + username, reader -> {
            User user = new User();
            user.name = username;
            String name;

            try {
                reader.beginObject();
                if (reader.nextName().equals("user")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        name = reader.nextName();

                        switch (name) {
                            case "playcount":
                                user.playCount = reader.nextLong();
                                break;
                            case "name":
                                user.name = reader.nextString();
                                break;
                            case "country":
                                user.country = reader.nextString();
                                break;
                            case "realname":
                                user.realName = reader.nextString();
                                break;
                            case "url":
                                user.url = reader.nextString();
                                break;
                            case "registered":
                                user.since = parseDate(reader, "unixtime");
                                break;
                            case "image":
                                user.icon.putAll(parseImage(reader));
                                break;
                            default:
                                reader.skipValue();
                                break;
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            } catch (IOException ignored) {}

            return user;
        }, action);
    }

    /**
     * Gets the list of 50 most played artist for given user.
     *
     * @param username name of the Last.fm user.
     * @param action functor which determines what should happen with received list of most played artists.
     * @see <a href="https://www.last.fm/api/show/user.getTopArtists">
     *      API reference</a>
     */
    public void getTopArtists(String username, Consumer<Pair<QueryInfo, List<Artist>>> action) {
        getTopArtists(username, 50, action);
    }

    /**
     * Gets the list of most played artist for given user.
     *
     * @param username name of the Last.fm user.
     * @param limit number of artists to receive.
     * @param action functor which determines what should happen with received list of most played artists.
     * @see <a href="https://www.last.fm/api/show/user.getTopArtists">
     *      API reference</a>
     */
    public void getTopArtists(String username, int limit, Consumer<Pair<QueryInfo, List<Artist>>> action) {
        execute("method=user.gettopartists&user=" + username + "&limit=" + limit, reader -> {
            List<Artist> list = new ArrayList<>();
            QueryInfo info = null;
            String name;

            try {
                reader.beginObject();
                if (reader.nextName().equals("topartists")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        name = reader.nextName();

                        if (name.equals("artist")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                reader.beginObject();
                                Artist artist = new Artist();

                                while (reader.hasNext()) {
                                    switch (reader.nextName()) {
                                        case "name":
                                            artist.name = reader.nextString();
                                            break;
                                        case "playcount":
                                            artist.playCount = reader.nextInt();
                                            break;
                                        case "url":
                                            artist.url = reader.nextString();
                                            break;
                                        case "image":
                                            artist.icon.putAll(parseImage(reader));
                                            break;
                                        case "@attr":
                                            reader.beginObject();
                                            while (reader.hasNext()) {
                                                if (reader.nextName().equals("rank"))
                                                    artist.rank = reader.nextInt();
                                                else
                                                    reader.skipValue();
                                            }
                                            reader.endObject();
                                            break;
                                        default:
                                            reader.skipValue();
                                    }
                                }

                                list.add(artist);
                                reader.endObject();
                            }
                            reader.endArray();
                        } else if (name.equals("@attr")) {
                            info = parseQueryInfo(reader);
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            } catch (IOException ignored) {}

            Collections.sort(list, (a, b) -> a.rank - b.rank);
            return Pair.create(info, list);
        }, action);
    }

    /**
     * Gets the list of recent played tracks by given user.
     *
     * @param username name of the Last.fm user.
     * @param action functor which determines what should happen with received list of recent played tracks.
     * @see <a href="https://www.last.fm/api/show/user.getRecentTracks">
     *      API reference</a>
     */
    public void getRecentTracks(String username, Consumer<Pair<QueryInfo, List<Track>>> action) {
        execute("method=user.getrecenttracks&user=" + username, reader -> {
            List<Track> tracks = new ArrayList<>();
            QueryInfo info = null;
            String name;

            try {
                reader.beginObject();
                if (reader.nextName().equals("recenttracks")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        name = reader.nextName();

                        if (name.equals("track")) {
                            reader.beginArray();
                            while (reader.hasNext())
                                tracks.add(parseTrack(reader));
                            reader.endArray();
                        } else if (name.equals("@attr")) {
                            info = parseQueryInfo(reader);
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                }
                reader.endObject();
            } catch (IOException ignore) {}

            return Pair.create(info, tracks);
        }, action);
    }

    /**
     * Parses single value for given tag of JSON object. Rest of the JSON object is ignored.
     *
     * @param reader JSON reader.
     * @param tag the name of the tag we're looking for.
     * @return the value for given {@code tag}.
     */
    private String parseSingle(JsonReader reader, String tag) {
        String value = null;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals(tag))
                    value = reader.nextString();
                else
                    reader.skipValue();
            }
            reader.endObject();
        } catch (IOException ignore) {}

        return value;
    }

    /**
     * Parses query info.
     *
     * @param reader JSON reader.
     * @return information about current query.
     */
    private QueryInfo parseQueryInfo(JsonReader reader) {
        QueryInfo info = new QueryInfo();

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "page":
                        info.page = reader.nextInt();
                        break;
                    case "total":
                        info.total = reader.nextInt();
                        break;
                    case "totalPages":
                        info.totalPages = reader.nextInt();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignored) {}

        return info;
    }

    /**
     * Parses the "image" JSON object.
     *
     * @param reader JSON reader.
     * @return a dictionary of URL images associated with their sizes.
     */
    private Map<String, String> parseImage(JsonReader reader) {
        Map<String, String> map = new HashMap<>();

        try {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();

                String size = "";
                String url = null;

                while (reader.hasNext()) {
                    String name = reader.nextName();

                    if (name.equals("size")) {
                        size = reader.nextString();
                    } else if (name.equals("#text")) {
                        url = reader.nextString();
                    } else {
                        reader.skipValue();
                    }
                }

                map.put(size, url);
                reader.endObject();
            }
            reader.endArray();
        } catch (IOException ignore) {}

        return map;
    }

    /**
     * Parses date from unix epoch format.
     *
     * @param reader JSON reader.
     * @param timeTag the JSON tag name to look for.
     * @return date object parsed from the unix epoch.
     */
    private Date parseDate(JsonReader reader, String timeTag) {
        Date date = null;

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.nextName().equals(timeTag))
                    date = new Date(reader.nextLong() * 1000);
                else
                    reader.skipValue();
            }
            reader.endObject();
        } catch (IOException ignore) {}

        return date;
    }

    /**
     * Parses single track entry.
     *
     * @param reader JSON reader.
     * @return track information.
     */
    private Track parseTrack(JsonReader reader) {
        Track track = new Track();

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "name":
                        track.title = reader.nextString();
                        break;
                    case "artist":
                        track.artist = parseSingle(reader, "#text");
                        break;
                    case "album":
                        track.album = parseSingle(reader, "#text");
                        break;
                    case "image":
                        track.icon.putAll(parseImage(reader));
                        break;
                    case "date":
                        track.date = parseDate(reader, "uts");
                        break;
                    case "url":
                        track.url = reader.nextString();
                        break;
                    case "@attr":
                        reader.beginObject();
                        while (reader.hasNext()) {
                            if (reader.nextName().equals("nowplaying"))
                                track.nowPlaying = reader.nextString().equals("true");
                            else
                                reader.skipValue();
                        }
                        reader.endObject();
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignore) {}

        return track;
    }

    /**
     * Connects to the Last.fm server, using authorization with appropriate API key.
     *
     * @param req the URL to connect to.
     * @return input stream or {@code null} if server didn't responded
     *         with {@link HttpURLConnection#HTTP_OK} code.
     */
    @Override
    protected InputStream connect(String req) {
        try {
            URL url = new URL(ROOT_URL + "?" + req + "&api_key=" + API_KEY + FORMAT);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.connect();
            return (conn.getResponseCode() != HttpURLConnection.HTTP_OK) ?
                    null : conn.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
