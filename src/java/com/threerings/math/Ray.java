//
// $Id$

package com.threerings.math;

/**
 * A ray consisting of an origin point and a unit direction vector.
 */
public final class Ray
{
    /**
     * Creates a ray with the values contained in the supplied origin point and unit direction
     * vector.
     */
    public Ray (Vector3f origin, Vector3f direction)
    {
        set(origin, direction);
    }

    /**
     * Copy constructor.
     */
    public Ray (Ray other)
    {
        set(other);
    }

    /**
     * Creates an empty (invalid) ray.
     */
    public Ray ()
    {
    }

    /**
     * Returns a reference to the ray's point of origin.
     */
    public Vector3f getOrigin ()
    {
        return _origin;
    }

    /**
     * Returns a reference to the ray's unit direction vector.
     */
    public Vector3f getDirection ()
    {
        return _direction;
    }

    /**
     * Transforms this ray in-place.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray transformLocal (Transform3D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this ray.
     *
     * @return a new ray containing the result.
     */
    public Ray transform (Transform3D transform)
    {
        return transform(transform, new Ray());
    }

    /**
     * Transforms this ray, placing the result in the object provided.
     *
     * @return a reference to the result ray, for chaining.
     */
    public Ray transform (Transform3D transform, Ray result)
    {
        transform.transformPoint(_origin, result._origin);
        transform.transformVector(_direction, result._direction).normalizeLocal();
        return result;
    }

    /**
     * Copies the parameters of another ray.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray set (Ray other)
    {
        return set(other.getOrigin(), other.getDirection());
    }

    /**
     * Sets the ray parameters to the values contained in the supplied vectors.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray set (Vector3f origin, Vector3f direction)
    {
        _origin.set(origin);
        _direction.set(direction);
        return this;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[origin=" + _origin + ", direction=" + _direction + "]";
    }

    /** The ray's point of origin. */
    protected Vector3f _origin = new Vector3f();

    /** The ray's unit direction vector. */
    protected Vector3f _direction = new Vector3f();
}
