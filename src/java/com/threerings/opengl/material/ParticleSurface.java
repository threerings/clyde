//
// $Id$

package com.threerings.opengl.material;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform;
import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.ParticleGeometry;
import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.material.ParticleHost.Alignment;
import com.threerings.opengl.model.VisibleMesh;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.util.GlContext;

/**
 * Extends {@link DefaultSurface} to add support for particle geometry.
 */
public class ParticleSurface extends DefaultSurface
{
    public ParticleSurface (GlContext ctx, DefaultMaterial material, ParticleGeometry geom)
    {
        super(ctx, material, geom);
        RenderState[] states = _bbatch.getStates();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.DEPTH_STATE] = DepthState.TEST;
    }

    @Override // documentation inherited
    public void setHost (SurfaceHost host)
    {
        super.setHost(host);
        _parthost = (ParticleHost)host;
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // get a reference to the vertex array to which we will write
        ArrayState astate = (ArrayState)_bbatch.getStates()[RenderState.ARRAY_STATE];
        astate.setDirty(true);
        ClientArray tcarray = astate.getTexCoordArrays()[0];
        int vpos = (int)tcarray.offset/4, vinc = tcarray.stride/4;

        // update the vertex buffer according to the particle type
        Alignment alignment = _parthost.getAlignment();
        if (_vertices != null) {
            updateMeshBuffer(vpos, vinc);
        } else {
            int mode = _geom.getMode();
            if (mode == GL11.GL_POINTS) {
                updatePointBuffer(vpos, vinc);
            } else if (mode == GL11.GL_LINES) {
                if (_geom.getSegments() == 0) {
                    updateLineBuffer(vpos, vinc);
                } else {
                    updateMultiSegmentLineBuffer(vpos, vinc);
                }
            } else { // mode == GL11.GL_TRIANGLES
                if (_geom.getSegments() == 0) {
                    updateTriangleBuffer(vpos, vinc);
                } else {
                    updateMultiSegmentTriangleBuffer(vpos, vinc);
                }
            }
        }

        // update the draw command
        SimpleBatch.DrawElements command = (SimpleBatch.DrawElements)_bbatch.getCommand();
        int living = _parthost.getLiving();
        int vpp = _geom.getVerticesPerParticle(), ipp = _geom.getIndicesPerParticle();
        command.setLimits(0, living * ipp);
        command.setRange(0, living * vpp - 1);

        // copy the working buffer to the vertex buffer all at once
        tcarray.floatArray.put(_vbuf, 0, living * vpp * DEST_VERTEX_SIZE).rewind();

        // enqueue the batch for rendering
        _batch.depth = _parthost.getDepth();
        _ctx.getRenderer().enqueueTransparent(_batch);
    }

    @Override // documentation inherited
    public Object clone ()
    {
        // make a deep(ish) copy of the array state
        ParticleSurface osurface = (ParticleSurface)super.clone();
        RenderState[] states = _bbatch.getStates(), ostates = osurface._bbatch.getStates();
        ostates[RenderState.ARRAY_STATE] =
            copyArrayState((ArrayState)states[RenderState.ARRAY_STATE]);
        return osurface;
    }

    @Override // documentation inherited
    protected SimpleBatch createBaseBatch (Geometry geom)
    {
        // copy the mesh data, if any
        _geom = (ParticleGeometry)geom;
        VisibleMesh mesh = _geom.getMesh();
        if (mesh != null) {
            int start = mesh.getStart(), vcount = 1 + mesh.getEnd() - start;
            _vertices = new float[vcount * SOURCE_VERTEX_SIZE];
            ClientArray tcarray = mesh.getTexCoordArrays()[0];
            ClientArray narray = mesh.getNormalArray(), varray = mesh.getVertexArray();
            int tcinc = tcarray.stride/4, tcpos = (int)tcarray.offset/4 + start*tcinc;
            int ninc = narray.stride/4, npos = (int)narray.offset/4 + start*ninc;
            int vinc = varray.stride/4, vpos = (int)varray.offset/4 + start*vinc;
            for (int ii = 0, vidx = 0; ii < vcount; ii++) {
                _vertices[vidx++] = tcarray.floatArray.get(tcpos);
                _vertices[vidx++] = tcarray.floatArray.get(tcpos + 1);
                tcpos += tcinc;

                _vertices[vidx++] = narray.floatArray.get(npos);
                _vertices[vidx++] = narray.floatArray.get(npos + 1);
                _vertices[vidx++] = narray.floatArray.get(npos + 2);
                npos += ninc;

                _vertices[vidx++] = varray.floatArray.get(vpos);
                _vertices[vidx++] = varray.floatArray.get(vpos + 1);
                _vertices[vidx++] = varray.floatArray.get(vpos + 2);
                vpos += vinc;
            }
        }

        // create the array for transformed vertex data
        ClientArray varray = _geom.getVertexArray();
        _vbuf = new float[varray.floatArray.capacity()];

        // return a non-compiled batch
        return geom.createBatch(_ctx.getRenderer(), true);
    }

    /**
     * Updates the vertex buffer according to the particle states for mesh particles.
     */
    protected void updateMeshBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf, vertices = _vertices;
        Transform xform = _xform;
        Quaternion vrot = _vrot, rotation = _rotation;
        Vector3f s = _s, t = _t, r = _r, view = _view;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        Alignment alignment = _parthost.getAlignment();
        if (alignment == Alignment.VELOCITY) {
            // find the view vector in local coordinates
            _host.getModelview().invert(_xform).transformVector(Vector3f.UNIT_Z, view);

        } else if (alignment == Alignment.BILLBOARD) {
            // get the inverse of the modelview rotation (better not be a general matrix!)
            Transform modelview = _host.getModelview();
            modelview.update(Transform.RIGID);
            modelview.getRotation().invert(vrot);
        }

        // determine the number of vertices per particle
        int vpp = vertices.length / SOURCE_VERTEX_SIZE;

        // update the living particles
        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            // extract the color
            Color4f color = particle.getColor();
            float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

            // compute the particle transform matrix
            float m00, m10, m20, m30;
            float m01, m11, m21, m31;
            float m02, m12, m22, m32;
            if (alignment == Alignment.VELOCITY) {
                Vector3f velocity = particle.getVelocity();
                view.cross(velocity, t);
                float length = t.length();
                if (length > FloatMath.EPSILON) {
                    t.multLocal(1f / length);
                    velocity.normalize(s);
                    s.cross(t, r);
                } else {
                    s.set(Vector3f.ZERO);
                    t.set(Vector3f.ZERO);
                    r.set(Vector3f.ZERO);
                }
                Vector3f position = particle.getPosition();
                float size = particle.getSize();
                m00 = s.x*size; m10 = t.x*size; m20 = r.x*size; m30 = position.x;
                m01 = s.y*size; m11 = t.y*size; m21 = r.y*size; m31 = position.y;
                m02 = s.z*size; m12 = t.z*size; m22 = r.z*size; m32 = position.z;

            } else {
                xform.set(
                    particle.getPosition(),
                    (alignment == Alignment.BILLBOARD) ?
                        vrot.mult(particle.getOrientation(), rotation) :
                        particle.getOrientation(),
                    particle.getSize());
                xform.update(Transform.AFFINE);
                Matrix4f m = xform.getMatrix();
                m00 = m.m00; m10 = m.m10; m20 = m.m20; m30 = m.m30;
                m01 = m.m01; m11 = m.m11; m21 = m.m21; m31 = m.m31;
                m02 = m.m02; m12 = m.m12; m22 = m.m22; m32 = m.m32;
            }

            for (int jj = 0, svpos = 0; jj < vpp; jj++) {
                // write the texture coordinates
                writeTexCoords(vbuf, vpos,
                    uoff + vertices[svpos]*uscale,
                    voff + vertices[svpos + 1]*vscale);

                // write the color
                writeColor(vbuf, vpos, cr, cg, cb, ca);

                // write the transformed normal
                float nx = vertices[svpos + 2];
                float ny = vertices[svpos + 3];
                float nz = vertices[svpos + 4];
                writeNormal(vbuf, vpos,
                    m00*nx + m10*ny + m20*nz,
                    m01*nx + m11*ny + m21*nz,
                    m02*nx + m12*ny + m22*nz);

                // write the transformed vertex
                float vx = vertices[svpos + 5];
                float vy = vertices[svpos + 6];
                float vz = vertices[svpos + 7];
                writeVertex(vbuf, vpos,
                    m00*vx + m10*vy + m20*vz + m30,
                    m01*vx + m11*vy + m21*vz + m31,
                    m02*vx + m12*vy + m22*vz + m32);

                vpos += vinc;
                svpos += SOURCE_VERTEX_SIZE;
            }
        }
    }

    /**
     * Updates the vertex buffer according to the particle states for point particles.
     */
    protected void updatePointBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        // update the living particles
        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            // write the vertex attributes
            writeTexCoords(vbuf, vpos, uoff, voff);
            writeColor(vbuf, vpos, particle.getColor());
            writeVertex(vbuf, vpos, particle.getPosition());
            vpos += vinc;
        }
    }

    /**
     * Updates the vertex buffer according to the particle states for line particles.
     */
    protected void updateLineBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf;
        Vector3f s = _s;
        Quaternion rotation = _rotation, vrot = _vrot;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        Alignment alignment = _parthost.getAlignment();
        if (alignment == Alignment.BILLBOARD) {
            // get the inverse of the modelview rotation (better not be a general matrix!)
            Transform modelview = _host.getModelview();
            modelview.update(Transform.RIGID);
            modelview.getRotation().invert(vrot);
        }

        // update the living particles
        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            // extract the color
            Color4f color = particle.getColor();
            float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

            // and the position
            Vector3f position = particle.getPosition();
            float px = position.x, py = position.y, pz = position.z;

            // compute the offset
            if (alignment == Alignment.VELOCITY) {
                Vector3f velocity = particle.getVelocity();
                float length = velocity.length();
                if (length < FloatMath.EPSILON) {
                    s.set(Vector3f.ZERO);
                } else {
                    velocity.mult(particle.getSize() / length, s);
                }
            } else {
                Quaternion rot = (alignment == Alignment.BILLBOARD) ?
                    vrot.mult(particle.getOrientation(), rotation) :
                    particle.getOrientation();
                rot.transformUnitX(s).multLocal(particle.getSize());
            }
            float sx = s.x, sy = s.y, sz = s.z;

            writeTexCoords(vbuf, vpos, uoff, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px - sx, py - sy, pz - sz);
            vpos += vinc;

            writeTexCoords(vbuf, vpos, uoff + uscale, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + sx, py + sy, pz + sz);
            vpos += vinc;
        }
    }

    /**
     * Updates the vertex buffer according to the particle states for multi-segment line particles.
     */
    protected void updateMultiSegmentLineBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf;
        Vector3f position = _position;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        // update the living particles
        int segments = _geom.getSegments();
        float tscale = 1f / segments;
        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            // extract the color
            Color4f color = particle.getColor();
            float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

            // write the initial segments, then the final one
            for (int jj = 0; jj < segments; jj++) {
                float frac = jj * tscale;
                particle.getPosition(frac, position);
                writeTexCoords(vbuf, vpos, uoff + frac*uscale, voff);
                writeColor(vbuf, vpos, cr, cg, cb, ca);
                writeVertex(vbuf, vpos, position);
                vpos += vinc;
            }
            writeTexCoords(vbuf, vpos, uoff + uscale, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, particle.getPosition());
            vpos += vinc;
        }
    }

    /**
     * Updates the vertex buffer according to the particle states for fixed-alignment triangle
     * particles.
     */
    protected void updateTriangleBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf;
        Vector3f s = _s, t = _t, view = _view;
        Quaternion rotation = _rotation, vrot = _vrot;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        Alignment alignment = _parthost.getAlignment();
        if (alignment == Alignment.VELOCITY) {
            // find the view vector in local coordinates
            _host.getModelview().invert(_xform).transformVector(Vector3f.UNIT_Z, view);

        } else if (alignment == Alignment.BILLBOARD) {
            // get the inverse of the modelview rotation (better not be a general matrix!)
            Transform modelview = _host.getModelview();
            modelview.update(Transform.RIGID);
            modelview.getRotation().invert(vrot);
        }

        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            // extract the color
            Color4f color = particle.getColor();
            float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

            // and the position
            Vector3f position = particle.getPosition();
            float px = position.x, py = position.y, pz = position.z;

            // compute the offsets
            float size = particle.getSize();
            if (alignment == Alignment.VELOCITY) {
                Vector3f velocity = particle.getVelocity();
                view.cross(velocity, t);
                float length = t.length();
                if (length > FloatMath.EPSILON) {
                    t.multLocal(size / length);
                    velocity.normalize(s).multLocal(size);
                } else {
                    s.set(Vector3f.ZERO);
                    t.set(Vector3f.ZERO);
                }
            } else {
                Quaternion rot = (alignment == Alignment.BILLBOARD) ?
                    vrot.mult(particle.getOrientation(), rotation) :
                    particle.getOrientation();
                rot.transformUnitX(s).multLocal(size);
                rot.transformUnitY(t).multLocal(size);
            }
            float sx = s.x, sy = s.y, sz = s.z;
            float tx = t.x, ty = t.y, tz = t.z;

            float vtop = voff + vscale;
            writeTexCoords(vbuf, vpos, uoff, vtop);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + tx - sx, py + ty - sy, pz + tz - sz);
            vpos += vinc;

            writeTexCoords(vbuf, vpos, uoff, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px - sx - tx, py - sy - ty, pz - sz - tz);
            vpos += vinc;

            float uright = uoff + uscale;
            writeTexCoords(vbuf, vpos, uright, vtop);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + sx + tx, py + sy + ty, pz + sz + tz);
            vpos += vinc;

            writeTexCoords(vbuf, vpos, uright, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + sx - tx, py + sy - ty, pz + sz - tz);
            vpos += vinc;
        }
    }

    /**
     * Updates the vertex arrays according to the particle states for multi-segment triangle
     * particles.
     */
    protected void updateMultiSegmentTriangleBuffer (int vpos, int vinc)
    {
        // get everything in local variables
        float[] vbuf = _vbuf;
        Vector3f position = _position, last = _last, next = _next;
        Vector3f s = _s, t = _t;

        // figure out the texture coordinate parameters
        int udivs = _parthost.getTextureDivisionsS();
        float uscale = 1f / udivs;
        float vscale = 1f / _parthost.getTextureDivisionsT();

        // find the view vector in local coordinates
        Vector3f view = _host.getModelview().invert(_xform).transformVector(
            Vector3f.UNIT_Z, _view);

        // update the living particles
        int segments = _geom.getSegments();
        float tscale = 1f / segments;
        Particle[] particles = _parthost.getParticles();
        for (int ii = 0, nn = _parthost.getLiving(); ii < nn; ii++) {
            Particle particle = particles[ii];

            // extract the color
            Color4f color = particle.getColor();
            float cr = color.r, cg = color.g, cb = color.b, ca = color.a;
            float size = particle.getSize();

            // compute the position
            particle.getPosition(0f, position);
            float px = position.x, py = position.y, pz = position.z;

            // compute the next position and use it to find the offset
            particle.getPosition(tscale, next);
            computeOffset(view, next.subtract(position, s), size, t);
            float tx = t.x, ty = t.y, tz = t.z;

            // determine the texture coordinate offsets
            int frame = Math.round(particle.getFrame());
            float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

            float vtop = voff + vscale;
            writeTexCoords(vbuf, vpos, uoff, vtop);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + tx, py + ty, pz + tz);
            vpos += vinc;

            writeTexCoords(vbuf, vpos, uoff, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px - tx, py - ty, pz - tz);
            vpos += vinc;

            for (int jj = 1; jj < segments; jj++) {
                // update the last, current, and next positions
                _last.set(px, py, pz);
                px = next.x; py = next.y; pz = next.z;
                float frac = jj * tscale;
                particle.getPosition(frac + tscale, next);

                // compute the offset using last and next positions
                computeOffset(view, next.subtract(last, s), size, t);
                tx = t.x; ty = t.y; tz = t.z;

                float u = uoff + frac * uscale;
                writeTexCoords(vbuf, vpos, u, vtop);
                writeColor(vbuf, vpos, cr, cg, cb, ca);
                writeVertex(vbuf, vpos, px + tx, py + ty, pz + tz);
                vpos += vinc;

                writeTexCoords(vbuf, vpos, u, voff);
                writeColor(vbuf, vpos, cr, cg, cb, ca);
                writeVertex(vbuf, vpos, px - tx, py - ty, pz - tz);
                vpos += vinc;
            }
            // update the last and current positions
            last.set(px, py, pz);
            position.set(px = next.x, py = next.y, pz = next.z);

            // compute the offset
            computeOffset(view, position.subtract(last, s), size, t);
            tx = t.x; ty = t.y; tz = t.z;

            float uright = uoff + uscale;
            writeTexCoords(vbuf, vpos, uright, vtop);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px + tx, py + ty, pz + tz);
            vpos += vinc;

            writeTexCoords(vbuf, vpos, uright, voff);
            writeColor(vbuf, vpos, cr, cg, cb, ca);
            writeVertex(vbuf, vpos, px - tx, py - ty, pz - tz);
            vpos += vinc;
        }
    }

    /**
     * Computes an offset vector orthogonal to the two provided vectors with the specified size.
     */
    protected static void computeOffset (Vector3f v1, Vector3f v2, float size, Vector3f result)
    {
        v1.cross(v2, result);
        float length = result.length();
        if (length > FloatMath.EPSILON) {
            result.multLocal(size / length);
        } else {
            result.set(Vector3f.ZERO);
        }
    }

    /**
     * Writes a set of texture coordinates to the vertex buffer at the specified position (the
     * hope is that these methods will be inlined; if not, they should be expanded manually).
     */
    protected static void writeTexCoords (float[] vbuf, int vpos, float s, float t)
    {
        vbuf[vpos] = s;
        vbuf[vpos + 1] = t;
    }

    /**
     * Writes a color to the vertex buffer at the specified position.
     */
    protected static void writeColor (float[] vbuf, int vpos, Color4f color)
    {
        vbuf[vpos + 2] = color.r;
        vbuf[vpos + 3] = color.g;
        vbuf[vpos + 4] = color.b;
        vbuf[vpos + 5] = color.a;
    }

    /**
     * Writes a color to the vertex buffer at the specified position.
     */
    protected static void writeColor (float[] vbuf, int vpos, float r, float g, float b, float a)
    {
        vbuf[vpos + 2] = r;
        vbuf[vpos + 3] = g;
        vbuf[vpos + 4] = b;
        vbuf[vpos + 5] = a;
    }

    /**
     * Writes a normal to the vertex buffer at the specified position.
     */
    protected static void writeNormal (float[] vbuf, int vpos, float x, float y, float z)
    {
        vbuf[vpos + 6] = x;
        vbuf[vpos + 7] = y;
        vbuf[vpos + 8] = z;
    }

    /**
     * Writes a vertex to the vertex buffer at the specified position.
     */
    protected static void writeVertex (float[] vbuf, int vpos, Vector3f vertex)
    {
        vbuf[vpos + 9] = vertex.x;
        vbuf[vpos + 10] = vertex.y;
        vbuf[vpos + 11] = vertex.z;
    }

    /**
     * Writes a vertex to the vertex buffer at the specified position.
     */
    protected static void writeVertex (float[] vbuf, int vpos, float x, float y, float z)
    {
        vbuf[vpos + 9] = x;
        vbuf[vpos + 10] = y;
        vbuf[vpos + 11] = z;
    }

    /** A casted reference to the host. */
    protected ParticleHost _parthost;

    /** The source vertex and normal data (for mesh particles). */
    protected float[] _vertices;

    /** Holds the transformed vertex data. */
    protected float[] _vbuf;

    /** The particle geometry. */
    protected ParticleGeometry _geom;

    /** Used to compose particle transforms. */
    protected Transform _xform = new Transform();

    /** Used to compute particle positions. */
    protected Vector3f _position = new Vector3f(), _last = new Vector3f(), _next = new Vector3f();

    /** Used to store the view vector. */
    protected Vector3f _view = new Vector3f();

    /** Used to compute particle rotations. */
    protected Quaternion _rotation = new Quaternion(), _vrot = new Quaternion();

    /** Used to compute particle offsets. */
    protected Vector3f _s = new Vector3f(), _t = new Vector3f(), _r = new Vector3f();

    /** The number of float values in each source vertex. */
    protected static final int SOURCE_VERTEX_SIZE = 2 + 3 + 3;

    /** The number of float values in each destination vertex. */
    protected static final int DEST_VERTEX_SIZE = 16;
}
