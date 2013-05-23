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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;

/**
 * An OpenGL shader object.
 */
public abstract class ShaderObject
{
    /**
     * Creates a shader object for the specified renderer.
     */
    public ShaderObject (Renderer renderer)
    {
        _renderer = renderer;
    }

    /**
     * Returns the OpenGL identifier for this object.
     */
    public final int getId ()
    {
        return _id;
    }

    /**
     * Retrieves and returns the shader object info log.
     */
    public String getInfoLog ()
    {
        // get the length of the log
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
            _id, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, ibuf);

        // then the log itself (if the length is greater than zero)
        int length = ibuf.get(0);
        if (length <= 0) {
            return "";
        }
        ByteBuffer bbuf = BufferUtils.createByteBuffer(length);
        ARBShaderObjects.glGetInfoLogARB(_id, ibuf, bbuf);
        bbuf.limit(length);

        // convert from ASCII and return
        return ASCII_CHARSET.decode(bbuf).toString();
    }

    /**
     * Deletes this object, rendering it unusable.
     */
    public void delete ()
    {
        ARBShaderObjects.glDeleteObjectARB(_id);
        _id = 0;
        _renderer.shaderObjectDeleted();
    }

    @Override
    protected void finalize ()
        throws Throwable
    {
        super.finalize();
        if (_id > 0) {
            _renderer.shaderObjectFinalized(_id);
        }
    }

    /** The renderer that loaded this object. */
    protected Renderer _renderer;

    /** The OpenGL identifier for this object. */
    protected int _id;

    /** The ASCII charset. */
    protected static final Charset ASCII_CHARSET = Charset.forName("US-ASCII");
}
