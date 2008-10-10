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
        return ray.getIntersection(_start, _end, result);
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
        Vector2f pt = point.getLocation();
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        if (dx == 0f && dy == 0f) {
            return _start.equals(pt);
        } else if (Math.abs(dx) > Math.abs(dy)) {
            float t = (pt.x - _start.x) / dx;
            return t >= 0f && t <= 1f && _start.y + t*dy == pt.y;
        } else {
            float t = (pt.y - _start.y) / dy;
            return t >= 0f && t <= 1f && _start.x + t*dx == pt.x;
        }
    }

    @Override // documentation inherited
    public boolean intersects (Segment segment)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean intersects (Circle circle)
    {
        return circle.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return capsule.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Polygon polygon)
    {
        return polygon.intersects(this);
    }

    @Override // documentation inherited
    public boolean intersects (Compound compound)
    {
        return compound.intersects(this);
    }

    /** The start and end vertices. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
