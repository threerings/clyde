//
// $Id$

package com.threerings.tudey.shape;

import org.lwjgl.opengl.GL11;

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

    /**
     * Checks whether the capsule contains the specified point.
     */
    public boolean contains (Vector2f pt)
    {
        return contains(pt.x, pt.y);
    }

    /**
     * Checks whether the capsule contains the specified point.
     */
    public boolean contains (float x, float y)
    {
        // see if the point lies before the start (this also handles the case where start and
        // end are equal)
        float a = _end.x - _start.x, b = _end.y - _start.y;
        float dp = a*x + b*y;
        if (dp <= a*_start.x + b*_start.y) {
            float dx = x - _start.x, dy = y - _start.y;
            return dx*dx + dy*dy <= radius*radius;
        }
        // now see if it lies after the end
        if (dp >= a*_end.x + b*_end.y) {
            float dx = x - _end.x, dy = y - _end.y;
            return dx*dx + dy*dy <= radius*radius;
        }
        // it's in the middle, so check the distance to the line
        a = _start.y - _end.y;
        b = _end.x - _start.x;
        float d = a*(x - _start.x) + b*(y - _start.y);
        return d*d <= radius*radius * (a*a + b*b);
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
    public Vector2f getCenter (Vector2f result)
    {
        return _start.add(_end, result).multLocal(0.5f);
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
        return ray.getIntersection(_start, _end, radius, result);
    }

    @Override // documentation inherited
    public IntersectionType getIntersectionType (Rect rect)
    {
        // check the corners of the rectangle
        Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
        int ccount =
            (contains(min.x, min.y) ? 1 : 0) +
            (contains(max.x, min.y) ? 1 : 0) +
            (contains(max.x, max.y) ? 1 : 0) +
            (contains(min.x, max.y) ? 1 : 0);
        if (ccount > 0) {
            return (ccount == 4) ? IntersectionType.CONTAINS : IntersectionType.INTERSECTS;
        }
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
        return contains(point.getLocation());
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

    @Override // documentation inherited
    public void draw (boolean outline)
    {
        float offset = FloatMath.atan2(_end.x - _start.x, _start.y - _end.y);
        GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
        for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
            float angle = ii * CIRCLE_INCREMENT + offset;
            GL11.glVertex2f(
                _start.x + FloatMath.cos(angle) * radius,
                _start.y + FloatMath.sin(angle) * radius);
        }
        for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
            float angle = ii * CIRCLE_INCREMENT - offset;
            GL11.glVertex2f(
                _end.x + FloatMath.cos(angle) * radius,
                _end.y + FloatMath.sin(angle) * radius);
        }
        GL11.glEnd();
    }

    /** The start and end vertices of the capsule. */
    protected Vector2f _start = new Vector2f(), _end = new Vector2f();
}
