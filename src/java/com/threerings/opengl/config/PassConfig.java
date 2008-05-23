//
// $Id$

package com.threerings.opengl.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;

import com.threerings.opengl.renderer.Color4f;

/**
 * A single pass
 */
public class PassConfig
    implements Exportable
{
    /** Alpha test functions. */
    public enum AlphaTestFunc
    {
        NEVER(GL11.GL_NEVER),
        LESS(GL11.GL_LESS),
        EQUAL(GL11.GL_EQUAL),
        LEQUAL(GL11.GL_LEQUAL),
        GREATER(GL11.GL_GREATER),
        NOTEQUAL(GL11.GL_NOTEQUAL),
        GEQUAL(GL11.GL_GEQUAL),
        ALWAYS(GL11.GL_ALWAYS);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        AlphaTestFunc (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Source blend factor constants. */
    public enum SourceBlendFactor
    {
        ZERO(GL11.GL_ZERO),
        ONE(GL11.GL_ONE),
        DST_COLOR(GL11.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GL11.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GL11.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GL11.GL_ONE_MINUS_DST_ALPHA),
        SRC_ALPHA_SATURATE(GL11.GL_SRC_ALPHA_SATURATE);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        SourceBlendFactor (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Dest blend factor constants. */
    public enum DestBlendFactor
    {
        ZERO(GL11.GL_ZERO),
        ONE(GL11.GL_ONE),
        SRC_COLOR(GL11.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GL11.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GL11.GL_ONE_MINUS_DST_ALPHA);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        DestBlendFactor (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Depth test function constants. */
    public enum DepthTestFunc
    {
        NEVER(GL11.GL_NEVER),
        LESS(GL11.GL_LESS),
        EQUAL(GL11.GL_EQUAL),
        LEQUAL(GL11.GL_LEQUAL),
        GREATER(GL11.GL_GREATER),
        NOTEQUAL(GL11.GL_NOTEQUAL),
        GEQUAL(GL11.GL_GEQUAL),
        ALWAYS(GL11.GL_ALWAYS);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        DepthTestFunc (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** The ambient reflectivity. */
    @Editable(mode="alpha")
    public Color4f ambient = new Color4f(Color4f.DARK_GRAY);

    /** The diffuse reflectivity. */
    @Editable(mode="alpha")
    public Color4f diffuse = new Color4f(Color4f.GRAY);

    /** The specular reflectivity. */
    @Editable(mode="alpha")
    public Color4f specular = new Color4f(Color4f.BLACK);

    /** The emissive color. */
    @Editable(mode="alpha")
    public Color4f emission = new Color4f(Color4f.BLACK);

    /** The specular exponent. */
    @Editable(min=0, max=128, step=0.1)
    public float shininess;

    /** The alpha test function. */
    @Editable
    public AlphaTestFunc alphaTestFunc = AlphaTestFunc.ALWAYS;

    /** The alpha test reference value. */
    @Editable(min=0.0, max=1.0, step=0.01)
    public float alphaTestRef;

    /** The source blend factor. */
    @Editable
    public SourceBlendFactor srcBlendFactor = SourceBlendFactor.ONE;

    /** The dest blend factor. */
    @Editable
    public DestBlendFactor destBlendFactor = DestBlendFactor.ZERO;

    /** The depth test function. */
    @Editable
    public DepthTestFunc depthTestFunc = DepthTestFunc.ALWAYS;

    /** Whether or not to write to the depth buffer. */
    @Editable
    public boolean depthMask = true;

    /** Whether to write to the red channel. */
    @Editable
    public boolean redMask = true;

    /** Whether to write to the green channel. */
    @Editable
    public boolean greenMask = true;

    /** Whether to write to the blue channel. */
    @Editable
    public boolean blueMask = true;

    /** Whether to write to the alpha channel. */
    @Editable
    public boolean alphaMask = true;

    /** The texture units to use in this pass. */
    @Editable
    public TextureUnitConfig[] units = new TextureUnitConfig[0];
}
