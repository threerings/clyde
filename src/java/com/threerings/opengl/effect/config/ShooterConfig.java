//
// $Id$

package com.threerings.opengl.effect.config;

import com.samskivert.util.RandomUtil;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.Shooter;

/**
 * Determines the particles' initial velocities.
 */
@EditorTypes({ ShooterConfig.Cone.class, ShooterConfig.Outward.class })
public abstract class ShooterConfig extends DeepObject
    implements Exportable
{
    /**
     * Shoots particles in a cone pattern.
     */
    public static class Cone extends ShooterConfig
    {
        /** The direction vector. */
        @Editable(mode="normalized")
        public Vector3f direction = new Vector3f(0f, 0f, 1f);

        /** The minimum angle from the direction. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0)
        public float minimumAngle;

        /** The maximum angle from the direction. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0)
        public float maximumAngle = FloatMath.PI / 4f;

        @Override // documentation inherited
        public Shooter createShooter ()
        {
            final Matrix4f matrix = new Matrix4f();
            matrix.setToRotation(Vector3f.UNIT_Z, direction);
            return new Shooter() {
                public Vector3f shoot (Particle particle) {
                    // pick an angle off the vertical based on the surface area distribution
                    float cosa = FloatMath.random(
                        FloatMath.cos(minimumAngle), FloatMath.cos(maximumAngle));
                    float sina = FloatMath.sqrt(1f - cosa*cosa);
                    float theta = RandomUtil.getFloat(FloatMath.TWO_PI);

                    // set, transform
                    return matrix.transformVectorLocal(particle.getVelocity().set(
                        FloatMath.cos(theta) * sina,
                        FloatMath.sin(theta) * sina,
                        cosa));
                }
            };
        }
    }

    /**
     * Fires particles away from the origin.
     */
    public static class Outward extends ShooterConfig
    {
        /** The bias in the z direction. */
        @Editable(step=0.01)
        public float upwardBias;

        @Override // documentation inherited
        public Shooter createShooter ()
        {
            return new Shooter() {
                public Vector3f shoot (Particle particle) {
                    Vector3f velocity = particle.getVelocity().set(particle.getPosition());
                    float length = velocity.length();
                    if (length > 0.001f) { // use the vector from origin to particle
                        velocity.multLocal(1f / length);
                    } else { // pick a random direction
                        float cosa = FloatMath.random(-1f, +1f);
                        float sina = FloatMath.sqrt(1f - cosa*cosa);
                        float theta = RandomUtil.getFloat(FloatMath.TWO_PI);
                        velocity.set(
                            FloatMath.cos(theta) * sina,
                            FloatMath.sin(theta) * sina,
                            cosa);
                    }
                    return velocity.addLocal(0f, 0f, upwardBias).normalizeLocal();
                }
            };
        }
    }

    /**
     * Creates the shooter corresponding to this config.
     */
    public abstract Shooter createShooter ();
}
