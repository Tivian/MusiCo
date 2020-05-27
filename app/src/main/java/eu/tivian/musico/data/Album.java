package eu.tivian.musico.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class representation of a musical album.
 */
public class Album implements Parcelable {
    /**
     * ID of the album.
     * <br>
     * By default it's equals to -1 or actual ID from the database.
     */
    public long id;

    /**
     * Name of the artist which performs this album.
     * <br>
     * At current state only one artist per album is allowed.
     */
    public String artist;

    /**
     * Title of the album.
     */
    public String title;

    /**
     * Year of the original release of the album.
     * <br>
     * A valid year is considered to be between 1900 and 2100.
     */
    public int year;

    /**
     * Musical genre of the album.
     * <br>
     * Only one genre per album is allowed in this version.
     */
    public String genre;

    /**
     * A list of tracks.
     */
    public List<Song> tracks;

    /**
     * An URL of the cover for the album.
     * <br>
     * Valid only when adding new album using the {@link eu.tivian.musico.AlbumView}.
     */
    public String coverUrl;

    /**
     * A cover for the album saved in the JPEG format.
     */
    public byte[] cover;

    /**
     * A description of purchase of the album.
     */
    public Purchase purchase;

    /**
     * Class describing the purchase of the album.
     */
    public static class Purchase {
        /**
         * A date of the purchase.
         */
        public Date date;

        /**
         * Name of the store where the album was bought.
         */
        public String store;

        /**
         * A price of the album.
         */
        public double price;

        /**
         * Currency used when buying this album.
         */
        public String currency;

        /**
         * Private constructor, so only outer class can instantiate the {@link Purchase} object.
         */
        private Purchase() {}
    }

    /**
     * Mandatory static field for every class which implements the {@link Parcelable} interface.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Album createFromParcel(Parcel in) {
            return new Album(in);
        }

        public Album[] newArray(int size) {
            return new Album[size];
        }
    };

    /**
     * Default constructor.
     * Initialize the public fields with default values.
     */
    public Album() {
        id = -1;
        tracks = new ArrayList<>();
        purchase = new Purchase();
    }

    /**
     * Returns total duration of the album.
     *
     * @return total duration of the album.
     */
    public Duration getDuration() {
        Duration duration = Duration.ZERO;
        for (Song s : tracks)
            duration = duration.add(s.duration == null ? Duration.ZERO : s.duration);
        return duration;
    }

    /**
     * Constructor used for restoring the inner state from the {@link Parcel} object.
     *
     * @param in The Parcel from which the object should be read.
     */
    private Album(Parcel in) {
        this();

        id = in.readLong();
        artist = in.readString();
        title = in.readString();
        year = in.readInt();
        genre = in.readString();
        in.readList(tracks, Song.class.getClassLoader());
        coverUrl = in.readString();
        int size = in.readInt();
        if (size > 0) {
            cover = new byte[size];
            in.readByteArray(cover);
        }
        purchase.date = (Date) in.readSerializable();
        purchase.store = in.readString();
        purchase.price = in.readDouble();
        purchase.currency = in.readString();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable instance's marshaled representation.
     * For example, if the object will include a file descriptor in the output of {@link Parcelable#writeToParcel(Parcel, int)},
     * the return value of this method must include the {@link Parcelable#CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *        May be 0 or {@link Parcelable#PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(artist);
        dest.writeString(title);
        dest.writeInt(year);
        dest.writeString(genre);
        dest.writeList(tracks);
        dest.writeString(coverUrl);
        dest.writeInt(cover == null ? 0 : cover.length);
        if (cover != null)
            dest.writeByteArray(cover);
        dest.writeSerializable(purchase.date);
        dest.writeString(purchase.store);
        dest.writeDouble(purchase.price);
        dest.writeString(purchase.currency);
    }
}
