//
// $Id$

package com.threerings.tudey.util;

/**
 * Represents the four cardinal and four ordinal directions and their offsets in the Tudey
 * coordinate system.
 */
public enum Direction
{
    NORTH(+0, +1),
    NORTHWEST(-1, +1),
    WEST(-1, +0),
    SOUTHWEST(-1, -1),
    SOUTH(+0, -1),
    SOUTHEAST(+1, -1),
    EAST(+1, +0),
    NORTHEAST(+1, +1);

    /** The cardinal directions. */
    public static final Direction[] CARDINAL_VALUES = { NORTH, WEST, SOUTH, EAST };

    /**
     * Returns the x offset corresponding to the direction.
     */
    public int getX ()
    {
        return _x;
    }

    /**
     * Returns the y offset corresponding to the direction.
     */
    public int getY ()
    {
        return _y;
    }

    Direction (int x, int y)
    {
        _x = x;
        _y = y;
    }

    /** The x and y offsets corresponding to the direction. */
    protected int _x, _y;
}
