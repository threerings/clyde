//
// $Id$

package com.threerings.opengl.geometry;

import java.nio.Buffer;
import java.nio.ShortBuffer;
import java.util.IdentityHashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * Represents a piece of indexed geometry data.
 */
public class IndexedGeometry extends Geometry
{
    /**
     * Default constructor.
     */
    public IndexedGeometry (
        int mode, boolean solid, Box bounds, ClientArray[] texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray,
        int start, int end, ShortBuffer indices)
    {
        super(mode, solid, bounds, texCoordArrays, colorArray, normalArray, vertexArray);
        _start = start;
        _end = end;
        _indices = indices;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public IndexedGeometry ()
    {
    }

    /**
     * Returns the minimum index.
     */
    public int getStart ()
    {
        return _start;
    }

    /**
     * Returns the maximum index.
     */
    public int getEnd ()
    {
        return _end;
    }

    /**
     * Returns the index buffer.
     */
    public ShortBuffer getIndices ()
    {
        return _indices;
    }

    @Override // documentation inherited
    protected ArrayState createArrayState (Renderer renderer, boolean compile)
    {
        BufferObject ibobj = null;
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            ibobj = new BufferObject(renderer);
            ibobj.setData(_indices);
        }
        if (compile) {
            IdentityHashMap<Buffer, BufferObject> bufobjs =
                new IdentityHashMap<Buffer, BufferObject>();
            return new ArrayState(
                0, null,
                compileArrays(renderer, bufobjs, _texCoordArrays),
                compileArray(renderer, bufobjs, _colorArray),
                compileArray(renderer, bufobjs, _normalArray),
                compileArray(renderer, bufobjs, _vertexArray),
                ibobj);
        } else {
            return new ArrayState(
                0, null, _texCoordArrays, _colorArray, _normalArray, _vertexArray, ibobj);
        }
    }

    @Override // documentation inherited
    protected SimpleBatch.DrawCommand createDrawCommand ()
    {
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            return SimpleBatch.createDrawBufferElements(
                _mode, _start, _end, _indices.remaining(), GL11.GL_UNSIGNED_SHORT, 0);
        } else {
            return SimpleBatch.createDrawShortElements(_mode, _start, _end, _indices);
        }
    }

    /** The minimum index. */
    protected int _start;

    /** The maximum index. */
    protected int _end;

    /** The index buffer. */
    protected ShortBuffer _indices;
}
