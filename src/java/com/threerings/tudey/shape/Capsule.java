//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.space.SpaceElement;

/**
 * A capsule.
 */
public class Capsule extends Shape
{
    /** The radius of the capsule. */
    public float radius;

    /**
     * Creates a capsule with the supplied center and radius.
     */
    public Capsule (Vector2f start, Vector2f end, float radius)
    {
        _start.set(start);
        _end.set(end);
        this.radius = radius;
        updateBounds();
    }

    /**
     * Creates an uninitialized capsule.
     */
    public Capsule ()
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
        _bounds.expandLocal(radius, radius);
    }

    @Override // documentation inherited
    public Shape transform (Transform2D transform, Shape result)
    {
        Capsule cresult = (result instanceof Capsule) ? ((Capsule)result) : new Capsule();
        transform.transformPoint(_start, cresult._start);
        transform.transformPoint(_end, cresult._end);
        cresult.radius = radius * transform.approximateUniformScale();
        cresult.updateBounds();
        return cresult;
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

    /** The start and end vertices of the capsule. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
