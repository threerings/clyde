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

import com.threerings.export.Exportable;

/**
 * An axis-aligned rectangle.
 */
public final class Rect
    implements Exportable
{
    /** The zero rect. */
    public static final Rect ZERO = new Rect(Vector2f.ZERO, Vector2f.ZERO);

    /** The empty rect. */
    public static final Rect EMPTY = new Rect(Vector2f.MAX_VALUE, Vector2f.MIN_VALUE);

    /** A rect that's as large as rects can get. */
    public static final Rect MAX_VALUE = new Rect(Vector2f.MIN_VALUE, Vector2f.MAX_VALUE);

    /**
     * Creates a rectangle with the values contained in the supplied minimum and maximum extents.
     */
    public Rect (Vector2f minExtent, Vector2f maxExtent)
    {
        set(minExtent, maxExtent);
    }

    /**
     * Copy constructor.
     */
    public Rect (Rect other)
    {
        set(other);
    }

    /**
     * Creates an empty rectangle.
     */
    public Rect ()
    {
        setToEmpty();
    }

    /**
     * Returns a reference to the rectangle's minimum extent.
     */
    public Vector2f getMinimumExtent ()
    {
        return _minExtent;
    }

    /**
     * Returns a reference to the rectangle's maximum extent.
     */
    public Vector2f getMaximumExtent ()
    {
        return _maxExtent;
    }

    /**
     * Returns the center of the rectangle as a new vector.
     */
    public Vector2f getCenter ()
    {
        return getCenter(new Vector2f());
    }

    /**
     * Places the location of the center of the rectangle into the given result vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f getCenter (Vector2f result)
    {
        return _minExtent.add(_maxExtent, result).multLocal(0.5f);
    }

    /**
     * Returns the length of the rectangle's longest edge.
     */
    public float getLongestEdge ()
    {
        return Math.max(_maxExtent.x - _minExtent.x, _maxExtent.y - _minExtent.y);
    }

    /**
     * Returns the length of the rectangle's shortest edge.
     */
    public float getShortestEdge ()
    {
        return Math.min(_maxExtent.x - _minExtent.x, _maxExtent.y - _minExtent.y);
    }

    /**
     * Returns the width of the rectangle.
     */
    public float getWidth ()
    {
        return _maxExtent.x - _minExtent.x;
    }

    /**
     * Returns the height of the rectangle.
     */
    public float getHeight ()
    {
        return _maxExtent.y - _minExtent.y;
    }

    /**
     * Determines whether the rect is empty (whether any of its minima are greater than their
     * corresponding maxima).
     */
    public boolean isEmpty ()
    {
        return _minExtent.x > _maxExtent.x || _minExtent.y > _maxExtent.y;
    }

    /**
     * Initializes this rectangle with the extents of an array of points.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect fromPoints (Vector2f... points)
    {
        setToEmpty();
        for (Vector2f point : points) {
            addLocal(point);
        }
        return this;
    }

    /**
     * Expands this rectangle in-place to include the specified point.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect addLocal (Vector2f point)
    {
        return add(point, this);
    }

    /**
     * Expands this rectangle to include the specified point.
     *
     * @return a new rectangle containing the result.
     */
    public Rect add (Vector2f point)
    {
        return add(point, new Rect());
    }

    /**
     * Expands this rectangle to include the specified point, placing the result in the object
     * provided.
     *
     * @return a reference to the result rectangle, for chaining.
     */
    public Rect add (Vector2f point, Rect result)
    {
        result.getMinimumExtent().set(
            Math.min(_minExtent.x, point.x),
            Math.min(_minExtent.y, point.y));
        result.getMaximumExtent().set(
            Math.max(_maxExtent.x, point.x),
            Math.max(_maxExtent.y, point.y));
        return result;
    }

    /**
     * Expands this rectangle to include the bounds of another rectangle.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect addLocal (Rect other)
    {
        return add(other, this);
    }

    /**
     * Expands this rectangle to include the bounds of another rectangle.
     *
     * @return a new rectangle containing the result.
     */
    public Rect add (Rect other)
    {
        return add(other, new Rect());
    }

    /**
     * Expands this rectangle to include the bounds of another rectangle, placing the result in the
     * object provided.
     *
     * @return a reference to the result rectangle, for chaining.
     */
    public Rect add (Rect other, Rect result)
    {
        Vector2f omin = other.getMinimumExtent(), omax = other.getMaximumExtent();
        result.getMinimumExtent().set(
            Math.min(_minExtent.x, omin.x),
            Math.min(_minExtent.y, omin.y));
        result.getMaximumExtent().set(
            Math.max(_maxExtent.x, omax.x),
            Math.max(_maxExtent.y, omax.y));
        return result;
    }

    /**
     * Finds the intersection between this rectangle and another rectangle and places the result in
     * this rectangle.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect intersectLocal (Rect other)
    {
        return intersect(other, this);
    }

    /**
     * Finds the intersection between this rectangle and another rectangle.
     *
     * @return a new rectangle containing the result.
     */
    public Rect intersect (Rect other)
    {
        return intersect(other, new Rect());
    }

    /**
     * Finds the intersection between this rectangle and another rectangle and places the result in
     * the provided object.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect intersect (Rect other, Rect result)
    {
        Vector2f omin = other.getMinimumExtent(), omax = other.getMaximumExtent();
        result.getMinimumExtent().set(
            Math.max(_minExtent.x, omin.x),
            Math.max(_minExtent.y, omin.y));
        result.getMaximumExtent().set(
            Math.min(_maxExtent.x, omax.x),
            Math.min(_maxExtent.y, omax.y));
        return result;
    }

    /**
     * Transforms this rectangle in-place.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect transformLocal (Transform2D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this rectangle.
     *
     * @return a new rectangle containing the result.
     */
    public Rect transform (Transform2D transform)
    {
        return transform(transform, new Rect());
    }

    /**
     * Transforms this rectangle, placing the result in the provided object.
     *
     * @return a reference to the result rectangle, for chaining.
     */
    public Rect transform (Transform2D transform, Rect result)
    {
        // the corners of the rectangle cover the four permutations of ([minX|maxX], [minY|maxY]).
        // to find the new minimum and maximum for each element, we transform selecting either the
        // minimum or maximum for each component based on whether it will increase or decrease the
        // total (which depends on the sign of the matrix element).
        transform.update(Transform3D.AFFINE);
        Matrix3f matrix = transform.getMatrix();
        float minx =
            matrix.m00 * (matrix.m00 > 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m10 * (matrix.m10 > 0f ? _minExtent.y : _maxExtent.y) + matrix.m20;
        float miny =
            matrix.m01 * (matrix.m01 > 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m11 * (matrix.m11 > 0f ? _minExtent.y : _maxExtent.y) + matrix.m21;
        float maxx =
            matrix.m00 * (matrix.m00 < 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m10 * (matrix.m10 < 0f ? _minExtent.y : _maxExtent.y) + matrix.m20;
        float maxy =
            matrix.m01 * (matrix.m01 < 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m11 * (matrix.m11 < 0f ? _minExtent.y : _maxExtent.y) + matrix.m21;
        result.getMinimumExtent().set(minx, miny);
        result.getMaximumExtent().set(maxx, maxy);
        return result;
    }

    /**
     * Expands the rectangle in-place by the specified amounts.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect expandLocal (float x, float y)
    {
        return expand(x, y, this);
    }

    /**
     * Expands the rectangle by the specified amounts.
     *
     * @return a new rectangle containing the result.
     */
    public Rect expand (float x, float y)
    {
        return expand(x, y, new Rect());
    }

    /**
     * Expands the rectangle by the specified amounts, placing the result in the object provided.
     *
     * @return a reference to the result rectangle, for chaining.
     */
    public Rect expand (float x, float y, Rect result)
    {
        result.getMinimumExtent().set(_minExtent.x - x, _minExtent.y - y);
        result.getMaximumExtent().set(_maxExtent.x + x, _maxExtent.y + y);
        return result;
    }

    /**
     * Sets the parameters of the rectangle to the empty values ({@link Vector2f#MAX_VALUE} and
     * {@link Vector2f#MIN_VALUE} for the minimum and maximum, respectively).
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect setToEmpty ()
    {
        return set(Vector2f.MAX_VALUE, Vector2f.MIN_VALUE);
    }

    /**
     * Copies the parameters of another rectangle.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect set (Rect other)
    {
        return set(other.getMinimumExtent(), other.getMaximumExtent());
    }

    /**
     * Sets the rectangle parameters to the values contained in the supplied vectors.
     *
     * @return a reference to this rectangle, for chaining.
     */
    public Rect set (Vector2f minExtent, Vector2f maxExtent)
    {
        _minExtent.set(minExtent);
        _maxExtent.set(maxExtent);
        return this;
    }

    /**
     * Returns a path that goes counter-clockwise around the rectangle, starting and ending at the
     * minimum extent.
     */
    public Vector2f[] getPerimeterPath ()
    {
        return new Vector2f[] {
            getVertex(0), getVertex(2), getVertex(3), getVertex(1), getVertex(0) };
    }

    /**
     * Retrieves one of the four vertices of the rectangle.  The code parameter identifies the
     * vertex with flags indicating which values should be selected from the minimum extent, and
     * which from the maximum extent.  For example, the code 01b selects the vertex with the
     * minimum x and maximum y.
     *
     * @return a new vector containing the result.
     */
    public Vector2f getVertex (int code)
    {
        return getVertex(code, new Vector2f());
    }

    /**
     * Retrieves one of the four vertices of the rectangle.  The code parameter identifies the
     * vertex with flags indicating which values should be selected from the minimum extent, and
     * which from the maximum extent.  For example, the code 01b selects the vertex with the
     * minimum x and maximum y.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f getVertex (int code, Vector2f result)
    {
        return result.set(
            ((code & (1 << 1)) == 0) ? _minExtent.x : _maxExtent.x,
            ((code & (1 << 0)) == 0) ? _minExtent.y : _maxExtent.y);
    }

    /**
     * Determines whether this rectangle contains the specified point.
     */
    public boolean contains (Vector2f point)
    {
        return contains(point.x, point.y);
    }

    /**
     * Determines whether this rectangle contains the specified point.
     */
    public boolean contains (float x, float y)
    {
        return x >= _minExtent.x && x <= _maxExtent.x &&
            y >= _minExtent.y && y <= _maxExtent.y;
    }

    /**
     * Determines whether this rectangle completely contains the specified rectangle.
     */
    public boolean contains (Rect other)
    {
        Vector2f omin = other._minExtent, omax = other._maxExtent;
        return omin.x >= _minExtent.x && omax.x <= _maxExtent.x &&
            omin.y >= _minExtent.y && omax.y <= _maxExtent.y;
    }

    /**
     * Determines whether this rectangle intersects the specified other rectangle.
     */
    public boolean intersects (Rect other)
    {
        return _maxExtent.x >= other._minExtent.x && _minExtent.x <= other._maxExtent.x &&
            _maxExtent.y >= other._minExtent.y && _minExtent.y <= other._maxExtent.y;
    }

    /**
     * Determines whether the specified ray intersects this rectangle.
     */
    public boolean intersects (Ray2D ray)
    {
        Vector2f dir = ray.getDirection();
        return
            Math.abs(dir.x) > FloatMath.EPSILON &&
                (intersectsX(ray, _minExtent.x) || intersectsX(ray, _maxExtent.x)) ||
            Math.abs(dir.y) > FloatMath.EPSILON &&
                (intersectsY(ray, _minExtent.y) || intersectsY(ray, _maxExtent.y));
    }

    /**
     * Finds the location of the (first) intersection between the specified ray and this rectangle.
     * This will be the ray origin if the ray starts inside the rectangle.
     *
     * @param result a vector to hold the location of the intersection.
     * @return true if the ray intersects the rectangle (in which case the result vector will be
     * populated with the location of the intersection), false if not.
     */
    public boolean getIntersection (Ray2D ray, Vector2f result)
    {
        Vector2f origin = ray.getOrigin();
        if (contains(origin)) {
            result.set(origin);
            return true;
        }
        Vector2f dir = ray.getDirection();
        float t = Float.MAX_VALUE;
        if (Math.abs(dir.x) > FloatMath.EPSILON) {
            t = Math.min(t, getIntersectionX(ray, _minExtent.x));
            t = Math.min(t, getIntersectionX(ray, _maxExtent.x));
        }
        if (Math.abs(dir.y) > FloatMath.EPSILON) {
            t = Math.min(t, getIntersectionY(ray, _minExtent.y));
            t = Math.min(t, getIntersectionY(ray, _maxExtent.y));
        }
        if (t == Float.MAX_VALUE) {
            return false;
        }
        origin.addScaled(dir, t, result);
        return true;
    }

    @Override
    public String toString ()
    {
        return "[min=" + _minExtent + ", max=" + _maxExtent + "]";
    }

    @Override
    public int hashCode ()
    {
        return _minExtent.hashCode() + 31*_maxExtent.hashCode();
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Rect)) {
            return false;
        }
        Rect orect = (Rect)other;
        return _minExtent.equals(orect._minExtent) && _maxExtent.equals(orect._maxExtent);
    }

    /**
     * Helper method for {@link #intersects(Ray2D)}.  Determines whether the ray intersects the
     * rectangle at the line where x equals the value specified.
     */
    protected boolean intersectsX (Ray2D ray, float x)
    {
        Vector2f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (x - origin.x) / dir.x;
        if (t < 0f) {
            return false;
        }
        float iy = origin.y + t*dir.y;
        return iy >= _minExtent.y && iy <= _maxExtent.y;
    }

    /**
     * Helper method for {@link #intersects(Ray2D)}.  Determines whether the ray intersects the
     * rectangle at the line where y equals the value specified.
     */
    protected boolean intersectsY (Ray2D ray, float y)
    {
        Vector2f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (y - origin.y) / dir.y;
        if (t < 0f) {
            return false;
        }
        float ix = origin.x + t*dir.x;
        return ix >= _minExtent.x && ix <= _maxExtent.x;
    }

    /**
     * Helper method for {@link #getIntersection}.  Finds the <code>t</code> value where the ray
     * intersects the rectangle at the line where x equals the value specified, or returns
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersectionX (Ray2D ray, float x)
    {
        Vector2f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (x - origin.x) / dir.x;
        if (t < 0f) {
            return Float.MAX_VALUE;
        }
        float iy = origin.y + t*dir.y;
        return (iy >= _minExtent.y && iy <= _maxExtent.y) ? t : Float.MAX_VALUE;
    }

    /**
     * Helper method for {@link #getIntersection}.  Finds the <code>t</code> value where the ray
     * intersects the rectangle at the line where y equals the value specified, or returns
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersectionY (Ray2D ray, float y)
    {
        Vector2f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (y - origin.y) / dir.y;
        if (t < 0f) {
            return Float.MAX_VALUE;
        }
        float ix = origin.x + t*dir.x;
        return (ix >= _minExtent.x && ix <= _maxExtent.x) ? t : Float.MAX_VALUE;
    }

    /** The rectangle's minimum extent. */
    protected Vector2f _minExtent = new Vector2f();

    /** The rectangle's maximum extent. */
    protected Vector2f _maxExtent = new Vector2f();
}
