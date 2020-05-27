package eu.tivian.musico.net;

import android.os.AsyncTask;
import android.util.JsonReader;

import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

import java.io.InputStream;

import eu.tivian.musico.utility.Utilities;

/**
 * Base class for any Internet based content provider which uses JSON output.
 */
public abstract class Service {
    /**
     * Opens the network connection using proper app authorization.
     *
     * @param req the URL to connect to.
     * @return input stream or {@code null} if the proper connection wasn't established.
     */
    protected abstract InputStream connect(String req);

    /**
     * Internal class used for asynchronous connection with the external services.
     *
     * @param <T> type of object to be returned from the external API.
     */
    private static class ServiceTask<T> extends AsyncTask<Void, Void, T> {
        /**
         * The URL to connect to.
         */
        private String url;

        /**
         * Functor used to connect to the service with proper authorization.
         * Executes outside the UI thread.
         */
        private Function<String, InputStream> connect;

        /**
         * Functor which processes the input JSON into the desired object denoted by {@code T} type.
         * Executes outside the UI thread.
         */
        private Function<JsonReader, T> process;

        /**
         * Functor called after the resource was parsed by the {@link #process} functor.
         * Executes on the UI thread.
         */
        private Consumer<T> action;

        /**
         * Functor called only if any I/O errors occurred while retrieving information from the server.
         * Executes on the UI thread.
         */
        private Consumer<Exception> onError;

        /**
         * Used to store any thrown I/O exception.
         */
        private Exception exception;

        /**
         * Sets every necessary field for the instance of this class.
         *
         * @param url the URL to connect to.
         * @param connect functor used to connect to the service with proper authorization.
         * @param process functor which processes the input JSON into the desired object denoted by {@code T} type.
         * @param action functor called after the resource was parsed by the {@link #process} functor.
         * @param onError functor called only if any I/O errors occurred while retrieving information from the server.
         */
        private ServiceTask(String url, Function<String, InputStream> connect,
                Function<JsonReader, T> process, Consumer<T> action, Consumer<Exception> onError) {
            super();
            this.url = url;
            this.connect = connect;
            this.process = process;
            this.action = action;
            this.onError = onError;
        }

        /**
         * Parses the information received from the server into the object of type {@code T}.
         *
         * @param voids dummy argument
         * @return the object parsed from JSON.
         */
        @Override
        protected T doInBackground(Void... voids) {
            try (JsonReader reader = Utilities.getReader(connect.apply(url))) {
                return process.apply(reader);
            } catch (Exception ex) {
                exception = ex;
                return null;
            }
        }

        /**
         * Processes the {@code obj} received from the server.
         *
         * @param obj information parsed from the server.
         */
        @Override
        protected void onPostExecute(T obj) {
            super.onPostExecute(obj);
            if (obj != null)
                action.accept(obj);
            else {
                exception.printStackTrace();
                if (onError != null)
                    onError.accept(exception);
            }

        }
    }

    /**
     * Parses the JSON available at the {@code url}.
     *
     * @param url the URL to connect to.
     * @param process functor which processes the input JSON into the desired object denoted by {@code T} type.
     * @param action functor called after the resource was parsed by the {@code process} functor.
     * @param <T> type of object to be returned from the external API.
     */
    protected <T> void execute(String url, Function<JsonReader, T> process, Consumer<T> action) {
        execute(url, process, action, null);
    }

    /**
     * Parses the JSON available at the {@code url}.
     *
     * @param url the URL to connect to.
     * @param process functor which processes the input JSON into the desired object denoted by {@code T} type.
     * @param action functor called after the resource was parsed by the {@code process} functor.
     * @param onError functor called only if any I/O errors occurred while retrieving information from the server.
     * @param <T> type of object to be returned from the external API.
     */
    protected <T> void execute(String url, Function<JsonReader, T> process, Consumer<T> action, Consumer<Exception> onError) {
        new ServiceTask<T>(url, this::connect, process, action, onError).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
