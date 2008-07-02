//
// $Id$

package com.threerings.opengl.geom.config;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;

import com.threerings.export.Exportable;
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

    /**
     * Superclass of configurations with stored geometry.
     */
    public static abstract class Stored extends GeometryConfig
    {
        /** The type of primitives to render. */
        protected Mode _mode = Mode.TRIANGLES;

        /** The array containing the interleaved vertex data. */
        @Shallow
        protected FloatBuffer _array;

        /** The stride between adjacent vertices. */
        protected int _stride;

        /** The vertex attribute arrays. */
        protected AttributeArrayConfig[] _vertexAttribArrays;

        /** The texture coordinate arrays. */
        protected ArrayConfig[] _texCoordArrays;

        /** The color array. */
        protected ArrayConfig _colorArray;

        /** The normal array. */
        protected ArrayConfig _normalArray;

        /** The vertex array. */
        protected ArrayConfig _vertexArray;
    }

    /**
     * Array geometry.
     */
    public static class ArrayStored extends Stored
    {
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
        @Override // documentation inherited
        public Geometry createGeometry (GlContext ctx, PassDescriptor[] passes)
        {
            return null;
        }

        /** The index buffer. */
        @Shallow
        protected ShortBuffer _indices;
    }

    /**
     * Describes an interleaved array.
     */
    public static class ArrayConfig extends DeepObject
        implements Exportable
    {
        /** The number of components in each element. */
        public int size;

        /** The offset of the first component. */
        public int offset;
    }

    /**
     * Describes an interleaved attribute array.
     */
    public static class AttributeArrayConfig extends ArrayConfig
    {
        /** The name of the attribute. */
        public String name;
    }

    /**
     * Creates an instance of the geometry described by this config.
     */
    public abstract Geometry createGeometry (GlContext ctx, PassDescriptor[] passes);
}
