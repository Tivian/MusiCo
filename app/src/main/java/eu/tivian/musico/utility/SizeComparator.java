package eu.tivian.musico.utility;

import android.util.Size;

import java.util.Comparator;

/**
 * Compares the two {@link Size} objects.
 */
public class SizeComparator implements Comparator<Size> {
    /**
     * Compares the two {@link Size} objects.
     *
     * @param a first size object.
     * @param b second size object.
     * @return a negative integer, zero, or a positive integer as the first size is smaller,
     *         equal to, or larger than the second size.
     */
    @Override
    public int compare(Size a, Size b) {
        return b.getHeight() * b.getWidth() - a.getHeight() * a.getWidth();
    }
}
