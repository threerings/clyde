//
// $Id$

package com.threerings.math;

/**
 * A ray consisting of an origin point and a unit direction vector.
 */
public final class Ray2D
{
    /**
     * Creates a ray with the values contained in the supplied origin point and unit direction
     * vector.
     */
    public Ray2D (Vector2f origin, Vector2f direction)
    {
        set(origin, direction);
    }

    /**
     * Copy constructor.
     */
    public Ray2D (Ray2D other)
    {
        set(other);
    }

    /**
     * Creates an empty (invalid) ray.
     */
    public Ray2D ()
    {
    }

    /**
     * Returns a reference to the ray's point of origin.
     */
    public Vector2f getOrigin ()
    {
        return _origin;
    }

    /**
     * Returns a reference to the ray's unit direction vector.
     */
    public Vector2f getDirection ()
    {
        return _direction;
    }

    /**
     * Transforms this ray in-place.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray2D transformLocal (Transform2D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this ray.
     *
     * @return a new ray containing the result.
     */
    public Ray2D transform (Transform2D transform)
    {
        return transform(transform, new Ray2D());
    }

    /**
     * Transforms this ray, placing the result in the object provided.
     *
     * @return a reference to the result ray, for chaining.
     */
    public Ray2D transform (Transform2D transform, Ray2D result)
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
    public Ray2D set (Ray2D other)
    {
        return set(other.getOrigin(), other.getDirection());
    }

    /**
     * Sets the ray parameters to the values contained in the supplied vectors.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray2D set (Vector2f origin, Vector2f direction)
    {
        _origin.set(origin);
        _direction.set(direction);
        return this;
    }

    /**
     * Determines whether the ray intersects the specified point.
     */
    public boolean intersects (Vector2f pt)
    {
        if (Math.abs(_direction.x) > Math.abs(_direction.y)) {
            float t = (pt.x - _origin.x) / _direction.x;
            return t >= 0f && _origin.y + t*_direction.y == pt.y;
        } else {
            float t = (pt.y - _origin.y) / _direction.y;
            return t >= 0f && _origin.x + t*_direction.x == pt.x;
        }
    }

    /**
     * Finds the intersection between the ray and a line segment with the given start and end
     * points.
     *
     * @return true if the ray intersected the segment (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Vector2f start, Vector2f end, Vector2f result)
    {
        // ray is a + t*b, segment is c + s*d
        float ax = _origin.x, ay = _origin.y;
        float bx = _direction.x, by = _direction.y;
        float cx = start.x, cy = start.y;
        float dx = end.x - start.x, dy = end.y - start.y;

        float divisor = bx*dy - by*dx;
        if (Math.abs(divisor) < FloatMath.EPSILON) {
            // the lines are parallel (or the segment is zero-length)
            float t = Math.min(getIntersection(start), getIntersection(end));
            boolean isect = (t != Float.MAX_VALUE);
            if (isect) {
                _origin.addScaled(_direction, t, result);
            }
            return isect;
        }
        float cxax = cx - ax, cyay = cy - ay;
        float s = (by*cxax - bx*cyay) / divisor;
        if (s < 0f || s > 1f) {
            return false;
        }
        float t = (dy*cxax - dx*cyay) / divisor;
        boolean isect = (t >= 0f);
        if (isect) {
            _origin.addScaled(_direction, t, result);
        }
        return isect;
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "[origin=" + _origin + ", direction=" + _direction + "]";
    }

    /**
     * Returns the parameter of the ray when it intersects the supplied point, or
     * {@link Float.MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersection (Vector2f pt)
    {
        if (Math.abs(_direction.x) > Math.abs(_direction.y)) {
            float t = (pt.x - _origin.x) / _direction.x;
            return (t >= 0f && _origin.y + t*_direction.y == pt.y) ? t : Float.MAX_VALUE;
        } else {
            float t = (pt.y - _origin.y) / _direction.y;
            return (t >= 0f && _origin.x + t*_direction.x == pt.x) ? t : Float.MAX_VALUE;
        }
    }

    /** The ray's point of origin. */
    protected Vector2f _origin = new Vector2f();

    /** The ray's unit direction vector. */
    protected Vector2f _direction = new Vector2f();
}
