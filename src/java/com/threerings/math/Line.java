//
// $Id$

package com.threerings.math;

/**
 * A line consisting of a unit normal and a constant.  All points on the line satisfy the
 * equation <code>Ax + By + C = 0</code>, where (A, B) is the line normal and C is the
 * constant.
 */
public class Line
{
    /** The X axis. */
    public static final Line X_AXIS = new Line(Vector2f.UNIT_Y, 0f);

    /** The Y axis. */
    public static final Line Y_AXIS = new Line(Vector2f.UNIT_X, 0f);

    /** The line constant. */
    public float constant;

    /**
     * Creates a line from the specified normal and constant.
     */
    public Line (Vector2f normal, float constant)
    {
        set(normal, constant);
    }

    /**
     * Creates a line with the specified parameters.
     */
    public Line (float a, float b, float c)
    {
        set(a, b, c);
    }

    /**
     * Copy constructor.
     */
    public Line (Line other)
    {
        set(other);
    }

    /**
     * Creates an empty (invalid) line.
     */
    public Line ()
    {
    }

    /**
     * Returns a reference to the line normal.
     */
    public Vector2f getNormal ()
    {
        return _normal;
    }

    /**
     * Sets this line based on the two points provided.
     *
     * @return a reference to the line (for chaining).
     */
    public Line fromPoints (Vector2f p1, Vector2f p2)
    {
        _normal.set(p1.y - p2.y, p2.x - p1.x).normalizeLocal();
        constant = -_normal.dot(p1);
        return this;
    }

    /**
     * Copies the parameters of another line.
     *
     * @return a reference to this line (for chaining).
     */
    public Line set (Line other)
    {
        return set(other.getNormal(), other.constant);
    }

    /**
     * Sets the parameters of the line.
     *
     * @return a reference to this line (for chaining).
     */
    public Line set (Vector2f normal, float constant)
    {
        return set(normal.x, normal.y, constant);
    }

    /**
     * Sets the parameters of the line.
     *
     * @return a reference to this line (for chaining).
     */
    public Line set (float a, float b, float c)
    {
        _normal.set(a, b);
        constant = c;
        return this;
    }

    /**
     * Computes the intersection of the supplied ray with this line, placing the result
     * in the given vector (if the ray intersects).
     *
     * @return true if the ray intersects the line (in which case the result will contain
     * the point of intersection), false if not.
     */
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        float distance = getDistance(ray);
        if (Float.isNaN(distance) || distance < 0f) {
            return false;
        } else {
            ray.getOrigin().addScaled(ray.getDirection(), distance, result);
            return true;
        }
    }

    /**
     * Computes the signed distance to this line along the specified ray.
     *
     * @return the signed distance, or {Float#NaN} if the ray runs parallel to the line.
     */
    public float getDistance (Ray2D ray)
    {
        float dividend = -getDistance(ray.getOrigin());
        float divisor = _normal.dot(ray.getDirection());
        if (Math.abs(dividend) < FloatMath.EPSILON) {
            return 0f; // origin is on line
        } else if (Math.abs(divisor) < FloatMath.EPSILON) {
            return Float.NaN; // ray is parallel to line
        } else {
            return dividend / divisor;
        }
    }

    /**
     * Computes and returns the signed distance from the line to the specified point.
     */
    public float getDistance (Vector2f pt)
    {
        return _normal.dot(pt) + constant;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return _normal.hashCode() ^ Float.floatToIntBits(constant);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof Line)) {
            return false;
        }
        Line oline = (Line)other;
        return constant == oline.constant && _normal.equals(oline.getNormal());
    }

    /** The line normal. */
    protected Vector2f _normal = new Vector2f();
}
