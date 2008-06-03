//
// $Id$

package com.threerings.opengl.config;

import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureEnvDot3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.Color4f;

/**
 * Describes the configuration of a single texture unit.
 */
public class TextureUnitConfig extends DeepObject
    implements Exportable
{
    /** Environment mode constants. */
    public enum EnvMode
    {
        MODULATE(GL11.GL_MODULATE),
        DECAL(GL11.GL_DECAL),
        BLEND(GL11.GL_BLEND),
        REPLACE(GL11.GL_REPLACE),
        ADD(GL11.GL_ADD) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_add;
            }
        },
        COMBINE(ARBTextureEnvCombine.GL_COMBINE_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_combine;
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

        EnvMode (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** RGB combination modes. */
    public enum RGBCombine
    {
        REPLACE(GL11.GL_REPLACE),
        MODULATE(GL11.GL_MODULATE),
        ADD(GL11.GL_ADD),
        ADD_SIGNED(ARBTextureEnvCombine.GL_ADD_SIGNED_ARB),
        INTERPOLATE(ARBTextureEnvCombine.GL_INTERPOLATE_ARB),
        SUBTRACT(ARBTextureEnvCombine.GL_SUBTRACT_ARB),
        DOT3_RGB(ARBTextureEnvDot3.GL_DOT3_RGB_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_dot3;
            }
        },
        DOT3_RGBA(ARBTextureEnvDot3.GL_DOT3_RGBA_ARB) {
            public boolean isSupported () {
                return GLContext.getCapabilities().GL_ARB_texture_env_dot3;
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

        RGBCombine (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Alpha combination modes. */
    public enum AlphaCombine
    {
        REPLACE(GL11.GL_REPLACE),
        MODULATE(GL11.GL_MODULATE),
        ADD(GL11.GL_ADD),
        ADD_SIGNED(ARBTextureEnvCombine.GL_ADD_SIGNED_ARB),
        INTERPOLATE(ARBTextureEnvCombine.GL_INTERPOLATE_ARB),
        SUBTRACT(ARBTextureEnvCombine.GL_SUBTRACT_ARB);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        AlphaCombine (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Combine sources. */
    public enum Source
    {
        TEXTURE(GL11.GL_TEXTURE),
        CONSTANT(ARBTextureEnvCombine.GL_CONSTANT_ARB),
        PRIMARY_COLOR(ARBTextureEnvCombine.GL_PRIMARY_COLOR_ARB),
        PREVIOUS(ARBTextureEnvCombine.GL_PREVIOUS_ARB);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        Source (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** RGB combination operands. */
    public enum RGBOperand
    {
        SRC_COLOR(GL11.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        RGBOperand (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** Alpha combination operands. */
    public enum AlphaOperand
    {
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA);

        public int getGLConstant ()
        {
            return _glConstant;
        }

        AlphaOperand (int glConstant)
        {
            _glConstant = glConstant;
        }

        protected int _glConstant;
    }

    /** The combination scale values. */
    public enum Scale
    {
        ONE(1f),
        TWO(2f),
        FOUR(4f);

        public float getValue ()
        {
            return _value;
        }

        Scale (float value)
        {
            _value = value;
        }

        protected float _value;
    }

    /** The texture environment mode. */
    @Editable
    public EnvMode envMode = EnvMode.MODULATE;

    /** The texture environment color. */
    @Editable(mode="alpha")
    public Color4f envColor = new Color4f(0f, 0f, 0f, 0f);

    /** The RGB combine mode. */
    @Editable
    public RGBCombine rgbCombine = RGBCombine.MODULATE;

    /** The alpha combine mode. */
    @Editable
    public AlphaCombine alphaCombine = AlphaCombine.MODULATE;

    /** The first RGB combine source. */
    @Editable
    public Source rgbSource0 = Source.TEXTURE;

    /** The second RGB combine source. */
    @Editable
    public Source rgbSource1 = Source.PREVIOUS;

    /** The third RGB combine source. */
    @Editable
    public Source rgbSource2 = Source.CONSTANT;

    /** The first alpha combine source. */
    @Editable
    public Source alphaSource0 = Source.TEXTURE;

    /** The second alpha combine source. */
    @Editable
    public Source alphaSource1 = Source.PREVIOUS;

    /** The third alpha combine source. */
    @Editable
    public Source alphaSource2 = Source.CONSTANT;

    /** The first RGB combine operand. */
    @Editable
    public RGBOperand rgbOperand0 = RGBOperand.SRC_COLOR;

    /** The second RGB combine operand. */
    @Editable
    public RGBOperand rgbOperand1 = RGBOperand.SRC_COLOR;

    /** The third RGB combine operand. */
    @Editable
    public RGBOperand rgbOperand2 = RGBOperand.SRC_ALPHA;

    /** The first alpha combine operand. */
    @Editable
    public AlphaOperand alphaOperand0 = AlphaOperand.SRC_ALPHA;

    /** The second alpha combine operand. */
    @Editable
    public AlphaOperand alphaOperand1 = AlphaOperand.SRC_ALPHA;

    /** The third alpha combine operand. */
    @Editable
    public AlphaOperand alphaOperand2 = AlphaOperand.SRC_ALPHA;

    /** The RGB combine scale. */
    @Editable
    public Scale rgbScale = Scale.ONE;

    /** The alpha combine scale. */
    @Editable
    public Scale alphaScale = Scale.ONE;

    /** The texture level of detail bias. */
    @Editable(step=0.01)
    public float lodBias;
}
