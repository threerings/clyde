//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A point.
 */
public class Point extends Shape
{
    /**
     * Creates a point at the specified location.
     */
    public Point (Vector2f location)
    {
        _location.set(location);
        updateBounds();
    }

    /**
     * Creates an uninitialized point.
     */
    public Point ()
    {
    }

    /**
     * Returns a reference to the location of the point.
     */
    public Vector2f getLocation ()
    {
        return _location;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.getMinimumExtent().set(_location);
        _bounds.getMaximumExtent().set(_location);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Point presult = (result instanceof Point) ? ((Point)result) : new Point();
        transform.transformPoint(_location, presult._location);
        presult.updateBounds();
        return presult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        return rect.contains(_location) ? IntersectionType.INTERSECTS : IntersectionType.NONE;
    }

    @Override // documentation inherited
    public boolean intersects (SpaceElement element)
    {
        return element.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Shape shape)
    {
        return shape.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Point point)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Quad quad)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return false;
    }

    /** The location of the point. */
    protected Vector2f _location = new Vector2f();
}
