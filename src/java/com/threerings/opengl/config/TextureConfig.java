//
// $Id$

package com.threerings.opengl.config;

import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.config.ManagedConfig;
import com.threerings.editor.Editable;

import com.threerings.opengl.renderer.Color4f;

/**
 * Texture metadata.
 */
public class TextureConfig extends ManagedConfig
{
    /** Minification filter constants. */
    public enum MinFilter
    {
        /** Chooses the nearest texel to the coordinates. */
        NEAREST(GL11.GL_NEAREST),

        /** Linearly interpolates between the texels. */
        LINEAR(GL11.GL_LINEAR),

        /** Chooses the nearest texel on the nearest mipmap. */
        NEAREST_MIPMAP_NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST),

        /** Linearly interpolates between the texels on the nearest mipmap. */
        LINEAR_MIPMAP_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST),

        /** Chooses the nearest texel on each mipmap, then linearly interpolates between them. */
        NEAREST_MIPMAP_LINEAR(GL11.GL_NEAREST_MIPMAP_LINEAR),

        /** Linearly interpolates between the mipmaps, then between the texels. */
        LINEAR_MIPMAP_LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR);

        /**
         * Returns the corresponding OpenGL constant.
         */
        public int getGLConstant ()
        {
            return _glConstant;
        }

        MinFilter (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Magnification filter constants. */
    public enum MagFilter
    {
        /** Chooses the nearest texel to the coordinates. */
        NEAREST(GL11.GL_NEAREST),

        /** Linearly interpolates between the texels. */
        LINEAR(GL11.GL_LINEAR);

        /**
         * Returns the corresponding OpenGL constant.
         */
        public int getGLConstant ()
        {
            return _glConstant;
        }

        MagFilter (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Wrap constants. */
    public enum Wrap
    {
        /** Clamps the texture coordinates. */
        CLAMP(GL11.GL_CLAMP),

        /** Clamps to the edge. */
        CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE) {
            public boolean isSupported () {
                return GLContext.getCapabilities().OpenGL12;
            }
        },

        /** Repeats past the edge. */
        REPEAT(GL11.GL_REPEAT),

        /** Clamps to the border. */
        CLAMP_TO_BORDER(ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_border_clamp;
            }
        },

        /** Repeats past the edge, flipping each time. */
        MIRRORED_REPEAT(ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat;
            }
        };

        /**
         * Returns the corresponding OpenGL constant.
         */
        public int getGLConstant ()
        {
            return _glConstant;
        }

        /**
         * Checks whether the option is available on the current platform.
         */
        public boolean isSupported ()
        {
            return true;
        }

        Wrap (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** The minification filter. */
    @Editable
    public MinFilter minFilter = MinFilter.NEAREST_MIPMAP_LINEAR;

    /** The magnification filter. */
    @Editable
    public MagFilter magFilter = MagFilter.LINEAR;

    /** The s wrap mode. */
    @Editable
    public Wrap wrapS = Wrap.REPEAT;

    /** The t wrap mode. */
    @Editable
    public Wrap wrapT = Wrap.REPEAT;

    /** The r wrap mode. */
    @Editable
    public Wrap wrapR = Wrap.REPEAT;

    /** The border color. */
    @Editable(mode="alpha")
    public Color4f borderColor = new Color4f(0f, 0f, 0f, 0f);
}
