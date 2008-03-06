//
// $Id$

package com.threerings.opengl.model;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.IdentityHashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.math.Box;

import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.state.ArrayState;

/**
 * A mesh with bone indices and weights for each vertex.
 */
public class SkinMesh extends VisibleMesh
{
    /** The maximum number of bones that can influence a single mesh. */
    public static final int MAX_BONE_COUNT = 31;

    /** The location of the bone index attribute. */
    public static final int BONE_INDEX_ATTRIB = 9;

    /** The location of the bone weight attribute. */
    public static final int BONE_WEIGHT_ATTRIB = 10;

    /**
     * Creates a new skin mesh.
     */
    public SkinMesh (
        String texture, boolean solid, Box bounds,
        FloatBuffer vertices, ShortBuffer indices, String[] bones)
    {
        super(texture, solid, bounds, vertices, indices);
        _bones = bones;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SkinMesh ()
    {
    }

    /**
     * Returns the array containing the names of the bones that influence the mesh.
     */
    public String[] getBones ()
    {
        return _bones;
    }

    /**
     * Returns the bone index array.
     */
    public ClientArray getBoneIndexArray ()
    {
        return _boneIndexArray;
    }

    /**
     * Returns the bone weight array.
     */
    public ClientArray getBoneWeightArray ()
    {
        return _boneWeightArray;
    }

    @Override // documentation inherited
    protected void initTransientFields ()
    {
        _mode = GL11.GL_TRIANGLES;
        _start = 0;
        _end = _vertices.capacity()/16 - 1;
        _boneIndexArray = new ClientArray(4, 64, 0, _vertices);
        _boneWeightArray = new ClientArray(4, 64, 16, _vertices);
        _texCoordArrays = new ClientArray[] { new ClientArray(2, 64, 32, _vertices) };
        _normalArray = new ClientArray(3, 64, 40, _vertices);
        _vertexArray = new ClientArray(3, 64, 52, _vertices);
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
            ClientArray[] attribs = new ClientArray[] { _boneIndexArray, _boneWeightArray };
            return new ArrayState(
                BONE_INDEX_ATTRIB, compileArrays(renderer, bufobjs, attribs),
                compileArrays(renderer, bufobjs, _texCoordArrays),
                compileArray(renderer, bufobjs, _colorArray),
                compileArray(renderer, bufobjs, _normalArray),
                compileArray(renderer, bufobjs, _vertexArray),
                ibobj);
        } else {
            // don't bother including the skin attributes; we're not using the shader
            return new ArrayState(
                0, null, _texCoordArrays, _colorArray, _normalArray, _vertexArray, ibobj);
        }
    }

    /** The names of the bones influencing the mesh. */
    protected String[] _bones;

    /** The bone index array. */
    protected transient ClientArray _boneIndexArray;

    /** The bone weight array. */
    protected transient ClientArray _boneWeightArray;
}
