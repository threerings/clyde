//
// $Id$

package com.threerings.tudey.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;

/**
 * The configuration for a shape.
 */
@EditorTypes({
    ShapeConfig.Point.class, ShapeConfig.Line.class, ShapeConfig.Rectangle.class,
    ShapeConfig.Circle.class, ShapeConfig.Capsule.class, ShapeConfig.Polygon.class,
    ShapeConfig.Compound.class })
public abstract class ShapeConfig extends DeepObject
    implements Exportable
{
    /**
     * A point.
     */
    public static class Point extends ShapeConfig
    {
        @Override // documentation inherited
        public void drawOutline ()
        {
            GL11.glBegin(GL11.GL_POINTS);
            GL11.glVertex2f(0f, 0f);
            GL11.glEnd();
        }
    }

    /**
     * A line segment.
     */
    public static class Line extends ShapeConfig
    {
        /** The length of the segment. */
        @Editable(min=0, step=0.01)
        public float length = 1f;

        @Override // documentation inherited
        public void drawOutline ()
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

        /** The length of the rectangle. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float length = 1f;

        @Override // documentation inherited
        public void drawOutline ()
        {
            float hwidth = width * 0.5f, hlength = length * 0.5f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(-hwidth, -hlength);
            GL11.glVertex2f(-hwidth, +hlength);
            GL11.glVertex2f(+hwidth, +hlength);
            GL11.glVertex2f(+hwidth, -hlength);
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

        @Override // documentation inherited
        public void drawOutline ()
        {
            GL11.glBegin(GL11.GL_LINE_LOOP);
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

        @Override // documentation inherited
        public void drawOutline ()
        {
            float hlength = length * 0.5f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
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

        @Override // documentation inherited
        public void drawOutline ()
        {
            GL11.glBegin(GL11.GL_LINE_LOOP);
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
        implements Exportable
    {
        /** The vertex coordinates. */
        @Editable(column=true)
        public float x, y;
    }

    /**
     * A compound shape.
     */
    public static class Compound extends ShapeConfig
    {
        /** The component shapes. */
        @Editable
        public TransformedShape[] shapes = new TransformedShape[0];

        @Override // documentation inherited
        public void drawOutline ()
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
                    tshape.shape.drawOutline();
                } finally {
                    GL11.glPopMatrix();
                }
            }
        }
    }

    /**
     * Combines a shape with its transform.
     */
    public static class TransformedShape extends DeepObject
        implements Exportable
    {
        /** The shape. */
        @Editable
        public ShapeConfig shape = new Point();

        /** The shape's transform. */
        @Editable(step=0.01)
        public Transform2D transform = new Transform2D();
    }

    /**
     * Draws the outline of this shape in immediate mode.
     */
    public abstract void drawOutline ();

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
