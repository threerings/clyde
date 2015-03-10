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

package com.threerings.opengl.effect.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix3f;
import com.threerings.math.Quaternion;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;
import com.threerings.util.NoiseUtil;

import com.threerings.opengl.effect.BaseParticleSystem.Layer;
import com.threerings.opengl.effect.Influence;
import com.threerings.opengl.effect.Particle;

/**
 * Modifies the state of a set of particles.
 */
@EditorTypes({
    InfluenceConfig.Gravity.class, InfluenceConfig.Wind.class,
    InfluenceConfig.LinearDrag.class, InfluenceConfig.QuadraticDrag.class,
    InfluenceConfig.CylindricalVortex.class, InfluenceConfig.ToroidalVortex.class,
    InfluenceConfig.Wander.class, InfluenceConfig.Jitter.class,
    InfluenceConfig.AngularAcceleration.class })
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

        @Override
        public Influence createInfluence (final Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    layer.vectorToLayer(acceleration.mult(elapsed, _delta), rotateWithEmitter);
                }
                public void apply (Particle particle) {
                    particle.getVelocity().addLocal(_delta);
                }
                protected Vector3f _delta = new Vector3f();
            };
        }
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

        @Override
        public Influence createInfluence (final Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    layer.vectorToLayer(direction.mult(strength * elapsed, _delta),
                        rotateWithEmitter);
                }
                public void apply (Particle particle) {
                    particle.getVelocity().addLocal(_delta);
                }
                protected Vector3f _delta = new Vector3f();
            };
        }
    }

    /**
     * An influence representing the resistance to a particle's motion in proportion to its speed.
     */
    public static class LinearDrag extends InfluenceConfig
    {
        /** The amount of drag. */
        @Editable(min=0.0, step=0.01)
        public float amount = 1f;

        @Override
        public Influence createInfluence (Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    _drag = Math.max(0f, 1f - amount*elapsed);
                }
                public void apply (Particle particle) {
                    particle.getVelocity().multLocal(_drag);
                }
                protected float _drag;
            };
        }
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

        @Override
        public Influence createInfluence (Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    _drag = amount * elapsed;
                }
                public void apply (Particle particle) {
                    Vector3f velocity = particle.getVelocity();
                    velocity.multLocal(Math.max(0f, 1f - _drag*velocity.length()));
                }
                protected float _drag;
            };
        }
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

        @Override
        public Influence createInfluence (final Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    // compute the delta amount
                    _delta = strength * elapsed;

                    // transform the origin and axis into layer space
                    layer.pointToLayer(_torigin.set(Vector3f.ZERO), true);
                    layer.vectorToLayer(_taxis.set(axis), rotateWithEmitter);

                    // find divergence rotation
                    _rotation.setToRotation(-divergence, _taxis);
                }
                public void apply (Particle particle) {
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
                protected float _delta;
                protected Vector3f _torigin = new Vector3f();
                protected Vector3f _taxis = new Vector3f();
                protected Vector3f _vector = new Vector3f();
                protected Matrix3f _rotation = new Matrix3f();
            };
        }
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

        @Override
        public Influence createInfluence (final Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    // compute the delta amount
                    _delta = strength * elapsed;

                    // transform the origin and axis into layer space
                    layer.pointToLayer(_torigin.set(Vector3f.ZERO), true);
                    layer.vectorToLayer(_taxis.set(axis), rotateWithEmitter);
                }
                public void apply (Particle particle) {
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
                    _vector.multLocal(radius).addScaledLocal(
                        _taxis, height).subtractLocal(_position);
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
                protected float _delta;
                protected Vector3f _torigin = new Vector3f();
                protected Vector3f _taxis = new Vector3f();
                protected Vector3f _position = new Vector3f();
                protected Vector3f _tangent = new Vector3f();
                protected Vector3f _vector = new Vector3f();
                protected Quaternion _rotation = new Quaternion();
            };
        }
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

        @Override
        public Influence createInfluence (Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    // the unscaled strength was based on an expected frame rate of sixty
                    // frames per second
                    _time += elapsed * frequency;
                    _sstrength = strength * elapsed * 60f;
                }
                public void apply (Particle particle) {
                    // using the system hash gives each particle a consistent unique identity;
                    // adding an offset to the time prevents synchronization of the zero points
                    // (the noise function is always zero at integers)
                    int pid = System.identityHashCode(particle);
                    float time = _time + (pid & 255) / 256f;
                    particle.getVelocity().addLocal(
                        NoiseUtil.getNoise(time, pid) * _sstrength,
                        NoiseUtil.getNoise(time, pid + 1) * _sstrength,
                        NoiseUtil.getNoise(time, pid + 2) * _sstrength);
                }
                protected float _time, _sstrength;
            };
        }
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

        @Override
        public Influence createInfluence (Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    _time += elapsed * frequency;
                    _sstrength = strength * elapsed * 60f;
                }
                public void apply (Particle particle) {
                    // jitter is just like wander, except it directly influences the position
                    int pid = System.identityHashCode(particle);
                    float time = _time + (pid & 255) / 256f;
                    particle.getPosition().addLocal(
                        NoiseUtil.getNoise(time, pid) * _sstrength,
                        NoiseUtil.getNoise(time, pid + 1) * _sstrength,
                        NoiseUtil.getNoise(time, pid + 2) * _sstrength);
                }
                protected float _time, _sstrength;
            };
        }
    }

    /**
     * Applies an angular acceleration.
     */
    public static class AngularAcceleration extends InfluenceConfig
    {
        /** The acceleration vector. */
        @Editable(scale=Math.PI/180.0)
        public Vector3f acceleration = new Vector3f();

        @Override
        public Influence createInfluence (final Layer layer)
        {
            return new Influence() {
                public void tick (float elapsed) {
                    acceleration.mult(elapsed, _delta);
                }
                public void apply (Particle particle) {
                    particle.getAngularVelocity().addLocal(_delta);
                }
                protected Vector3f _delta = new Vector3f();
            };
        }
    }

    /**
     * Creates the influence corresponding to this config for the specified layer.
     */
    public abstract Influence createInfluence (Layer layer);
}
