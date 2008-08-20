//
// $Id$

package com.threerings.opengl.effect.config;

import java.io.IOException;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.export.Importer;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

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
    public static class Outward extends ShooterConfig
    {
        /** The bias in the z direction. */
        @Editable(step=0.01)
        public float upwardBias;
    }
}
