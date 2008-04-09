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
        return getMinimumDistanceSquared(line.getX1(), line.getY1(), line.getX2(), line.getY2()) <= _radius*_radius;
/*
        // check if the line starts or ends inside the capsule
        if (getMinimumDistanceSquared(line.getX1(), line.getY1()) <= _radius*_radius ||
                getMinimumDistanceSquared(line.getX2(), line.getY2()) <= _radius*_radius) {
            return true;
        }
        // otherwise check if the line intersects the capsule's line
        return GeomUtil.checkIntersects(_x1, _y1, _x2, _y2,
            line.getX1(), line.getY1(), line.getX2(), line.getY2());
*/
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
/*
        float cradius2 = _radius*_radius + capsule.getRadius()*capsule.getRadius();
        // check if the other capsule starts or ends inside the capsule
        if (getMinimumDistanceSquared(capsule.getX1(), capsule.getY1()) <= cradius2 ||
                getMinimumDistanceSquared(capsule.getX2(), capsule.getY2()) <= cradius2) {
            return true;
        }
        // otherwise check if the two capsule lines intersect
        return GeomUtil.checkIntersects(_x1, _y1, _x2, _y2,
            capsule.getX1(), capsule.getY1(), capsule.getX2(), capsule.getY2());
*/
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
System.out.println("getMinimumDistanceSquared:");
        //NOTE: adapted from http://local.wasp.uwa.edu.au/~pbourke/geometry/lineline3d/
        float vx = _x2 - _x1, vy = _y2 - _y1; // p21
        float ox = x2 - x1, oy = y2 - y1; // p43
        float ux = _x1 - x1, uy = _y1 - y1; // p13

        float duo = ux*ox + uy*oy;
        float dov = ox*vx + oy*vy;
        float duv = ux*vx + uy*vy;
        float dvv = vx*vx + vy*vy;
        float doo = ox*ox + oy*oy;

        float d = dvv*doo - dov*dov;
        if (FloatMath.epsilonEquals(0f, d)) {
            return -1f; //TODO
        }
        float n = duo*dov - duv*doo;

        // find the parameters
        float s = n / d;
        float t = (duo + dov * s) / doo;
System.out.println("s=" + s + ", t=" + t);
        if (s < 0f) {
            if (t < 0f) {
                return GeomUtil.getMinimumDistanceSquared(x1, y1, x2, y2, _x1, _y1);
            } else if (t > 1f) {
            } else {
            }
        } else if (s > 1f) {
            return GeomUtil.getMinimumDistanceSquared(x1, y1, x2, y2, _x2, _y2);
        }

        if (t < 0f) {
            return GeomUtil.getMinimumDistanceSquared(_x1, _y1, _x2, _y2, x1, y1);
        } else if (t > 1f) {
            return GeomUtil.getMinimumDistanceSquared(_x1, _y1, _x2, _y2, x2, y2);
        }

        // find the closest points
        float ivx = _x1 + s * vx, ivy = _y1 + s * vy;
        float iox = x1 + t * ox, ioy = y1 + t * oy;

System.out.println(new Point(ivx, ivy));
System.out.println(new Point(iox, ioy));

        // find the distance between the closest points
        float dist2 = (iox-ivx)*(iox-ivx) + (ioy-ivy)*(ioy-ivy);
System.out.println("dist2=" + dist2);

        return dist2;
    }

    /** The first vertex. */
    protected float _x1, _y1;

    /** The second vertex. */
    protected float _x2, _y2;

    /** The radius. */
    protected float _radius;
}
