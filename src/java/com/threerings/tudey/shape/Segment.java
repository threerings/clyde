//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A line segment.
 */
public class Segment extends Shape
{
    /**
     * Creates a segment between the specified points.
     */
    public Segment (Vector2f start, Vector2f end)
    {
        _start.set(start);
        _end.set(end);
        updateBounds();
    }

    /**
     * Creates an uninitialized segment.
     */
    public Segment ()
    {
    }

    /**
     * Returns a reference to the start vertex.
     */
    public Vector2f getStart ()
    {
        return _start;
    }

    /**
     * Returns a reference to the end vertex.
     */
    public Vector2f getEnd ()
    {
        return _end;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.setToEmpty();
        _bounds.addLocal(_start);
        _bounds.addLocal(_end);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Segment sresult = (result instanceof Segment) ? ((Segment)result) : new Segment();
        transform.transformPoint(_start, sresult._start);
        transform.transformPoint(_end, sresult._end);
        sresult.updateBounds();
        return sresult;
    }

    @Override // documentation inherited
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        return false;
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        return IntersectionType.NONE;
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

    /** The start and end vertices. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
