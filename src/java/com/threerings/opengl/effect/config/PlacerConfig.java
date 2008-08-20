//
// $Id$

package com.threerings.opengl.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Determines the particles' initial positions.
 */
@EditorTypes({
    PlacerConfig.Point.class, PlacerConfig.Line.class, PlacerConfig.Box.class,
    PlacerConfig.Ring.class, PlacerConfig.Shell.class, PlacerConfig.Frustum.class })
public abstract class PlacerConfig extends DeepObject
    implements Exportable
{
    /**
     * Places points at the local origin.
     */
    public static class Point extends PlacerConfig
    {
    }

    /**
     * Places points along a line segment.
     */
    public static class Line extends PlacerConfig
    {
        /** The length of the segment. */
        @Editable(min=0.0, step=0.01)
        public float length = 1f;
    }

    /**
     * Places points within a box.
     */
    public static class Box extends PlacerConfig
    {
        /** The size of the box on the x axis. */
        @Editable(min=0.0, step=0.01)
        public float width = 1f;

        /** The size of the box on the y axis. */
        @Editable(min=0.0, step=0.01)
        public float length = 1f;

        /** The size of the box on the z axis. */
        @Editable(min=0.0, step=0.01)
        public float height = 1f;

        /** Whether to include the interior of the box (as opposed to just the faces). */
        @Editable
        public boolean solid = true;
    }

    /**
     * Places points within a ring or disc.
     */
    public static class Ring extends PlacerConfig
    {
        /** The inner radius of the ring. */
        @Editable(min=0.0, step=0.01)
        public float innerRadius = 0f;

        /** The outer radius of the ring. */
        @Editable(min=0.0, step=0.01)
        public float outerRadius = 1f;
    }

    /**
     * Places points within a sphere or spherical shell.
     */
    public static class Shell extends PlacerConfig
    {
        /** The inner radius of the shell. */
        @Editable(min=0.0, step=0.01)
        public float innerRadius;

        /** The outer radius of the shell. */
        @Editable(min=0.0, step=0.01)
        public float outerRadius = 1f;
    }

    /**
     * Places points within the view frustum.
     */
    public static class Frustum extends PlacerConfig
    {
        /** The distance to the near plane. */
        @Editable(min=0.0, step=0.1)
        public float nearDistance = 10f;

        /** The distance to the far plane. */
        @Editable(min=0.0, step=0.1)
        public float farDistance = 20f;

        /** Whether to include the interior of the frustum (as opposed to just the edges). */
        @Editable
        public boolean solid = true;
    }
}
