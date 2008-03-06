//
// $Id$

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

    @Override // documentation inherited
    public int getType ()
    {
        return ARRAY_STATE;
    }

    @Override // documentation inherited
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
