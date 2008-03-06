//
// $Id$

package com.threerings.opengl.model;

import java.io.IOException;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;

import com.threerings.export.Exportable;
import com.threerings.export.Exporter;
import com.threerings.export.Importer;

import com.threerings.opengl.geometry.IndexedGeometry;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.util.BatchFactory;

/**
 * Contains an indexed triangle mesh for display.
 */
public class VisibleMesh extends IndexedGeometry
    implements Exportable
{
    /**
     * Creates a new mesh.
     */
    public VisibleMesh (
        String texture, boolean solid, Box bounds,
        FloatBuffer vertices, ShortBuffer indices)
    {
        _texture = texture;
        _solid = solid;
        _bounds.set(bounds);
        _vertices = vertices;
        _indices = indices;

        // initialize the derived fields
        initTransientFields();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public VisibleMesh ()
    {
    }

    /**
     * Returns the name of this mesh's texture.
     */
    public String getTexture ()
    {
        return _texture;
    }

    /**
     * Writes out the fields of the object.
     */
    public void writeFields (Exporter out)
        throws IOException
    {
        out.defaultWriteFields();

        // write out the inherited fields
        out.write("solid", _solid, true);
        out.write("bounds", _bounds, null, Box.class);
        out.write("indices", _indices, (ShortBuffer)null);
    }

    /**
     * Reads in the fields of the object.
     */
    public void readFields (Importer in)
        throws IOException
    {
        in.defaultReadFields();

        // read in the inherited fields
        _solid = in.read("solid", true);
        _bounds = in.read("bounds", null, Box.class);
        _indices = in.read("indices", (ShortBuffer)null);

        // initialize the transient fields
        initTransientFields();
    }

    /**
     * Initializes the object's transient fields after construction or deserialization.
     */
    protected void initTransientFields ()
    {
        _mode = GL11.GL_TRIANGLES;
        _start = 0;
        _end = _vertices.capacity()/8 - 1;
        _texCoordArrays = new ClientArray[] { new ClientArray(2, 32, 0, _vertices) };
        _normalArray = new ClientArray(3, 32, 8, _vertices);
        _vertexArray = new ClientArray(3, 32, 20, _vertices);
    }

    /** The name of the texture. */
    protected String _texture;

    /** The interleaved vertex data. */
    protected FloatBuffer _vertices;
}
