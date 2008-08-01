//
// $Id$

package com.threerings.opengl.geom.config;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.google.common.collect.Maps;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;

import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Box;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.renderer.config.ClientArrayConfig;
import com.threerings.opengl.renderer.ClientArray;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.ArrayState;
import com.threerings.opengl.util.GlContext;

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

        protected int _constant;
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
         * Creates a set of array states for this geometry.
         */
        protected ArrayState[] createArrayStates (GlContext ctx, PassDescriptor[] passes)
        {
            // find out which attributes the passes use
            HashSet<String> vertexAttribs = new HashSet<String>();
            ArrayIntSet texCoordSets = new ArrayIntSet();
            boolean colors = false, normals = false;
            for (PassDescriptor pass : passes) {
                Collections.addAll(vertexAttribs, pass.vertexAttribs);
                texCoordSets.add(pass.texCoordSets);
                colors |= pass.colors;
                normals |= pass.normals;
            }

            // create the base arrays
            HashMap<String, ClientArray> vertexAttribArrays = Maps.newHashMap();
            for (String attrib : vertexAttribs) {
                AttributeArrayConfig vertexAttribArray = getVertexAttribArray(attrib);
                if (vertexAttribArray != null) {
                    vertexAttribArrays.put(attrib, vertexAttribArray.createClientArray());
                }
            }
            HashIntMap<ClientArray> texCoordArrays = new HashIntMap<ClientArray>();
            for (int set : texCoordSets) {
                ClientArrayConfig texCoordArray = getTexCoordArray(set);
                if (texCoordArray != null) {
                    texCoordArrays.put(set, texCoordArray.createClientArray());
                }
            }
            ClientArray colorArray = (colors && this.colorArray != null) ?
                this.colorArray.createClientArray() : null;
            ClientArray normalArray = (normals && this.normalArray != null) ?
                this.normalArray.createClientArray() : null;
            ClientArray vertexArray = this.vertexArray.createClientArray();

            // put them all in a list
            ArrayList<ClientArray> arrays = new ArrayList<ClientArray>();
            arrays.addAll(vertexAttribArrays.values());
            arrays.addAll(texCoordArrays.values());
            if (colorArray != null) {
                arrays.add(colorArray);
            }
            if (normalArray != null) {
                arrays.add(normalArray);
            }
            arrays.add(vertexArray);

            // compute the offsets and stride
            int offset = 0;
            for (ClientArray array : arrays) {
                array.offset = offset;
                offset += array.getElementBytes();
            }

            // bump the stride up to the nearest power of two
            int stride = (Integer.bitCount(offset) > 1) ?
                (Integer.highestOneBit(offset) << 1) : offset;

            // allocate the buffer and update the arrays
            int vsize = stride / 4;
            FloatBuffer vbuf = BufferUtils.createFloatBuffer(getVertexCount() * vsize);
            for (ClientArray array : arrays) {
                array.stride = stride;
                array.floatArray = vbuf;
            }

            // populate the buffer
            for (Map.Entry<String, ClientArray> entry : vertexAttribArrays.entrySet()) {
                getVertexAttribArray(entry.getKey()).populateClientArray(entry.getValue());
            }
            for (Map.Entry<Integer, ClientArray> entry : texCoordArrays.entrySet()) {
                getTexCoordArray(entry.getKey()).populateClientArray(entry.getValue());
            }
            if (colorArray != null) {
                this.colorArray.populateClientArray(colorArray);
            }
            if (normalArray != null) {
                this.normalArray.populateClientArray(normalArray);
            }
            this.vertexArray.populateClientArray(vertexArray);

            // create the states for each pass
            ArrayState[] states = new ArrayState[passes.length];
            for (int ii = 0; ii < passes.length; ii++) {
                PassDescriptor pass = passes[ii];
                ClientArray[] attribArrays = new ClientArray[pass.vertexAttribs.length];
                for (int jj = 0; jj < attribArrays.length; jj++) {
                    attribArrays[jj] = vertexAttribArrays.get(pass.vertexAttribs[jj]);
                }
                ClientArray[] coordArrays = new ClientArray[pass.texCoordSets.length];
                for (int jj = 0; jj < coordArrays.length; jj++) {
                    coordArrays[jj] = texCoordArrays.get(pass.texCoordSets[jj]);
                }
                states[ii] = new ArrayState(
                    pass.firstVertexAttribIndex, attribArrays, coordArrays,
                    (pass.colors ? colorArray : null), (pass.normals ? normalArray : null),
                    vertexArray, null);
            }
            return states;
        }

        /**
         * Returns the number of vertices in the arrays.
         */
        protected int getVertexCount ()
        {
            return (vertexArray.floatArray.capacity() * 4) / vertexArray.stride;
        }

        /**
         * Returns the vertex attribute array config with the specified name.
         */
        protected AttributeArrayConfig getVertexAttribArray (String name)
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
        protected ClientArrayConfig getTexCoordArray (int idx)
        {
            return (texCoordArrays != null && texCoordArrays.length > idx) ?
                texCoordArrays[idx] : null;
        }
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

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            return null;
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

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            return null;
        }
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

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, Scope scope, PassDescriptor[] passes)
        {
            return null;
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
    }

    /**
     * Creates an instance of the geometry described by this config.
     */
    public abstract Geometry createGeometry (GlContext ctx, Scope scope, PassDescriptor[] passes);
}
