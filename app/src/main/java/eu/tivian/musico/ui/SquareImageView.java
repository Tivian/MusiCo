package eu.tivian.musico.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Special version of {@link android.widget.ImageView} which forces itself to stay in square proportions.
 */
public class SquareImageView extends AppCompatImageView {
    /**
     * Creates the image view.
     *
     * @param context the {@link Context} the view is running in, through which it
     *                can access the current theme, resources, etc.
     */
    public SquareImageView(Context context) {
        super(context);
    }

    /**
     * Creates the image view.
     *
     * @param context the {@link Context} the view is running in, through which it
     *                can access the current theme, resources, etc.
     * @param attrs this value may be null.
     */
    public SquareImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Creates the image view.
     *
     * @param context the {@link Context} the view is running in, through which it
     *                can access the current theme, resources, etc.
     * @param attrs this value may be null.
     * @param defStyleAttr an attribute in the current theme that contains a reference to
     *        a style resource that supplies default values for the view.
     *        Can be 0 to not look for defaults.
     */
    public SquareImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Measure the view and its content to determine the measured width and the measured height.
     * This function forces the view to stay in square proportion.
     *
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        setMeasuredDimension(width, width);
    }
}
