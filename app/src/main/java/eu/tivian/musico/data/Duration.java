package eu.tivian.musico.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An amount of time consisted of minutes and seconds.
 */
public class Duration implements Parcelable {
    /**
     * Regular expression used to convert {@link String} into the {@link Duration} object.
     */
    private static final Pattern REGEX = Pattern.compile("(\\d{1,2}):(\\d{2})$");

    /**
     * An amount of minutes.
     */
    private final int minutes;

    /**
     * An amount of seconds.
     * <br>
     * Valid value is between 0 and 59.
     */
    private final int seconds;

    /**
     * Mandatory static field for every class which implements the {@link Parcelable} interface.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Duration createFromParcel(Parcel in) {
            return new Duration(in);
        }

        public Duration[] newArray(int size) {
            return new Duration[size];
        }
    };

    /**
     * Constant for a duration of zero.
     */
    public static final Duration ZERO = new Duration(0, 0);

    /**
     * Private constructor which sets both - minutes and seconds.
     *
     * @param minutes an amount of minutes.
     * @param seconds an amount of seconds.
     */
    private Duration(int minutes, int seconds) {
        this.minutes = minutes;
        this.seconds = seconds;
    }

    /**
     * Adds this duration to the another object.
     *
     * @param another an object used for the addition.
     * @return an object of the same type with the adjustment made, not null
     */
    public Duration add(Duration another) {
        return another == null ? add(0, 0) : add(another.minutes, another.seconds);
    }

    /**
     * Adds to this duration specified amount of minutes and seconds.
     *
     * @param minutes an amount of minutes to add.
     * @param seconds an amount of seconds to add.
     * @return an object of the same type with the adjustment made, not null
     */
    public Duration add(int minutes, int seconds) {
        long sec = toSeconds() + seconds + minutes * 60;
        return new Duration((int) (sec / 60), (int) (sec % 60));
    }

    /**
     * Converts the duration object into equivalent time in minutes.
     *
     * @return a total amount of minutes described by this duration.
     */
    public double toMinutes() {
        return minutes + seconds / 60.0;
    }

    /**
     * Converts the duration object into equivalent time in seconds.
     *
     * @return a total amount of seconds described by this duration.
     */
    public long toSeconds() {
        return minutes * 60 + seconds;
    }

    /**
     * A string representation equivalent to MM:ss.
     *
     * @return a string representation of this duration.
     */
    @NonNull
    @Override
    public String toString() {
        return minutes == 0 && seconds == 0 ?
            "" : String.format(Locale.getDefault(), "%2d:%02d", minutes, seconds);
    }

    /**
     * Parses the supplied string into the {@link Duration} object.
     *
     * @param string the text to parse
     * @return {@link #ZERO} if conversion was unsuccessful, valid duration object otherwise
     */
    public static Duration from(String string) {
        Matcher matcher = REGEX.matcher(string);
        if (matcher.find()) {
            String minutes = matcher.group(1);
            String seconds = matcher.group(2);

            if (minutes != null && seconds != null)
                return new Duration(Integer.parseInt(minutes), Integer.parseInt(seconds));
        }

        return ZERO;
    }

    /**
     * Constructor used for restoring the inner state from the {@link Parcel} object.
     *
     * @param in The Parcel from which the object should be read.
     */
    private Duration(Parcel in) {
        minutes = in.readInt();
        seconds = in.readInt();
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
        dest.writeInt(minutes);
        dest.writeInt(seconds);
    }
}
