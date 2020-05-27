package eu.tivian.musico.net;

import android.annotation.SuppressLint;
import android.util.JsonReader;

import androidx.core.util.Consumer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 * Provides the information about current or pas exchange rates.
 *
 * @see <a href="https://exchangeratesapi.io/">Exchange rates API reference</a>
 */
public class Exchange extends Service {
    /**
     * The base URL for the exchange API.
     */
    private static final String ROOT_URL = "https://api.exchangeratesapi.io/";

    /**
     * Date formatter used to create URL links for past exchange rates.
     */
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Default base currency.
     */
    public static final String BASE_CURRENCY = "PLN";

    /**
     * A class representation of the exchange rates report.
     */
    public static class Rates {
        /**
         * A dictionary of exchange rates.
         * Keys are {@link <a href="https://www.iban.com/currency-codes">ISO 4217</a>} based.
         */
        public Map<String, Double> rate;

        /**
         * Determines the base currency, meaning it's value will always be equal to 1.
         */
        public String base;

        /**
         * Date of the exchange rates report.
         */
        public Date date;

        /**
         * Initializes the class fields with default values.
         */
        private Rates() {
            rate = new HashMap<>();
            base = "";
            date = null;
        }
    }

    /**
     * Helper class to ensure thread-safe singleton creation.
     */
    private static class InstanceHolder {
        /**
         * Singleton instance.
         */
        private static final Exchange instance = new Exchange();
    }

    /**
     * Private constructor to prevent instantiating from outside of this class.
     */
    private Exchange() {}

    /**
     * Returns the singleton.
     *
     * @return the singleton instance.
     */
    public static Exchange get() {
        return InstanceHolder.instance;
    }

    /**
     * Gets the exchange rates for today, using the default base currency (denoted by {@link #BASE_CURRENCY}.
     *
     * @param action functor determining what should happen with received data.
     */
    public void getRates(Consumer<Rates> action) {
        getRates(BASE_CURRENCY, action);
    }

    /**
     * Gets the exchange rates for today, based on provided base currency.
     *
     * @param base the base currency, for which the exchange rate will be equal to 1.
     * @param action functor determining what should happen with received data.
     */
    public void getRates(String base, Consumer<Rates> action) {
        getRates(base, null, action);
    }

    /**
     * Gets the exchange rates for specified date using provided base currency.
     *
     * @param base the base currency, for which the exchange rate will be equal to 1.
     * @param date the date of desired exchange rates list.
     * @param action functor determining what should happen with received data.
     */
    public void getRates(String base, Date date, Consumer<Rates> action) {
        execute((date == null ? "latest" : getDate(date)) + "?base=" + base, this::parseRates, action);
    }

    /**
     * Parses the data from server into the list of exchange rates.
     *
     * @param reader input JSON stream.
     * @return the exchange rates list.
     */
    private Rates parseRates(JsonReader reader) {
        Rates rates = new Rates();

        try {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "rates":
                        reader.beginObject();
                        while (reader.hasNext())
                            rates.rate.put(reader.nextName(), reader.nextDouble());
                        reader.endObject();
                        break;
                    case "base":
                        rates.base = reader.nextString();
                        break;
                    case "date":
                        rates.date = toDate(reader.nextString());
                        break;
                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException ignored) {}

        return rates;
    }

    /**
     * Converts the {@link Date} object into string.
     *
     * @param date date to parse.
     * @return string in format "yyyy-MM-dd".
     */
    private String getDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    /**
     * Parses string into the {@link Date} object.
     *
     * @param str text to parse.
     * @return parsed date or {@code null} if format of string was invalid.
     */
    private Date toDate(String str) {
        try {
            return DATE_FORMAT.parse(str);
        } catch (ParseException ex) {
            return null;
        }
    }

    /**
     * Connects to the Exchange rates website.
     * None authorization is required.
     *
     * @param req the URL to connect to.
     * @return input stream.
     */
    @Override
    protected InputStream connect(String req) {
        try {
            URL url = new URL(ROOT_URL + req);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            return conn.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
