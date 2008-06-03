//
// $Id$

package com.threerings.opengl.config;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureBorderClamp;
import org.lwjgl.opengl.ARBTextureMirroredRepeat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;

import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Texture;

/**
 * Texture metadata.
 */
public class TextureConfig extends ParameterizedConfig
{
    /** Minification filter constants. */
    public enum MinFilter
    {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST),
        LINEAR_MIPMAP_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL11.GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR);

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
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR);

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
        CLAMP(GL11.GL_CLAMP),
        CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE) {
            public boolean isSupported () {
                return GLContext.getCapabilities().OpenGL12;
            }
        },
        REPEAT(GL11.GL_REPEAT),
        CLAMP_TO_BORDER(ARBTextureBorderClamp.GL_CLAMP_TO_BORDER_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_border_clamp;
            }
        },
        MIRRORED_REPEAT(ARBTextureMirroredRepeat.GL_MIRRORED_REPEAT_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_mirrored_repeat;
            }
        };

        public int getGLConstant ()
        {
            return _glConstant;
        }

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

    /** Compare mode constants. */
    public enum CompareMode
    {
        NONE(GL11.GL_NONE),
        COMPARE_R_TO_TEXTURE(ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_shadow;
            }
        };

        public int getGLConstant ()
        {
            return _glConstant;
        }

        public boolean isSupported ()
        {
            return true;
        }

        CompareMode (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Compare function constants. */
    public enum CompareFunc
    {
        LEQUAL(GL11.GL_LEQUAL),
        GEQUAL(GL11.GL_GEQUAL);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        CompareFunc (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Depth mode constants. */
    public enum DepthMode
    {
        LUMINANCE(GL11.GL_LUMINANCE),
        INTENSITY(GL11.GL_INTENSITY),
        ALPHA(GL11.GL_ALPHA);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        DepthMode (int glConstant)
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

    /** The maximum degree of anisotropy. */
    @Editable(min=1.0, step=0.01)
    public float maxAnisotropy = 1f;

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

    /** The texture compare mode. */
    @Editable
    public CompareMode compareMode = CompareMode.NONE;

    /** The texture compare function. */
    @Editable
    public CompareFunc compareFunc = CompareFunc.LEQUAL;

    /** The depth texture mode. */
    @Editable
    public DepthMode depthMode = DepthMode.LUMINANCE;

    /**
     * Applies this configuration to the specified texture.
     */
    public void apply (Texture texture)
    {
        texture.setFilters(minFilter.getGLConstant(), magFilter.getGLConstant());
        texture.setMaxAnisotropy(maxAnisotropy);
        texture.setWrap(wrapS.getGLConstant(), wrapT.getGLConstant(), wrapR.getGLConstant());
        texture.setBorderColor(borderColor);
        texture.setCompare(compareMode.getGLConstant(), compareFunc.getGLConstant());
        texture.setDepthMode(depthMode.getGLConstant());
    }
}
