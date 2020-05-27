package eu.tivian.musico.utility;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

/**
 * Simplified version of {@link RequestListener} to be used for listening for successful loading of a resource.
 *
 * @param <R> the type of resource being loaded.
 */
public interface SuccessListener<R> extends RequestListener<R> {
    /**
     * Called when an exception occurs during a load, immediately before {@link Target#onLoadFailed(Drawable)}.
     *
     * @param e the maybe null exception containing information about why the request failed.
     * @param model the model we were trying to load when the exception occurred.
     * @param target the {@link Target} we were trying to load the image into.
     * @param isFirstResource {@code true} if this exception is for the first resource to load.
     * @return always false to allow {@link Target#onLoadFailed(Drawable)} to be called on target.
     */
    @Override
    default boolean onLoadFailed(@Nullable GlideException e, Object model, Target<R> target, boolean isFirstResource) {
        return false;
    }

    /**
     * Called when a load completes successfully, immediately before {@link Target#onResourceReady(Object, Transition)}.
     *
     * @param resource the resource that was loaded for the target.
     * @param model the specific model that was used to load the image.
     * @param target the target the model was loaded into.
     * @param dataSource the {@link DataSource} the resource was loaded from.
     * @param isFirstResource {@code true} if this is the first resource to in this load to be loaded into the target.
     * @return always false to allow {@link Target#onLoadFailed(Drawable)} to be called on target.
     */
    @Override
    default boolean onResourceReady(R resource, Object model, Target<R> target, DataSource dataSource, boolean isFirstResource) {
        onSuccess(resource);
        return false;
    }

    /**
     * Called when a load completes successfully.
     *
     * @param resource the resource that was loaded for the target.
     */
    void onSuccess(R resource);
}
