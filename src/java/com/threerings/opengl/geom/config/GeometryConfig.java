//
// $Id$

package com.threerings.opengl.geom.config;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;

import com.threerings.export.Exportable;
import com.threerings.math.Box;
import com.threerings.util.DeepObject;
import com.threerings.util.Shallow;

import com.threerings.opengl.geom.Geometry;
import com.threerings.opengl.renderer.config.LightStateConfig;
import com.threerings.opengl.renderer.config.ShaderConfig;
import com.threerings.opengl.renderer.config.ShaderStateConfig;
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

    /** Type constants. */
    public enum Type
    {
        BYTE(GL11.GL_BYTE),
        UNSIGNED_BYTE(GL11.GL_UNSIGNED_BYTE),
        SHORT(GL11.GL_SHORT),
        UNSIGNED_SHORT(GL11.GL_UNSIGNED_SHORT),
        INT(GL11.GL_INT),
        UNSIGNED_INT(GL11.GL_UNSIGNED_INT),
        FLOAT(GL11.GL_FLOAT),
        DOUBLE(GL11.GL_DOUBLE);

        public int getConstant ()
        {
            return _constant;
        }

        Type (int constant)
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

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, PassDescriptor[] passes)
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

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, PassDescriptor[] passes)
        {
            return null;
        }
    }

    /**
     * Skinned indexed geometry.
     */
    public static class SkinnedIndexStored extends IndexedStored
    {
        /** The names of the bones referenced by the <code>boneIndices</code> attribute. */
        public String[] bones;

        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, PassDescriptor[] passes)
        {
            return null;
        }
    }

    /**
     * Describes an interleaved array.
     */
    public static class ClientArrayConfig extends DeepObject
        implements Exportable
    {
        /** The number of components in each element. */
        public int size;

        /** The type of the components. */
        public Type type;

        /** Whether or not to normalize the components. */
        public boolean normalized;

        /** The stride between adjacent elements. */
        public int stride;

        /** The offset of the first component. */
        public int offset;

        /** The float array, if using one. */
        @Shallow
        public FloatBuffer floatArray;
    }

    /**
     * Describes an interleaved attribute array.
     */
    public static class AttributeArrayConfig extends ClientArrayConfig
    {
        /** The name of the attribute. */
        public String name;
    }

    /**
     * Creates an instance of the geometry described by this config.
     */
    public abstract Geometry createGeometry (GlContext ctx, PassDescriptor[] passes);
}
