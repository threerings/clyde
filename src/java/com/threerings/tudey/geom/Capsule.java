//
// $Id$

package com.threerings.tudey.geom;

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
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Line line)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Circle circle)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Rectangle rectangle)
    {
        return false; // TODO
    }

    @Override // documentation inherited
    protected boolean checkIntersects (Capsule capsule)
    {
        return false; // TODO
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;

    /** The radius. */
    protected float _radius;
}
