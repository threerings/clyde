//
// $Id$

package com.threerings.opengl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Various static methods of general utility.
 */
public class GlUtil
{
    /**
     * Returns the smallest power-of-two that is greater than or equal to the supplied (positive)
     * value.
     */
    public static int nextPowerOfTwo (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
    }

    /**
     * If the supplied path can be parsed as an absolute filename, returns a
     * {@link FileInputStream} for that file; otherwise, requests the stream
     * from the resource manager.
     */
    public static InputStream getInputStream (GlContext ctx, String path)
        throws IOException
    {
        File file = new File(path);
        return file.isAbsolute() ?
            new FileInputStream(file) : ctx.getResourceManager().getResource(path);
    }

    /**
     * Loads a set of properties from the specified file.
     */
    public static Properties loadProperties (File pfile)
        throws IOException
    {
        Properties props = new Properties();
        FileInputStream in = new FileInputStream(pfile);
        props.load(in);
        in.close();
        return props;
    }

    /**
     * Normalizes any relative paths in the provided properties.
     */
    public static void normalizeProperties (String path, Properties props)
    {
        String[] keys = props.keySet().toArray(new String[props.size()]);
        for (String key : keys) {
            String value = props.getProperty(key);
            if (isFile(value)) {
                props.put(key, getPath(path, value));
            }
        }
    }

    /**
     * Determines whether the specified value ends with one of our known file extensions.
     */
    public static boolean isFile (String value)
    {
        for (String ext : FILE_EXTENSIONS) {
            if (value.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Combines an absolute parent path (either a filename or a resource path) with a
     * child path to return a new path.
     */
    public static String getPath (String parent, String child)
    {
        File pfile = new File(parent);
        return pfile.isAbsolute() ?
            new File(pfile, child).toString() : cleanPath(parent + "/" + child);
    }

    /**
     * Normalizes the path given.
     */
    public static String cleanPath (String path)
    {
        String npath = path.replaceAll(PATH_DOTDOT, "");
        while (!npath.equals(path)) {
            path = npath;
            npath = path.replaceAll(PATH_DOTDOT, "");
        }
        return npath;
    }

    /**
     * Given a parent path and a child path (both absolute), returns a relative path for the child
     * with respect to the parent.
     */
    public static String relativizePath (String parent, String child)
    {
        // split the paths up into their components
        String[] pcs = FILE_SEPARATOR.split(parent);
        String[] ccs = FILE_SEPARATOR.split(child);

        // advance past any common components
        int ii = 0;
        while (ii < pcs.length && ii < ccs.length && pcs[ii].equals(ccs[ii])) {
            ii++;
        }
        // rise out of the parent components
        StringBuffer path = new StringBuffer();
        for (int jj = ii; jj < pcs.length; jj++) {
            path.append("..").append(File.separatorChar);
        }
        // append the remaining child components
        for (int jj = ii; jj < ccs.length; jj++) {
            path.append(ccs[jj]);
            if (jj < ccs.length - 1) {
                path.append(File.separatorChar);
            }
        }
        return path.toString();
    }

    /**
     * Creates a hash key from the provided list of elements that uses {@link Arrays#deepHashCode}
     * and {@link Arrays#deepEquals} to hash/compare the elements.
     */
    public static Object createKey (Object... elements)
    {
        return new ArrayKey(elements);
    }

    /**
     * Divides the provided array into two halves, so that the first half contains all of the
     * elements less than the median, and the second half contains all of the elements equal to
     * or greater than the median.  This code is adapted from Wikipedia's page on
     * <a href="http://en.wikipedia.org/wiki/Selection_algorithm">Selection algorithms</a>.
     */
    public static <T> void divide (T[] a, Comparator<? super T> comp)
    {
        int left = 0, right = a.length - 1, k = a.length / 2;
        while (true) {
            int pivotIndex = (left + right) / 2;
            int pivotNewIndex = partition(a, left, right, pivotIndex, comp);
            if (k == pivotNewIndex) {
                return;
            } else if (k < pivotNewIndex) {
                right = pivotNewIndex - 1;
            } else {
                left = pivotNewIndex + 1;
            }
        }
    }

    /**
     * Partitions the [left, right] sub-array about the element at the supplied pivot index.
     *
     * @return the new (final) location of the pivot.
     */
    protected static <T> int partition (
        T[] a, int left, int right, int pivotIndex, Comparator<? super T> comp)
    {
        T pivotValue = a[pivotIndex];
        swap(a, pivotIndex, right); // Move pivot to end
        int storeIndex = left;
        for (int ii = left; ii < right; ii++) {
            if (comp.compare(a[ii], pivotValue) <= 0) {
                swap(a, storeIndex, ii);
                storeIndex++;
            }
        }
        swap(a, right, storeIndex); // Move pivot to its final place
        return storeIndex;
    }

    /**
     * Swaps two elements in an array.
     */
    protected static <T> void swap (T[] a, int idx1, int idx2)
    {
        T tmp = a[idx1];
        a[idx1] = a[idx2];
        a[idx2] = tmp;
    }

    /**
     * A wrapper around an array of elements allowing it to be used as a hash key.
     */
    protected static class ArrayKey
    {
        public ArrayKey (Object[] elements)
        {
            _elements = elements;
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return Arrays.deepHashCode(_elements);
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return other instanceof ArrayKey &&
                Arrays.deepEquals(_elements, ((ArrayKey)other)._elements);
        }

        /** The elements to compare. */
        protected Object[] _elements;
    }

    /** Used to normalize relative paths. */
    protected static final String PATH_DOTDOT = "[^/.]+/\\.\\./";

    /** Used to identify file properties. */
    protected static final String[] FILE_EXTENSIONS = { ".png", ".jpg", ".bmp", ".properties" };

    /** Used to split paths up into components. */
    protected static Pattern FILE_SEPARATOR = Pattern.compile(File.separator, Pattern.LITERAL);
}
