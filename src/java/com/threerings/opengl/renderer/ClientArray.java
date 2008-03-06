//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

/**
 * Represents the state of a single client array.
 */
public class ClientArray
{
    /** The number of components in each element. */
    public int size;

    /** The type of the components. */
    public int type;

    /** Whether or not to normalize the components. */
    public boolean normalized;

    /** The stride between adjacent elements. */
    public int stride;

    /** The offset into the buffer. */
    public long offset;

    /** The buffer object, if using one. */
    public BufferObject arrayBuffer;

    /** The float array, if using one. */
    public FloatBuffer floatArray;

    /** Set when the array parameters have changed and must be reapplied. */
    public boolean dirty;

    /**
     * Creates a simple array that draws from a buffer object.
     */
    public ClientArray (int size, int type, BufferObject arrayBuffer)
    {
        this.size = size;
        this.type = type;
        this.arrayBuffer = arrayBuffer;
    }

    /**
     * Creates an array that draws from part of a buffer object.
     */
    public ClientArray (int size, int type, int stride, long offset, BufferObject arrayBuffer)
    {
        this.size = size;
        this.type = type;
        this.stride = stride;
        this.offset = offset;
        this.arrayBuffer = arrayBuffer;
    }

    /**
     * Creates a simple array that draws from a float buffer.
     */
    public ClientArray (int size, FloatBuffer floatArray)
    {
        this.size = size;
        this.type = GL11.GL_FLOAT;
        this.floatArray = floatArray;
    }

    /**
     * Creates an array that draws from part of a float buffer.
     */
    public ClientArray (int size, int stride, long offset, FloatBuffer floatArray)
    {
        this.size = size;
        this.type = GL11.GL_FLOAT;
        this.stride = stride;
        this.offset = offset;
        this.floatArray = floatArray;
    }

    /**
     * Creates an uninitialized array.
     */
    public ClientArray ()
    {
    }

    /**
     * Copies the values in the supplied object into this one.
     *
     * @return a reference to this array, for chaining.
     */
    public ClientArray set (ClientArray array)
    {
        size = array.size;
        type = array.type;
        normalized = array.normalized;
        stride = array.stride;
        offset = array.offset;
        arrayBuffer = array.arrayBuffer;
        floatArray = array.floatArray;
        return this;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        ClientArray oarray = (ClientArray)other;
        return size == oarray.size && type == oarray.type &&
            normalized == oarray.normalized && stride == oarray.stride &&
            offset == oarray.offset && arrayBuffer == oarray.arrayBuffer &&
            floatArray == oarray.floatArray;
    }
}
