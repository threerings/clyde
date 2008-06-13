//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.AlphaState;

/**
 * Configurable alpha state.
 */
public class AlphaStateConfig extends DeepObject
    implements Exportable
{
    /** Test functions. */
    public enum TestFunc
    {
        NEVER(GL11.GL_NEVER),
        LESS(GL11.GL_LESS),
        EQUAL(GL11.GL_EQUAL),
        LEQUAL(GL11.GL_LEQUAL),
        GREATER(GL11.GL_GREATER),
        NOTEQUAL(GL11.GL_NOTEQUAL),
        GEQUAL(GL11.GL_GEQUAL),
        ALWAYS(GL11.GL_ALWAYS);

        public int getConstant ()
        {
            return _constant;
        }

        TestFunc (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
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

        public int getConstant ()
        {
            return _constant;
        }

        SourceBlendFactor (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
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

        public int getConstant ()
        {
            return _constant;
        }

        DestBlendFactor (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** The alpha test function. */
    @Editable(hgroup="t")
    public TestFunc testFunc = TestFunc.ALWAYS;

    /** The alpha test reference value. */
    @Editable(min=0.0, max=1.0, step=0.01, hgroup="t")
    public float testRef;

    /** The source blend factor. */
    @Editable(hgroup="b")
    public SourceBlendFactor srcBlendFactor = SourceBlendFactor.ONE;

    /** The dest blend factor. */
    @Editable(hgroup="b")
    public DestBlendFactor destBlendFactor = DestBlendFactor.ZERO;

    /**
     * Returns the corresponding alpha state.
     */
    public AlphaState getState ()
    {
        return AlphaState.getInstance(
            testFunc.getConstant(), testRef, srcBlendFactor.getConstant(),
            destBlendFactor.getConstant());
    }
}
