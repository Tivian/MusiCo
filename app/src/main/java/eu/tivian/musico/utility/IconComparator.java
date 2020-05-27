package eu.tivian.musico.utility;

import java.util.Comparator;

/**
 * Custom comparator for Last.fm icons.
 */
public class IconComparator implements Comparator<String> {
    /**
     * List of possible size names.
     */
    private static final String[] order = { "small", "medium", "large", "extralarge", "mega" };

    /**
     * Finds the position of given size name in the {@link #order}.
     *
     * @param str size name.
     * @return index of the name.
     */
    private int getPosition(String str) {
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(str))
                return i;
        }

        return -1;
    }

    /**
     * Compares two size names to order them in descending order.
     *
     * @param s1 first size name.
     * @param s2 second size name.
     * @return a negative integer, zero, or a positive integer as the first size is smaller,
     *         equal to, or larger than the other size.
     */
    @Override
    public int compare(String s1, String s2) {
        return getPosition(s2) - getPosition(s1);
    }
}
