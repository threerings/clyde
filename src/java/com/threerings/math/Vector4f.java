//
// $Id$

package com.threerings.math;

import java.nio.FloatBuffer;

/**
 * A four element vector.
 */
public final class Vector4f
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

    @Override // documentation inherited
    public String toString ()
    {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        Vector4f ovec = (Vector4f)other;
        return (x == ovec.x && y == ovec.y && z == ovec.z && w == ovec.w);
    }
}
