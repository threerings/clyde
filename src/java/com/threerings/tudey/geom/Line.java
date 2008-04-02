//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * A line segment.
 */
public final class Line extends Shape
{
    /**
     * Creates a line segment between the specified points.
     */
    public Line (float x1, float y1, float x2, float y2)
    {
        set(x1, y1, x2, y2);
    }

    /**
     * Copy constructor.
     */
    public Line (Line other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized line segment.
     */
    public Line ()
    {
    }

    /**
     * Copies the parameters of another line segment.
     */
    public void set (Line other)
    {
        set(other.getX1(), other.getY1(), other.getX2(), other.getY2());
    }

    /**
     * Sets the parameters of the line segment.
     */
    public void set (float x1, float y1, float x2, float y2)
    {
        if (_x1 == x1 && _y1 == y1 && _x2 == x2 && _y2 == y2) {
            return;
        }
        willMove();
        _x1 = x1;
        _y1 = y1;
        _x2 = x2;
        _y2 = y2;
        _bounds.set(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2));
        didMove();
    }

    /**
     * Returns the x coordinate of the first vertex.
     */
    public float getX1 ()
    {
        return _x1;
    }

    /**
     * Returns the y coordinate of the first vertex.
     */
    public float getY1 ()
    {
        return _y1;
    }

    /**
     * Returns the x coordinate of the second vertex.
     */
    public float getX2 ()
    {
        return _x2;
    }

    /**
     * Returns the y coordinate of the second vertex.
     */
    public float getY2 ()
    {
        return _y2;
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        //NOTE: adapted from http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
        float ux = x2 - x1, uy = y2 - y1;
        float vx = line._x2 - line._x1, vy = line._y2 - line._y1;
        float d = vy * ux - vx * uy;
        // check if the segments are parallel
        if (FloatMath.epsilonEquals(d, 0f)) {
            return false;
        }
        // check if the segments intersect
        float wx = x1 - line._x1, wy = y1 - line._y1;
        float s = vx * wy - vy * wx;
        if (s < 0f || s > d) {
            return false;
        }
        float t = ux * wy - uy * wx;
        if (t < 0f || t > d) {
            return false;
        }
        return true;
    }

    /**
     * Determines whether this shape intersects the given circle.
     */
    protected boolean checkIntersects (Circle circle)
    {
        return circle.checkIntersects(this);
    }

    /**
     * Determines whether this shape intersects the given rectangle.
     */
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return rectangle.checkIntersects(this);
    }

    /**
     * Determines whether this shape intersects the given capsule.
     */
    protected boolean checkIntersects (Capsule capsule)
    {
        return capsule.checkIntersects(this);
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;
}
