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

    /**
     * Returns the minimum distance from the capsule to the specified point.
     */
    public float getMinimumDistance (float x, float y)
    {
        return Math.max(0f, GeomUtil.getMinimumDistanceSquared(_x1, _y1, _x2, _y2, x, y) - _radius);
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
        return getMinimumDistanceSquared(point.getX(), point.getY()) <= _radius*_radius;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return false; // TODO
        //return getMinimumDistanceSquared(line.getX1(), line.getY1(), line.getX2(), line.getY2()) <= _radius*_radius;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        float rsum = _radius + circle.getRadius();
        return getMinimumDistanceSquared(circle.getX(), circle.getY()) <= rsum*rsum;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        // check if the segment starts or ends inside the rectangle
        if (rectangle.checkIntersects(_x1, _y1) ||
                rectangle.checkIntersects(_x2, _y2)) {
            return true;
        }
        return Math.abs(_x2 - _x1) > FloatMath.EPSILON &&
                   (intersectsX(rectangle, rectangle.getMinimumX()) || intersectsX(rectangle, rectangle.getMaximumX())) ||
               Math.abs(_y2 - _y1) > FloatMath.EPSILON &&
                   (intersectsY(rectangle, rectangle.getMinimumY()) || intersectsY(rectangle, rectangle.getMaximumY()));
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return false; // TODO
    }

    /**
     * Returns the square of the minimum distance from the line to the specified point.
     */
    protected float getMinimumDistanceSquared (float x, float y)
    {
        return GeomUtil.getMinimumDistanceSquared(_x1, _y1, _x2, _y2, x, y);
    }

    /**
     * Returns the minimum distance between two line segments.
     */
    protected float getMinimumDistanceSquared (
        float x1, float y1, float x2, float y2)
    {
        //NOTE: adapted from www.geometrictools.com/Documentation/DistanceLine3Line3.pdf
        float vx = _x2 - _x1, vy = _y2 - _y1;
        float ox = x2 - x1, oy = y2 - y1;
        float ux = _x1 - x1, uy = _y1 - y1;

        float a = vx*vx + vy*vy;
        float b = -vx*ox + -vy*oy;
        float c = ox*ox + oy*oy;
        float d = vx*ux + vy*uy;
        float e = -ox*ux + -oy*uy;

        return -1f; // TODO
    }

    /**
     * Helper method for {@link #checkIntersects(Rectangle)}. Determines whether 
     * the capsule segment intersects the rectangle on the side where x equals
     * the value specified.
     */
    protected boolean intersectsX (Rectangle rect, float x)
    {
        float t = (x - _x1) / (_x2 - _x1);
        if (t >= 0f && t <= 1f) {
            float iy = _y1 + t * (_y2 - _y1);
            return iy >= rect.getMinimumY() - _radius && iy <= rect.getMaximumY() + _radius;
        }
        return false;
    }

    /**
     * Helper method for {@link #checkIntersects(Rectangle)}. Determines whether 
     * the capsule segment intersects the rectangle on the side where y equals
     * the value specified.
     */
    protected boolean intersectsY (Rectangle rect, float y)
    {
        float t = (y - _y1) / (_y2 - _y1);
        if (t >= 0f && t <= 1f) {
            float ix = _x1 + t * (_x2 - _x1);
            return ix >= rect.getMinimumX() - _radius && ix <= rect.getMaximumX() + _radius;
        }
        return false;
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;

    /** The radius. */
    protected float _radius;
}
