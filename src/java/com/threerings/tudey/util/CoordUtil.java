//
// $Id$

package com.threerings.tudey.util;

/**
 * Static methods for dealing with packed integer coordinates in the range [-32767, +32767].
 */
public class CoordUtil
{
    /** Represents an unused coordinate. */
    public static final int UNUSED = getCoord(Short.MIN_VALUE, Short.MIN_VALUE);

    /**
     * Creates an encoded coordinate pair.
     */
    public static int getCoord (int x, int y)
    {
        return (x << 16) | (y & 0xFFFF);
    }

    /**
     * Extracts the x coordinate from an encoded coordinate pair.
     */
    public static int getX (int coord)
    {
        return (coord >> 16);
    }

    /**
     * Extracts the y coordinate from an encoded coordinate pair.
     */
    public static int getY (int coord)
    {
        return (coord << 16) >> 16;
    }

    /**
     * Returns the Manhattan distance between two points.
     */
    public static int getDistance (int x1, int y1, int x2, int y2)
    {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
}
