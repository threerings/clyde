//
// $Id$

package com.threerings.tudey.geom;

import com.threerings.math.FloatMath;

/**
 * A capsule shape.
 */
public final class Capsule extends Shape
{
    /**
     * Creates a capsule with the specified parameters.
     */
    public Capsule (float x1, float y1, float x2, float y2, float radius)
    {
        set(x1, y1, x2, y2, radius);
    }

    /**
     * Copy constructor.
     */
    public Capsule (Capsule other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized capsule.
     */
    public Capsule ()
    {
    }

    /**
     * Sets the location of the capsule's vertices.
     */
    public void setVertices (float x1, float y1, float x2, float y2)
    {
        set(x1, y1, x2, y2, _radius);
    }

    /**
     * Sets the radius of the capsule.
     */
    public void setRadius (float radius)
    {
        set(_x1, _y1, _x2, _y2, radius);
    }

    /**
     * Copies the parameters of another capsule.
     */
    public void set (Capsule other)
    {
        set(other.getX1(), other.getY1(), other.getX2(), other.getY2(), other.getRadius());
    }

    /**
     * Sets the parameters of the capsule.
     */
    public void set (float x1, float y1, float x2, float y2, float radius)
    {
        if (_x1 == x1 && _y1 == y1 && _x2 == x2 && _y2 == y2 && _radius == radius) {
            return;
        }
        if (radius < 0f) {
            throw new IllegalArgumentException("Radius cannot be negative.");
        }
        willMove();
        _x1 = x1;
        _y1 = y1;
        _x2 = x2;
        _y2 = y2;
        _radius = radius;
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

    /**
     * Returns the radius of the capsule.
     */
    public float getRadius ()
    {
        return _radius;
    }

    @Override // documentation inherited
    public void updateBounds ()
    {
        _bounds.set(
            Math.min(_x1, _x2) - _radius, Math.min(_y1, _y2) - _radius,
            Math.max(_x1, _x2) + _radius, Math.max(_y1, _y2) + _radius);
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Capsule ocapsule;
        return super.equals(other) &&
            _x1 == (ocapsule = (Capsule)other)._x1 && _y1 == ocapsule._y1 &&
            _x2 == ocapsule._x2 && _y2 == ocapsule._y2 &&
            _radius == ocapsule._radius;
    }

    @Override // documenation inherited
    public int hashCode ()
    {
        return calculateHashCode(_x1, _y1, _x2, _y2, _radius);
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Capsule[");
        builder.append("x1=").append(_x1).append(", ");
        builder.append("y1=").append(_y1).append(", ");
        builder.append("x2=").append(_x2).append(", ");
        builder.append("y2=").append(_y2).append(", ");
        builder.append("radius=").append(_radius);
        builder.append("]");
        return builder.toString();
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Point point)
    {
        return getLinePointDistance2(point.getX(), point.getY()) <= _radius*_radius;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return checkIntersects(line.getX1(), line.getY1(), line.getX2(), line.getY2());
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        float rsum = _radius + circle.getRadius();
        return getLinePointDistance2(circle.getX(), circle.getY()) <= rsum*rsum;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        // check if the segment starts or ends inside the rectangle
        if (rectangle.checkIntersects(_x1, _y1) ||
                rectangle.checkIntersects(_x2, _y2)) {
            return true;
        }
        // check if the capsule intersects any side of the rectangle
        return checkIntersects(rectangle.getMinimumX(), rectangle.getMinimumY(), rectangle.getMaximumX(), rectangle.getMinimumY()) ||
                checkIntersects(rectangle.getMaximumX(), rectangle.getMinimumY(), rectangle.getMaximumX(), rectangle.getMaximumY()) ||
                checkIntersects(rectangle.getMaximumX(), rectangle.getMaximumY(), rectangle.getMinimumX(), rectangle.getMaximumY()) ||
                checkIntersects(rectangle.getMinimumX(), rectangle.getMaximumY(), rectangle.getMinimumX(), rectangle.getMinimumY());
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        float rsum = _radius + capsule.getRadius();
        return getLineLineDistance2(capsule.getX1(), capsule.getY1(), capsule.getX2(), capsule.getY2()) <= rsum*rsum;
    }

    /**
     * Returns whether the capsule intersects the specified line segment.
     */
    protected boolean checkIntersects (
        float x1, float y1, float x2, float y2)
    {
        return getLineLineDistance2(x1, y1, x2, y2) <= _radius*_radius;
    }

    /**
     * Returns the square of the minimum distance between the capsule segment
     * and the specified line segment.
     */
    protected float getLineLineDistance2 (
        float x1, float y1, float x2, float y2)
    {
        return DistanceUtil.getLineLine2(_x1, _y1, _x2, _y2, x1, y1, x2, y2);
    }

    /**
     * Returns the square of the minimum distance from the capsule segment to
     * the specified point.
     */
    protected float getLinePointDistance2 (float x, float y)
    {
        return DistanceUtil.getLinePoint2(_x1, _y1, _x2, _y2, x, y);
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;

    /** The radius. */
    protected float _radius;
}
