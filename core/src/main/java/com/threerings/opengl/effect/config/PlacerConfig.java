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
import com.threerings.math.Vector3f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.camera.Camera;
import com.threerings.opengl.effect.BaseParticleSystem.Layer;
import com.threerings.opengl.effect.Particle;
import com.threerings.opengl.effect.Placer;

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
        @Override
        public Placer createPlacer (Layer layer)
        {
            return new SimplePlacer(layer) {
                protected Vector3f place (Vector3f position) {
                    return position.set(0f, 0f, 0f);
                }
            };
        }
    }

    /**
     * Places points along a line segment.
     */
    public static class Line extends PlacerConfig
    {
        /** The length of the segment. */
        @Editable(min=0.0, step=0.01)
        public float length = 1f;

        @Override
        public Placer createPlacer (Layer layer)
        {
            return new SimplePlacer(layer) {
                protected Vector3f place (Vector3f position) {
                    return position.set(FloatMath.random(-0.5f, +0.5f) * length, 0f, 0f);
                }
            };
        }
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

        @Override
        public Placer createPlacer (Layer layer)
        {
            return new SimplePlacer(layer) {
                protected Vector3f place (Vector3f position) {
                    if (solid) {
                        return position.set(
                            FloatMath.random(-0.5f, +0.5f) * width,
                            FloatMath.random(-0.5f, +0.5f) * length,
                            FloatMath.random(-0.5f, +0.5f) * height);
                    }
                    // choose a face pair according to their dimensions
                    float xy = width * length;
                    float xz = width * height;
                    float yz = length * height;
                    Randoms r = Randoms.threadLocal();
                    float rand = r.getFloat(xy + xz + yz);
                    if (rand < xy) {
                        return position.set(
                            r.getInRange(-0.5f, +0.5f) * width,
                            r.getInRange(-0.5f, +0.5f) * length,
                            (r.getBoolean() ? -0.5f : +0.5f) * height);
                    } else if (rand < xy + xz) {
                        return position.set(
                            r.getInRange(-0.5f, +0.5f) * width,
                            (r.getBoolean() ? -0.5f : +0.5f) * length,
                            r.getInRange(-0.5f, +0.5f) * height);
                    } else {
                        return position.set(
                            (r.getBoolean() ? -0.5f : +0.5f) * width,
                            r.getInRange(-0.5f, +0.5f) * length,
                            r.getInRange(-0.5f, +0.5f) * height);
                    }
                }
            };
        }
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

        @Override
        public Placer createPlacer (Layer layer)
        {
            return new SimplePlacer(layer) {
                protected Vector3f place (Vector3f position) {
                    // find a radius based on the area distribution
                    float radius = FloatMath.sqrt(
                        FloatMath.random(innerRadius*innerRadius, outerRadius*outerRadius));
                    float angle = Randoms.threadLocal().getFloat(FloatMath.TWO_PI);
                    return position.set(
                        radius * FloatMath.cos(angle),
                        radius * FloatMath.sin(angle),
                        0f);
                }
            };
        }
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

        @Override
        public Placer createPlacer (Layer layer)
        {
            return new SimplePlacer(layer) {
                protected Vector3f place (Vector3f position) {
                    // find a radius based on the volume distribution
                    float radius = FloatMath.pow(
                        FloatMath.random(
                            innerRadius*innerRadius*innerRadius,
                            outerRadius*outerRadius*outerRadius),
                        1f / 3f);
                    // elevation based on the surface area distribution
                    float sine = FloatMath.random(-1f, +1f);
                    float cose = FloatMath.sqrt(1f - sine*sine);
                    float angle = Randoms.threadLocal().getFloat(FloatMath.TWO_PI);
                    return position.set(
                        radius * FloatMath.cos(angle) * cose,
                        radius * FloatMath.sin(angle) * cose,
                        radius * sine);
                }
            };
        }
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

        @Override
        public Placer createPlacer (final Layer layer)
        {
            return new Placer() {
                public void place (Particle particle) {
                    // choose a distance according to the volume distribution
                    // or surface area distribution, depending on solidity
                    float exp = solid ? 3f : 2f;
                    float distance = FloatMath.pow(FloatMath.random(
                        FloatMath.pow(nearDistance, exp), FloatMath.pow(farDistance, exp)),
                            1f / exp);

                    // find the location of the edges at the distance
                    Camera camera = layer.getCamera();
                    float scale = distance / camera.getNear();
                    float left = camera.getLeft() * scale;
                    float right = camera.getRight() * scale;
                    float bottom = camera.getBottom() * scale;
                    float top = camera.getTop() * scale;

                    // if it's solid, choose a random point in the rect; otherwise, choose an edge
                    // pair according to their lengths
                    Vector3f position = particle.getPosition();
                    if (solid) {
                        position.set(
                            FloatMath.random(left, right),
                            FloatMath.random(bottom, top),
                            -distance);
                    } else {
                        float width = right - left;
                        float height = top - bottom;
                        Randoms r = Randoms.threadLocal();
                        if (r.getFloat(width + height) < width) {
                            position.set(
                                r.getInRange(left, right),
                                r.getBoolean() ? top : bottom,
                                -distance);
                        } else {
                            position.set(
                                r.getBoolean() ? left : right,
                                r.getInRange(top, bottom),
                                -distance);
                        }
                    }

                    // transform into world space, then into layer space
                    layer.pointToLayer(camera.getWorldTransform().transformPointLocal(position),
                        false);
                }
            };
        }
    }

    /**
     * Creates the placer corresponding to this config.
     */
    public abstract Placer createPlacer (Layer layer);

    /**
     * Base class for simple emitter space placers.
     */
    protected static abstract class SimplePlacer
        implements Placer
    {
        public SimplePlacer (Layer layer)
        {
            _layer = layer;
        }

        // documentation inherited from interface Placer
        public void place (Particle particle)
        {
            _layer.pointToLayer(place(particle.getPosition()), true);
        }

        /**
         * Sets the position in emitter space.
         *
         * @return a reference to the position point, for chaining.
         */
        protected abstract Vector3f place (Vector3f position);

        /** The owning layer. */
        protected Layer _layer;
    }
}
