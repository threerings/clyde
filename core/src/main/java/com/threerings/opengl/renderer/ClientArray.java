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
        this(size, type, 0, 0L, arrayBuffer);
    }

    /**
     * Creates an array that draws from part of a buffer object.
     */
    public ClientArray (int size, int type, int stride, long offset, BufferObject arrayBuffer)
    {
        this(size, type, false, stride, offset, arrayBuffer, null);
    }

    /**
     * Creates a simple array that draws from a float buffer.
     */
    public ClientArray (int size, FloatBuffer floatArray)
    {
        this(size, 0, 0L, floatArray);
    }

    /**
     * Creates an array that draws from part of a float buffer.
     */
    public ClientArray (int size, int stride, long offset, FloatBuffer floatArray)
    {
        this(size, GL11.GL_FLOAT, false, stride, offset, null, floatArray);
    }

    /**
     * Creates an array.
     */
    public ClientArray (
        int size, int type, boolean normalized, int stride, long offset,
        BufferObject arrayBuffer, FloatBuffer floatArray)
    {
        set(size, type, normalized, stride, offset, arrayBuffer, floatArray);
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
        return set(
            array.size, array.type, array.normalized, array.stride,
            array.offset, array.arrayBuffer, array.floatArray);
    }

    /**
     * Sets all fields at once.
     *
     * @return a reference to the array, for chaining.
     */
    public ClientArray set (
        int size, int type, boolean normalized, int stride, long offset,
        BufferObject arrayBuffer, FloatBuffer floatArray)
    {
        this.size = size;
        this.type = type;
        this.normalized = normalized;
        this.stride = stride;
        this.offset = offset;
        this.arrayBuffer = arrayBuffer;
        this.floatArray = floatArray;
        return this;
    }

    /**
     * Returns the number of bytes in each element.
     */
    public int getElementBytes ()
    {
        return size * getComponentBytes();
    }

    /**
     * Returns the number of bytes in each component.
     */
    public int getComponentBytes ()
    {
        switch (type) {
            case GL11.GL_BYTE: case GL11.GL_UNSIGNED_BYTE: return 1;
            case GL11.GL_SHORT: case GL11.GL_UNSIGNED_SHORT: return 2;
            case GL11.GL_INT: case GL11.GL_UNSIGNED_INT: case GL11.GL_FLOAT: return 4;
            case GL11.GL_DOUBLE: return 8;
        }
        return -1;
    }

    @Override
    public boolean equals (Object other)
    {
        ClientArray oarray = (ClientArray)other;
        return size == oarray.size && type == oarray.type &&
            normalized == oarray.normalized && stride == oarray.stride &&
            offset == oarray.offset && arrayBuffer == oarray.arrayBuffer &&
            floatArray == oarray.floatArray;
    }

    @Override
    public int hashCode ()
    {
        int result = size;
        result = 31 * result + type;
        result = 31 * result + (normalized ? 1 : 0);
        result = 31 * result + stride;
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        result = 31 * result + (arrayBuffer != null ? arrayBuffer.hashCode() : 0);
        result = 31 * result + (floatArray != null ? floatArray.hashCode() : 0);
        return result;
    }
}
