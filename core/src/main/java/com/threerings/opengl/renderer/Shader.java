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

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;

/**
 * Contains a shader.
 */
public class Shader extends ShaderObject
{
    /**
     * Creates a new shader.
     */
    public Shader (Renderer renderer, int type)
    {
        super(renderer);
        _id = ARBShaderObjects.glCreateShaderObjectARB(type);
        _renderer.shaderObjectCreated();
    }

    /**
     * Sets the source to the shader and compiles it.  If compilation fails (or even if it
     * succeeds), {@link #getInfoLog} can be used to return more information.
     *
     * @return true if the shader compiled successfully, false if there was an error.
     */
    public boolean setSource (String source)
    {
        // convert the source to an ASCII buffer
        ByteBuffer buf = ASCII_CHARSET.encode(source);

        // and copy that to a direct buffer
        buf = BufferUtils.createByteBuffer(buf.remaining()).put(buf);
        buf.rewind();

        // load the source and compile
        ARBShaderObjects.glShaderSourceARB(_id, buf);
        ARBShaderObjects.glCompileShaderARB(_id);

        // check the result
        IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterARB(
            _id, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB, ibuf);
        return (ibuf.get(0) == GL11.GL_TRUE);
    }
}
