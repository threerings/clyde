//
// $Id$

package com.threerings.tudey.config;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The configuration for a shape.
 */
public abstract class ShapeConfig extends DeepObject
    implements Exportable
{
    /**
     * A point.
     */
    public static class Point extends ShapeConfig
    {
    }

    /**
     * A line segment.
     */
    public static class Line extends ShapeConfig
    {
        /** The length of the segment. */
        @Editable(min=0, step=0.01)
        public float length = 1f;
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
    }

    /**
     * A circle.
     */
    public static class Circle extends ShapeConfig
    {
        /** The radius of the circle. */
        @Editable(min=0, step=0.01)
        public float radius = 1f;
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
    }

    /**
     * A polygon.
     */
    public static class Polygon extends ShapeConfig
    {
        /** The vertices of the polygon. */
        @Editable(editor="table")
        public Vertex[] vertices = new Vertex[0];
    }

    /**
     * A single vertex in a polygon.
     */
    public static class Vertex extends DeepObject
        implements Exportable
    {
        /** The vertex coordinates. */
        @Editable(step=0.01, hgroup="c")
        public float x, y;
    }

    /**
     * A compound shape.
     */
    public static class Compound extends ShapeConfig
    {
        /** The component shapes. */
        @Editable
        public ShapeConfig[] shapes = new ShapeConfig[0];
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] {
            Point.class, Line.class, Rectangle.class, Circle.class,
            Capsule.class, Polygon.class, Compound.class };
    }
}
