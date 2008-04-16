//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * A line segment.
 */
public class Line extends Shape
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
        updateBounds();
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
    public void updateBounds ()
    {
        _bounds.set(Math.min(_x1, _x2), Math.min(_y1, _y2),
                    Math.max(_x1, _x2), Math.max(_y1, _y2));
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Line oline;
        return super.equals(other) &&
            _x1 == (oline = (Line)other)._x1 && _y1 == oline._y1 &&
            _x2 == oline._x2 && _y2 == oline._y2;
    }

    @Override // documenation inherited
    public int hashCode ()
    {
        return calculateHashCode(_x1, _y1, _x2, _y2);
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Line[");
        builder.append("x1=").append(_x1).append(", ");
        builder.append("y1=").append(_y1).append(", ");
        builder.append("x2=").append(_x2).append(", ");
        builder.append("y2=").append(_y2);
        builder.append("]");
        return builder.toString();
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        //NOTE: adapted from http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline2d/
        float ux = _x2 - _x1, uy = _y2 - _y1;
        float vx = line.getX2() - line.getX1(), vy = line.getY2() - line.getY1();
        float wx = _x1 - line.getX1(), wy = _y1 - line.getY1();
        float d = vy * ux - vx * uy;
        float s = vx * wy - vy * wx;
        // check if the segments are parallel or conincident
        if (FloatMath.abs(d) < FloatMath.EPSILON) {
            return FloatMath.abs(s) < FloatMath.EPSILON ? true /* coincident */ : false /* parallel */;
        }
        // check if the intersection point is outside either segment
        if ((d > 0f && (s < 0f || s > d)) ||
            (d < 0f && (s > 0f || s < d))) {
            return false;
        }
        float t = ux * wy - uy * wx;
        if ((d > 0f && (t < 0f || t > d)) ||
            (d < 0f && (t > 0f || t < d))) {
            return false;
        }
        return true;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        return circle.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return rectangle.checkIntersects(this);
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return capsule.checkIntersects(this);
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;
}
