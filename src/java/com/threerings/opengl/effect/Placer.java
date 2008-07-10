//
// $Id$

package com.threerings.opengl.effect;

import com.samskivert.util.RandomUtil;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector3f;

import com.threerings.opengl.camera.Camera;

import com.threerings.opengl.effect.ParticleSystem.Layer;

/**
 * Determines the particles' initial positions.
 */
@EditorTypes({
    Placer.Point.class, Placer.Line.class, Placer.Box.class,
    Placer.Ring.class, Placer.Shell.class, Placer.Frustum.class })
public abstract class Placer extends DeepObject
    implements Exportable
{
    /**
     * Places points at the local origin.
     */
    public static class Point extends Placer
    {
        @Override // documentation inherited
        protected Vector3f place (Vector3f position)
        {
            return position.set(0f, 0f, 0f);
        }
    }

    /**
     * Places points along a line segment.
     */
    public static class Line extends Placer
    {
        /** The length of the segment. */
        @Editable(min=0.0, step=0.01)
        public float length = 1f;

        @Override // documentation inherited
        protected Vector3f place (Vector3f position)
        {
            return position.set(FloatMath.random(-0.5f, +0.5f) * length, 0f, 0f);
        }
    }

    /**
     * Places points within a box.
     */
    public static class Box extends Placer
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

        @Override // documentation inherited
        protected Vector3f place (Vector3f position)
        {
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
            float rand = RandomUtil.getFloat(xy + xz + yz);
            if (rand < xy) {
                return position.set(
                    FloatMath.random(-0.5f, +0.5f) * width,
                    FloatMath.random(-0.5f, +0.5f) * length,
                    (RandomUtil.rand.nextBoolean() ? -0.5f : +0.5f) * height);
            } else if (rand < xy + xz) {
                return position.set(
                    FloatMath.random(-0.5f, +0.5f) * width,
                    (RandomUtil.rand.nextBoolean() ? -0.5f : +0.5f) * length,
                    FloatMath.random(-0.5f, +0.5f) * height);
            } else {
                return position.set(
                    (RandomUtil.rand.nextBoolean() ? -0.5f : +0.5f) * width,
                    FloatMath.random(-0.5f, +0.5f) * length,
                    FloatMath.random(-0.5f, +0.5f) * height);
            }
        }
    }

    /**
     * Places points within a ring or disc.
     */
    public static class Ring extends Placer
    {
        /** The inner radius of the ring. */
        @Editable(min=0.0, step=0.01)
        public float innerRadius = 0f;

        /** The outer radius of the ring. */
        @Editable(min=0.0, step=0.01)
        public float outerRadius = 1f;

        @Override // documentation inherited
        protected Vector3f place (Vector3f position)
        {
            // find a radius based on the area distribution
            float radius = FloatMath.sqrt(
                FloatMath.random(innerRadius*innerRadius, outerRadius*outerRadius));
            float angle = RandomUtil.getFloat(FloatMath.TWO_PI);
            return position.set(
                radius * FloatMath.cos(angle),
                radius * FloatMath.sin(angle),
                0f);
        }
    }

    /**
     * Places points within a sphere or spherical shell.
     */
    public static class Shell extends Placer
    {
        /** The inner radius of the shell. */
        @Editable(min=0.0, step=0.01)
        public float innerRadius;

        /** The outer radius of the shell. */
        @Editable(min=0.0, step=0.01)
        public float outerRadius = 1f;

        @Override // documentation inherited
        protected Vector3f place (Vector3f position)
        {
            // find a radius based on the volume distribution
            float radius = FloatMath.pow(
                FloatMath.random(
                    innerRadius*innerRadius*innerRadius,
                    outerRadius*outerRadius*outerRadius),
                1f / 3f);
            // elevation based on the surface area distribution
            float sine = FloatMath.random(-1f, +1f);
            float cose = FloatMath.sqrt(1f - sine*sine);
            float angle = RandomUtil.getFloat(FloatMath.TWO_PI);
            return position.set(
                radius * FloatMath.cos(angle) * cose,
                radius * FloatMath.sin(angle) * cose,
                radius * sine);
        }
    }

    /**
     * Places points within the view frustum.
     */
    public static class Frustum extends Placer
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

        @Override // documentation inherited
        public void place (Layer layer, Particle particle)
        {
            // choose a distance according to the volume distribution
            // or surface area distribution, depending on solidity
            float exp = solid ? 3f : 2f;
            float distance = FloatMath.pow(FloatMath.random(
                FloatMath.pow(nearDistance, exp), FloatMath.pow(farDistance, exp)), 1f / exp);

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
                if (RandomUtil.getFloat(width + height) < width) {
                    position.set(
                        FloatMath.random(left, right),
                        RandomUtil.rand.nextBoolean() ? top : bottom,
                        -distance);
                } else {
                    position.set(
                        RandomUtil.rand.nextBoolean() ? left : right,
                        FloatMath.random(top, bottom),
                        -distance);
                }
            }

            // transform into world space, then into layer space
            layer.pointToLayer(camera.getWorldTransform().transformPointLocal(position), false);
        }
    }

    /**
     * Configures the supplied particle with an initial position.
     */
    public void place (Layer layer, Particle particle)
    {
        layer.pointToLayer(place(particle.getPosition()), true);
    }

    /**
     * Basic placement method for emitter-space placers.
     *
     * @return a reference to the position point, for chaining.
     */
    protected Vector3f place (Vector3f position)
    {
        return position;
    }
}
