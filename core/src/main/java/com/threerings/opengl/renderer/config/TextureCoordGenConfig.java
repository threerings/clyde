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

        @Override
        public boolean usesNormals ()
        {
            return false;
        }
    }

    /**
     * Generates coordinates using a linear function in object (model) space.
     */
    public static class ObjectLinear extends Linear
    {
        @Override
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
        @Override
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
        @Override
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
        @Override
        public boolean isSupported (boolean fallback)
        {
            return GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override
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
        @Override
        public boolean isSupported (boolean fallback)
        {
            return GLContext.getCapabilities().GL_ARB_texture_cube_map;
        }

        @Override
        public int getModeAndPlane (Vector4f plane)
        {
            return ARBTextureCubeMap.GL_REFLECTION_MAP_ARB;
        }
    }

    /**
     * Checks whether the mode is supported.
     */
    public boolean isSupported (boolean fallback)
    {
        return true;
    }

    /**
     * Returns the OpenGL constant for the mode and populates the supplied object with the plane
     * coefficients (if applicable).
     */
    public abstract int getModeAndPlane (Vector4f plane);

    /**
     * Checks whether the texture coordinate generation function uses vertex normals.
     */
    public boolean usesNormals ()
    {
        return true;
    }
}
