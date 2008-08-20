//
// $Id$

package com.threerings.opengl.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

/**
 * Modifies the state of a set of particles.
 */
@EditorTypes({
    InfluenceConfig.Gravity.class, InfluenceConfig.Wind.class,
    InfluenceConfig.LinearDrag.class, InfluenceConfig.QuadraticDrag.class,
    InfluenceConfig.CylindricalVortex.class, InfluenceConfig.ToroidalVortex.class,
    InfluenceConfig.Wander.class, InfluenceConfig.Jitter.class })
public abstract class InfluenceConfig extends DeepObject
    implements Exportable
{
    /**
     * A constant acceleration influence.
     */
    public static class Gravity extends InfluenceConfig
    {
        /** The gravity acceleration vector. */
        @Editable(step=0.1)
        public Vector3f acceleration = new Vector3f(0f, 0f, -1f);

        /** Whether or not to rotate the acceleration vector with the emitter. */
        @Editable
        public boolean rotateWithEmitter;
    }

    /**
     * A varying acceleration influence.
     */
    public static class Wind extends InfluenceConfig
    {
        /** The wind direction. */
        @Editable(mode="normalized")
        public Vector3f direction = new Vector3f(1f, 0f, 0f);

        /** The wind strength. */
        @Editable(min=0.0, step=0.01)
        public float strength = 2f;

        /** Whether or not to rotate the wind vector with the emitter. */
        @Editable
        public boolean rotateWithEmitter;
    }

    /**
     * An influence representing the resistance to a particle's motion in proportion to its speed.
     */
    public static class LinearDrag extends InfluenceConfig
    {
        /** The amount of drag. */
        @Editable(min=0.0, step=0.01)
        public float amount = 1f;
    }

    /**
     * An influence representing the resistance to a particle's motion in proportion to the square
     * of its speed.
     */
    public static class QuadraticDrag extends InfluenceConfig
    {
        /** The amount of drag. */
        @Editable(min=0.0, step=0.01)
        public float amount = 1f;
    }

    /**
     * Spins particles around an axis.
     */
    public static class CylindricalVortex extends InfluenceConfig
    {
        /** The vortex axis. */
        @Editable(mode="normalized")
        public Vector3f axis = new Vector3f(0f, 0f, 1f);

        /** The strength of the vortex. */
        @Editable(step=0.01)
        public float strength = 2f;

        /** The divergence angle. */
        @Editable(min=-90, max=+90, scale=Math.PI/180)
        public float divergence;

        /** Whether or not to rotate the axis with the emitter. */
        @Editable
        public boolean rotateWithEmitter;
    }

    /**
     * Spins particles around a ring.
     */
    public static class ToroidalVortex extends InfluenceConfig
    {
        /** The ring axis. */
        @Editable(mode="normalized")
        public Vector3f axis = new Vector3f(0f, 0f, 1f);

        /** The height of the ring. */
        @Editable(step=0.01)
        public float height = 1f;

        /** The radius of the ring. */
        @Editable(min=0.0, step=0.01)
        public float radius = 1f;

        /** The strength of the vortex. */
        @Editable(step=0.01)
        public float strength = 2f;

        /** The divergence angle. */
        @Editable(min=-90, max=+90, scale=Math.PI/180)
        public float divergence;

        /** Whether or not to rotate the ring with the emitter. */
        @Editable
        public boolean rotateWithEmitter;
    }

    /**
     * Makes particles wander around randomly.
     */
    public static class Wander extends InfluenceConfig
    {
        /** The frequency of the effect. */
        @Editable(min=0.0, step=0.01)
        public float frequency = 2f;

        /** The strength of the effect. */
        @Editable(min=0.0, step=0.01)
        public float strength = 0.05f;
    }

    /**
     * Makes particles wander around randomly.
     */
    public static class Jitter extends InfluenceConfig
    {
        /** The frequency of the effect. */
        @Editable(min=0.0, step=0.01)
        public float frequency = 5f;

        /** The strength of the effect. */
        @Editable(min=0.0, step=0.01)
        public float strength = 0.02f;
    }
}
