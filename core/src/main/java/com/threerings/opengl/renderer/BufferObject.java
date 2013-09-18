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
        _renderer.bufferObjectCreated();
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
        setBytes((int)size);
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
        setBytes(data.remaining() * 4);
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
        setBytes(data.remaining() * 2);
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
        _renderer.bufferObjectDeleted(_bytes);
    }

    /**
     * Creates an invalid buffer object (used by the renderer to force reapplication).
     */
    protected BufferObject ()
    {
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.bufferObjectFinalized(_id, _bytes);
        }
    }

    /**
     * Notes the size of the buffer (and notifies the renderer).
     */
    protected void setBytes (int bytes)
    {
        _renderer.bufferObjectResized(bytes - _bytes);
        _bytes = bytes;
    }

    /** The renderer that loaded this buffer. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this buffer. */
    protected int _id;

    /** The current size of the buffer, in bytes. */
    protected int _bytes;
}
