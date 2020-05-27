package eu.tivian.musico.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * A representation of an individual song.
 */
public class Song implements Parcelable {
    /**
     * The title of the song.
     */
    public String title;

    /**
     * The duration of the song.
     */
    public Duration duration;

    /**
     * Mandatory static field for every class which implements the {@link Parcelable} interface.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Song createFromParcel(Parcel in) {
            return new Song(in);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };

    /**
     * Default constructor which initializes the duration with the value of {@link Duration#ZERO}.
     */
    public Song() {
        duration = Duration.ZERO;
    }

    /**
     * Constructor which initializes both fields of the {@link Song} object.
     *
     * @param title the title of the song.
     * @param duration the duration of the song.
     */
    public Song(String title, Duration duration) {
        this.title = title;
        this.duration = duration;
    }

    /**
     * A string representation of this song in a format "{@link #title} {@link #duration}".
     *
     * @return a string representation of this song title and duration.
     */
    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%-64s %-4s", title, duration);
    }

    /**
     * Constructor used for restoring the inner state from the {@link Parcel} object.
     *
     * @param in The Parcel from which the object should be read.
     */
    private Song(Parcel in) {
        this();

        title = in.readString();
        duration = in.readParcelable(Duration.class.getClassLoader());
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
        dest.writeString(title);
        dest.writeParcelable(duration, flags);
    }
}
