//
// $Id$

package com.threerings.tudey.shape;

import com.threerings.math.FloatMath;
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
    public boolean intersects (Circle circle)
    {
        // this test is equivalent to checking the line segment from _start to _end against
        // a circle with the same center as the parameter and a radius equal to the sum of
        // the capsule radius and the circle radius

        // see if we start or end inside the circle
        Vector2f center = circle.getCenter();
        float r = circle.radius + radius, r2 = r*r;
        if (_start.distanceSquared(center) <= r2 || _end.distanceSquared(center) <= r2) {
            return true;
        }
        // then if we intersect the circle
        float ax = _start.x - center.x, ay = _start.y - center.y;
        float dx = _end.x - _start.x, dy = _end.y - _start.y;
        float a = dx*dx + dy*dy;
        float b = 2f*(dx*ax + dy*ay);
        float c = ax*ax + ay*ay - r2;
        float radicand = b*b - 4f*a*c;
        if (radicand < 0f) {
            return false;
        }
        float t = (-b - FloatMath.sqrt(radicand)) / (2f*a);
        return t >= 0f && t <= 1f;
    }

    @Override // documentation inherited
    public boolean intersects (Capsule capsule)
    {
        return false;
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

    /** The start and end vertices of the capsule. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
