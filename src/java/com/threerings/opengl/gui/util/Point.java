//
// $Id$

package com.threerings.opengl.gui.util;

/**
 * Represents the location of a component.
 */
public class Point
    implements Cloneable
{
    /** The x position of the entity in question. */
    public int x;

    /** The y position of the entity in question. */
    public int y;

    public Point (int x, int y)
    {
        set(x, y);
    }

    public Point (Point other)
    {
        set(other);
    }

    public Point ()
    {
    }

    /**
     * Sets the value of this point to that of the specified other.
     */
    public Point set (Point point)
    {
        return set(point.x, point.y);
    }

    /**
     * Sets the value of this point.
     *
     * @return a reference to this point, for chaining.
     */
    public Point set (int x, int y)
    {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // will not happen
        }
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return x*31 + y;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof Point)) {
            return false;
        }
        Point opoint = (Point)other;
        return x == opoint.x && y == opoint.y;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return (x >= 0 ? "+" : "") + x + (y >= 0 ? "+" : "") + y;
    }
}
