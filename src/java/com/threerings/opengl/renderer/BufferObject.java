//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBVertexBufferObject;

/**
 * An OpenGL buffer object.
 */
public class BufferObject
{
    /**
     * Creates a new buffer object for the specified renderer.
     */
    public BufferObject (Renderer renderer)
    {
        _renderer = renderer;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        ARBBufferObject.glGenBuffersARB(idbuf);
        _id = idbuf.get(0);
    }

    /**
     * Returns this buffer's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Initializes the data in this buffer.
     */
    public void setData (long size)
    {
        setData(size, ARBBufferObject.GL_STATIC_DRAW_ARB);
    }

    /**
     * Initializes the data in this buffer.
     */
    public void setData (long size, int usage)
    {
        _renderer.setArrayBuffer(this);
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, size, usage);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (FloatBuffer data)
    {
        setData(data, ARBBufferObject.GL_STATIC_DRAW_ARB);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (FloatBuffer data, int usage)
    {
        _renderer.setArrayBuffer(this);
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, data, usage);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (ShortBuffer data)
    {
        setData(data, ARBBufferObject.GL_STATIC_DRAW_ARB);
    }

    /**
     * Sets the data in this buffer.
     */
    public void setData (ShortBuffer data, int usage)
    {
        _renderer.setArrayBuffer(this);
        ARBBufferObject.glBufferDataARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, data, usage);
    }

    /**
     * Sets part of the data in this buffer.
     */
    public void setSubData (long offset, FloatBuffer data)
    {
        _renderer.setArrayBuffer(this);
        ARBBufferObject.glBufferSubDataARB(
            ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offset, data);
    }

    /**
     * Sets part of the data in this buffer.
     */
    public void setSubData (long offset, ShortBuffer data)
    {
        _renderer.setArrayBuffer(this);
        ARBBufferObject.glBufferSubDataARB(
            ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, offset, data);
    }

    /**
     * Deletes this buffer object, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        ARBBufferObject.glDeleteBuffersARB(idbuf);
        _id = 0;
    }

    /**
     * Creates an invalid buffer object (used by the renderer to force reapplication).
     */
    protected BufferObject ()
    {
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.bufferObjectFinalized(_id);
        }
    }

    /** The renderer that loaded this buffer. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this buffer. */
    protected int _id;
}
