package eu.tivian.musico.utility;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Variety of helping utility functions.
 */
public class Utilities {
    /**
     * Date format used for displaying date on an album list.
     */
    private static final String DATE_PATTERN = "dd/MM/yy";
    /**
     * The format used to save the cover arts.
     */
    private static final Bitmap.CompressFormat DEFAULT_FORMAT = Bitmap.CompressFormat.JPEG;
    /**
     * The default quality for the JPEG saved cover arts.
     */
    private static final int DEFAULT_QUALITY = 80;

    /**
     * Gets the dimensions of the phone screen.
     *
     * @param activity currency activity.
     * @return the dimension of the screen.
     */
    public static Point getScreenDim(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return new Point(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    /**
     * Gets the relative position of the given view.
     *
     * @param view a view.
     * @return the relative position of the view.
     */
    public static Point getRelativePoint(View view) {
        return new Point(getRelativeLeft(view), getRelativeTop(view));
    }

    /**
     * Gets the relative X position of the given view.
     *
     * @param view a view.
     * @return the relative X position of the view.
     */
    public static int getRelativeLeft(View view) {
        return view.getLeft() +
            ((view.getParent() == view.getRootView()) ?
                    0 :
                    getRelativeLeft((View) view.getParent()));
    }

    /**
     * Gets the relative Y position of the given view.
     *
     * @param view a view.
     * @return the relative Y position of the view.
     */
    public static int getRelativeTop(View view) {
        return view.getTop() +
            ((view.getParent() == view.getRootView()) ?
                    0 :
                    getRelativeTop((View) view.getParent()));
    }

    /**
     * Parses the string into the {@link Date} object.
     *
     * @param date the text to parse.
     * @param pattern the pattern to be used for parsing.
     * @return parsed date or {@code null} if the format was invalid.
     */
    public static Date parseDate(String date, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(date);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Formats the date using default date format.
     *
     * @param date date to be formatted.
     * @return a string formatted as "dd/MM/yy" or
     *         {@code null} if {@code date} was {@code null}.
     */
    public static String toString(Date date) {
        return toString(date, DATE_PATTERN);
    }

    /**
     * Formats the date using given date format.
     *
     * @param date date to be formatted.
     * @param pattern the date format used to format the date.
     * @return a formatted string or {@code null} if either
     *         {@code date} or {@code pattern} was {@code null}.
     */
    public static String toString(Date date, String pattern) {
        return date == null || pattern == null ? null
                : new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    /**
     * Gets current time in milliseconds.
     *
     * @return the current time in milliseconds.
     */
    public static long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     * Converts given {@link Bitmap} into JPEG encoded raw {@link Byte} array.
     *
     * @param bitmap bitmap to be converted.
     * @return JPEG encoded array.
     */
    public static byte[] getBytes(Bitmap bitmap) {
        return getBytes(bitmap, DEFAULT_QUALITY);
    }

    /**
     * Converts given {@link Bitmap} into JPEG encoded raw {@link Byte} array with given quality.
     *
     * @param bitmap bitmap to be converted.
     * @param quality the quality of the generated JPEG.
     * @return JPEG encoded array.
     */
    public static byte[] getBytes(Bitmap bitmap, int quality) {
        if (bitmap == null)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(DEFAULT_FORMAT, quality, stream);
        return stream.toByteArray();
    }

    /**
     * Gets the bitmap from the {@link ImageView}.
     *
     * @param imageView the image view.
     * @return the bitmap from the {@link ImageView}.
     */
    public static Bitmap getBitmap(ImageView imageView) {
        if (imageView == null)
            return  null;

        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        return drawable == null ? null : drawable.getBitmap();
    }

    /**
     * Gets the color of center pixel of given {@link Bitmap}.
     *
     * @param bitmap the bitmap.
     * @return the color of center pixel.
     */
    public static int getCenterPixel(Bitmap bitmap) {
        return bitmap.getPixel(bitmap.getWidth() / 2, bitmap.getHeight() / 2);
    }

    /**
     * Decides whether black or white color is more visible on given background.
     *
     * @param background the color of the background
     * @return {@link Color#WHITE} or {@link Color#BLACK} depending
     *  which color is more visible on the background.
     * @see <a href="https://en.wikipedia.org/wiki/Luma_(video)#Rec._601_luma_versus_Rec._709_luma_coefficients">
     *      Formula reference</a>
     */
    public static int getContrastColor(int background) {
        int red   = (background & 0xFF0000) >> 16;
        int green = (background & 0x00FF00) >> 8;
        int blue  = (background & 0x0000FF);
        return (red * 0.299 + green * 0.587 + blue * 0.114) > 150 ? Color.BLACK : Color.WHITE;
    }

    /**
     * List of supported languages.
     */
    public static final String[] LANGUAGES = { "en", "pl" };

    /**
     * Sets the locale of the app to desired language.
     *
     * @param context current context.
     * @param id id of the language (according to the {@link #LANGUAGES}.
     */
    public static void setLocale(Activity context, int id) {
        setLocale(context, LANGUAGES[id]);
    }

    /**
     * Sets the locale of the app to desired language.
     *
     * @param context current context.
     * @param lang locale string (should be
     *        {@link <a href="https://en.wikipedia.org/wiki/ISO_639">ISO 639</a>} encoded.
     */
    @SuppressWarnings("deprecation")
    public static void setLocale(Activity context, String lang) {
        if (context == null || TextUtils.isEmpty(lang))
            return;

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Resources appRes = context.getApplicationContext().getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        appRes.updateConfiguration(configuration, appRes.getDisplayMetrics());
    }

    /**
     * Translates a string into {@code application/x-www-form-urlencoded}
     *  format using a specific encoding scheme.
     *
     * @param str {@link String} to be translated.
     * @return the translated {@link String}
     *         or blank {@link String} if UTF-8 is not supported.
     */
    public static String encodeURL(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

    /**
     * Simplified way of getting the JSON reader from the {@link InputStream}.
     *
     * @param stream an input stream.
     * @return the JSON reader.
     */
    public static JsonReader getReader(InputStream stream) {
        return new JsonReader(new BufferedReader(new InputStreamReader(stream)));
    }

    /**
     * Simplified way of getting the JSON reader from the {@link HttpURLConnection}.
     *
     * @param http an HTTP connection.
     * @return the JSON reader.
     */
    public static JsonReader getReader(HttpURLConnection http) throws IOException {
        return getReader(http.getInputStream());
    }
}
