//
// $Id$

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

    @Override // documentation inherited
    public boolean requiresUpdate ()
    {
        return true;
    }

    @Override // documentation inherited
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
