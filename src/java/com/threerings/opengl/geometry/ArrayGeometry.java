//
// $Id$

package com.threerings.opengl.geometry;

import java.nio.Buffer;
import java.util.IdentityHashMap;

import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * Represents a piece of non-indexed geometry data.
 */
public class ArrayGeometry extends Geometry
{
    /**
     * Default constructor.
     */
    public ArrayGeometry (
        int mode, boolean solid, Box bounds, ClientArray[] texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray,
        int first, int count)
    {
        super(mode, solid, bounds, texCoordArrays, colorArray, normalArray, vertexArray);
        _first = first;
        _count = count;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ArrayGeometry ()
    {
    }

    /**
     * Returns the starting array index.
     */
    public int getFirst ()
    {
        return _first;
    }

    /**
     * Returns the number of indices to render.
     */
    public int getCount ()
    {
        return _count;
    }

    @Override // documentation inherited
    protected ArrayState createArrayState (Renderer renderer, boolean compile)
    {
        if (compile) {
            IdentityHashMap<Buffer, BufferObject> bufobjs =
                new IdentityHashMap<Buffer, BufferObject>();
            return new ArrayState(
                0, null,
                compileArrays(renderer, bufobjs, _texCoordArrays),
                compileArray(renderer, bufobjs, _colorArray),
                compileArray(renderer, bufobjs, _normalArray),
                compileArray(renderer, bufobjs, _vertexArray),
                null);
        } else {
            return new ArrayState(
                0, null, _texCoordArrays, _colorArray, _normalArray, _vertexArray, null);
        }
    }

    @Override // documentation inherited
    protected SimpleBatch.DrawCommand createDrawCommand ()
    {
        return new SimpleBatch.DrawArrays(_mode, _first, _count);
    }

    /** The starting array index. */
    protected int _first;

    /** The number of indices to render. */
    protected int _count;
}
