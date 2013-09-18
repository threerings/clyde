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

package com.threerings.opengl.geometry;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;

import com.threerings.opengl.renderer.BufferObject;

/**
 * Base class for dynamic geometry.
 */
public abstract class DynamicGeometry extends Geometry
{
    /**
     * Creates a new dynamic geometry that will draw from a VBO.
     *
     * @param data the array from which vertex data will be copied at each update.
     * @param arrayBuffer the VBO into which the vertex data will be copied.
     */
    public DynamicGeometry (float[] data, BufferObject arrayBuffer)
    {
        this(data, arrayBuffer, null);
    }

    /**
     * Creates a new dynamic geometry that will draw directly from a float buffer.
     *
     * @param data the array from which vertex data will be copied at each update.
     * @param floatArray the buffer into which vertex data will be copied.
     */
    public DynamicGeometry (float[] data, FloatBuffer floatArray)
    {
        this(data, null, floatArray);
    }

    /**
     * Creates a new dynamic geometry.
     *
     * @param data the array from which vertex data will be copied at each update.
     * @param arrayBuffer the VBO into which the vertex data will be copied, or <code>null</code>
     * for none.
     * @param floatArray the buffer into which vertex data will be copied, or <code>null</code>
     * if using a VBO.
     */
    public DynamicGeometry (float[] data, BufferObject arrayBuffer, FloatBuffer floatArray)
    {
        _data = data;
        _arrayBuffer = arrayBuffer;
        _floatArray = (floatArray == null) ? getScratchBuffer(data.length) : floatArray;
    }

    @Override
    public boolean requiresUpdate ()
    {
        return true;
    }

    @Override
    public void update ()
    {
        // update the vertex data
        updateData();

        // copy from array to buffer
        _floatArray.clear();
        _floatArray.put(_data).flip();

        // copy from buffer to vbo if using one
        if (_arrayBuffer != null) {
            _arrayBuffer.setData(_floatArray, ARBBufferObject.GL_STREAM_DRAW_ARB);
        }
    }

    /**
     * Constructor for subclasses.
     */
    protected DynamicGeometry ()
    {
    }

    /**
     * Updates the vertex data.
     */
    protected abstract void updateData ();

    /**
     * Returns a reference to the scratch buffer, (re)creating it if necessary to provide the
     * supplied size.
     */
    protected static FloatBuffer getScratchBuffer (int size)
    {
        if (_scratchBuffer == null || _scratchBuffer.capacity() < size) {
            _scratchBuffer = BufferUtils.createFloatBuffer(size);
        }
        return _scratchBuffer;
    }

    /** The vertex data. */
    protected float[] _data;

    /** The VBO, if we're using one. */
    protected BufferObject _arrayBuffer;

    /** The float array. */
    protected FloatBuffer _floatArray;

    /** The shared scratch buffer used to hold vertex data before copying to the VBO. */
    protected static FloatBuffer _scratchBuffer;
}
