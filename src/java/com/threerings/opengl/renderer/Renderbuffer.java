//
// $Id$

package com.threerings.opengl.renderer;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;

/**
 * An OpenGL render buffer object.
 */
public class Renderbuffer
{
    /**
     * Creates a render buffer object for the specified renderer.
     */
    public Renderbuffer (Renderer renderer)
    {
        _renderer = renderer;
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        EXTFramebufferObject.glGenRenderbuffersEXT(idbuf);
        _id = idbuf.get(0);
    }

    /**
     * Returns this render buffer's OpenGL identifier.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Sets the storage parameters of this buffer.
     */
    public void setStorage (int format, int width, int height)
    {
        _renderer.setRenderbuffer(this);
        EXTFramebufferObject.glRenderbufferStorageEXT(
            EXTFramebufferObject.GL_RENDERBUFFER_EXT, format, width, height);
    }

    /**
     * Deletes this render buffer, rendering it unusable.
     */
    public void delete ()
    {
        IntBuffer idbuf = BufferUtils.createIntBuffer(1);
        idbuf.put(_id).rewind();
        EXTFramebufferObject.glDeleteRenderbuffersEXT(idbuf);
        _id = 0;
    }

    @Override // documentation inherited
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.renderbufferFinalized(_id);
        }
    }

    /** The renderer responsible for this render buffer. */
    protected Renderer _renderer;

    /** The OpenGL identifer for the render buffer. */
    protected int _id;
}
