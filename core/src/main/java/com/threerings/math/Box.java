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
 * An axis-aligned box.
 */
public final class Box
    implements Exportable
{
    /** The unit box. */
    public static final Box UNIT = new Box(Vector3f.UNIT_XYZ.negate(), Vector3f.UNIT_XYZ);

    /** The zero box. */
    public static final Box ZERO = new Box(Vector3f.ZERO, Vector3f.ZERO);

    /** The empty box. */
    public static final Box EMPTY = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);

    /** A box that's as large as boxes can get. */
    public static final Box MAX_VALUE = new Box(Vector3f.MIN_VALUE, Vector3f.MAX_VALUE);

    /**
     * Creates a box with the values contained in the supplied minimum and maximum extents.
     */
    public Box (Vector3f minExtent, Vector3f maxExtent)
    {
        set(minExtent, maxExtent);
    }

    /**
     * Copy constructor.
     */
    public Box (Box other)
    {
        set(other);
    }

    /**
     * Creates an empty box.
     */
    public Box ()
    {
        setToEmpty();
    }

    /**
     * Returns a reference to the box's minimum extent.
     */
    public Vector3f getMinimumExtent ()
    {
        return _minExtent;
    }

    /**
     * Returns a reference to the box's maximum extent.
     */
    public Vector3f getMaximumExtent ()
    {
        return _maxExtent;
    }

    /**
     * Returns the center of the box as a new vector.
     */
    public Vector3f getCenter ()
    {
        return getCenter(new Vector3f());
    }

    /**
     * Places the location of the center of the box into the given result vector.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f getCenter (Vector3f result)
    {
        return _minExtent.add(_maxExtent, result).multLocal(0.5f);
    }

    /**
     * Returns the length of the box's diagonal (the distance from minimum to maximum extent).
     */
    public float getDiagonalLength ()
    {
        return _minExtent.distance(_maxExtent);
    }

    /**
     * Returns the length of the box's longest edge.
     */
    public float getLongestEdge ()
    {
        return Math.max(
            Math.max(_maxExtent.x - _minExtent.x, _maxExtent.y - _minExtent.y),
            _maxExtent.z - _minExtent.z);
    }

    /**
     * Determines whether the box is empty (whether any of its minima are greater than their
     * corresponding maxima).
     */
    public boolean isEmpty ()
    {
        return _minExtent.x > _maxExtent.x ||
            _minExtent.y > _maxExtent.y ||
            _minExtent.z > _maxExtent.z;
    }

    /**
     * Initializes this box with the extents of an array of points.
     *
     * @return a reference to this box, for chaining.
     */
    public Box fromPoints (Vector3f... points)
    {
        setToEmpty();
        for (Vector3f point : points) {
            addLocal(point);
        }
        return this;
    }

    /**
     * Expands this box in-place to include the specified point.
     *
     * @return a reference to this box, for chaining.
     */
    public Box addLocal (Vector3f point)
    {
        return add(point, this);
    }

    /**
     * Expands this box to include the specified point.
     *
     * @return a new box containing the result.
     */
    public Box add (Vector3f point)
    {
        return add(point, new Box());
    }

    /**
     * Expands this box to include the specified point, placing the result in the object
     * provided.
     *
     * @return a reference to the result box, for chaining.
     */
    public Box add (Vector3f point, Box result)
    {
        result.getMinimumExtent().set(
            Math.min(_minExtent.x, point.x),
            Math.min(_minExtent.y, point.y),
            Math.min(_minExtent.z, point.z));
        result.getMaximumExtent().set(
            Math.max(_maxExtent.x, point.x),
            Math.max(_maxExtent.y, point.y),
            Math.max(_maxExtent.z, point.z));
        return result;
    }

    /**
     * Expands this box to include the bounds of another box.
     *
     * @return a reference to this box, for chaining.
     */
    public Box addLocal (Box other)
    {
        return add(other, this);
    }

    /**
     * Expands this box to include the bounds of another box.
     *
     * @return a new box containing the result.
     */
    public Box add (Box other)
    {
        return add(other, new Box());
    }

    /**
     * Expands this box to include the bounds of another box, placing the result in the object
     * provided.
     *
     * @return a reference to the result box, for chaining.
     */
    public Box add (Box other, Box result)
    {
        Vector3f omin = other.getMinimumExtent(), omax = other.getMaximumExtent();
        result.getMinimumExtent().set(
            Math.min(_minExtent.x, omin.x),
            Math.min(_minExtent.y, omin.y),
            Math.min(_minExtent.z, omin.z));
        result.getMaximumExtent().set(
            Math.max(_maxExtent.x, omax.x),
            Math.max(_maxExtent.y, omax.y),
            Math.max(_maxExtent.z, omax.z));
        return result;
    }

    /**
     * Finds the intersection between this box and another box and places the result in this box.
     *
     * @return a reference to this box, for chaining.
     */
    public Box intersectLocal (Box other)
    {
        return intersect(other, this);
    }

    /**
     * Finds the intersection between this box and another box.
     *
     * @return a new box containing the result.
     */
    public Box intersect (Box other)
    {
        return intersect(other, new Box());
    }

    /**
     * Finds the intersection between this box and another box and places the result in the
     * provided object.
     *
     * @return a reference to this box, for chaining.
     */
    public Box intersect (Box other, Box result)
    {
        Vector3f omin = other.getMinimumExtent(), omax = other.getMaximumExtent();
        result.getMinimumExtent().set(
            Math.max(_minExtent.x, omin.x),
            Math.max(_minExtent.y, omin.y),
            Math.max(_minExtent.z, omin.z));
        result.getMaximumExtent().set(
            Math.min(_maxExtent.x, omax.x),
            Math.min(_maxExtent.y, omax.y),
            Math.min(_maxExtent.z, omax.z));
        return result;
    }

    /**
     * Transforms this box in-place.
     *
     * @return a reference to this box, for chaining.
     */
    public Box transformLocal (Transform3D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this box.
     *
     * @return a new box containing the result.
     */
    public Box transform (Transform3D transform)
    {
        return transform(transform, new Box());
    }

    /**
     * Transforms this box, placing the result in the provided object.
     *
     * @return a reference to the result box, for chaining.
     */
    public Box transform (Transform3D transform, Box result)
    {
        // the corners of the box cover the eight permutations of ([minX|maxX], [minY|maxY],
        // [minZ|maxZ]).  to find the new minimum and maximum for each element, we transform
        // selecting either the minimum or maximum for each component based on whether it will
        // increase or decrease the total (which depends on the sign of the matrix element).
        transform.update(Transform3D.AFFINE);
        Matrix4f matrix = transform.getMatrix();
        float minx =
            matrix.m00 * (matrix.m00 > 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m10 * (matrix.m10 > 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m20 * (matrix.m20 > 0f ? _minExtent.z : _maxExtent.z) + matrix.m30;
        float miny =
            matrix.m01 * (matrix.m01 > 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m11 * (matrix.m11 > 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m21 * (matrix.m21 > 0f ? _minExtent.z : _maxExtent.z) + matrix.m31;
        float minz =
            matrix.m02 * (matrix.m02 > 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m12 * (matrix.m12 > 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m22 * (matrix.m22 > 0f ? _minExtent.z : _maxExtent.z) + matrix.m32;
        float maxx =
            matrix.m00 * (matrix.m00 < 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m10 * (matrix.m10 < 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m20 * (matrix.m20 < 0f ? _minExtent.z : _maxExtent.z) + matrix.m30;
        float maxy =
            matrix.m01 * (matrix.m01 < 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m11 * (matrix.m11 < 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m21 * (matrix.m21 < 0f ? _minExtent.z : _maxExtent.z) + matrix.m31;
        float maxz =
            matrix.m02 * (matrix.m02 < 0f ? _minExtent.x : _maxExtent.x) +
            matrix.m12 * (matrix.m12 < 0f ? _minExtent.y : _maxExtent.y) +
            matrix.m22 * (matrix.m22 < 0f ? _minExtent.z : _maxExtent.z) + matrix.m32;
        result.getMinimumExtent().set(minx, miny, minz);
        result.getMaximumExtent().set(maxx, maxy, maxz);
        return result;
    }

    /**
     * Projects this box in-place.
     *
     * @return a reference to this box, for chaining.
     */
    public Box projectLocal (Matrix4f matrix)
    {
        return project(matrix, this);
    }

    /**
     * Projects this box.
     *
     * @return a new box containing the result.
     */
    public Box project (Matrix4f matrix)
    {
        return project(matrix, new Box());
    }

    /**
     * Projects this box, placing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Box project (Matrix4f matrix, Box result)
    {
        float minx = +Float.MAX_VALUE, miny = +Float.MAX_VALUE, minz = +Float.MAX_VALUE;
        float maxx = -Float.MAX_VALUE, maxy = -Float.MAX_VALUE, maxz = -Float.MAX_VALUE;
        for (int ii = 0; ii < 8; ii++) {
            float x = ((ii & (1 << 2)) == 0) ? _minExtent.x : _maxExtent.x;
            float y = ((ii & (1 << 1)) == 0) ? _minExtent.y : _maxExtent.y;
            float z = ((ii & (1 << 0)) == 0) ? _minExtent.z : _maxExtent.z;
            float rw = 1f / (matrix.m03*x + matrix.m13*y + matrix.m23*z + matrix.m33);
            float px = (matrix.m00*x + matrix.m10*y + matrix.m20*z + matrix.m30) * rw;
            float py = (matrix.m01*x + matrix.m11*y + matrix.m21*z + matrix.m31) * rw;
            float pz = (matrix.m02*x + matrix.m12*y + matrix.m22*z + matrix.m32) * rw;
            minx = Math.min(minx, px);
            miny = Math.min(miny, py);
            minz = Math.min(minz, pz);
            maxx = Math.max(maxx, px);
            maxy = Math.max(maxy, py);
            maxz = Math.max(maxz, pz);
        }
        result.getMinimumExtent().set(minx, miny, minz);
        result.getMaximumExtent().set(maxx, maxy, maxz);
        return result;
    }

    /**
     * Expands the box in-place by the specified amounts.
     *
     * @return a reference to this box, for chaining.
     */
    public Box expandLocal (float x, float y, float z)
    {
        return expand(x, y, z, this);
    }

    /**
     * Expands the box by the specified amounts.
     *
     * @return a new box containing the result.
     */
    public Box expand (float x, float y, float z)
    {
        return expand(x, y, z, new Box());
    }

    /**
     * Expands the box by the specified amounts, placing the result in the object provided.
     *
     * @return a reference to the result box, for chaining.
     */
    public Box expand (float x, float y, float z, Box result)
    {
        result.getMinimumExtent().set(_minExtent.x - x, _minExtent.y - y, _minExtent.z - z);
        result.getMaximumExtent().set(_maxExtent.x + x, _maxExtent.y + y, _maxExtent.z + z);
        return result;
    }

    /**
     * Sets the parameters of the box to the empty values ({@link Vector3f#MAX_VALUE} and
     * {@link Vector3f#MIN_VALUE} for the minimum and maximum, respectively).
     *
     * @return a reference to this box, for chaining.
     */
    public Box setToEmpty ()
    {
        return set(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
    }

    /**
     * Copies the parameters of another box.
     *
     * @return a reference to this box, for chaining.
     */
    public Box set (Box other)
    {
        return set(other.getMinimumExtent(), other.getMaximumExtent());
    }

    /**
     * Sets the box parameters to the values contained in the supplied vectors.
     *
     * @return a reference to this box, for chaining.
     */
    public Box set (Vector3f minExtent, Vector3f maxExtent)
    {
        _minExtent.set(minExtent);
        _maxExtent.set(maxExtent);
        return this;
    }

    /**
     * Retrieves one of the eight vertices of the box.  The code parameter identifies the vertex
     * with flags indicating which values should be selected from the minimum extent, and which
     * from the maximum extent.  For example, the code 011b selects the vertex with the minimum
     * x, maximum y, and maximum z.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f getVertex (int code, Vector3f result)
    {
        return result.set(
            ((code & (1 << 2)) == 0) ? _minExtent.x : _maxExtent.x,
            ((code & (1 << 1)) == 0) ? _minExtent.y : _maxExtent.y,
            ((code & (1 << 0)) == 0) ? _minExtent.z : _maxExtent.z);
    }

    /**
     * Determines whether this box contains the specified point.
     */
    public boolean contains (Vector3f point)
    {
        return contains(point.x, point.y, point.z);
    }

    /**
     * Determines whether this box contains the specified point.
     */
    public boolean contains (float x, float y, float z)
    {
        return x >= _minExtent.x && x <= _maxExtent.x &&
            y >= _minExtent.y && y <= _maxExtent.y &&
            z >= _minExtent.z && z <= _maxExtent.z;
    }

    /**
     * Returns the sum of the Manhattan distances between the extents of this box and the
     * specified other box.
     */
    public float getExtentDistance (Box other)
    {
        return other.getMinimumExtent().manhattanDistance(_minExtent) +
            other.getMaximumExtent().manhattanDistance(_maxExtent);
    }

    /**
     * Determines whether this box completely contains the specified box.
     */
    public boolean contains (Box other)
    {
        Vector3f omin = other._minExtent, omax = other._maxExtent;
        return omin.x >= _minExtent.x && omax.x <= _maxExtent.x &&
            omin.y >= _minExtent.y && omax.y <= _maxExtent.y &&
            omin.z >= _minExtent.z && omax.z <= _maxExtent.z;
    }

    /**
     * Determines whether this box intersects the specified other box.
     */
    public boolean intersects (Box other)
    {
        Vector3f omin = other._minExtent, omax = other._maxExtent;
        return _maxExtent.x >= omin.x && _minExtent.x <= omax.x &&
            _maxExtent.y >= omin.y && _minExtent.y <= omax.y &&
            _maxExtent.z >= omin.z && _minExtent.z <= omax.z;
    }

    /**
     * Determines whether the specified ray intersects this box.
     */
    public boolean intersects (Ray3D ray)
    {
        Vector3f dir = ray.getDirection();
        return
            Math.abs(dir.x) > FloatMath.EPSILON &&
                (intersectsX(ray, _minExtent.x) || intersectsX(ray, _maxExtent.x)) ||
            Math.abs(dir.y) > FloatMath.EPSILON &&
                (intersectsY(ray, _minExtent.y) || intersectsY(ray, _maxExtent.y)) ||
            Math.abs(dir.z) > FloatMath.EPSILON &&
                (intersectsZ(ray, _minExtent.z) || intersectsZ(ray, _maxExtent.z));
    }

    /**
     * Finds the location of the (first) intersection between the specified ray and this box.
     * This will be the ray origin if the ray starts inside the box.
     *
     * @param result a vector to hold the location of the intersection.
     * @return true if the ray intersects the box (in which case the result vector will be
     * populated with the location of the intersection), false if not.
     */
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        Vector3f origin = ray.getOrigin();
        if (contains(origin)) {
            result.set(origin);
            return true;
        }
        Vector3f dir = ray.getDirection();
        float t = Float.MAX_VALUE;
        if (Math.abs(dir.x) > FloatMath.EPSILON) {
            t = Math.min(t, getIntersectionX(ray, _minExtent.x));
            t = Math.min(t, getIntersectionX(ray, _maxExtent.x));
        }
        if (Math.abs(dir.y) > FloatMath.EPSILON) {
            t = Math.min(t, getIntersectionY(ray, _minExtent.y));
            t = Math.min(t, getIntersectionY(ray, _maxExtent.y));
        }
        if (Math.abs(dir.z) > FloatMath.EPSILON) {
            t = Math.min(t, getIntersectionZ(ray, _minExtent.z));
            t = Math.min(t, getIntersectionZ(ray, _maxExtent.z));
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
        if (!(other instanceof Box)) {
            return false;
        }
        Box obox = (Box)other;
        return _minExtent.equals(obox._minExtent) && _maxExtent.equals(obox._maxExtent);
    }

    /**
     * Helper method for {@link #intersects(Ray3D)}.  Determines whether the ray intersects the box
     * at the plane where x equals the value specified.
     */
    protected boolean intersectsX (Ray3D ray, float x)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (x - origin.x) / dir.x;
        if (t < 0f) {
            return false;
        }
        float iy = origin.y + t*dir.y, iz = origin.z + t*dir.z;
        return iy >= _minExtent.y && iy <= _maxExtent.y &&
            iz >= _minExtent.z && iz <= _maxExtent.z;
    }

    /**
     * Helper method for {@link #intersects(Ray3D)}.  Determines whether the ray intersects the box
     * at the plane where y equals the value specified.
     */
    protected boolean intersectsY (Ray3D ray, float y)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (y - origin.y) / dir.y;
        if (t < 0f) {
            return false;
        }
        float ix = origin.x + t*dir.x, iz = origin.z + t*dir.z;
        return ix >= _minExtent.x && ix <= _maxExtent.x &&
            iz >= _minExtent.z && iz <= _maxExtent.z;
    }

    /**
     * Helper method for {@link #intersects(Ray3D)}.  Determines whether the ray intersects the box
     * at the plane where z equals the value specified.
     */
    protected boolean intersectsZ (Ray3D ray, float z)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (z - origin.z) / dir.z;
        if (t < 0f) {
            return false;
        }
        float ix = origin.x + t*dir.x, iy = origin.y + t*dir.y;
        return ix >= _minExtent.x && ix <= _maxExtent.x &&
            iy >= _minExtent.y && iy <= _maxExtent.y;
    }

    /**
     * Helper method for {@link #getIntersection}.  Finds the <code>t</code> value where the ray
     * intersects the box at the plane where x equals the value specified, or returns
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersectionX (Ray3D ray, float x)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (x - origin.x) / dir.x;
        if (t < 0f) {
            return Float.MAX_VALUE;
        }
        float iy = origin.y + t*dir.y, iz = origin.z + t*dir.z;
        return (iy >= _minExtent.y && iy <= _maxExtent.y &&
            iz >= _minExtent.z && iz <= _maxExtent.z) ? t : Float.MAX_VALUE;
    }

    /**
     * Helper method for {@link #getIntersection}.  Finds the <code>t</code> value where the ray
     * intersects the box at the plane where y equals the value specified, or returns
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersectionY (Ray3D ray, float y)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (y - origin.y) / dir.y;
        if (t < 0f) {
            return Float.MAX_VALUE;
        }
        float ix = origin.x + t*dir.x, iz = origin.z + t*dir.z;
        return (ix >= _minExtent.x && ix <= _maxExtent.x &&
            iz >= _minExtent.z && iz <= _maxExtent.z) ? t : Float.MAX_VALUE;
    }

    /**
     * Helper method for {@link #getIntersection}.  Finds the <code>t</code> value where the ray
     * intersects the box at the plane where z equals the value specified, or returns
     * {@link Float#MAX_VALUE} if there is no such intersection.
     */
    protected float getIntersectionZ (Ray3D ray, float z)
    {
        Vector3f origin = ray.getOrigin(), dir = ray.getDirection();
        float t = (z - origin.z) / dir.z;
        if (t < 0f) {
            return Float.MAX_VALUE;
        }
        float ix = origin.x + t*dir.x, iy = origin.y + t*dir.y;
        return (ix >= _minExtent.x && ix <= _maxExtent.x &&
            iy >= _minExtent.y && iy <= _maxExtent.y) ? t : Float.MAX_VALUE;
    }

    /** The box's minimum extent. */
    protected Vector3f _minExtent = new Vector3f();

    /** The box's maximum extent. */
    protected Vector3f _maxExtent = new Vector3f();
}
