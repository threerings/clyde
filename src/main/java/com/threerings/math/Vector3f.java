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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.FloatBuffer;

import com.samskivert.util.StringUtil;

import com.threerings.io.Streamable;

import com.threerings.export.Encodable;

/**
 * A three element vector.
 */
public final class Vector3f
    implements Encodable, Streamable
{
    /** A unit vector in the X+ direction. */
    public static final Vector3f UNIT_X = new Vector3f(1f, 0f, 0f);

    /** A unit vector in the Y+ direction. */
    public static final Vector3f UNIT_Y = new Vector3f(0f, 1f, 0f);

    /** A unit vector in the Z+ direction. */
    public static final Vector3f UNIT_Z = new Vector3f(0f, 0f, 1f);

    /** A vector containing unity for all components. */
    public static final Vector3f UNIT_XYZ = new Vector3f(1f, 1f, 1f);

    /** A normalized version of UNIT_XYZ. */
    public static final Vector3f NORMAL_XYZ = UNIT_XYZ.normalize();

    /** The zero vector. */
    public static final Vector3f ZERO = new Vector3f(0f, 0f, 0f);

    /** A vector containing the minimum floating point value for all components
     * (note: the components are -{@link Float#MAX_VALUE}, not {@link Float#MIN_VALUE}). */
    public static final Vector3f MIN_VALUE =
        new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

    /** A vector containing the maximum floating point value for all components. */
    public static final Vector3f MAX_VALUE =
        new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

    /** The components of the vector. */
    public float x, y, z;

    /**
     * Creates a vector from three components.
     */
    public Vector3f (float x, float y, float z)
    {
        set(x, y, z);
    }

    /**
     * Creates a vector from an array of values.
     */
    public Vector3f (float[] values)
    {
        set(values);
    }

    /**
     * Copy constructor.
     */
    public Vector3f (Vector3f other)
    {
        set(other);
    }

    /**
     * Creates a zero vector.
     */
    public Vector3f ()
    {
    }

    /**
     * Computes and returns the dot product of this and the specified other vector.
     */
    public float dot (Vector3f other)
    {
        return x*other.x + y*other.y + z*other.z;
    }

    /**
     * Computes the cross product of this and the specified other vector, storing the result
     * in this vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f crossLocal (Vector3f other)
    {
        return cross(other, this);
    }

    /**
     * Computes the cross product of this and the specified other vector.
     *
     * @return a new vector containing the result.
     */
    public Vector3f cross (Vector3f other)
    {
        return cross(other, new Vector3f());
    }

    /**
     * Computes the cross product of this and the specified other vector, placing the result
     * in the object supplied.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f cross (Vector3f other, Vector3f result)
    {
        return result.set(
            y*other.z - z*other.y,
            z*other.x - x*other.z,
            x*other.y - y*other.x);
    }

    /**
     * Computes the triple product of this and the specified other vectors, which is equal to
     * <code>this.dot(b.cross(c))</code>.
     */
    public float triple (Vector3f b, Vector3f c)
    {
        return x*(b.y*c.z - b.z*c.y) + y*(b.z*c.x - b.x*c.z) + z*(b.x*c.y - b.y*c.x);
    }

    /**
     * Negates this vector in-place.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f negateLocal ()
    {
        return negate(this);
    }

    /**
     * Negates this vector.
     *
     * @return a new vector containing the result.
     */
    public Vector3f negate ()
    {
        return negate(new Vector3f());
    }

    /**
     * Negates this vector, storing the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f negate (Vector3f result)
    {
        return result.set(-x, -y, -z);
    }

    /**
     * Normalizes this vector in-place.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f normalizeLocal ()
    {
        return normalize(this);
    }

    /**
     * Normalizes this vector.
     *
     * @return a new vector containing the result.
     */
    public Vector3f normalize ()
    {
        return normalize(new Vector3f());
    }

    /**
     * Normalizes this vector, storing the result in the object supplied.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f normalize (Vector3f result)
    {
        return mult(1f / length(), result);
    }

    /**
     * Returns the angle between this vector and the specified other vector.
     */
    public float angle (Vector3f other)
    {
        return FloatMath.acos(dot(other) / (length() * other.length()));
    }

    /**
     * Returns the length of this vector.
     */
    public float length ()
    {
        return FloatMath.sqrt(lengthSquared());
    }

    /**
     * Returns the squared length of this vector.
     */
    public float lengthSquared ()
    {
        return (x*x + y*y + z*z);
    }

    /**
     * Returns the distance from this vector to the specified other vector.
     */
    public float distance (Vector3f other)
    {
        return FloatMath.sqrt(distanceSquared(other));
    }

    /**
     * Returns the squared distance from this vector to the specified other.
     */
    public float distanceSquared (Vector3f other)
    {
        float dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx*dx + dy*dy + dz*dz;
    }

    /**
     * Returns the Manhattan distance between this vector and the specified other.
     */
    public float manhattanDistance (Vector3f other)
    {
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }

    /**
     * Multiplies this vector in-place by a scalar.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f multLocal (float v)
    {
        return mult(v, this);
    }

    /**
     * Multiplies this vector by a scalar.
     *
     * @return a new vector containing the result.
     */
    public Vector3f mult (float v)
    {
        return mult(v, new Vector3f());
    }

    /**
     * Multiplies this vector by a scalar and places the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f mult (float v, Vector3f result)
    {
        return result.set(x*v, y*v, z*v);
    }

    /**
     * Multiplies this vector in-place by another.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f multLocal (Vector3f other)
    {
        return mult(other, this);
    }

    /**
     * Multiplies this vector by another.
     *
     * @return a new vector containing the result.
     */
    public Vector3f mult (Vector3f other)
    {
        return mult(other, new Vector3f());
    }

    /**
     * Multiplies this vector by another, storing the result in the object provided.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector3f mult (Vector3f other, Vector3f result)
    {
        return result.set(x*other.x, y*other.y, z*other.z);
    }

    /**
     * Adds a vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f addLocal (Vector3f other)
    {
        return add(other, this);
    }

    /**
     * Adds a vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector3f add (Vector3f other)
    {
        return add(other, new Vector3f());
    }

    /**
     * Adds a vector to this one, storing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f add (Vector3f other, Vector3f result)
    {
        return add(other.x, other.y, other.z, result);
    }

    /**
     * Subtracts a vector in-place from this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f subtractLocal (Vector3f other)
    {
        return subtract(other, this);
    }

    /**
     * Subtracts a vector from this one.
     *
     * @return a new vector containing the result.
     */
    public Vector3f subtract (Vector3f other)
    {
        return subtract(other, new Vector3f());
    }

    /**
     * Subtracts a vector from this one and places the result in the supplied
     * object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f subtract (Vector3f other, Vector3f result)
    {
        return add(-other.x, -other.y, -other.z, result);
    }

    /**
     * Adds a vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f addLocal (float x, float y, float z)
    {
        return add(x, y, z, this);
    }

    /**
     * Adds a vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector3f add (float x, float y, float z)
    {
        return add(x, y, z, new Vector3f());
    }

    /**
     * Adds a vector to this one and stores the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f add (float x, float y, float z, Vector3f result)
    {
        return result.set(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Adds a scaled vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f addScaledLocal (Vector3f other, float v)
    {
        return addScaled(other, v, this);
    }

    /**
     * Adds a scaled vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector3f addScaled (Vector3f other, float v)
    {
        return addScaled(other, v, new Vector3f());
    }

    /**
     * Adds a scaled vector to this one and stores the result in the supplied vector.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f addScaled (Vector3f other, float v, Vector3f result)
    {
        return result.set(x + other.x*v, y + other.y*v, z + other.z*v);
    }

    /**
     * Linearly interpolates between this and the specified other vector in-place by the supplied
     * amount.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f lerpLocal (Vector3f other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other vector by the supplied
     * amount.
     *
     * @return a new vector containing the result.
     */
    public Vector3f lerp (Vector3f other, float t)
    {
        return lerp(other, t, new Vector3f());
    }

    /**
     * Linearly interpolates between this and the supplied other vector by the supplied amount,
     * storing the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector3f lerp (Vector3f other, float t, Vector3f result)
    {
        return result.set(x + t*(other.x - x), y + t*(other.y - y), z + t*(other.z - z));
    }

    /**
     * Copies the elements of another vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f set (Vector3f other)
    {
        return set(other.x, other.y, other.z);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f set (float[] values)
    {
        return set(values[0], values[1], values[2]);
    }

    /**
     * Sets all of the elements of the vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector3f set (float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Returns the element at the idx'th position of the vector.
     */
    public float get (int idx)
    {
        switch (idx) {
            case 0: return x;
            case 1: return y;
            case 2: return z;
        }
        throw new IndexOutOfBoundsException(Integer.toString(idx));
    }

    /**
     * Populates the supplied array with the contents of this vector.
     */
    public void get (float[] values)
    {
        values[0] = x;
        values[1] = y;
        values[2] = z;
    }

    /**
     * Populates the supplied buffer with the contents of this vector.
     *
     * @return a reference to the buffer, for chaining.
     */
    public FloatBuffer get (FloatBuffer buf)
    {
        return buf.put(x).put(y).put(z);
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return x + ", " + y + ", " + z;
    }

    // documentation inherited from interface Encodable
    public void decodeFromString (String string)
        throws Exception
    {
        set(StringUtil.parseFloatArray(string));
    }

    // documentation inherited from interface Encodable
    public void encodeToStream (DataOutputStream out)
        throws IOException
    {
        out.writeFloat(x);
        out.writeFloat(y);
        out.writeFloat(z);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(x) ^ Float.floatToIntBits(y) ^ Float.floatToIntBits(z);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Vector3f)) {
            return false;
        }
        Vector3f ovec = (Vector3f)other;
        return (x == ovec.x && y == ovec.y && z == ovec.z);
    }
}
