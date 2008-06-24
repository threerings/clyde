//
// $Id$

package com.threerings.opengl.effect;

import java.io.IOException;

import com.samskivert.util.RandomUtil;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.export.Importer;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector3f;

/**
 * Determines the particles' initial velocities.
 */
@EditorTypes({ Shooter.Cone.class, Shooter.Outward.class })
public abstract class Shooter extends DeepObject
    implements Exportable
{
    /**
     * Shoots particles in a cone pattern.
     */
    public static class Cone extends Shooter
    {
        /** The minimum angle from the direction. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0)
        public float minimumAngle;

        /** The maximum angle from the direction. */
        @Editable(min=0.0, max=180.0, scale=Math.PI/180.0)
        public float maximumAngle = FloatMath.PI / 4f;

        /**
         * Sets the direction vector.
         */
        @Editable(mode="normalized")
        public void setDirection (Vector3f direction)
        {
            _direction = direction;
            updateMatrix();
        }

        /**
         * Retrieves the direction vector.
         */
        @Editable
        public Vector3f getDirection ()
        {
            return _direction;
        }

        /**
         * Reads the fields of the object.
         */
        public void readFields (Importer in)
            throws IOException
        {
            in.defaultReadFields();
            updateMatrix();
        }

        @Override // documentation inherited
        public Vector3f shoot (Particle particle)
        {
            // pick an angle off the vertical based on the surface area distribution
            float cosa = FloatMath.random(
                FloatMath.cos(minimumAngle), FloatMath.cos(maximumAngle));
            float sina = FloatMath.sqrt(1f - cosa*cosa);
            float theta = RandomUtil.getFloat(FloatMath.TWO_PI);

            // set, transform
            return _matrix.transformVectorLocal(particle.getVelocity().set(
                FloatMath.cos(theta) * sina,
                FloatMath.sin(theta) * sina,
                cosa));
        }

        /**
         * Updates the rotation matrix based on the direction vector.
         */
        protected void updateMatrix ()
        {
            _matrix.setToRotation(Vector3f.UNIT_Z, _direction);
        }

        /** The direction. */
        protected Vector3f _direction = new Vector3f(0f, 0f, 1f);

        /** Used to rotate vectors onto the direction. */
        protected transient Matrix4f _matrix = new Matrix4f();
    }

    /**
     * Fires particles away from the origin.
     */
    public static class Outward extends Shooter
    {
        /** The bias in the z direction. */
        @Editable(step=0.01)
        public float upwardBias;

        @Override // documentation inherited
        public Vector3f shoot (Particle particle)
        {
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
    }

    /**
     * Configures the supplied particle with an initial velocity.
     *
     * @return a reference to the particle's velocity, for chaining.
     */
    public abstract Vector3f shoot (Particle particle);
}
