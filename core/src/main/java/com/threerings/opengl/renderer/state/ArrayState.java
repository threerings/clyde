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

package com.threerings.opengl.renderer.state;

import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the client array state.
 */
public class ArrayState extends RenderState
{
    /** A state that disables all of the client arrays. */
    public static final ArrayState DISABLED = new ArrayState(0, null, null, null, null, null, null);

    /**
     * Creates a new array state.
     */
    public ArrayState (
        int firstVertexAttribIndex, ClientArray[] vertexAttribArrays,
        ClientArray[] texCoordArrays, ClientArray colorArray, ClientArray normalArray,
        ClientArray vertexArray, BufferObject elementArrayBuffer)
    {
        _firstVertexAttribIndex = firstVertexAttribIndex;
        _vertexAttribArrays = vertexAttribArrays;
        _texCoordArrays = texCoordArrays;
        _colorArray = colorArray;
        _normalArray = normalArray;
        _vertexArray = vertexArray;
        _elementArrayBuffer = elementArrayBuffer;
    }

    /**
     * Returns the attribute index of the first vertex attribute array.
     */
    public int getFirstVertexAttribIndex ()
    {
        return _firstVertexAttribIndex;
    }

    /**
     * Returns a reference to the array of vertex attribute arrays.
     */
    public ClientArray[] getVertexAttribArrays ()
    {
        return _vertexAttribArrays;
    }

    /**
     * Returns a reference to the array of texture coordinate arrays.
     */
    public ClientArray[] getTexCoordArrays ()
    {
        return _texCoordArrays;
    }

    /**
     * Returns a reference to the color array.
     */
    public ClientArray getColorArray ()
    {
        return _colorArray;
    }

    /**
     * Returns a reference to the normal array.
     */
    public ClientArray getNormalArray ()
    {
        return _normalArray;
    }

    /**
     * Returns a reference to the vertex array.
     */
    public ClientArray getVertexArray ()
    {
        return _vertexArray;
    }

    /**
     * Returns a reference to the element array buffer object.
     */
    public BufferObject getElementArrayBuffer ()
    {
        return _elementArrayBuffer;
    }

    @Override
    public int getType ()
    {
        return ARRAY_STATE;
    }

    @Override
    public void apply (Renderer renderer)
    {
        renderer.setArrayState(
            _firstVertexAttribIndex, _vertexAttribArrays, _texCoordArrays,
            _colorArray, _normalArray, _vertexArray, _elementArrayBuffer);
    }

    /** The attribute index of the first vertex attribute array. */
    protected int _firstVertexAttribIndex;

    /** The generic vertex attribute arrays. */
    protected ClientArray[] _vertexAttribArrays;

    /** The texture coordinate arrays. */
    protected ClientArray[] _texCoordArrays;

    /** The color array. */
    protected ClientArray _colorArray;

    /** The normal array. */
    protected ClientArray _normalArray;

    /** The vertex array. */
    protected ClientArray _vertexArray;

    /** The element array buffer. */
    protected BufferObject _elementArrayBuffer;
}
