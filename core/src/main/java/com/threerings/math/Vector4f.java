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
 * A four element vector.
 */
public final class Vector4f
    implements Encodable, Streamable
{
    /** The components of the vector. */
    public float x, y, z, w;

    /**
     * Creates a vector from four components.
     */
    public Vector4f (float x, float y, float z, float w)
    {
        set(x, y, z, w);
    }

    /**
     * Creates a vector from four components.
     */
    public Vector4f (float[] values)
    {
        set(values);
    }

    /**
     * Creates a vector from a float buffer.
     */
    public Vector4f (FloatBuffer buf)
    {
        set(buf);
    }

    /**
     * Copy constructor.
     */
    public Vector4f (Vector4f other)
    {
        set(other);
    }

    /**
     * Creates a zero vector.
     */
    public Vector4f ()
    {
    }

    /**
     * Copies the elements of another vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector4f set (Vector4f other)
    {
        return set(other.x, other.y, other.z, other.w);
    }

    /**
     * Sets all of the elements of the vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector4f set (float[] values)
    {
        return set(values[0], values[1], values[2], values[3]);
    }

    /**
     * Sets all of the elements of the vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector4f set (FloatBuffer buf)
    {
        return set(buf.get(), buf.get(), buf.get(), buf.get());
    }

    /**
     * Sets all of the elements of the vector.
     *
     * @return a reference to this vector, for chaining.
     */
    public Vector4f set (float x, float y, float z, float w)
    {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Populates the supplied buffer with the contents of this vector.
     *
     * @return a reference to the buffer, for chaining.
     */
    public FloatBuffer get (FloatBuffer buf)
    {
        return buf.put(x).put(y).put(z).put(w);
    }

    /**
     * Compares this vector to another with the provided epsilon.
     */
    public boolean epsilonEquals (Vector4f other, float epsilon)
    {
        return
            Math.abs(x - other.x) < epsilon &&
            Math.abs(y - other.y) < epsilon &&
            Math.abs(z - other.z) < epsilon &&
            Math.abs(w - other.w) < epsilon;
    }

    // documentation inherited from interface Encodable
    public String encodeToString ()
    {
        return x + ", " + y + ", " + z + ", " + w;
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
        out.writeFloat(w);
    }

    // documentation inherited from interface Encodable
    public void decodeFromStream (DataInputStream in)
        throws IOException
    {
        set(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
    }

    @Override
    public String toString ()
    {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override
    public int hashCode ()
    {
        return Float.floatToIntBits(x) ^ Float.floatToIntBits(y) ^
            Float.floatToIntBits(z) ^ Float.floatToIntBits(w);
    }

    @Override
    public boolean equals (Object other)
    {
        if (!(other instanceof Vector4f)) {
            return false;
        }
        Vector4f ovec = (Vector4f)other;
        return (x == ovec.x && y == ovec.y && z == ovec.z && w == ovec.w);
    }
}
