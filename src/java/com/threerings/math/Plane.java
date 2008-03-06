//
// $Id$

package com.threerings.math;

import java.nio.DoubleBuffer;

/**
 * A plane consisting of a unit normal and a constant.  All points on the plane satisfy the
 * equation <code>Ax + By + Cz + D = 0</code>, where (A, B, C) is the plane normal and D is the
 * constant.
 */
public class Plane
{
    /** The X/Y plane. */
    public static final Plane XY_PLANE = new Plane(Vector3f.UNIT_Z, 0f);

    /** The X/Z plane. */
    public static final Plane XZ_PLANE = new Plane(Vector3f.UNIT_Y, 0f);

    /** The Y/Z plane. */
    public static final Plane YZ_PLANE = new Plane(Vector3f.UNIT_X, 0f);

    /** The plane constant. */
    public float constant;

    /**
     * Creates a plane from the specified normal and constant.
     */
    public Plane (Vector3f normal, float constant)
    {
        set(normal, constant);
    }

    /**
     * Copy constructor.
     */
    public Plane (Plane other)
    {
        set(other);
    }

    /**
     * Creates an empty (invalid) plane.
     */
    public Plane ()
    {
    }

    /**
     * Returns a reference to the plane normal.
     */
    public Vector3f getNormal ()
    {
        return _normal;
    }

    /**
     * Sets this plane based on the three points provided.
     *
     * @return a reference to the plane (for chaining).
     */
    public Plane fromPoints (Vector3f p1, Vector3f p2, Vector3f p3)
    {
        // compute the normal by taking the cross product of the two vectors formed
        p2.subtract(p1, _v1);
        p3.subtract(p1, _v2);
        _v1.cross(_v2, _normal).normalizeLocal();

        // use the first point to determine the constant
        constant = -_normal.dot(p1);
        return this;
    }

    /**
     * Copies the parameters of another plane.
     *
     * @return a reference to this plane (for chaining).
     */
    public Plane set (Plane other)
    {
        return set(other.getNormal(), other.constant);
    }

    /**
     * Sets the parameters of the plane.
     *
     * @return a reference to this plane (for chaining).
     */
    public Plane set (Vector3f normal, float constant)
    {
        _normal.set(normal);
        this.constant = constant;
        return this;
    }

    /**
     * Computes the intersection of the supplied ray with this plane, placing the result
     * in the given vector (if the ray intersects).
     *
     * @return true if the ray intersects the plane (in which case the result will contain
     * the point of intersection), false if not.
     */
    public boolean getIntersection (Ray ray, Vector3f result)
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
     * Computes the signed distance to this plane along the specified ray.
     *
     * @return the signed distance, or {Float#NaN} if the ray runs parallel to the plane.
     */
    public float getDistance (Ray ray)
    {
        float dividend = -getDistance(ray.getOrigin());
        float divisor = _normal.dot(ray.getDirection());
        if (Math.abs(dividend) < FloatMath.EPSILON) {
            return 0f; // origin is on plane
        } else if (Math.abs(divisor) < FloatMath.EPSILON) {
            return Float.NaN; // ray is parallel to plane
        } else {
            return dividend / divisor;
        }
    }

    /**
     * Computes and returns the signed distance from the plane to the specified point.
     */
    public float getDistance (Vector3f pt)
    {
        return _normal.dot(pt) + constant;
    }

    /**
     * Stores the contents of this plane into the specified buffer.
     */
    public DoubleBuffer get (DoubleBuffer buf)
    {
        return buf.put(_normal.x).put(_normal.y).put(_normal.z).put(constant);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof Plane)) {
            return false;
        }
        Plane oplane = (Plane)other;
        return constant == oplane.constant && _normal.equals(oplane.getNormal());
    }

    /** The plane normal. */
    protected Vector3f _normal = new Vector3f();

    /** Working vectors for computation. */
    protected Vector3f _v1 = new Vector3f(), _v2 = new Vector3f();
}
