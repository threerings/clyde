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

package com.threerings.tudey.shape.config;

import java.lang.ref.SoftReference;

import org.lwjgl.opengl.GL11;

import com.threerings.io.Streamable;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.shape.Shape;

/**
 * The configuration for a shape.
 */
@EditorTypes({
    ShapeConfig.Point.class, ShapeConfig.Segment.class, ShapeConfig.Rectangle.class,
    ShapeConfig.Circle.class, ShapeConfig.Capsule.class, ShapeConfig.Polygon.class,
    ShapeConfig.Compound.class, ShapeConfig.Global.class  })
public abstract class ShapeConfig extends DeepObject
    implements Exportable, Streamable
{
    /**
     * A point.
     */
    public static class Point extends ShapeConfig
    {
        @Override
        protected Shape createShape ()
        {
            return new com.threerings.tudey.shape.Point(Vector2f.ZERO);
        }

        @Override
        protected void draw (boolean outline)
        {
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex2f(0f, 0f);
            GL11.glEnd();
        }
    }

    /**
     * A line segment.
     */
    public static class Segment extends ShapeConfig
    {
        /** The length of the segment. */
        @Editable(min=0, step=0.01)
        public float length = 1f;

        @Override
        protected Shape createShape ()
        {
            float hlength = length * 0.5f;
            return new com.threerings.tudey.shape.Segment(
                new Vector2f(-hlength, 0f), new Vector2f(+hlength, 0f));
        }

        @Override
        protected void draw (boolean outline)
        {
            float hlength = length * 0.5f;
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(-hlength, 0f);
            GL11.glVertex2f(+hlength, 0f);
            GL11.glEnd();
        }
    }

    /**
     * A rectangle.
     */
    public static class Rectangle extends ShapeConfig
    {
        /** The width of the rectangle. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float width = 1f;

        /** The height of the rectangle. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float height = 1f;

        @Override
        protected Shape createShape ()
        {
            float hwidth = width * 0.5f, hheight = height * 0.5f;
            return new com.threerings.tudey.shape.Polygon(
                new Vector2f(-hwidth, -hheight), new Vector2f(+hwidth, -hheight),
                new Vector2f(+hwidth, +hheight), new Vector2f(-hwidth, +hheight));
        }

        @Override
        protected void draw (boolean outline)
        {
            float hwidth = width * 0.5f, hheight = height * 0.5f;
            GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_QUADS);
            GL11.glVertex2f(-hwidth, -hheight);
            GL11.glVertex2f(+hwidth, -hheight);
            GL11.glVertex2f(+hwidth, +hheight);
            GL11.glVertex2f(-hwidth, +hheight);
            GL11.glEnd();
        }
    }

    /**
     * A circle.
     */
    public static class Circle extends ShapeConfig
    {
        /** The radius of the circle. */
        @Editable(min=0, step=0.01)
        public float radius = 1f;

        @Override
        protected Shape createShape ()
        {
            return new com.threerings.tudey.shape.Circle(Vector2f.ZERO, radius);
        }

        @Override
        protected void draw (boolean outline)
        {
            GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
            for (int ii = 0; ii < CIRCLE_SEGMENTS; ii++) {
                float angle = ii * CIRCLE_INCREMENT;
                GL11.glVertex2f(FloatMath.cos(angle) * radius, FloatMath.sin(angle) * radius);
            }
            GL11.glEnd();
        }
    }

    /**
     * A capsule.
     */
    public static class Capsule extends ShapeConfig
    {
        /** The radius of the capsule. */
        @Editable(min=0, step=0.01, hgroup="c")
        public float radius = 1f;

        /** The length of the capsule. */
        @Editable(min=0, step=0.01, hgroup="c")
        public float length = 1f;

        @Override
        protected Shape createShape ()
        {
            float hlength = length * 0.5f;
            return new com.threerings.tudey.shape.Capsule(
                new Vector2f(-hlength, 0f), new Vector2f(+hlength, 0f), radius);
        }

        @Override
        protected void draw (boolean outline)
        {
            float hlength = length * 0.5f;
            GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
            for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
                float angle = ii * CIRCLE_INCREMENT + FloatMath.HALF_PI;
                GL11.glVertex2f(FloatMath.cos(angle) * radius - hlength,
                    FloatMath.sin(angle) * radius);
            }
            for (int ii = 0, nn = CIRCLE_SEGMENTS / 2; ii <= nn; ii++) {
                float angle = ii * CIRCLE_INCREMENT - FloatMath.HALF_PI;
                GL11.glVertex2f(FloatMath.cos(angle) * radius + hlength,
                    FloatMath.sin(angle) * radius);
            }
            GL11.glEnd();
        }
    }

    /**
     * A polygon.
     */
    public static class Polygon extends ShapeConfig
    {
        /** The vertices of the polygon. */
        @Editable(editor="table")
        public Vertex[] vertices = new Vertex[0];

        @Override
        protected Shape createShape ()
        {
            Vector2f[] vectors = new Vector2f[vertices.length];
            for (int ii = 0; ii < vertices.length; ii++) {
                vectors[ii] = vertices[ii].createVector();
            }
            return new com.threerings.tudey.shape.Polygon(vectors);
        }

        @Override
        protected void draw (boolean outline)
        {
            GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_POLYGON);
            for (Vertex vertex : vertices) {
                GL11.glVertex2f(vertex.x, vertex.y);
            }
            GL11.glEnd();
        }
    }

    /**
     * A single vertex in a polygon.
     */
    public static class Vertex extends DeepObject
        implements Exportable, Streamable
    {
        /** The vertex coordinates. */
        @Editable(column=true)
        public float x, y;

        /**
         * Creates a vector from this vertex.
         */
        public Vector2f createVector ()
        {
            return new Vector2f(x, y);
        }
    }

    /**
     * A compound shape.
     */
    public static class Compound extends ShapeConfig
    {
        /** The component shapes. */
        @Editable
        public TransformedShape[] shapes = new TransformedShape[0];

        @Override
        public void invalidate ()
        {
            super.invalidate();
            for (TransformedShape tshape : shapes) {
                tshape.invalidate();
            }
        }

        @Override
        protected Shape createShape ()
        {
            Shape[] tshapes = new Shape[shapes.length];
            for (int ii = 0; ii < shapes.length; ii++) {
                tshapes[ii] = shapes[ii].getShape();
            }
            return new com.threerings.tudey.shape.Compound(tshapes);
        }

        @Override
        protected void draw (boolean outline)
        {
            for (TransformedShape tshape : shapes) {
                Transform2D transform = tshape.transform;
                transform.update(Transform2D.UNIFORM);
                Vector2f translation = transform.getTranslation();
                float rotation = transform.getRotation();
                float scale = transform.getScale();
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(translation.x, translation.y, 0f);
                    GL11.glRotatef(FloatMath.toDegrees(rotation), 0f, 0f, 1f);
                    GL11.glScalef(scale, scale, scale);
                    tshape.shape.draw(outline);
                } finally {
                    GL11.glPopMatrix();
                }
            }
        }
    }

    /**
     * A non-shape.
     */
    public static class None extends ShapeConfig
    {
        @Override
        protected Shape createShape ()
        {
            return new com.threerings.tudey.shape.None(Vector2f.ZERO);
        }

        @Override
        protected void draw (boolean outline)
        {
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex2f(0f, 0f);
            GL11.glEnd();
        }
    }

    /**
     * A global shape.
     */
    public static class Global extends ShapeConfig
    {
        @Override
        protected Shape createShape ()
        {
            return com.threerings.tudey.shape.Global.getShape();
        }

        @Override
        protected void draw (boolean outline)
        {
            // do nothing
        }
    }

    /**
     * Combines a shape with its transform.
     */
    public static class TransformedShape extends DeepObject
        implements Exportable, Streamable
    {
        /** The shape. */
        @Editable
        public ShapeConfig shape = new Point();

        /** The shape's transform. */
        @Editable(step=0.01)
        public Transform2D transform = new Transform2D();

        /**
         * Returns the transformed shape.
         */
        public Shape getShape ()
        {
            if (_shape == null) {
                _shape = shape.getShape().transform(transform);
            }
            return _shape;
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            shape.invalidate();
            _shape = null;
        }

        /** The transformed shape. */
        @DeepOmit
        protected transient Shape _shape;
    }

    /**
     * Returns a reference to the untransformed shape.
     */
    public Shape getShape ()
    {
        if (_shape == null) {
            _shape = createShape();
        }
        return _shape;
    }

    /**
     * Returns a reference to the untransformed bounds of the shape.
     */
    public Box getBounds ()
    {
        if (_bounds == null) {
            Rect rect = getShape().getBounds();
            Vector2f min = rect.getMinimumExtent(), max = rect.getMaximumExtent();
            _bounds = new Box();
            _bounds.getMinimumExtent().set(min.x, min.y, 0f);
            _bounds.getMaximumExtent().set(max.x, max.y, 0f);
        }
        return _bounds;
    }

    /**
     * Returns the cached display list to draw this shape.
     *
     * @param outline if true, return the outline list; otherwise, the solid list.
     */
    public DisplayList getList (GlContext ctx, boolean outline)
    {
        SoftReference<DisplayList> ref = outline ? _outlineList : _solidList;
        DisplayList list = (ref == null) ? null : ref.get();
        if (list == null) {
            ref = new SoftReference<DisplayList>(list = new DisplayList(ctx.getRenderer()));
            if (outline) {
                _outlineList = ref;
            } else {
                _solidList = ref;
            }
            list.begin();
            draw(outline);
            list.end();
        }
        return list;
    }

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        _shape = null;
        _bounds = null;
        _solidList = _outlineList = null;
    }

    /**
     * Creates the untransformed shape corresponding to this config.
     */
    protected abstract Shape createShape ();

    /**
     * Draws this shape in immediate mode.
     *
     * @param outline if true, draw the outline of the shape; otherwise, draw the solid form.
     */
    protected abstract void draw (boolean outline);

    /** The untransformed shape. */
    @DeepOmit
    protected transient Shape _shape;

    /** The untransformed bounds of the shape. */
    @DeepOmit
    protected transient Box _bounds;

    /** The display lists containing the solid and outline representations. */
    @DeepOmit
    protected transient SoftReference<DisplayList> _solidList, _outlineList;

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
