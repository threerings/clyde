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
 * A two element vector.
 */
public final class Vector2f
    implements Encodable, Streamable
{
    /** A unit vector in the X+ direction. */
    public static final Vector2f UNIT_X = new Vector2f(1f, 0f);

    /** A unit vector in the Y+ direction. */
    public static final Vector2f UNIT_Y = new Vector2f(0f, 1f);

    /** The zero vector. */
    public static final Vector2f ZERO = new Vector2f(0f, 0f);

    /** A vector containing the minimum floating point value for all components
     * (note: the components are -{@link Float#MAX_VALUE}, not {@link Float#MIN_VALUE}). */
    public static final Vector2f MIN_VALUE = new Vector2f(-Float.MAX_VALUE, -Float.MAX_VALUE);

    /** A vector containing the maximum floating point value for all components. */
    public static final Vector2f MAX_VALUE = new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);

    /** The components of the vector. */
    public float x, y;

    /**
     * Creates a vector from two components.
     */
    public Vector2f (float x, float y)
    {
        set(x, y);
    }

    /**
     * Creates a vector from an array of values.
     */
    public Vector2f (float[] values)
    {
        set(values);
    }

    /**
     * Copy constructor.
     */
    public Vector2f (Vector2f other)
    {
        set(other);
    }

    /**
     * Creates a zero vector.
     */
    public Vector2f ()
    {
    }

    /**
     * Computes and returns the dot product of this and the specified other vector.
     */
    public float dot (Vector2f other)
    {
        return dot(other.x, other.y);
    }

    /**
     * Computes and returns the dot product of this and the specified other vector.
     */
    public float dot (float otherX, float otherY)
    {
        return x*otherX + y*otherY;
    }

    /**
     * Negates this vector in-place.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f negateLocal ()
    {
        return negate(this);
    }

    /**
     * Negates this vector.
     *
     * @return a new vector containing the result.
     */
    public Vector2f negate ()
    {
        return negate(new Vector2f());
    }

    /**
     * Negates this vector, storing the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f negate (Vector2f result)
    {
        return result.set(-x, -y);
    }

    /**
     * Normalizes this vector in-place.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f normalizeLocal ()
    {
        return normalize(this);
    }

    /**
     * Normalizes this vector.
     *
     * @return a new vector containing the result.
     */
    public Vector2f normalize ()
    {
        return normalize(new Vector2f());
    }

    /**
     * Normalizes this vector, storing the result in the object supplied.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f normalize (Vector2f result)
    {
        return mult(1f / length(), result);
    }

    /**
     * Returns the angle between this vector and the specified other vector.
     */
    public float angle (Vector2f other)
    {
        float cos = dot(other) / (length() * other.length());
        return cos >= 1f ? 0f : FloatMath.acos(cos);
    }

    /**
     * Returns the direction of a vector pointing from this point to the specified other
     * point.
     */
    public float direction (Vector2f other)
    {
        return FloatMath.atan2(other.y - y, other.x - x);
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
        return (x*x + y*y);
    }

    /**
     * Returns the distance from this vector to the specified other vector.
     */
    public float distance (Vector2f other)
    {
        return FloatMath.sqrt(distanceSquared(other));
    }

    /**
     * Returns the squared distance from this vector to the specified other.
     */
    public float distanceSquared (Vector2f other)
    {
        float dx = x - other.x, dy = y - other.y;
        return dx*dx + dy*dy;
    }

    /**
     * Multiplies this vector in-place by a scalar.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f multLocal (float v)
    {
        return mult(v, this);
    }

    /**
     * Multiplies this vector by a scalar.
     *
     * @return a new vector containing the result.
     */
    public Vector2f mult (float v)
    {
        return mult(v, new Vector2f());
    }

    /**
     * Multiplies this vector by a scalar and places the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f mult (float v, Vector2f result)
    {
        return result.set(x*v, y*v);
    }

    /**
     * Multiplies this vector in-place by another.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f multLocal (Vector2f other)
    {
        return mult(other, this);
    }

    /**
     * Multiplies this vector by another.
     *
     * @return a new vector containing the result.
     */
    public Vector2f mult (Vector2f other)
    {
        return mult(other, new Vector2f());
    }

    /**
     * Multiplies this vector by another, storing the result in the object provided.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f mult (Vector2f other, Vector2f result)
    {
        return result.set(x*other.x, y*other.y);
    }

    /**
     * Adds a vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f addLocal (Vector2f other)
    {
        return add(other, this);
    }

    /**
     * Adds a vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector2f add (Vector2f other)
    {
        return add(other, new Vector2f());
    }

    /**
     * Adds a vector to this one, storing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f add (Vector2f other, Vector2f result)
    {
        return add(other.x, other.y, result);
    }

    /**
     * Subtracts a vector in-place from this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f subtractLocal (Vector2f other)
    {
        return subtract(other, this);
    }

    /**
     * Subtracts a vector from this one.
     *
     * @return a new vector containing the result.
     */
    public Vector2f subtract (Vector2f other)
    {
        return subtract(other, new Vector2f());
    }

    /**
     * Subtracts a vector from this one and places the result in the supplied
     * object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f subtract (Vector2f other, Vector2f result)
    {
        return add(-other.x, -other.y, result);
    }

    /**
     * Adds a vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f addLocal (float x, float y)
    {
        return add(x, y, this);
    }

    /**
     * Adds a vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector2f add (float x, float y)
    {
        return add(x, y, new Vector2f());
    }

    /**
     * Adds a vector to this one and stores the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f add (float x, float y, Vector2f result)
    {
        return result.set(this.x + x, this.y + y);
    }

    /**
     * Adds a scaled vector in-place to this one.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f addScaledLocal (Vector2f other, float v)
    {
        return addScaled(other, v, this);
    }

    /**
     * Adds a scaled vector to this one.
     *
     * @return a new vector containing the result.
     */
    public Vector2f addScaled (Vector2f other, float v)
    {
        return addScaled(other, v, new Vector2f());
    }

    /**
     * Adds a scaled vector to this one and stores the result in the supplied vector.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f addScaled (Vector2f other, float v, Vector2f result)
    {
        return result.set(x + other.x*v, y + other.y*v);
    }

    /**
     * Rotates this vector in-place by the specified angle.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f rotateLocal (float angle)
    {
        return rotate(angle, this);
    }

    /**
     * Rotates this vector by the specified angle.
     *
     * @return a new vector containing the result.
     */
    public Vector2f rotate (float angle)
    {
        return rotate(angle, new Vector2f());
    }

    /**
     * Rotates this vector by the specified angle, storing the result in the vector provided.
     *
     * @return a reference to the result vector, for chaining.
     */
    public Vector2f rotate (float angle, Vector2f result)
    {
        float sina = FloatMath.sin(angle), cosa = FloatMath.cos(angle);
        return result.set(x*cosa - y*sina, x*sina + y*cosa);
    }

    /**
     * Rotates this vector by the specified angle and adds another vector to it, placing the result
     * in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f rotateAndAdd (float angle, Vector2f add, Vector2f result)
    {
        float sina = FloatMath.sin(angle), cosa = FloatMath.cos(angle);
        return result.set(
            x*cosa - y*sina + add.x,
            x*sina + y*cosa + add.y);
    }

    /**
     * Rotates this vector by the specified angle, applies a uniform scale, and adds another vector
     * to it, placing the result in the object provided.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f rotateScaleAndAdd (float angle, float scale, Vector2f add, Vector2f result)
    {
        float sina = FloatMath.sin(angle), cosa = FloatMath.cos(angle);
        return result.set(
            (x*cosa - y*sina)*scale + add.x,
            (x*sina + y*cosa)*scale + add.y);
    }

    /**
     * Linearly interpolates between this and the specified other vector in-place by the supplied
     * amount.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f lerpLocal (Vector2f other, float t)
    {
        return lerp(other, t, this);
    }

    /**
     * Linearly interpolates between this and the specified other vector by the supplied
     * amount.
     *
     * @return a new vector containing the result.
     */
    public Vector2f lerp (Vector2f other, float t)
    {
        return lerp(other, t, new Vector2f());
    }

    /**
     * Linearly interpolates between this and the supplied other vector by the supplied amount,
     * storing the result in the supplied object.
     *
     * @return a reference to the result, for chaining.
     */
    public Vector2f lerp (Vector2f other, float t, Vector2f result)
    {
        return result.set(x + t*(other.x - x), y + t*(other.y - y));
    }

    /**
     * Copies the elements of another vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f set (Vector2f other)
    {
        return set(other.x, other.y);
    }

    /**
     * Copies the elements of an array.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f set (float[] values)
    {
        return set(values[0], values[1]);
    }

    /**
     * Sets all of the elements of the vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector2f set (float x, float y)
    {
        this.x = x;
        this.y = y;
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
    }

    /**
     * Populates the supplied buffer with the contents of this vector.
     *
     * @return a reference to the buffer, for chaining.
     */
    public FloatBuffer get (FloatBuffer buf)
    {
        return buf.put(x).put(y);
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return x + ", " + y;
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
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[" + x + ", " + y + "]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(x) ^ Float.floatToIntBits(y);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Vector2f)) {
            return false;
        }
        Vector2f ovec = (Vector2f)other;
        return (x == ovec.x && y == ovec.y);
    }
}
