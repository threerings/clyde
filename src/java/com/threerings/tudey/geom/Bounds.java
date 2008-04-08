//
// $Id$

package com.threerings.tudey.geom;

/**
 * An axis-aligned bounding rectangle.
 */
public final class Bounds
{
    /** The minimum extent of the rectangle. */
    public float minX, minY;

    /** The maximum extent of the rectangle. */
    public float maxX, maxY;

    /**
     * Creates a new set of bounds with the specified extents.
     */
    public Bounds (float minX, float minY, float maxX, float maxY)
    {
        set(minX, minY, maxX, maxY);
    }

    /**
     * Copy constructor.
     */
    public Bounds (Bounds other)
    {
        set(other);
    }

    /**
     * Creates a set of empty bounds.
     */
    public Bounds ()
    {
    }

    /**
     * Adds the specified set of bounds in-place to this one.
     *
     * @return a reference to these bounds, for chaining.
     */
    public Bounds addLocal (Bounds other)
    {
        return add(other, this);
    }

    /**
     * Adds the specified set of bounds to this one, returning a new set of bounds.
     */
    public Bounds add (Bounds other)
    {
        return add(other, new Bounds());
    }

    /**
     * Adds the specified set of bounds to this one and places the result in the supplied
     * object.
     *
     * @return a reference to the result object, for chaining.
     */
    public Bounds add (Bounds other, Bounds result)
    {
        return result.set(
            Math.min(minX, other.minX),
            Math.min(minY, other.minY),
            Math.max(maxX, other.maxX),
            Math.max(maxY, other.maxY));
    }

    /**
     * Copies the parameters of another set of bounds.
     *
     * @return a reference to these bounds, for chaining.
     */
    public Bounds set (Bounds other)
    {
        return set(other.minX, other.minY, other.maxX, other.maxY);
    }

    /**
     * Sets the parameters of these bounds.
     *
     * @return a reference to these bounds, for chaining.
     */
    public Bounds set (float minX, float minY, float maxX, float maxY)
    {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        return this;
    }

    /**
     * Determines whether these bounds intersect the other bounds specified.
     */
    public boolean intersect (Bounds other)
    {
        return minX <= other.maxX && minY <= other.maxY &&
            maxX >= other.minX && maxY >= other.minY;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Bounds obounds = (Bounds)other;
        return other != null && other instanceof Bounds &&
            minX == (obounds = (Bounds)other).minX && minY == obounds.minY &&
            maxX == obounds.maxX && maxY == obounds.maxY;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Bounds[");
        builder.append("minx=").append(minX).append(", ");
        builder.append("miny=").append(minY).append(", ");
        builder.append("maxx=").append(maxX).append(", ");
        builder.append("maxy=").append(maxY);
        builder.append("]");
        return builder.toString();
    }
}
