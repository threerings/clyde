//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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

    /**
     * Finds the intersection between the ray and a capsule with the given start point, end point,
     * and radius.
     *
     * @return true if the ray intersected the circle (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Vector2f start, Vector2f end, float radius, Vector2f result)
    {
        // compute the segment's line parameters
        float a = start.y - end.y, b = end.x - start.x;
        float len = FloatMath.hypot(a, b);
        if (len < FloatMath.EPSILON) { // start equals end; check as circle
            return getIntersection(start, radius, result);
        }
        float rlen = 1f / len;
        a *= rlen;
        b *= rlen;
        float c = -a*start.x - b*start.y;

        // find out where the origin lies with respect to the top and bottom
        float dist = a*_origin.x + b*_origin.y + c;
        boolean above = (dist > +radius), below = (dist < -radius);
        float x, y;
        if (above || below) { // check the intersection with the top/bottom boundary
            float divisor = a*_direction.x + b*_direction.y;
            if (Math.abs(divisor) < FloatMath.EPSILON) { // lines are parallel
                return false;
            }
            c += (above ? -radius : +radius);
            float t = (-a*_origin.x - b*_origin.y - c) / divisor;
            if (t < 0f) { // wrong direction
                return false;
            }
            x = _origin.x + t*_direction.x;
            y = _origin.y + t*_direction.y;

        } else { // middle; check the origin
            x = _origin.x;
            y = _origin.y;
        }
        // see where the test point lies with respect to the start and end boundaries
        float tmp = a;
        a = b;
        b = -tmp;
        c = -a*start.x - b*start.y;
        dist = a*x + b*y + c;
        if (dist < 0f) { // before start
            return getIntersection(start, radius, result);
        } else if (dist > len) { // after end
            return getIntersection(end, radius, result);
        } else { // middle
            result.set(x, y);
            return true;
        }
    }

    /**
     * Finds the intersection between the ray and a circle with the given center and radius.
     *
     * @return true if the ray intersected the circle (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public boolean getIntersection (Vector2f center, float radius, Vector2f result)
    {
        // see if we start inside the circle
        if (_origin.distanceSquared(center) <= radius*radius) {
            result.set(_origin);
            return true;
        }
        // then if we intersect the circle
        float ax = _origin.x - center.x, ay = _origin.y - center.y;
        float b = 2f*(_direction.x*ax + _direction.y*ay);
        float c = ax*ax + ay*ay - radius*radius;
        float radicand = b*b - 4f*c;
        if (radicand < 0f) {
            return false;
        }
        float t = (-b - FloatMath.sqrt(radicand)) * 0.5f;
        boolean isect = (t >= 0f);
        if (isect) {
            _origin.addScaled(_direction, t, result);
        }
        return isect;
    }

    /**
     * Resturns the nearest point on the Ray to the supplied point.
     */
    public Vector2f getNearestPoint (Vector2f point, Vector2f result)
    {
        if (result == null) {
            result = new Vector2f();
        }
        float r = point.subtract(_origin).dot(_direction);
        result.set(_origin.add(_direction.mult(r)));
        return result;
    }

    @Override
    public String toString ()
    {
        return "[origin=" + _origin + ", direction=" + _direction + "]";
    }

    /**
     * Returns the parameter of the ray when it intersects the supplied point, or
     * {@link Float#MAX_VALUE} if there is no such intersection.
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
