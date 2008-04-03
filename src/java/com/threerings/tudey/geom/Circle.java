//
// $Id$

package com.threerings.tudey.geom;

/**
 * A circle.
 */
public final class Circle extends Shape
{
    /**
     * Creates a circle with the specified location and radius.
     */
    public Circle (float x, float y, float radius)
    {
        set(x, y, radius);
    }

    /**
     * Copy constructor.
     */
    public Circle (Circle other)
    {
        set(other);
    }

    /**
     * Creates an uninitialized circle.
     */
    public Circle ()
    {
    }

    /**
     * Sets the location of the circle.
     */
    public void setLocation (float x, float y)
    {
        set(x, y, _radius);
    }

    /**
     * Sets the radius of the circle.
     */
    public void setRadius (float radius)
    {
        set(_x, _y, radius);
    }

    /**
     * Copies the parameters of another circle.
     */
    public void set (Circle other)
    {
        set(other.getX(), other.getY(), other.getRadius());
    }

    /**
     * Sets the parameters of the circle.
     */
    public void set (float x, float y, float radius)
    {
        if (_x == x && _y == y && _radius == radius) {
            return;
        }
        willMove();
        _x = x;
        _y = y;
        _radius = radius;
        _bounds.set(x - radius, y - radius, x + radius, y + radius);
        didMove();
    }

    /**
     * Returns the x coordinate of the circle's center.
     */
    public float getX ()
    {
        return _x;
    }

    /**
     * Returns the y coordinate of the circle's center.
     */
    public float getY ()
    {
        return _y;
    }

    /**
     * Returns the radius of the circle.
     */
    public float getRadius ()
    {
        return _radius;
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return other.checkIntersects(this);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Circle ocircle;
        return super.equals(other) &&
            _x == (ocircle = (Circle)other)._x && _y == ocircle._y && _radius == ocircle._radius;
    }

    @Override // documentation inherited
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Circle[");
        builder.append("x=").append(_x).append(", ");
        builder.append("y=").append(_y).append(", ");
        builder.append("radius=").append(_radius);
        builder.append("]");
        return builder.toString();
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Point point)
    {
        float dx = _x - point.getX();
        float dy = _y - point.getY();
        return (dx*dx + dy*dy) <= _radius*_radius;
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        float dx = _x - circle.getX();
        float dy = _y - circle.getY();
        float rsum = _radius + circle.getRadius();
        return (dx*dx + dy*dy) <= rsum*rsum;
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

    /** The location of the circle's center. */
    protected float _x, _y;

    /** The radius of the circle. */
    protected float _radius;
}
