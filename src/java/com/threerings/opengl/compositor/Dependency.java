//
// $Id$

package com.threerings.opengl.compositor;

import com.threerings.math.Plane;
import com.threerings.math.Rect;
import com.threerings.math.Transform3D;

import com.threerings.opengl.renderer.Light;
import com.threerings.opengl.renderer.Texture;

/**
 * Represents a dependency to be resolved in the course of rendering: shadows from a particular
 * light, a reflection off a plane, etc.
 */
public abstract class Dependency
{
    /**
     * The base class of the various planar reflection/refraction dependencies.
     */
    public static abstract class Planar extends Dependency
    {
        /** The eye space plane of reflection or refraction. */
        public Plane plane = new Plane();

        /** The bounds of the affected region in normalized device coordinates. */
        public Rect bounds = new Rect();

        @Override // documentation inherited
        public void merge (Dependency dependency)
        {
            bounds.addLocal(((Planar)dependency).bounds);
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return plane.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Planar)other).plane.equals(plane);
        }
    }

    /**
     * A stencil reflection.
     */
    public static class StencilReflection extends Planar
    {
    }

    /**
     * A stencil refraction.
     */
    public static class StencilRefraction extends Planar
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((StencilRefraction)other).ratio == ratio;
        }
    }

    /**
     * The base class for planar reflection/refraction textures.
     */
    public static abstract class PlanarTexture extends Planar
    {
        /** The texture to which we render. */
        public Texture texture;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((PlanarTexture)other).texture == texture;
        }
    }

    /**
     * A planar reflection texture.
     */
    public static class ReflectionTexture extends PlanarTexture
    {
    }

    /**
     * A planar refraction texture.
     */
    public static class RefractionTexture extends PlanarTexture
    {
        /** The refraction ratio (index of refraction below the surface over index of refraction
         * above the surface). */
        public float ratio = 1f;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((RefractionTexture)other).ratio == ratio;
        }
    }

    /**
     * The base class for shadows from a single light.
     */
    public static abstract class Shadows extends Dependency
    {
        /** The light casting the shadows. */
        public Light light;

        @Override // documentation inherited
        public int hashCode ()
        {
            return light.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return getClass() == other.getClass() && ((Shadows)other).light.equals(light);
        }
    }

    /**
     * Stencil shadow volumes.
     */
    public static class ShadowVolumes extends Shadows
    {
    }

    /**
     * A shadow depth texture.
     */
    public static class ShadowTexture extends Shadows
    {
        /** The shadow texture. */
        public Texture texture;

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return super.equals(other) && ((ShadowTexture)other).texture == texture;
        }
    }

    /**
     * A texture projection.
     */
    public static class TextureProjection extends Dependency
    {
        /** The texture to project. */
        public Texture texture;

        /** The projection frustum parameters. */
        public float left = -1f, right = +1f, bottom = -1f, top = +1f, near = +1f, far = -1f;

        /** Whether or not to use an orthographic projection. */
        public boolean ortho = true;

        /** The eye space frustum transform. */
        public Transform3D transform = new Transform3D();

        @Override // documentation inherited
        public int hashCode ()
        {
            return texture.hashCode() + 31*transform.hashCode();
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            if (!(other instanceof TextureProjection)) {
                return false;
            }
            TextureProjection oproj = (TextureProjection)other;
            return texture == oproj.texture && left == oproj.left && right == oproj.right &&
                bottom == oproj.bottom && top == oproj.top && near == oproj.near &&
                far == oproj.far && transform.equals(oproj.transform);
        }
    }

    /**
     * Merges another dependency (for which {@link #equals} returns true) into this one.
     */
    public void merge (Dependency dependency)
    {
        // nothing by default
    }
}
