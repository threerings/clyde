//
// $Id$

package com.threerings.opengl.effect;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.threerings.math.Box;
import com.threerings.math.Vector3f;

import com.threerings.opengl.geometry.IndexedGeometry;
import com.threerings.opengl.model.VisibleMesh;
import com.threerings.opengl.renderer.ClientArray;

/**
 * Contains information about a particle system's geometry.
 */
public class ParticleGeometry extends IndexedGeometry
{
    /**
     * Creates a new point/line/triangle geometry object.
     */
    public ParticleGeometry (int mode, int particles, int segments)
    {
        _mode = mode;
        _particles = particles;
        _segments = segments;

        // initialize the derived fields
        initTransientFields();
    }

    /**
     * Creates a new mesh geometry object.
     */
    public ParticleGeometry (VisibleMesh mesh, int particles)
    {
        _mode = mesh.getMode();
        _mesh = mesh;
        _particles = particles;

        // initialize the derived fields
        initTransientFields();
    }

    /**
     * No-arg constructor for deserialization.
     */
    public ParticleGeometry ()
    {
    }

    /**
     * If the particles are to be represented as meshes, returns the mesh to use to copy
     * for each particle.
     */
    public VisibleMesh getMesh ()
    {
        return _mesh;
    }

    /**
     * Returns the number of particles to display.
     */
    public int getParticles ()
    {
        return _particles;
    }

    /**
     * Returns the number of segments in each particle.
     */
    public int getSegments ()
    {
        return _segments;
    }

    /**
     * Returns the number of vertices in each particle.
     */
    public int getVerticesPerParticle ()
    {
        return _vpp;
    }

    /**
     * Returns the number of indices in each particle.
     */
    public int getIndicesPerParticle ()
    {
        return _ipp;
    }

    /**
     * Initializes the object's transient fields after construction or deserialization.
     */
    protected void initTransientFields ()
    {
        // initialize the bounds (which represent the bounds of one untransformed particle segment)
        // and determine the number of vertices and indices per particle
        int vsegs = Math.max(_segments, 1);
        if (_mesh != null) {
            _solid = _mesh.isSolid();
            _bounds = _mesh.getBounds();
            _vpp = _mesh.getEnd() + 1;
            _ipp = _mesh.getIndices().capacity();
        } else {
            _bounds = new Box(
                new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(+0.5f, +0.5f, +0.5f));
            if (_mode == GL11.GL_POINTS) {
                _vpp = _ipp = 1;
            } else if (_mode == GL11.GL_LINES) {
                _vpp = vsegs + 1;
                _ipp = vsegs * 2;
            } else { // _mode == GL11.GL_TRIANGLES
                _vpp = 2 + vsegs*2;
                _ipp = vsegs * 6;
            }
        }
        int vcount = _particles * _vpp;
        int icount = _particles * _ipp;

        // create the vertex buffer and client array definitions
        FloatBuffer vertices = BufferUtils.createFloatBuffer(vcount * 16);
        _start = 0;
        _end = vcount - 1;
        _texCoordArrays = new ClientArray[] { new ClientArray(2, 64, 0, vertices) };
        _colorArray = new ClientArray(4, 64, 8, vertices);
        _normalArray = new ClientArray(3, 64, 24, vertices);
        _vertexArray = new ClientArray(3, 64, 36, vertices);

        // create the prototype indices
        ShortBuffer pindices;
        if (_mesh != null) {
            pindices = _mesh.getIndices();
        } else {
            pindices = BufferUtils.createShortBuffer(_ipp);
            if (_mode == GL11.GL_POINTS) {
                pindices.put((short)0);
            } else {
                for (int ii = 0; ii < vsegs; ii++) {
                    if (_mode == GL11.GL_LINES) {
                        pindices.put((short)ii);
                        pindices.put((short)(ii + 1));

                    } else { // _mode == GL11.GL_TRIANGLES
                        pindices.put((short)(ii*2));
                        pindices.put((short)(ii*2 + 1));
                        pindices.put((short)(ii*2 + 2));

                        pindices.put((short)(ii*2 + 2));
                        pindices.put((short)(ii*2 + 1));
                        pindices.put((short)(ii*2 + 3));
                    }
                }
            }
            pindices.rewind();
        }

        // create the index buffer and populate it with translated copies of the prototype indices
        _indices = BufferUtils.createShortBuffer(icount);
        for (int ii = 0; ii < _particles; ii++) {
            int voffset = ii * _vpp;
            while (pindices.hasRemaining()) {
                _indices.put((short)(pindices.get() + voffset));
            }
            pindices.rewind();
        }
        _indices.rewind();
    }

    /** The mesh to clone for each particle. */
    protected VisibleMesh _mesh;

    /** The number of particles in the mesh. */
    protected int _particles;

    /** The number of segments in each particle. */
    protected int _segments;

    /** The number of vertices per particle. */
    protected transient int _vpp;

    /** The number of indices per particle. */
    protected transient int _ipp;
}
