//
// $Id$

package com.threerings.opengl.geometry;

import java.nio.Buffer;
import java.util.IdentityHashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * Represents a piece of geometry data in a format accessible to materials.
 */
public abstract class Geometry
{
    /**
     * Default constructor.
     */
    public Geometry (
        int mode, boolean solid, Box bounds, ClientArray[] texCoordArrays,
        ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray)
    {
        _mode = mode;
        _solid = solid;
        _bounds.set(bounds);
        _texCoordArrays = texCoordArrays;
        _colorArray = colorArray;
        _normalArray = normalArray;
        _vertexArray = vertexArray;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public Geometry ()
    {
    }

    /**
     * Returns the type of primitives to render (@link {GL11#GL_LINES}, etc).
     */
    public int getMode ()
    {
        return _mode;
    }

    /**
     * Checks whether or not this geometry is polygonal (i.e., whether back-face culling state
     * matters.
     */
    public boolean isPolygonal ()
    {
        return !(_mode == GL11.GL_POINTS || _mode == GL11.GL_LINE_STRIP ||
            _mode == GL11.GL_LINE_LOOP || _mode == GL11.GL_LINES);
    }

    /**
     * Checks whether or not this geometry is "solid" (i.e., whether we can enable back-face
     * culling).
     */
    public boolean isSolid ()
    {
        return _solid;
    }

    /**
     * Returns a reference to the geometry bounds.
     */
    public Box getBounds ()
    {
        return _bounds;
    }

    /**
     * Returns the array of texture coordinate arrays, if any.
     */
    public ClientArray[] getTexCoordArrays ()
    {
        return _texCoordArrays;
    }

    /**
     * Returns the color array, if any.
     */
    public ClientArray getColorArray ()
    {
        return _colorArray;
    }

    /**
     * Returns the normal array, if any.
     */
    public ClientArray getNormalArray ()
    {
        return _normalArray;
    }

    /**
     * Returns the vertex array.
     */
    public ClientArray getVertexArray ()
    {
        return _vertexArray;
    }

    /**
     * Creates a bare bones batch to render this geometry.
     *
     * @param deformable if true, return a batch whose arrays can be manipulated directly (as
     * opposed to one that uses a compiled display list, e.g.)
     */
    public SimpleBatch createBatch (Renderer renderer, boolean deformable)
    {
        RenderState[] states = RenderState.createEmptySet();
        SimpleBatch.DrawCommand command = createDrawCommand();
        if (deformable || GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            states[RenderState.ARRAY_STATE] = createArrayState(renderer, !deformable);
        } else {
            DisplayList list = new DisplayList(renderer);
            createArrayState(renderer, false).apply(renderer);
            GL11.glNewList(list.getId(), GL11.GL_COMPILE);
            command.call();
            GL11.glEndList();
            states[RenderState.ARRAY_STATE] = ArrayState.DISABLED;
            command = new SimpleBatch.CallList(
                list, _colorArray != null, command.getPrimitiveCount());
        }
        return new SimpleBatch(states, command);
    }

    /**
     * Creates the array state containing the geometry arrays.
     *
     * @param compile if true, compile the arrays to buffer objects.
     */
    protected abstract ArrayState createArrayState (Renderer renderer, boolean compile);

    /**
     * Creates the draw command.
     */
    protected abstract SimpleBatch.DrawCommand createDrawCommand ();

    /**
     * Compiles a list of arrays into buffer objects.
     */
    protected static ClientArray[] compileArrays (
        Renderer renderer, IdentityHashMap<Buffer, BufferObject> bufobjs, ClientArray[] arrays)
    {
        if (arrays == null) {
            return null;
        }
        ClientArray[] narrays = new ClientArray[arrays.length];
        for (int ii = 0; ii < arrays.length; ii++) {
            narrays[ii] = compileArray(renderer, bufobjs, arrays[ii]);
        }
        return narrays;
    }

    /**
     * Compiles an array into a buffer object.
     */
    protected static ClientArray compileArray (
        Renderer renderer, IdentityHashMap<Buffer, BufferObject> bufobjs, ClientArray array)
    {
        if (array == null) {
            return null;
        }
        BufferObject bufobj = bufobjs.get(array.floatArray);
        if (bufobj == null) {
            bufobjs.put(array.floatArray, bufobj = new BufferObject(renderer));
            bufobj.setData(array.floatArray);
        }
        return new ClientArray(array.size, array.type, array.stride, array.offset, bufobj);
    }

    /** The type of primitives to render. */
    protected int _mode;

    /** Whether or not the geometry is "solid" (i.e., whether to enable back-face culling). */
    protected boolean _solid;

    /** The bounds of the geometry. */
    protected Box _bounds = new Box();

    /** The texture coordinate arrays. */
    protected ClientArray[] _texCoordArrays;

    /** The color array. */
    protected ClientArray _colorArray;

    /** The normal array. */
    protected ClientArray _normalArray;

    /** The vertex array. */
    protected ClientArray _vertexArray;
}
