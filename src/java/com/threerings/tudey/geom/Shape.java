//
// $Id$

package com.threerings.tudey.geom;

/**
 * The superclass of 2D shapes.
 */
public abstract class Shape
{
    /**
     * Returns a reference to the bounds of the shape.
     */
    public Bounds getBounds ()
    {
        return _bounds;
    }

    /**
     * Sets the shape's intersection flags.
     */
    public void setIntersectionFlags (int flags)
    {
        _intersectionFlags = flags;
    }

    /**
     * Returns the shape's intersection flags.
     */
    public int getIntersectionFlags ()
    {
        return _intersectionFlags;
    }

    /**
     * Sets the shape's intersection mask.
     */
    public void setIntersectionMask (int mask)
    {
        _intersectionMask = mask;
    }

    /**
     * Returns the shape's intersection mask.
     */
    public int getIntersectionMask ()
    {
        return _intersectionMask;
    }

    /**
     * Determines whether we should report an intersection with the specified shape based on
     * the intersection mask of this shape and the intersection flags of the other shape.
     */
    public boolean testIntersectionFlags (Shape other)
    {
        return (_intersectionMask & other.getIntersectionFlags()) != 0;
    }

    /**
     * Sets the shape's user data.
     */
    public void setData (Object data)
    {
        _data = data;
    }

    /**
     * Returns the shape's user data.
     */
    public Object getData ()
    {
        return _data;
    }

    /**
     * Determines whether the specified shape intersects this one.  This method relies on
     * <a href="http://en.wikipedia.org/wiki/Double_dispatch">double-dispatch</a> to invoke
     * the correct intersection algorithm.
     */
    public abstract boolean intersects (Shape other);

    /**
     * Sets the space containing this shape.
     */
    protected void setSpace (Space space)
    {
        _space = space;
    }

    /**
     * Returns the space containing this shape.
     */
    protected Space getSpace ()
    {
        return _space;
    }

    /**
     * Determines whether this shape intersects the given point.
     */
    protected boolean checkIntersects (Point point)
    {
        return false;
    }

    /**
     * Determines whether this shape intersects the given line.
     */
    protected boolean checkIntersects (Line line)
    {
        return false;
    }

    /**
     * Determines whether this shape intersects the given circle.
     */
    protected boolean checkIntersects (Circle circle)
    {
        return false;
    }

    /**
     * Determines whether this shape intersects the given rectangle.
     */
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return false;
    }

    /**
     * Determines whether this shape intersects the given capsule.
     */
    protected boolean checkIntersects (Capsule capsule)
    {
        return false;
    }

    /**
     * Should be called immediately before the shape moves.
     */
    protected void willMove ()
    {
        if (_space != null) {
            _space.shapeWillMove(this);
        }
    }

    /**
     * Should be called immediately after the shape moves.
     */
    protected void didMove ()
    {
        if (_space != null) {
            _space.shapeDidMove(this);
        }
    }

    /** The space in which this shape is contained (if any). */
    protected Space _space;

    /** The bounds of the shape. */
    protected Bounds _bounds = new Bounds();

    /** The shape's intersection flags. */
    protected int _intersectionFlags = ~0;

    /** The shape's intersection mask. */
    protected int _intersectionMask = ~0;

    /** The shape's user data. */
    protected Object _data;
}
