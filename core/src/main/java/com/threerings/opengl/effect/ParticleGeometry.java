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

package com.threerings.opengl.effect;

import java.lang.ref.SoftReference;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.samskivert.util.HashIntMap;

import com.threerings.expr.Bound;
import com.threerings.expr.MutableInteger;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Quaternion;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.config.ParticleSystemConfig;
import com.threerings.opengl.effect.config.ParticleSystemConfig.Alignment;
import com.threerings.opengl.geometry.DynamicGeometry;
import com.threerings.opengl.geometry.config.GeometryConfig;
import com.threerings.opengl.geometry.config.PassDescriptor;
import com.threerings.opengl.geometry.util.GeometryUtil;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.SimpleBatch.DrawElements;
import com.threerings.opengl.renderer.config.ClientArrayConfig;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.renderer.state.TransformState;
import com.threerings.opengl.util.GlContext;

/**
 * Represents a particle geometry instance.
 */
public abstract class ParticleGeometry extends DynamicGeometry
{
    /**
     * Renders particles as points.
     */
    public static class Points extends ParticleGeometry
    {
        public Points (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override
        protected int getMode ()
        {
            return GL11.GL_POINTS;
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return 1;
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return 1;
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0 };
        }

        @Override
        protected void updateData ()
        {
            // get everything into local variables
            Particle[] particles = _particles;
            float[] data = _data;
            int stride = _stride;
            Vector3f n = _n;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            // update the living particles
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = FloatMath.round(particle.getFrame());
                float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

                // write the vertex attributes and advance the positions
                texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                colorIdx = write(data, colorIdx, stride, particle.getColor());
                if (normals) {
                    normalIdx = write(data, normalIdx, stride,
                        particle.getOrientation().transformUnitZ(n));
                }
                vertexIdx = write(data, vertexIdx, stride, particle.getPosition());
            }
        }
    }

    /**
     * Renders particles as single line segments.
     */
    public static class Lines extends ParticleGeometry
    {
        public Lines (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override
        protected int getMode ()
        {
            return GL11.GL_LINES;
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return 2;
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return 2;
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0, 1 };
        }

        @Override
        protected void updateData ()
        {
            // get everything in local variables
            Particle[] particles = _particles;
            float[] data = _data;
            int stride = _stride;
            Vector3f s = _s, n = _n;
            Quaternion rotation = _rotation, vrot = _vrot;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            Alignment alignment = _config.alignment;
            if (alignment == Alignment.BILLBOARD) {
                // get the inverse of the modelview rotation
                _transformState.getModelview().extractRotation(vrot).invertLocal();
            }

            // update the living particles
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = FloatMath.round(particle.getFrame());
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
                    if (normals) {
                        particle.getOrientation().transformUnitZ(n);
                    }
                } else {
                    Quaternion rot = (alignment == Alignment.BILLBOARD) ?
                        vrot.mult(particle.getOrientation(), rotation) :
                        particle.getOrientation();
                    rot.transformUnitX(s).multLocal(particle.getSize());
                    if (normals) {
                        rot.transformUnitZ(n);
                    }
                }
                float sx = s.x, sy = s.y, sz = s.z;
                if (normals) {
                    float nx = n.x, ny = n.y, nz = n.z;

                    // write the first vertex
                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px - sx, py - sy, pz - sz);

                    // then the second
                    texCoordIdx = write(data, texCoordIdx, stride, uoff + uscale, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px + sx, py + sy, pz + sz);

                } else {
                    // write the first vertex
                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px - sx, py - sy, pz - sz);

                    // then the second
                    texCoordIdx = write(data, texCoordIdx, stride, uoff + uscale, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px + sx, py + sy, pz + sz);
                }
            }
        }
    }

    /**
     * Renders particles as multi-segment line trails.
     */
    public static class LineTrails extends ParticleGeometry
    {
        public LineTrails (GlContext ctx, Scope scope, PassDescriptor[] passes, int segments)
        {
            super(scope);
            _segments = segments;
            init(ctx, passes);
        }

        @Override
        protected int getMode ()
        {
            return GL11.GL_LINES;
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return _segments + 1;
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return _segments * 2;
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_segments * 2];
            for (int ii = 0, idx = 0; ii < _segments; ii++) {
                prototype[idx++] = ii;
                prototype[idx++] = ii + 1;
            }
            return prototype;
        }

        @Override
        protected void updateData ()
        {
            // get everything in local variables
            Particle[] particles = _particles;
            float[] data = _data;
            int stride = _stride;
            int segments = _segments;
            Vector3f position = _position, n = _n;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            // update the living particles
            float tscale = 1f / segments;
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = FloatMath.round(particle.getFrame());
                float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

                // extract the color
                Color4f color = particle.getColor();
                float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

                // write the initial segments, then the final one
                if (normals) {
                    particle.getOrientation().transformUnitZ(n);
                    float nx = n.x, ny = n.y, nz = n.z;

                    for (int jj = 0; jj <= segments; jj++) {
                        float frac = jj * tscale;
                        particle.getPosition(frac, position);
                        texCoordIdx = write(data, texCoordIdx, stride, uoff + frac*uscale, voff);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                        vertexIdx = write(data, vertexIdx, stride, position);
                    }
                } else {
                    for (int jj = 0; jj <= segments; jj++) {
                        float frac = jj * tscale;
                        particle.getPosition(frac, position);
                        texCoordIdx = write(data, texCoordIdx, stride, uoff + frac*uscale, voff);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        vertexIdx = write(data, vertexIdx, stride, position);
                    }
                }
            }
        }

        /** The number of segments in each particle. */
        protected int _segments;
    }

    /**
     * Renders particles as single quads.
     */
    public static class Quads extends ParticleGeometry
    {
        public Quads (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            super(scope);
            init(ctx, passes);
        }

        @Override
        protected int getMode ()
        {
            return GL11.GL_TRIANGLES;
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return 4;
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return 6;
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            return new int[] { 0, 1, 2, 2, 1, 3 };
        }

        @Override
        protected void updateData ()
        {
            // get everything in local variables
            Particle[] particles = _particles;
            float[] data = _data;
            int stride = _stride;
            Vector3f s = _s, t = _t, n = _n, view = _view;
            Quaternion rotation = _rotation, vrot = _vrot;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            Alignment alignment = _config.alignment;
            if (alignment == Alignment.VELOCITY) {
                // find the view vector in local coordinates
                _transformState.getModelview().invert(_xform).transformVector(
                    Vector3f.UNIT_Z, view);

            } else if (alignment == Alignment.BILLBOARD) {
                // get the inverse of the modelview rotation
                _transformState.getModelview().extractRotation(vrot).invertLocal();
            }

            // update the living particles
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = FloatMath.round(particle.getFrame());
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
                    if (normals) {
                        computeOffset(s, t, 1f, n);
                    }
                } else {
                    Quaternion rot = (alignment == Alignment.BILLBOARD) ?
                        vrot.mult(particle.getOrientation(), rotation) :
                        particle.getOrientation();
                    rot.transformUnitX(s).multLocal(size);
                    rot.transformUnitY(t).multLocal(size);
                    if (normals) {
                        rot.transformUnitZ(n);
                    }
                }
                float sx = s.x, sy = s.y, sz = s.z;
                float tx = t.x, ty = t.y, tz = t.z;
                float vtop = voff + vscale, uright = uoff + uscale;

                // write the vertices
                if (normals) {
                    float nx = n.x, ny = n.y, nz = n.z;

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px+tx-sx, py+ty-sy, pz+tz-sz);

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px-sx-tx, py-sy-ty, pz-sz-tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px+sx+tx, py+sy+ty, pz+sz+tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px+sx-tx, py+sy-ty, pz+sz-tz);

                } else {
                    texCoordIdx = write(data, texCoordIdx, stride, uoff, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px+tx-sx, py+ty-sy, pz+tz-sz);

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px-sx-tx, py-sy-ty, pz-sz-tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px+sx+tx, py+sy+ty, pz+sz+tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px+sx-tx, py+sy-ty, pz+sz-tz);
                }
            }
        }
    }

    /**
     * Renders particles as multi-segment quad trails.
     */
    public static class QuadTrails extends ParticleGeometry
    {
        public QuadTrails (GlContext ctx, Scope scope, PassDescriptor[] passes, int segments)
        {
            super(scope);
            _segments = segments;
            init(ctx, passes);
        }

        @Override
        protected int getMode ()
        {
            return GL11.GL_TRIANGLES;
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return (_segments + 1) * 2;
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return _segments * 6;
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_segments * 6];
            for (int ii = 0, idx = 0; ii < _segments; ii++) {
                int offset = ii*2;
                prototype[idx++] = offset;
                prototype[idx++] = offset + 1;
                prototype[idx++] = offset + 2;

                prototype[idx++] = offset + 2;
                prototype[idx++] = offset + 1;
                prototype[idx++] = offset + 3;
            }
            return prototype;
        }

        @Override
        protected void updateData ()
        {
            // get everything in local variables
            Particle[] particles = _particles;
            float[] data = _data;
            int stride = _stride;
            int segments = _segments;
            Vector3f position = _position, last = _last, next = _next;
            Vector3f s = _s, t = _t, n = _n;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            // find the view vector in local coordinates
            Vector3f view = _transformState.getModelview().invert(_xform).transformVector(
                Vector3f.UNIT_Z, _view);

            // update the living particles
            float tscale = 1f / segments;
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // extract the color and size
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
                int frame = FloatMath.round(particle.getFrame());
                float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

                // write the first two vertices
                float vtop = voff + vscale;
                if (normals) {
                    computeOffset(s, t, 1f, n);
                    float nx = n.x, ny = n.y, nz = n.z;

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);

                } else {
                    texCoordIdx = write(data, texCoordIdx, stride, uoff, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uoff, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);
                }

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
                    if (normals) {
                        computeOffset(s, t, 1f, n);
                        float nx = n.x, ny = n.y, nz = n.z;

                        texCoordIdx = write(data, texCoordIdx, stride, u, vtop);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                        vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                        texCoordIdx = write(data, texCoordIdx, stride, u, voff);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                        vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);

                    } else {
                        texCoordIdx = write(data, texCoordIdx, stride, u, vtop);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                        texCoordIdx = write(data, texCoordIdx, stride, u, voff);
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                        vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);
                    }
                }
                // update the last and current positions
                last.set(px, py, pz);
                position.set(px = next.x, py = next.y, pz = next.z);

                // compute the offset
                computeOffset(view, position.subtract(last, s), size, t);
                tx = t.x; ty = t.y; tz = t.z;

                // write the final two vertices
                float uright = uoff + uscale;
                if (normals) {
                    computeOffset(s, t, 1f, n);
                    float nx = n.x, ny = n.y, nz = n.z;

                    texCoordIdx = write(data, texCoordIdx, stride, uright, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    normalIdx = write(data, normalIdx, stride, nx, ny, nz);
                    vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);

                } else {
                    texCoordIdx = write(data, texCoordIdx, stride, uright, vtop);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px + tx, py + ty, pz + tz);

                    texCoordIdx = write(data, texCoordIdx, stride, uright, voff);
                    colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);
                    vertexIdx = write(data, vertexIdx, stride, px - tx, py - ty, pz - tz);
                }
            }
        }

        /** The number of segments in each particle. */
        protected int _segments;
    }

    /**
     * Renders particles as meshes.
     */
    public static class Meshes extends ParticleGeometry
    {
        /**
         * Attempts to create mesh geometry, falling back on points if the specified geometry is
         * unsuitable.
         */
        public static ParticleGeometry create (
            GlContext ctx, Scope scope, PassDescriptor[] passes, GeometryConfig geom)
        {
            if (geom instanceof GeometryConfig.IndexedStored) {
                GeometryConfig.IndexedStored stored = (GeometryConfig.IndexedStored)geom;
                if (stored.getTexCoordArray(0) != null) {
                    return new Meshes(ctx, scope, passes, stored);
                }
            }
            return new Points(ctx, scope, passes);
        }

        public Meshes (
            GlContext ctx, Scope scope, PassDescriptor[] passes, GeometryConfig.IndexedStored geom)
        {
            super(scope);
            _geom = geom;
            init(ctx, passes);

            // get the source data
            ClientArrayConfig texCoordArray = geom.texCoordArrays[0];
            ClientArrayConfig vertexArray = geom.vertexArray;
            ClientArrayConfig normalArray = geom.normalArray;
            if (_normalOffset >= 0) {
                _source = _geom.getFloatArray(false, texCoordArray, normalArray, vertexArray);
                _sourceStride = texCoordArray.size + normalArray.size + vertexArray.size;
                _sourceNormalOffset = texCoordArray.size;
                _sourceVertexOffset = texCoordArray.size + normalArray.size;
            } else {
                _source = _geom.getFloatArray(false, texCoordArray, vertexArray);
                _sourceStride = texCoordArray.size + vertexArray.size;
                _sourceVertexOffset = texCoordArray.size;
            }
            _sourceTexCoordOffset = 0;
        }

        @Override
        protected int getMode ()
        {
            return _geom.mode.getConstant();
        }

        @Override
        protected int getParticleVertexCount ()
        {
            return _geom.getVertexCount();
        }

        @Override
        protected int getParticleIndexCount ()
        {
            return _geom.indices.capacity();
        }

        @Override
        protected int[] getPrototypeIndices ()
        {
            int[] prototype = new int[_geom.indices.capacity()];
            for (int ii = 0; ii < prototype.length; ii++) {
                prototype[ii] = _geom.indices.get(ii);
            }
            return prototype;
        }

        @Override
        protected void updateData ()
        {
            // get everything in local variables
            Particle[] particles = _particles;
            float[] data = _data, source = _source;
            int stride = _stride, sourceStride = _sourceStride;
            Transform3D xform = _xform;
            Quaternion vrot = _vrot, rotation = _rotation;
            Vector3f s = _s, t = _t, r = _r, view = _view;
            boolean normals = (_normalOffset >= 0);

            // figure out the texture coordinate parameters
            int udivs = _config.textureDivisionsS;
            float uscale = 1f / udivs;
            float vscale = 1f / _config.textureDivisionsT;

            Alignment alignment = _config.alignment;
            if (alignment == Alignment.VELOCITY) {
                // find the view vector in local coordinates
                _transformState.getModelview().invert(_xform).transformVector(
                    Vector3f.UNIT_Z, view);

            } else if (alignment == Alignment.BILLBOARD) {
                // get the inverse of the modelview rotation
                _transformState.getModelview().extractRotation(vrot).invertLocal();
            }

            // update the living particles
            int vpp = _geom.getVertexCount();
            int texCoordIdx = _texCoordOffset;
            int colorIdx = _colorOffset;
            int normalIdx = _normalOffset;
            int vertexIdx = _vertexOffset;
            for (int ii = 0, nn = _living.value; ii < nn; ii++) {
                Particle particle = particles[ii];

                // determine the texture coordinate offsets
                int frame = FloatMath.round(particle.getFrame());
                float uoff = (frame % udivs) * uscale, voff = (frame / udivs) * vscale;

                // extract the color
                Color4f color = particle.getColor();
                float cr = color.r, cg = color.g, cb = color.b, ca = color.a;

                // compute the particle transform matrix
                float m00, m10, m20, m30;
                float m01, m11, m21, m31;
                float m02, m12, m22, m32;
                float size = particle.getSize();
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
                    m00 = s.x*size; m10 = t.x*size; m20 = r.x*size; m30 = position.x;
                    m01 = s.y*size; m11 = t.y*size; m21 = r.y*size; m31 = position.y;
                    m02 = s.z*size; m12 = t.z*size; m22 = r.z*size; m32 = position.z;

                } else {
                    xform.set(
                        particle.getPosition(),
                        (alignment == Alignment.BILLBOARD) ?
                            vrot.mult(particle.getOrientation(), rotation) :
                            particle.getOrientation(),
                        size);
                    xform.update(Transform3D.AFFINE);
                    Matrix4f m = xform.getMatrix();
                    m00 = m.m00; m10 = m.m10; m20 = m.m20; m30 = m.m30;
                    m01 = m.m01; m11 = m.m11; m21 = m.m21; m31 = m.m31;
                    m02 = m.m02; m12 = m.m12; m22 = m.m22; m32 = m.m32;
                }

                int sourceTexCoordIdx = _sourceTexCoordOffset;
                int sourceVertexIdx = _sourceVertexOffset;
                if (normals) {
                    int sourceNormalIdx = _sourceNormalOffset;
                    float rsize = 1f / particle.getSize();
                    float n00 = m00*rsize, n10 = m10*rsize, n20 = m20*rsize;
                    float n01 = m01*rsize, n11 = m11*rsize, n21 = m21*rsize;
                    float n02 = m02*rsize, n12 = m12*rsize, n22 = m22*rsize;

                    for (int jj = 0; jj < vpp; jj++) {
                        // write the texture coordinates
                        texCoordIdx = write(data, texCoordIdx, stride,
                            uoff + source[sourceTexCoordIdx]*uscale,
                            voff + source[sourceTexCoordIdx + 1]*vscale);
                        sourceTexCoordIdx += sourceStride;

                        // write the color
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);

                        // write the transformed normal
                        float nx = source[sourceNormalIdx];
                        float ny = source[sourceNormalIdx + 1];
                        float nz = source[sourceNormalIdx + 2];
                        normalIdx = write(data, normalIdx, stride,
                            n00*nx + n10*ny + n20*nz,
                            n01*nx + n11*ny + n21*nz,
                            n02*nx + n12*ny + n22*nz);
                        sourceNormalIdx += sourceStride;

                        // write the transformed vertex
                        float vx = source[sourceVertexIdx];
                        float vy = source[sourceVertexIdx + 1];
                        float vz = source[sourceVertexIdx + 2];
                        vertexIdx = write(data, vertexIdx, stride,
                            m00*vx + m10*vy + m20*vz + m30,
                            m01*vx + m11*vy + m21*vz + m31,
                            m02*vx + m12*vy + m22*vz + m32);
                        sourceVertexIdx += sourceStride;
                    }
                } else {
                    for (int jj = 0; jj < vpp; jj++) {
                        // write the texture coordinates
                        texCoordIdx = write(data, texCoordIdx, stride,
                            uoff + source[sourceTexCoordIdx]*uscale,
                            voff + source[sourceTexCoordIdx + 1]*vscale);
                        sourceTexCoordIdx += sourceStride;

                        // write the color
                        colorIdx = write(data, colorIdx, stride, cr, cg, cb, ca);

                        // write the transformed vertex
                        float vx = source[sourceVertexIdx];
                        float vy = source[sourceVertexIdx + 1];
                        float vz = source[sourceVertexIdx + 2];
                        vertexIdx = write(data, vertexIdx, stride,
                            m00*vx + m10*vy + m20*vz + m30,
                            m01*vx + m11*vy + m21*vz + m31,
                            m02*vx + m12*vy + m22*vz + m32);
                        sourceVertexIdx += sourceStride;
                    }
                }
            }
        }

        /** The geometry to render. */
        protected GeometryConfig.IndexedStored _geom;

        /** The source data. */
        protected float[] _source;

        /** The stride between the source vertices. */
        protected int _sourceStride;

        /** The offset of the texture coordinates in the source array. */
        protected int _sourceTexCoordOffset;

        /** The offset of the normals in the source array. */
        protected int _sourceNormalOffset;

        /** The offset of the vertices in the source array. */
        protected int _sourceVertexOffset;
    }

    /**
     * Creates a new geometry object.
     */
    public ParticleGeometry (Scope scope)
    {
        ScopeUtil.updateBound(this, scope);
    }

    @Override
    public Vector3f getCenter ()
    {
        return _center;
    }

    @Override
    public ArrayState getArrayState (int pass)
    {
        return _arrayStates[pass];
    }

    @Override
    public DrawCommand getDrawCommand (int pass)
    {
        return _drawCommand;
    }

    @Override
    public void update ()
    {
        super.update();

        // modify the draw command based on the number of living particles
        _drawCommand.setLimits(0, _living.value * getParticleIndexCount());
        _drawCommand.setRange(0, _living.value * getParticleVertexCount() - 1);
    }

    /**
     * Initializes the geometry.
     */
    protected void init (GlContext ctx, PassDescriptor[] passes)
    {
        // determine whether any of the passes require normals
        boolean normals = false;
        for (PassDescriptor pass : passes) {
            normals |= pass.normals;
        }

        // create the base arrays
        HashMap<String, ClientArray> vertexAttribArrays = new HashMap<String, ClientArray>(0);
        HashIntMap<ClientArray> texCoordArrays = new HashIntMap<ClientArray>();
        ClientArray texCoordArray = new ClientArray(2, (FloatBuffer)null);
        texCoordArrays.put(0, texCoordArray);
        ClientArray colorArray = new ClientArray(4, (FloatBuffer)null);
        ClientArray normalArray = normals ? new ClientArray(3, (FloatBuffer)null) : null;
        ClientArray vertexArray = new ClientArray(3, (FloatBuffer)null);

        // put them in a list and compute the offsets and stride
        ArrayList<ClientArray> arrays = GeometryUtil.createList(
            vertexAttribArrays, texCoordArrays, colorArray, normalArray, vertexArray);
        _stride = GeometryUtil.updateOffsetsAndStride(arrays) / 4;
        _texCoordOffset = (int)(texCoordArray.offset / 4);
        _colorOffset = (int)(colorArray.offset / 4);
        _normalOffset = normals ? (int)(normalArray.offset / 4) : -1;
        _vertexOffset = (int)(vertexArray.offset / 4);

        // (re)create the data array if necessary
        _data = (_config.data == null) ? null : _config.data.get();
        if (_data == null) {
            int size = _particles.length * getParticleVertexCount() * _stride;
            _config.data = new SoftReference<float[]>(_data = new float[size]);
        }

        // use a VBO if possible
        BufferObject elementArrayBuffer = null;
        if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
            _arrayBuffer = new BufferObject(ctx.getRenderer());
            _floatArray = getScratchBuffer(_data.length);

            // (re)create the shared element array buffer if necessary
            elementArrayBuffer = (_config.elementArrayBuffer == null) ?
                null : _config.elementArrayBuffer.get();
            if (elementArrayBuffer == null) {
                _config.elementArrayBuffer = new SoftReference<BufferObject>(
                    elementArrayBuffer = new BufferObject(ctx.getRenderer()));
                elementArrayBuffer.setData(createIndices());
            }
            _drawCommand = SimpleBatch.createDrawBufferElements(
                getMode(), 0, 0, 0, GL11.GL_UNSIGNED_SHORT, 0L);

        } else {
            _floatArray = BufferUtils.createFloatBuffer(_data.length);

            // (re)create the shared index buffer if necessary
            ShortBuffer indices = (_config.indices == null) ? null : _config.indices.get();
            if (indices == null) {
                _config.indices = new SoftReference<ShortBuffer>(
                    indices = createIndices());
            }
            _drawCommand = SimpleBatch.createDrawShortElements(getMode(), 0, 0, indices);
        }

        // set the array references
        for (ClientArray array : arrays) {
            array.arrayBuffer = _arrayBuffer;
            array.floatArray = (_arrayBuffer == null) ? _floatArray : null;
        }

        // create the array states
        _arrayStates = GeometryUtil.createArrayStates(
            vertexAttribArrays, texCoordArrays, colorArray,
            normalArray, vertexArray, elementArrayBuffer, passes);
    }

    /**
     * Creates and populates the index buffer.
     */
    protected ShortBuffer createIndices ()
    {
        int[] prototype = getPrototypeIndices();
        ShortBuffer indices = BufferUtils.createShortBuffer(_particles.length * prototype.length);
        int vpp = getParticleVertexCount();
        for (int ii = 0, offset = 0; ii < _particles.length; ii++, offset += vpp) {
            for (int index : prototype) {
                indices.put((short)(offset + index));
            }
        }
        indices.rewind();
        return indices;
    }

    /**
     * Returns the primitive mode.
     */
    protected abstract int getMode ();

    /**
     * Returns the number of vertices in each particle.
     */
    protected abstract int getParticleVertexCount ();

    /**
     * Returns the number of indices in each particle.
     */
    protected abstract int getParticleIndexCount ();

    /**
     * Returns the array of prototype indices that will be replicated at different offsets to
     * create the actual index buffer.
     */
    protected abstract int[] getPrototypeIndices ();

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
     * Writes a pair of values to the data buffer at the specified position and returns the
     * position advanced by the stride (the hope is that these methods will be inlined; if not,
     * they should be expanded manually).
     */
    protected static int write (float[] data, int idx, int stride, float v0, float v1)
    {
        data[idx] = v0;
        data[idx + 1] = v1;
        return idx + stride;
    }

    /**
     * Writes a triplet to the data buffer at the specified position and returns the position
     * advanced by the stride.
     */
    protected static int write (float[] data, int idx, int stride, float v0, float v1, float v2)
    {
        data[idx] = v0;
        data[idx + 1] = v1;
        data[idx + 2] = v2;
        return idx + stride;
    }

    /**
     * Writes a quadruplet to the data buffer at the specified position and returns the position
     * advanced by the stride.
     */
    protected static int write (
        float[] data, int idx, int stride, float v0, float v1, float v2, float v3)
    {
        data[idx] = v0;
        data[idx + 1] = v1;
        data[idx + 2] = v2;
        data[idx + 3] = v3;
        return idx + stride;
    }

    /**
     * Writes a vector to the data buffer at the specified position and returns the position
     * advanced by the stride.
     */
    protected static int write (float[] data, int idx, int stride, Vector3f value)
    {
        data[idx] = value.x;
        data[idx + 1] = value.y;
        data[idx + 2] = value.z;
        return idx + stride;
    }

    /**
     * Writes a color to the data buffer at the specified position and returns the position
     * advanced by the stride.
     */
    protected static int write (float[] data, int idx, int stride, Color4f value)
    {
        data[idx] = value.r;
        data[idx + 1] = value.g;
        data[idx + 2] = value.b;
        data[idx + 3] = value.a;
        return idx + stride;
    }

    /** The configuration of the layer. */
    @Bound
    protected ParticleSystemConfig.Layer _config;

    /** The particles to render. */
    @Bound
    protected Particle[] _particles;

    /** The number of particles currently active. */
    @Bound
    protected MutableInteger _living;

    /** The layer's transform state. */
    @Bound
    protected TransformState _transformState;

    /** The center of the geometry. */
    @Bound
    protected Vector3f _center;

    /** The stride (number of floats) between adjacent vertices. */
    protected int _stride;

    /** The offset of the texture coordinate data. */
    protected int _texCoordOffset;

    /** The offset of the color data. */
    protected int _colorOffset;

    /** The offset of the normal data, or -1 if not using normals. */
    protected int _normalOffset;

    /** The offset of the vertex data. */
    protected int _vertexOffset;

    /** The array states for each pass. */
    protected ArrayState[] _arrayStates;

    /** The draw command shared by all passes. */
    protected DrawElements _drawCommand;

    /** Used to compose particle transforms. */
    protected Transform3D _xform = new Transform3D();

    /** Used to compute particle positions. */
    protected Vector3f _position = new Vector3f(), _last = new Vector3f(), _next = new Vector3f();

    /** Used to store the view vector. */
    protected Vector3f _view = new Vector3f();

    /** Used to compute particle rotations. */
    protected Quaternion _rotation = new Quaternion(), _vrot = new Quaternion();

    /** Used to compute particle offsets. */
    protected Vector3f _s = new Vector3f(), _t = new Vector3f(), _r = new Vector3f();

    /** Used to compute particle normals. */
    protected Vector3f _n = new Vector3f();
}
