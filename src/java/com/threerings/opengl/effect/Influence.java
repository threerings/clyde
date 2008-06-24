//
// $Id$

package com.threerings.opengl.effect;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;
import com.threerings.util.NoiseUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Matrix3f;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;

import com.threerings.opengl.effect.ParticleSystem.Layer;

/**
 * Modifies the state of a set of particles.
 */
@EditorTypes({
    Influence.Gravity.class, Influence.Wind.class,
    Influence.LinearDrag.class, Influence.QuadraticDrag.class,
    Influence.CylindricalVortex.class, Influence.ToroidalVortex.class,
    Influence.Wander.class, Influence.Jitter.class })
public abstract class Influence extends DeepObject
    implements Exportable
{
    /**
     * A constant acceleration influence.
     */
    public static class Gravity extends Influence
    {
        /** The gravity acceleration vector. */
        @Editable(step=0.1)
        public Vector3f acceleration = new Vector3f(0f, 0f, -1f);

        /** Whether or not to rotate the acceleration vector with the emitter. */
        @Editable
        public boolean rotateWithEmitter;

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            layer.vectorToLayer(acceleration.mult(elapsed, _delta), rotateWithEmitter);
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            particle.getVelocity().addLocal(_delta);
        }

        /** Holds the change in velocity over time. */
        protected transient Vector3f _delta = new Vector3f();
    }

    /**
     * A varying acceleration influence.
     */
    public static class Wind extends Influence
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

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            layer.vectorToLayer(direction.mult(strength * elapsed, _delta), rotateWithEmitter);
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            particle.getVelocity().addLocal(_delta);
        }

        /** Holds the scaled strength in proportion to the area. */
        protected transient Vector3f _delta = new Vector3f();
    }

    /**
     * An influence representing the resistance to a particle's motion in proportion to its speed.
     */
    public static class LinearDrag extends Influence
    {
        /** The amount of drag. */
        @Editable(min=0.0, step=0.01)
        public float amount = 1f;

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            _drag = Math.max(0f, 1f - amount*elapsed);
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            particle.getVelocity().multLocal(_drag);
        }

        /** Drag multiplier. */
        protected transient float _drag;
    }

    /**
     * An influence representing the resistance to a particle's motion in proportion to the square
     * of its speed.
     */
    public static class QuadraticDrag extends Influence
    {
        /** The amount of drag. */
        @Editable(min=0.0, step=0.01)
        public float amount = 1f;

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            _drag = amount * elapsed;
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            Vector3f velocity = particle.getVelocity();
            velocity.multLocal(Math.max(0f, 1f - _drag*velocity.length()));
        }

        /** Drag coefficient. */
        protected transient float _drag;
    }

    /**
     * Spins particles around an axis.
     */
    public static class CylindricalVortex extends Influence
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

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            // compute the delta amount
            _delta = strength * elapsed;

            // transform the origin and axis into layer space
            layer.pointToLayer(_torigin.set(Vector3f.ZERO), true);
            layer.vectorToLayer(_taxis.set(axis), rotateWithEmitter);

            // find divergence rotation
            _rotation.setToRotation(-divergence, _taxis);
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            // cross product of vortex axis and relative position is direction
            _taxis.cross(particle.getPosition().subtract(_torigin, _vector), _vector);
            float length = _vector.length();
            if (length < FloatMath.EPSILON) {
                return; // particle is on the axis
            }
            // normalize direction, scale by delta, rotate, add to velocity
            particle.getVelocity().addLocal(
                _rotation.transformLocal(_vector.multLocal(_delta / length)));
        }

        /** Acceleration times elapsed time. */
        protected transient float _delta;

        /** The origin in layer space. */
        protected transient Vector3f _torigin = new Vector3f();

        /** The axis in layer space. */
        protected transient Vector3f _taxis = new Vector3f();

        /** Temporary vector. */
        protected transient Vector3f _vector = new Vector3f();

        /** Rotation matrix. */
        protected transient Matrix3f _rotation = new Matrix3f();
    }

    /**
     * Spins particles around a ring.
     */
    public static class ToroidalVortex extends Influence
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

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            // compute the delta amount
            _delta = strength * elapsed;

            // transform the origin and axis into layer space
            layer.pointToLayer(_torigin.set(Vector3f.ZERO), true);
            layer.vectorToLayer(_taxis.set(axis), rotateWithEmitter);
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            // cross product of ring axis and particle position is tangent
            particle.getPosition().subtract(_torigin, _position);
            _taxis.cross(_position, _tangent);
            float length = _tangent.length();
            if (length < FloatMath.EPSILON) {
                return; // particle is on the axis
            }
            _tangent.multLocal(1f / length);

            // cross product of tangent and axis is direction from axis to position
            _tangent.cross(_taxis, _vector);

            // find vector from closest point on ring to position
            _vector.multLocal(radius).addScaledLocal(_taxis, height).subtractLocal(_position);
            length = _vector.length();
            if (length < FloatMath.EPSILON) {
                return; // particle is on the ring
            }
            _vector.multLocal(1f / length);

            // compute the rotation angle
            _rotation.fromAngleAxis(-divergence, _tangent);

            // cross product of vector and tangent is direction
            particle.getVelocity().addLocal(
                _rotation.transformLocal(_vector.crossLocal(_tangent).multLocal(_delta)));
        }

        /** Acceleration times elapsed time. */
        protected transient float _delta;

        /** The origin in layer space. */
        protected transient Vector3f _torigin = new Vector3f();

        /** The axis in layer space. */
        protected transient Vector3f _taxis = new Vector3f();

        /** Holds the relative position. */
        protected transient Vector3f _position = new Vector3f();

        /** Holds the tangent vector. */
        protected transient Vector3f _tangent = new Vector3f();

        /** Temporary vector. */
        protected transient Vector3f _vector = new Vector3f();

        /** Rotation quaternion. */
        protected transient Quaternion _rotation = new Quaternion();
    }

    /**
     * Makes particles wander around randomly.
     */
    public static class Wander extends Influence
    {
        /** The frequency of the effect. */
        @Editable(min=0.0, step=0.01)
        public float frequency = 2f;

        /** The strength of the effect. */
        @Editable(min=0.0, step=0.01)
        public float strength = 0.05f;

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            _time += elapsed * frequency;
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            // using the system hash gives each particle a consistent unique identity;
            // adding an offset to the time prevents synchronization of the zero points
            // (the noise function is always zero at integers)
            int pid = System.identityHashCode(particle);
            float time = _time + (pid & 255) / 256f;
            particle.getVelocity().addLocal(
                NoiseUtil.getNoise(time, pid) * strength,
                NoiseUtil.getNoise(time, pid + 1) * strength,
                NoiseUtil.getNoise(time, pid + 2) * strength);
        }

        /** The time parameter. */
        protected transient float _time;
    }

    /**
     * Makes particles wander around randomly.
     */
    public static class Jitter extends Influence
    {
        /** The frequency of the effect. */
        @Editable(min=0.0, step=0.01)
        public float frequency = 5f;

        /** The strength of the effect. */
        @Editable(min=0.0, step=0.01)
        public float strength = 0.02f;

        @Override // documentation inherited
        public void tick (Layer layer, float elapsed)
        {
            _time += elapsed * frequency;
        }

        @Override // documentation inherited
        public void apply (Particle particle)
        {
            // jitter is just like wander, except it directly influences the position
            int pid = System.identityHashCode(particle);
            float time = _time + (pid & 255) / 256f;
            particle.getPosition().addLocal(
                NoiseUtil.getNoise(time, pid) * strength,
                NoiseUtil.getNoise(time, pid + 1) * strength,
                NoiseUtil.getNoise(time, pid + 2) * strength);
        }

        /** The time parameter. */
        protected transient float _time;
    }

    /**
     * Updates the influence for the current frame.
     */
    public abstract void tick (Layer layer, float elapsed);

    /**
     * Applies this influence to the specified particle.
     */
    public abstract void apply (Particle particle);
}
