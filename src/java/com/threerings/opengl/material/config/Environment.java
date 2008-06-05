//
// $Id$

package com.threerings.opengl.material.config;

import org.lwjgl.opengl.ARBTextureEnvCombine;
import org.lwjgl.opengl.ARBTextureEnvDot3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

/**
 * The fixed-function texture environment.
 */
public abstract class Environment extends DeepObject
    implements Exportable
{
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

    /**
     * The modulate environment mode.
     */
    public static class Modulate extends Environment
    {
        @Override // documentation inherited
        public int getGLConstant ()
        {
            return GL11.GL_MODULATE;
        }
    }

    /**
     * The decal environment mode.
     */
    public static class Decal extends Environment
    {
        @Override // documentation inherited
        public int getGLConstant ()
        {
            return GL11.GL_DECAL;
        }
    }

    public static class Blend extends Environment
    {
        @Override // documentation inherited
        public int getGLConstant ()
        {
            return GL11.GL_BLEND;
        }
    }

    public static class Replace extends Environment
    {
        @Override // documentation inherited
        public int getGLConstant ()
        {
            return GL11.GL_REPLACE;
        }
    }

    public static class Add extends Environment
    {
        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_env_add;
        }

        @Override // documentation inherited
        public int getGLConstant ()
        {
            return GL11.GL_ADD;
        }
    }

    /**
     * A single rgb combine parameter.
     */
    public static class RGBParam extends DeepObject
        implements Exportable
    {
        /** The source of the operand. */
        @Editable(hgroup="p")
        public Source source = Source.TEXTURE;

        /** The operand itself. */
        @Editable(hgroup="p")
        public RGBOperand operand = RGBOperand.SRC_COLOR;

        public RGBParam (Source source, RGBOperand operand)
        {
            this.source = source;
            this.operand = operand;
        }

        public RGBParam ()
        {
        }
    }

    /**
     * A single alpha source/operand pair.
     */
    public static class AlphaParam extends DeepObject
        implements Exportable
    {
        /** The source of the operand. */
        @Editable(hgroup="p")
        public Source source = Source.TEXTURE;

        /** The operand itself. */
        @Editable(hgroup="p")
        public AlphaOperand operand = AlphaOperand.SRC_ALPHA;

        public AlphaParam (Source source, AlphaOperand operand)
        {
            this.source = source;
            this.operand = operand;
        }

        public AlphaParam ()
        {
        }
    }

    /**
     * The combine environment mode.
     */
    public static class Combine extends Environment
    {
        /** The RGB combine mode. */
        @Editable(category="rgb")
        public RGBCombine rgbCombine = RGBCombine.MODULATE;

        /** The first RGB parameter. */
        @Editable(nullable=false, category="rgb")
        public RGBParam rgbParam0 = new RGBParam(Source.TEXTURE, RGBOperand.SRC_COLOR);

        /** The second RGB parameter. */
        @Editable(nullable=false, category="rgb")
        public RGBParam rgbParam1 = new RGBParam(Source.PREVIOUS, RGBOperand.SRC_COLOR);

        /** The third RGB parameter. */
        @Editable(nullable=false, category="rgb")
        public RGBParam rgbParam2 = new RGBParam(Source.CONSTANT, RGBOperand.SRC_ALPHA);

        /** The RGB combine scale. */
        @Editable(category="rgb")
        public Scale rgbScale = Scale.ONE;

        /** The alpha combine mode. */
        @Editable(category="alpha")
        public AlphaCombine alphaCombine = AlphaCombine.MODULATE;

        /** The first alpha parameter. */
        @Editable(nullable=false, category="alpha")
        public AlphaParam alphaParam0 = new AlphaParam(Source.TEXTURE, AlphaOperand.SRC_ALPHA);

        /** The first alpha parameter. */
        @Editable(nullable=false, category="alpha")
        public AlphaParam alphaParam1 = new AlphaParam(Source.PREVIOUS, AlphaOperand.SRC_ALPHA);

        /** The first alpha parameter. */
        @Editable(nullable=false, category="alpha")
        public AlphaParam alphaParam2 = new AlphaParam(Source.CONSTANT, AlphaOperand.SRC_ALPHA);

        /** The alpha combine scale. */
        @Editable(category="alpha")
        public Scale alphaScale = Scale.ONE;

        @Override // documentation inherited
        public boolean isSupported ()
        {
            return GLContext.getCapabilities().GL_ARB_texture_env_combine;
        }

        @Override // documentation inherited
        public int getGLConstant ()
        {
            return ARBTextureEnvCombine.GL_COMBINE_ARB;
        }
    }

    /**
     * Returns the subclasses available for selection in the editor.
     */
    public static Class[] getEditorTypes ()
    {
        return new Class[] {
            Modulate.class, Decal.class, Blend.class, Replace.class, Add.class, Combine.class };
    }

    /**
     * Checks whether the mode is supported.
     */
    public boolean isSupported ()
    {
        return true;
    }

    /**
     * Returns the OpenGL constant for the mode.
     */
    public abstract int getGLConstant ();
}
