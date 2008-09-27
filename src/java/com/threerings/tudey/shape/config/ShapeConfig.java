//
// $Id$

package com.threerings.tudey.shape.config;

import java.lang.ref.SoftReference;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.renderer.DisplayList;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.space.Intersector;

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
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            bounds.set(Rect.ZERO);
        }

        @Override // documentation inherited
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
    public static class Line extends ShapeConfig
    {
        /** The length of the segment. */
        @Editable(min=0, step=0.01)
        public float length = 1f;

        @Override // documentation inherited
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            float hlength = length * 0.5f;
            bounds.getMinimumExtent().set(-hlength, 0f);
            bounds.getMaximumExtent().set(+hlength, 0f);
        }

        @Override // documentation inherited
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

        /** The length of the rectangle. */
        @Editable(min=0, step=0.01, hgroup="r")
        public float length = 1f;

        @Override // documentation inherited
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            float hwidth = width * 0.5f, hlength = length * 0.5f;
            bounds.getMinimumExtent().set(-hwidth, -hlength);
            bounds.getMaximumExtent().set(+hwidth, +hlength);
        }

        @Override // documentation inherited
        protected void draw (boolean outline)
        {
            float hwidth = width * 0.5f, hlength = length * 0.5f;
            GL11.glBegin(outline ? GL11.GL_LINE_LOOP : GL11.GL_QUADS);
            GL11.glVertex2f(-hwidth, -hlength);
            GL11.glVertex2f(+hwidth, -hlength);
            GL11.glVertex2f(+hwidth, +hlength);
            GL11.glVertex2f(-hwidth, +hlength);
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
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            bounds.getMinimumExtent().set(-radius, -radius);
            bounds.getMaximumExtent().set(+radius, +radius);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            float hlength = length * 0.5f;
            bounds.getMinimumExtent().set(-hlength - radius, -radius);
            bounds.getMaximumExtent().set(+hlength + radius, +radius);
        }

        @Override // documentation inherited
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

        @Override // documentation inherited
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            bounds.setToEmpty();
            for (Vertex vertex : vertices) {
                bounds.addLocal(vertex.createVector());
            }
        }

        @Override // documentation inherited
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
        implements Exportable
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

        @Override // documentation inherited
        public boolean getIntersection (Ray2D ray, Vector2f result)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Point point)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Line line)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Quad quad)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Circle circle)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Capsule capsule)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Polygon polygon)
        {
            return false;
        }

        @Override // documentation inherited
        public boolean intersects (Intersector.Compound compound)
        {
            return false;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            super.invalidate();
            for (TransformedShape tshape : shapes) {
                tshape.invalidate();
            }
        }

        @Override // documentation inherited
        protected void computeBounds (Rect bounds)
        {
            bounds.setToEmpty();
            for (TransformedShape tshape : shapes) {
                bounds.addLocal(tshape.getBounds());
            }
        }

        @Override // documentation inherited
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

        /**
         * Returns the bounds of the transformed shape.
         */
        public Rect getBounds ()
        {
            if (_bounds == null) {
                _bounds = shape.getBounds().transform(transform);
            }
            return _bounds;
        }

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            _bounds = null;
        }

        /** The bounds of the transformed shape. */
        @DeepOmit
        protected transient Rect _bounds;
    }

    /**
     * Returns a reference to the bounds of the shape.
     */
    public Rect getBounds ()
    {
        if (_bounds == null) {
            computeBounds(_bounds = new Rect());
        }
        return _bounds;
    }

    /**
     * Finds the intersection of a ray with this object and places it in the supplied vector
     * (if it exists).
     *
     * @return true if the ray intersected the object (in which case the result will contain the
     * point of intersection), false otherwise.
     */
    public abstract boolean getIntersection (Ray2D ray, Vector2f result);

    /**
     * Determines whether this shape intersects the supplied point.
     */
    public abstract boolean intersects (Intersector.Point point);

    /**
     * Determines whether this shape intersects the supplied line.
     */
    public abstract boolean intersects (Intersector.Line line);

    /**
     * Determines whether this shape intersects the supplied quad.
     */
    public abstract boolean intersects (Intersector.Quad quad);

    /**
     * Determines whether this shape intersects the supplied circle.
     */
    public abstract boolean intersects (Intersector.Circle circle);

    /**
     * Determines whether this shape intersects the supplied capsule.
     */
    public abstract boolean intersects (Intersector.Capsule capsule);

    /**
     * Determines whether this shape intersects the supplied polygon.
     */
    public abstract boolean intersects (Intersector.Polygon polygon);

    /**
     * Determines whether this shape intersects the supplied compound.
     */
    public abstract boolean intersects (Intersector.Compound compound);

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
        _bounds = null;
        _solidList = _outlineList = null;
    }

    /**
     * Computes the bounds of the shape and stores them in the provided object.
     */
    protected abstract void computeBounds (Rect bounds);

    /**
     * Draws this shape in immediate mode.
     *
     * @param outline if true, draw the outline of the shape; otherwise, draw the solid form.
     */
    protected abstract void draw (boolean outline);

    /** The bounds of the shape. */
    @DeepOmit
    protected transient Rect _bounds;

    /** The display lists containing the solid and outline representations. */
    @DeepOmit
    protected transient SoftReference<DisplayList> _solidList, _outlineList;

    /** The number of segments to use when we render circles. */
    protected static final int CIRCLE_SEGMENTS = 16;

    /** The increment angle between circle segments. */
    protected static final float CIRCLE_INCREMENT = FloatMath.TWO_PI / CIRCLE_SEGMENTS;
}
