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

package com.threerings.opengl.geometry.config;

import java.lang.ref.SoftReference;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.util.HashIntMap;

import com.threerings.export.Exportable;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.math.Box;
import com.threerings.math.Matrix4f;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;
import com.threerings.util.ArrayKey;
import com.threerings.util.CacheUtil;
import com.threerings.util.DeepObject;
import com.threerings.util.IdentityKey;
import com.threerings.util.Shallow;

import com.threerings.opengl.geometry.Geometry;
import com.threerings.opengl.geometry.util.GeometryUtil;
import com.threerings.opengl.renderer.config.ClientArrayConfig;
import com.threerings.opengl.renderer.config.CoordSpace;
import com.threerings.opengl.renderer.BufferObject;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.SimpleBatch.DrawCommand;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.GlUtil;

/**
 * Geometry configuration.
 */
public abstract class GeometryConfig extends DeepObject
    implements Exportable
{
    /** Mode constants. */
    public enum Mode
    {
        POINTS(GL11.GL_POINTS),
        LINES(GL11.GL_LINES),
        LINE_STRIP(GL11.GL_LINE_STRIP),
        LINE_LOOP(GL11.GL_LINE_LOOP),
        TRIANGLES(GL11.GL_TRIANGLES),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN),
        QUADS(GL11.GL_QUADS),
        QUAD_STRIP(GL11.GL_QUAD_STRIP),
        POLYGON(GL11.GL_POLYGON);

        public int getConstant ()
        {
            return _constant;
        }

        Mode (int constant)
        {
            _constant = constant;
        }

        protected final int _constant;
    }

    /**
     * Superclass of configurations with stored geometry.
     */
    public static abstract class Stored extends GeometryConfig
    {
        /** The bounds of the geometry. */
        public Box bounds;

        /** The type of primitives to render. */
        public Mode mode;

        /** The vertex attribute arrays. */
        public AttributeArrayConfig[] vertexAttribArrays;

        /** The texture coordinate arrays. */
        public ClientArrayConfig[] texCoordArrays;

        /** The color array. */
        public ClientArrayConfig colorArray;

        /** The normal array. */
        public ClientArrayConfig normalArray;

        /** The vertex array. */
        public ClientArrayConfig vertexArray;

        public Stored (
            Box bounds, Mode mode, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray)
        {
            this.bounds = bounds;
            this.mode = mode;
            this.vertexAttribArrays = vertexAttribArrays;
            this.texCoordArrays = texCoordArrays;
            this.colorArray = colorArray;
            this.normalArray = normalArray;
            this.vertexArray = vertexArray;
        }

        public Stored ()
        {
        }

        /**
         * Returns the number of vertices in the arrays.
         */
        public int getVertexCount ()
        {
            return (vertexArray.floatArray.capacity() * 4) / vertexArray.stride;
        }

        /**
         * Returns the vertex attribute array config with the specified name.
         */
        public AttributeArrayConfig getVertexAttribArray (String name)
        {
            if (vertexAttribArrays != null) {
                for (AttributeArrayConfig array : vertexAttribArrays) {
                    if (array.name.equals(name)) {
                        return array;
                    }
                }
            }
            return null;
        }

        /**
         * Returns the texture coordinate array config at the specified index.
         */
        public ClientArrayConfig getTexCoordArray (int idx)
        {
            return (texCoordArrays != null && texCoordArrays.length > idx) ?
                texCoordArrays[idx] : null;
        }

        /**
         * Returns a float array containing the interleaved contents of the specified arrays.
         *
         * @param align if true, use power-of-two alignment.
         */
        public float[] getFloatArray (boolean align, ClientArrayConfig... arrays)
        {
            if (_floatArrays == null) {
                _floatArrays = CacheUtil.softValues(1);
            }
            IdentityKey key = new IdentityKey(align, (Object[])arrays);
            float[] array = _floatArrays.get(key);
            if (array == null) {
                int offset = 0;
                int[] offsets = new int[arrays.length];
                for (int ii = 0; ii < arrays.length; ii++) {
                    offsets[ii] = offset;
                    offset += arrays[ii].size;
                }
                int stride = align ? GlUtil.nextPowerOfTwo(offset) : offset;
                _floatArrays.put(key, array = new float[stride * getVertexCount()]);
                for (int ii = 0; ii < arrays.length; ii++) {
                    arrays[ii].populateFloatArray(array, offsets[ii], stride);
                }
            }
            return array;
        }

        /**
         * Returns an int array containing the interleaved contents of the specified arrays.
         *
         * @param align if true, use power-of-two alignment.
         */
        public int[] getIntArray (boolean align, ClientArrayConfig... arrays)
        {
            if (_intArrays == null) {
                _intArrays = CacheUtil.softValues(1);
            }
            IdentityKey key = new IdentityKey(align, (Object[])arrays);
            int[] array = _intArrays.get(key);
            if (array == null) {
                int offset = 0;
                int[] offsets = new int[arrays.length];
                for (int ii = 0; ii < arrays.length; ii++) {
                    offsets[ii] = offset;
                    offset += arrays[ii].size;
                }
                int stride = align ? GlUtil.nextPowerOfTwo(offset) : offset;
                _intArrays.put(key, array = new int[stride * getVertexCount()]);
                for (int ii = 0; ii < arrays.length; ii++) {
                    arrays[ii].populateIntArray(array, offsets[ii], stride);
                }
            }
            return array;
        }

        /**
         * Creates static geometry for the described passes.
         */
        public Geometry createStaticGeometry (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            final Matrix4f[] boneMatrices = getBoneMatrices(scope);
            final CoordSpace[] coordSpaces = getCoordSpaces(passes);
            final Vector3f center = bounds.getCenter();
            if (GLContext.getCapabilities().GL_ARB_vertex_buffer_object) {
                final ArrayState[] arrayStates = createArrayStates(ctx, passes, true, true);
                final DrawCommand drawCommand = createDrawCommand(true);
                return new Geometry() {
                    public Matrix4f[] getBoneMatrices () {
                        return boneMatrices;
                    }
                    public CoordSpace getCoordSpace (int pass) {
                        return coordSpaces[pass];
                    }
                    public Vector3f getCenter () {
                        return center;
                    }
                    public ArrayState getArrayState (int pass) {
                        return arrayStates[pass];
                    }
                    public DrawCommand getDrawCommand (int pass) {
                        return drawCommand;
                    }
                };
            } else {
                final DrawCommand[] drawCommands = getListCommands(ctx, passes);
                return new Geometry() {
                    public Matrix4f[] getBoneMatrices () {
                        return boneMatrices;
                    }
                    public CoordSpace getCoordSpace (int pass) {
                        return coordSpaces[pass];
                    }
                    public Vector3f getCenter () {
                        return center;
                    }
                    public ArrayState getArrayState (int pass) {
                        return ArrayState.DISABLED;
                    }
                    public DrawCommand getDrawCommand (int pass) {
                        return drawCommands[pass];
                    }
                };
            }
        }

        /**
         * Returns the matrices of the bones influencing this geometry, if any.
         */
        public Matrix4f[] getBoneMatrices (Scope scope)
        {
            return null;
        }

        /**
         * Creates a set of array states for this geometry.
         *
         * @param staticVBO if true, use a shared static buffer object for vertex data.
         * @param staticIBO if true, use a shared static buffer object for index data.
         */
        public ArrayState[] createArrayStates (
            GlContext ctx, PassDescriptor[] passes, boolean staticVBO, boolean staticIBO)
        {
            return createArrayStates(
                ctx, passes, new PassSummary(passes), staticVBO, staticIBO, null, null);
        }

        /**
         * Creates a set of array states for this geometry.
         *
         * @param staticVBO if true, use a shared static buffer object for vertex data.
         * @param staticIBO if true, use a shared static buffer object for index data.
         * @param arrayBuffer the precreated buffer object to use for the arrays, if any.
         * @param floatArray the precreated float buffer to use for the arrays, if any.
         */
        public ArrayState[] createArrayStates (
            GlContext ctx, PassDescriptor[] passes, PassSummary summary,
            boolean staticVBO, boolean staticIBO, BufferObject arrayBuffer, FloatBuffer floatArray)
        {
            // create the base arrays
            HashMap<String, ClientArray> vertexAttribArrays = Maps.newHashMap();
            for (String attrib : summary.vertexAttribs) {
                AttributeArrayConfig vertexAttribArray = getVertexAttribArray(attrib);
                if (vertexAttribArray != null) {
                    vertexAttribArrays.put(attrib, vertexAttribArray.createClientArray());
                }
            }
            HashIntMap<ClientArray> texCoordArrays = new HashIntMap<ClientArray>();
            for (int set : summary.texCoordSets) {
                ClientArrayConfig texCoordArray = getTexCoordArray(set);
                if (texCoordArray != null) {
                    texCoordArrays.put(set, texCoordArray.createClientArray());
                }
            }
            ClientArray colorArray = (summary.colors && this.colorArray != null) ?
                this.colorArray.createClientArray() : null;
            ClientArray normalArray = (summary.normals && this.normalArray != null) ?
                this.normalArray.createClientArray() : null;
            ClientArray vertexArray = this.vertexArray.createClientArray();

            // put them in a list and compute the offsets and stride
            ArrayList<ClientArray> arrays = GeometryUtil.createList(
                vertexAttribArrays, texCoordArrays, colorArray, normalArray, vertexArray);
            GeometryUtil.updateOffsetsAndStride(arrays);

            // if we have been given an array buffer or float array, set them
            if (arrayBuffer != null || floatArray != null) {
                for (ClientArray array : arrays) {
                    array.arrayBuffer = arrayBuffer;
                    array.floatArray = floatArray;
                }

            // otherwise, if we're to use a static vbo, look it up
            } else if (staticVBO) {
                if (_arrayBuffers == null) {
                    _arrayBuffers = CacheUtil.softValues(1);
                }
                arrayBuffer = _arrayBuffers.get(summary);
                if (arrayBuffer == null) {
                    _arrayBuffers.put(summary, arrayBuffer = new BufferObject(ctx.getRenderer()));
                    floatArray = populateClientArrays(
                        vertexAttribArrays, texCoordArrays, colorArray, normalArray, vertexArray);
                    arrayBuffer.setData(floatArray);
                }
                for (ClientArray array : arrays) {
                    array.arrayBuffer = arrayBuffer;
                    array.floatArray = null;
                }

            // otherwise, create and populate a new buffer
            } else {
                populateClientArrays(
                    vertexAttribArrays, texCoordArrays, colorArray, normalArray, vertexArray);
            }

            // create the states for each pass
            return GeometryUtil.createArrayStates(
                vertexAttribArrays, texCoordArrays, colorArray, normalArray,
                vertexArray, staticIBO ? getElementArrayBuffer(ctx) : null, passes);
        }

        /**
         * Creates the non-list draw command for this geometry.
         *
         * @param ibo if true, indices will be read from a buffer object.
         */
        public abstract DrawCommand createDrawCommand (boolean ibo);

        @Override
        public Box getBounds ()
        {
            return bounds;
        }

        @Override
        public Geometry createGeometry (
            GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes)
        {
            if (passes.length == 0) {
                return Geometry.EMPTY;
            }
            return (deformer == null) ? createStaticGeometry(ctx, scope, passes) :
                deformer.createGeometry(ctx, scope, this, passes);
        }

        @Override
        public GeometryConfig merge (List<TransformedGeometry> glist)
        {
            List<TransformedGeometry> merge = Lists.newArrayList();
            Class<? extends Stored> clazz = getClass();
            int vcount = 0;
            for (int ii = glist.size() - 1; ii >= 0; ii--) {
                TransformedGeometry tgeom = glist.get(ii);
                if (tgeom.geometry.getClass() != clazz) {
                    continue;
                }
                Stored stored = (Stored)tgeom.geometry;
                if (canMerge(stored)) {
                    glist.remove(ii);
                    merge.add(tgeom);
                    vcount += stored.getCount();
                }
            }
            if (merge.isEmpty()) {
                return null;
            }
            Stored merged = createMerged(merge);
            merged.bounds = new Box();
            merged.mode = mode;
            int stride = vertexArray.stride / 4;
            FloatBuffer vbuf = BufferUtils.createFloatBuffer(stride * vcount);
            if (vertexAttribArrays != null) {
                merged.vertexAttribArrays = new AttributeArrayConfig[vertexAttribArrays.length];
                for (int ii = 0; ii < vertexAttribArrays.length; ii++) {
                    AttributeArrayConfig attrArray = vertexAttribArrays[ii];
                    merged.vertexAttribArrays[ii] = new AttributeArrayConfig(
                        attrArray.size, attrArray.stride, attrArray.offset, vbuf, attrArray.name);
                }
            }
            if (texCoordArrays != null) {
                merged.texCoordArrays = new ClientArrayConfig[texCoordArrays.length];
                for (int ii = 0; ii < texCoordArrays.length; ii++) {
                    ClientArrayConfig texCoordArray = texCoordArrays[ii];
                    merged.texCoordArrays[ii] = new ClientArrayConfig(
                        texCoordArray.size, texCoordArray.stride, texCoordArray.offset, vbuf);
                }
            }
            if (colorArray != null) {
                merged.colorArray = new ClientArrayConfig(
                    colorArray.size, colorArray.stride, colorArray.offset, vbuf);
            }
            if (normalArray != null) {
                merged.normalArray = new ClientArrayConfig(
                    normalArray.size, normalArray.stride, normalArray.offset, vbuf);
            }
            merged.vertexArray = new ClientArrayConfig(
                vertexArray.size, vertexArray.stride, vertexArray.offset, vbuf);
            int vpos = 0;
            for (int ii = merge.size() - 1; ii >= 0; ii--) {
                TransformedGeometry tgeom = merge.get(ii);
                Stored stored = (Stored)tgeom.geometry;
                merged.bounds.addLocal(stored.bounds.transform(tgeom.transform));

                // perform a bulk transfer of the array data
                FloatBuffer obuf = stored.vertexArray.floatArray;
                int olimit = obuf.limit(), opos = obuf.position();
                int npos = opos + stored.getFirst() * stride;
                int count = stored.getCount();
                obuf.limit(npos + count * stride).position(npos);
                vbuf.put(stored.vertexArray.floatArray);
                obuf.limit(olimit).position(opos);

                // then transform the vertices and normals in-place
                tgeom.transform.update(Transform3D.AFFINE);
                Matrix4f mat = tgeom.transform.getMatrix();
                int voff = vertexArray.offset / 4;
                if (normalArray == null) {
                    for (int jj = 0; jj < count; jj++) {
                        int pos = vpos + voff;
                        float vx = vbuf.get(pos), vy = vbuf.get(pos + 1), vz = vbuf.get(pos + 2);
                        vbuf.put(pos, mat.m00*vx + mat.m10*vy + mat.m20*vz + mat.m30);
                        vbuf.put(pos + 1, mat.m01*vx + mat.m11*vy + mat.m21*vz + mat.m31);
                        vbuf.put(pos + 2, mat.m02*vx + mat.m12*vy + mat.m22*vz + mat.m32);
                        vpos += stride;
                    }
                } else {
                    int noff = normalArray.offset / 4;
                    for (int jj = 0; jj < count; jj++) {
                        int pos = vpos + voff;
                        float vx = vbuf.get(pos), vy = vbuf.get(pos + 1), vz = vbuf.get(pos + 2);
                        vbuf.put(pos, mat.m00*vx + mat.m10*vy + mat.m20*vz + mat.m30);
                        vbuf.put(pos + 1, mat.m01*vx + mat.m11*vy + mat.m21*vz + mat.m31);
                        vbuf.put(pos + 2, mat.m02*vx + mat.m12*vy + mat.m22*vz + mat.m32);

                        pos = vpos + noff;
                        float nx = vbuf.get(pos), ny = vbuf.get(pos + 1), nz = vbuf.get(pos + 2);
                        vbuf.put(pos, mat.m00*nx + mat.m10*ny + mat.m20*nz);
                        vbuf.put(pos + 1, mat.m01*nx + mat.m11*ny + mat.m21*nz);
                        vbuf.put(pos + 2, mat.m02*nx + mat.m12*ny + mat.m22*nz);
                        vpos += stride;
                    }
                }
            }
            vbuf.rewind();
            return merged;
        }

        /**
         * Checks whether, all other things being equal, this geometry can be merged with the
         * specified other.
         */
        protected boolean canMerge (Stored ostored)
        {
            return mode == ostored.mode &&
                vertexArray.canMerge(ostored.vertexArray) &&
                canMerge(normalArray, ostored.normalArray) &&
                canMerge(colorArray, ostored.colorArray) &&
                canMerge(texCoordArrays, ostored.texCoordArrays) &&
                canMerge(vertexAttribArrays, ostored.vertexAttribArrays);
        }

        /**
         * Checks whether the specified arrays of arrays (either or both of which may be
         * <code>null</code>) can be merged.
         */
        protected boolean canMerge (ClientArrayConfig[] a1, ClientArrayConfig[] a2)
        {
            if (a1 == null || a1.length == 0) {
                return a2 == null || a2.length == 0;
            }
            if (a2 == null || a2.length != a1.length) {
                return false;
            }
            for (int ii = 0; ii < a1.length; ii++) {
                if (!canMerge(a1[ii], a2[ii])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Checks whether the specified arrays (either or both of which may be <code>null</code>)
         * can be merged.
         */
        protected boolean canMerge (ClientArrayConfig a1, ClientArrayConfig a2)
        {
            return (a1 == null) ? (a2 == null) : (a2 != null && a1.canMerge(a2));
        }

        /**
         * Returns the index of the first vertex included in the geometry.
         */
        protected abstract int getFirst ();

        /**
         * Returns the number of vertices included in the geometry.
         */
        protected abstract int getCount ();

        /**
         * Creates the merged geometry (without initializing the arrays, etc.)
         */
        protected abstract Stored createMerged (List<TransformedGeometry> glist);

        /**
         * Extracts the coord spaces from the supplied passes.
         */
        protected CoordSpace[] getCoordSpaces (PassDescriptor[] passes)
        {
            CoordSpace[] spaces = new CoordSpace[passes.length];
            for (int ii = 0; ii < spaces.length; ii++) {
                spaces[ii] = passes[ii].coordSpace;
            }
            return spaces;
        }

        /**
         * Creates a new buffer and populates the specified client arrays with this geometry's
         * array data.
         *
         * @return a reference to the newly created and populated buffer.
         */
        protected FloatBuffer populateClientArrays (
            HashMap<String, ClientArray> vertexAttribArrays,
            HashIntMap<ClientArray> texCoordArrays,
            ClientArray colorArray, ClientArray normalArray, ClientArray vertexArray)
        {
            FloatBuffer floatArray = BufferUtils.createFloatBuffer(
                getVertexCount() * vertexArray.stride / 4);
            for (Map.Entry<String, ClientArray> entry : vertexAttribArrays.entrySet()) {
                ClientArray clientArray = entry.getValue();
                clientArray.floatArray = floatArray;
                getVertexAttribArray(entry.getKey()).populateClientArray(clientArray);
            }
            for (Map.Entry<Integer, ClientArray> entry : texCoordArrays.entrySet()) {
                ClientArray clientArray = entry.getValue();
                clientArray.floatArray = floatArray;
                getTexCoordArray(entry.getKey()).populateClientArray(clientArray);
            }
            if (colorArray != null) {
                colorArray.floatArray = floatArray;
                this.colorArray.populateClientArray(colorArray);
            }
            if (normalArray != null) {
                normalArray.floatArray = floatArray;
                this.normalArray.populateClientArray(normalArray);
            }
            vertexArray.floatArray = floatArray;
            this.vertexArray.populateClientArray(vertexArray);
            return floatArray;
        }

        /**
         * Retrieves a reference to the element array buffer, if one should be used.
         */
        protected BufferObject getElementArrayBuffer (GlContext ctx)
        {
            return null;
        }

        /**
         * Retrieves a set of display list draw commands for this geometry.
         */
        protected DrawCommand[] getListCommands (GlContext ctx, PassDescriptor[] passes)
        {
            if (_listCommands == null) {
                _listCommands = CacheUtil.softValues(1);
            }
            DrawCommand[] commands = new DrawCommand[passes.length];
            for (int ii = 0; ii < passes.length; ii++) {
                PassDescriptor pass = passes[ii];
                DrawCommand command = _listCommands.get(pass);
                if (command == null) {
                    _listCommands.put(pass, command = createListCommand(ctx, pass));
                }
                commands[ii] = command;
            }
            return commands;
        }

        /**
         * Creates a display list draw command for this geometry.
         */
        protected DrawCommand createListCommand (GlContext ctx, PassDescriptor pass)
        {
            Renderer renderer = ctx.getRenderer();
            DisplayList list = new DisplayList(renderer);
            ArrayState state = createArrayState(pass);
            DrawCommand command = createDrawCommand(false);
            state.apply(renderer);
            list.begin();
            command.call();
            list.end();
            return new SimpleBatch.CallList(
                list, state.getColorArray() != null, command.getPrimitiveCount());
        }

        /**
         * Creates an uncompiled array state for the supplied pass.
         */
        protected ArrayState createArrayState (PassDescriptor pass)
        {
            ClientArray[] vertexAttribArrays = new ClientArray[pass.vertexAttribs.length];
            for (int ii = 0; ii < vertexAttribArrays.length; ii++) {
                AttributeArrayConfig vertexAttribArray =
                    getVertexAttribArray(pass.vertexAttribs[ii]);
                vertexAttribArrays[ii] = (vertexAttribArray == null) ?
                    null : vertexAttribArray.createClientArray();
            }
            ClientArray[] texCoordArrays = new ClientArray[pass.texCoordSets.length];
            for (int ii = 0; ii < texCoordArrays.length; ii++) {
                ClientArrayConfig texCoordArray = getTexCoordArray(pass.texCoordSets[ii]);
                texCoordArrays[ii] = (texCoordArray == null) ?
                    null : texCoordArray.createClientArray();
            }
            ClientArray colorArray = (pass.colors && this.colorArray != null) ?
                this.colorArray.createClientArray() : null;
            ClientArray normalArray = (pass.normals && this.normalArray != null) ?
                this.normalArray.createClientArray() : null;
            ClientArray vertexArray = this.vertexArray.createClientArray();
            return new ArrayState(
                pass.firstVertexAttribIndex, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray, null);
        }

        /** Cached array buffers. */
        protected transient Map<PassSummary, BufferObject> _arrayBuffers;

        /** Cached display list commands. */
        protected transient Map<PassDescriptor, DrawCommand> _listCommands;

        /** Cached float arrays. */
        protected transient Map<IdentityKey, float[]> _floatArrays;

        /** Cached int arrays. */
        protected transient Map<IdentityKey, int[]> _intArrays;
    }

    /**
     * Array geometry.
     */
    public static class ArrayStored extends Stored
    {
        /** The starting array index. */
        public int first;

        /** The number of indices to render. */
        public int count;

        public ArrayStored (
            Box bounds, Mode mode, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray,
            int first, int count)
        {
            super(bounds, mode, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray);
            this.first = first;
            this.count = count;
        }

        public ArrayStored ()
        {
        }

        @Override
        public DrawCommand createDrawCommand (boolean ibo)
        {
            return new SimpleBatch.DrawArrays(mode.getConstant(), first, count);
        }

        @Override
        protected int getFirst ()
        {
            return first;
        }

        @Override
        protected int getCount ()
        {
            return count;
        }

        @Override
        protected Stored createMerged (List<TransformedGeometry> glist)
        {
            ArrayStored merged = new ArrayStored();
            for (int ii = 0, nn = glist.size(); ii < nn; ii++) {
                merged.count += ((ArrayStored)glist.get(ii).geometry).count;
            }
            return merged;
        }
    }

    /**
     * Indexed geometry.
     */
    public static class IndexedStored extends Stored
    {
        /** The minimum index. */
        public int start;

        /** The maximum index. */
        public int end;

        /** The index buffer. */
        @Shallow
        public ShortBuffer indices;

        public IndexedStored (
            Box bounds, Mode mode, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray,
            int start, int end, ShortBuffer indices)
        {
            super(bounds, mode, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray);
            this.start = start;
            this.end = end;
            this.indices = indices;
        }

        public IndexedStored ()
        {
        }

        @Override
        public DrawCommand createDrawCommand (boolean ibo)
        {
            return ibo ?
                SimpleBatch.createDrawBufferElements(
                    mode.getConstant(), start, end, indices.capacity(),
                    GL11.GL_UNSIGNED_SHORT, 0L) :
                SimpleBatch.createDrawShortElements(
                    mode.getConstant(), start, end, indices);
        }

        @Override
        protected int getFirst ()
        {
            return start;
        }

        @Override
        protected int getCount ()
        {
            return (end - start) + 1;
        }

        @Override
        protected Stored createMerged (List<TransformedGeometry> glist)
        {
            IndexedStored merged = new IndexedStored();
            int vcount = 0, icount = 0;
            for (int ii = 0, nn = glist.size(); ii < nn; ii++) {
                IndexedStored indexed = (IndexedStored)glist.get(ii).geometry;
                vcount += indexed.getCount();
                icount += indexed.indices.remaining();
            }
            merged.end = vcount - 1;
            ShortBuffer mindices = merged.indices = BufferUtils.createShortBuffer(icount);
            int offset = 0;
            for (int ii = glist.size() - 1; ii >= 0; ii--) {
                IndexedStored indexed = (IndexedStored)glist.get(ii).geometry;
                ShortBuffer sindices = indexed.indices;
                int delta = offset - indexed.start;
                for (int jj = sindices.position(), jjmax = sindices.limit(); jj < jjmax; jj++) {
                    mindices.put((short)(sindices.get(jj) + delta));
                }
                offset += indexed.getCount();
            }
            merged.indices.rewind();
            return merged;
        }

        @Override
        protected BufferObject getElementArrayBuffer (GlContext ctx)
        {
            BufferObject buffer = (_elementArrayBuffer == null) ? null : _elementArrayBuffer.get();
            if (buffer == null) {
                _elementArrayBuffer = new SoftReference<BufferObject>(
                    buffer = new BufferObject(ctx.getRenderer()));
                buffer.setData(indices);
            }
            return buffer;
        }

        /** The cached element array buffer. */
        protected transient SoftReference<BufferObject> _elementArrayBuffer;
    }

    /**
     * Skinned indexed geometry.
     */
    public static class SkinnedIndexedStored extends IndexedStored
    {
        /** The names of the bones referenced by the <code>boneIndices</code> attribute. */
        public String[] bones;

        public SkinnedIndexedStored (
            Box bounds, Mode mode, AttributeArrayConfig[] vertexAttribArrays,
            ClientArrayConfig[] texCoordArrays, ClientArrayConfig colorArray,
            ClientArrayConfig normalArray, ClientArrayConfig vertexArray,
            int start, int end, ShortBuffer indices, String[] bones)
        {
            super(bounds, mode, vertexAttribArrays, texCoordArrays,
                colorArray, normalArray, vertexArray, start, end, indices);
            this.bones = bones;
        }

        public SkinnedIndexedStored ()
        {
        }

        @Override
        public Matrix4f[] getBoneMatrices (Scope scope)
        {
            Function getBoneMatrix = ScopeUtil.resolve(scope, "getBoneMatrix", Function.NULL);
            Matrix4f[] matrices = new Matrix4f[bones.length];
            for (int ii = 0; ii < bones.length; ii++) {
                Matrix4f matrix = (Matrix4f)getBoneMatrix.call(bones[ii]);
                matrices[ii] = (matrix == null) ? new Matrix4f() : matrix;
            }
            return matrices;
        }
    }

    /**
     * Describes an interleaved attribute array.
     */
    public static class AttributeArrayConfig extends ClientArrayConfig
    {
        /** The name of the attribute. */
        public String name;

        public AttributeArrayConfig (int size, String name)
        {
            super(size);
            this.name = name;
        }

        public AttributeArrayConfig (
            int size, int stride, int offset, FloatBuffer floatArray, String name)
        {
            super(size, stride, offset, floatArray);
            this.name = name;
        }

        public AttributeArrayConfig ()
        {
        }

        @Override
        public boolean canMerge (ClientArrayConfig oarray)
        {
            return super.canMerge(oarray) && oarray instanceof AttributeArrayConfig &&
                ((AttributeArrayConfig)oarray).name.equals(name);
        }
    }

    /**
     * Returns a shared {@link GeometryConfig} instance representing a quad extending from
     * (-1, -1, 0) to (+1, +1, 0) with the specified number of divisions in x and y.
     */
    public static GeometryConfig getQuad (int divisionsX, int divisionsY)
    {
        ArrayKey key = new ArrayKey("quad", divisionsX, divisionsY);
        GeometryConfig quad = _generated.get(key);
        if (quad == null) {
            _generated.put(key, quad = createQuad(2f, 2f, divisionsX, divisionsY));
        }
        return quad;
    }

    /**
     * Creates a new {@link GeometryConfig} instance representing a quad extending from
     * (-sizeX/2, -sizeY/2, 0) to (+sizeX/2, +sizeY/2, 0) with the specified number of
     * divisions in x and y.
     */
    public static GeometryConfig createQuad (
        float sizeX, float sizeY, int divisionsX, int divisionsY)
    {
        int vx = divisionsX + 1, vy = divisionsY + 1;
        FloatBuffer floatArray = BufferUtils.createFloatBuffer(vx * vy * 8);
        for (int ii = 0; ii <= divisionsY; ii++) {
            float t = ii / (float)divisionsY;
            float y = sizeY * (t - 0.5f);
            for (int jj = 0; jj <= divisionsX; jj++) {
                float s = jj / (float)divisionsX;
                float x = sizeX * (s - 0.5f);
                floatArray.put(s).put(t);
                floatArray.put(0f).put(0f).put(1f);
                floatArray.put(x).put(y).put(0f);
            }
        }
        floatArray.rewind();

        ShortBuffer indices = BufferUtils.createShortBuffer(divisionsX * divisionsY * 2 * 3);
        for (int ii = 0; ii < divisionsY; ii++) {
            for (int jj = 0; jj < divisionsX; jj++) {
                short ll = (short)(ii*vx + jj);
                short ul = (short)(ll + vx);
                short ur = (short)(ul + 1);
                short lr = (short)(ll + 1);
                indices.put(ul).put(ll).put(ur);
                indices.put(ur).put(ll).put(lr);
            }
        }
        indices.rewind();

        ClientArrayConfig texCoordArray = new ClientArrayConfig(2, 32, 0, floatArray);
        ClientArrayConfig normalArray = new ClientArrayConfig(3, 32, 8, floatArray);
        ClientArrayConfig vertexArray = new ClientArrayConfig(3, 32, 20, floatArray);
        float hx = sizeX * 0.5f, hy = sizeY * 0.5f;
        return new IndexedStored(
            new Box(new Vector3f(-hx, -hy, 0f), new Vector3f(+hx, +hy, 0f)), Mode.TRIANGLES,
            new AttributeArrayConfig[0], new ClientArrayConfig[] { texCoordArray }, null,
            normalArray, vertexArray, 0, indices.capacity() - 1, indices);
    }

    /**
     * Returns the bounds of the geometry.
     */
    public abstract Box getBounds ();

    /**
     * Attempts to merge several transformed geometries.
     *
     * @param glist a list of the geometry to merge.  Any successfully merged elements will be
     * removed from the list.
     * @return the combined geometry, or <code>null</code> if we could not merge any.
     */
    public GeometryConfig merge (List<TransformedGeometry> glist)
    {
        return null;
    }

    /**
     * Creates an instance of the geometry described by this config.
     */
    public abstract Geometry createGeometry (
        GlContext ctx, Scope scope, DeformerConfig deformer, PassDescriptor[] passes);

    /** Cached generated configs. */
    protected static Map<ArrayKey, GeometryConfig> _generated = CacheUtil.softValues();
}
