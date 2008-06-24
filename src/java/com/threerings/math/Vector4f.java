//
// $Id$

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

    @Override // documentation inherited
    public String toString ()
    {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return Float.floatToIntBits(x) ^ Float.floatToIntBits(y) ^
            Float.floatToIntBits(z) ^ Float.floatToIntBits(w);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof Vector4f)) {
            return false;
        }
        Vector4f ovec = (Vector4f)other;
        return (x == ovec.x && y == ovec.y && z == ovec.z && w == ovec.w);
    }
}
