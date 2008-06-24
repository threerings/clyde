//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.ARBTextureCubeMap;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.math.Vector4f;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * Contains a texture coordinate generation function.
 */
@EditorTypes({
    TextureCoordGenConfig.ObjectLinear.class, TextureCoordGenConfig.EyeLinear.class,
    TextureCoordGenConfig.SphereMap.class, TextureCoordGenConfig.NormalMap.class,
    TextureCoordGenConfig.ReflectionMap.class })
public abstract class TextureCoordGenConfig extends DeepObject
    implements Exportable
{
    /**
     * Superclass of the linear texture coordinate generation functions.
     */
    public static abstract class Linear extends TextureCoordGenConfig
    {
        /** The x plane coefficient. */
        @Editable(step=0.01, hgroup="p")
        public float x;

        /** The y plane coefficient. */
        @Editable(step=0.01, hgroup="p")
        public float y;

        /** The z plane coefficient. */
        @Editable(step=0.01, hgroup="p")
        public float z;

        /** The w plane coefficient. */
        @Editable(step=0.01, hgroup="p")
        public float w;

        public Linear (Linear other)
        {
            x = other.x;
            y = other.y;
            z = other.z;
            w = other.w;
        }

        public Linear ()
        {
        }
    }

    /**
     * Generates coordinates using a linear function in object (model) space.
     */
    public static class ObjectLinear extends Linear
    {
        public ObjectLinear (Linear other)
        {
            super(other);
        }

        public ObjectLinear ()
        {
        }

        @Override // documentation inherited
        public int getModeAndPlane (Vector4f plane)
        {
            plane.set(x, y, z, w);
            return GL11.GL_OBJECT_LINEAR;
        }
    }

    /**
     * Generates coordinates using a linear function in eye space.
     */
    public static class EyeLinear extends Linear
    {
        public EyeLinear (Linear other)
        {
            super(other);
        }

        public EyeLinear ()
        {
        }

        @Override // documentation inherited
        public int getModeAndPlane (Vector4f plane)
        {
            plane.set(x, y, z, w);
            return GL11.GL_EYE_LINEAR;
        }
    }

    /**
     * Generates coordinates using a sphere map function.
     */
    public static class SphereMap extends TextureCoordGenConfig
    {
        @Override // documentation inherited
        public int getModeAndPlane (Vector4f plane)
        {
            return GL11.GL_SPHERE_MAP;
        }
    }

    /**
     * Generates coordinates using a normal map function.
     */
    public static class NormalMap extends TextureCoordGenConfig
    {
        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override // documentation inherited
        public int getModeAndPlane (Vector4f plane)
        {
            return ARBTextureCubeMap.GL_NORMAL_MAP_ARB;
        }
    }

    /**
     * Generates coordinates using a reflection map function.
     */
    public static class ReflectionMap extends TextureCoordGenConfig
    {
        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override // documentation inherited
        public int getModeAndPlane (Vector4f plane)
        {
            return ARBTextureCubeMap.GL_REFLECTION_MAP_ARB;
        }
    }

    /**
     * Checks whether the mode is supported.
     */
    public boolean isSupported ()
    {
        return true;
    }

    /**
     * Returns the OpenGL constant for the mode and populates the supplied object with the plane
     * coefficients (if applicable).
     */
    public abstract int getModeAndPlane (Vector4f plane);
}
