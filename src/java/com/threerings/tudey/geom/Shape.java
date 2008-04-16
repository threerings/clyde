//
// $Id$

package com.threerings.tudey.geom;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;

import com.threerings.export.Exportable;
import com.threerings.export.Importer;

/**
 * The superclass of 2D shapes.
 */
public abstract class Shape
    implements Streamable, Exportable, Cloneable
{
    /**
     * Returns a reference to the bounds of the shape.
     */
    public Bounds getBounds ()
    {
        return _bounds;
    }

    /**
     * Updates the bounds of the shape.
     */
    public void updateBounds ()
    {
        // nothing by default
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
     * Reads the fields of the shape.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        initTransientFields();
    }

    /**
     * Reads the fields of the shape.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();
        initTransientFields();
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other != null && getClass() == other.getClass();
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            // cloning mostly does the right thing since shapes are mostly primitives
            Shape nshape = (Shape)super.clone();
            nshape.initTransientFields();
            // make shallow copies of the space and data
            nshape._space = _space;
            nshape._data = _data;
            return nshape;
        } catch (CloneNotSupportedException e) {
            return null; // should never happen
        }
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

    /**
     * Calculates the hash code for the given list of floating point values.
     */
    protected int calculateHashCode(float... values)
    {
        // use the java.util.Arrays implementation
        int hash = 1;
        for (int ii = 0; ii < values.length; ii++) {
            hash = 31*hash + Float.floatToIntBits(values[ii]);
        }
        return hash;
    }

    /**
     * Initializes the transient fields after construction, deserialization, etc.
     */
    protected void initTransientFields ()
    {
        updateBounds();
    }

    /** The bounds of the shape. */
    protected transient Bounds _bounds = new Bounds();

    /** The shape's intersection flags. */
    protected int _intersectionFlags = ~0;

    /** The shape's intersection mask. */
    protected int _intersectionMask = ~0;

    /** The space in which this shape is contained (if any). */
    protected Space _space;

    /** The shape's user data. */
    protected transient Object _data;

}
