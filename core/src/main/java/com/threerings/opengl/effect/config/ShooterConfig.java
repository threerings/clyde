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

import com.samskivert.util.Randoms;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.math.FloatMath;
import com.threerings.math.Matrix4f;
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.effect.BaseParticleSystem.Layer;
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

        @Override
        public Shooter createShooter (Layer layer)
        {
            final Matrix4f matrix = new Matrix4f();
            matrix.setToRotation(Vector3f.UNIT_Z, direction);
            return new Shooter() {
                public Vector3f shoot (Particle particle) {
                    // pick an angle off the vertical based on the surface area distribution
                    float cosa = FloatMath.random(
                        FloatMath.cos(minimumAngle), FloatMath.cos(maximumAngle));
                    float sina = FloatMath.sqrt(1f - cosa*cosa);
                    float theta = Randoms.threadLocal().getFloat(FloatMath.TWO_PI);

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

        @Override
        public Shooter createShooter (final Layer layer)
        {
            return new Shooter() {
                public Vector3f shoot (Particle particle) {
                    Vector3f velocity = particle.getVelocity().set(Vector3f.ZERO);
                    layer.pointToLayer(velocity, true);
                    particle.getPosition().subtract(velocity, velocity);
                    float length = velocity.length();
                    if (length > 0.001f) { // use the vector from origin to particle
                        velocity.multLocal(1f / length);
                    } else { // pick a random direction
                        float cosa = FloatMath.random(-1f, +1f);
                        float sina = FloatMath.sqrt(1f - cosa*cosa);
                        float theta = Randoms.threadLocal().getFloat(FloatMath.TWO_PI);
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
    public abstract Shooter createShooter (Layer layer);
}
