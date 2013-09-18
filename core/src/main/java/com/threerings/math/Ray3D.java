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
public final class Ray3D
{
    /**
     * Creates a ray with the values contained in the supplied origin point and unit direction
     * vector.
     */
    public Ray3D (Vector3f origin, Vector3f direction)
    {
        set(origin, direction);
    }

    /**
     * Copy constructor.
     */
    public Ray3D (Ray3D other)
    {
        set(other);
    }

    /**
     * Creates an empty (invalid) ray.
     */
    public Ray3D ()
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
    public Ray3D transformLocal (Transform3D transform)
    {
        return transform(transform, this);
    }

    /**
     * Transforms this ray.
     *
     * @return a new ray containing the result.
     */
    public Ray3D transform (Transform3D transform)
    {
        return transform(transform, new Ray3D());
    }

    /**
     * Transforms this ray, placing the result in the object provided.
     *
     * @return a reference to the result ray, for chaining.
     */
    public Ray3D transform (Transform3D transform, Ray3D result)
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
    public Ray3D set (Ray3D other)
    {
        return set(other.getOrigin(), other.getDirection());
    }

    /**
     * Sets the ray parameters to the values contained in the supplied vectors.
     *
     * @return a reference to this ray, for chaining.
     */
    public Ray3D set (Vector3f origin, Vector3f direction)
    {
        _origin.set(origin);
        _direction.set(direction);
        return this;
    }

    @Override
    public String toString ()
    {
        return "[origin=" + _origin + ", direction=" + _direction + "]";
    }

    /** The ray's point of origin. */
    protected Vector3f _origin = new Vector3f();

    /** The ray's unit direction vector. */
    protected Vector3f _direction = new Vector3f();
}
