//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.StencilState;

/**
 * Configurable stencil state.
 */
public class StencilStateConfig extends DeepObject
    implements Exportable
{
    /** Stencil test function constants. */
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

    /** Stencil ops. */
    public enum Op
    {
        KEEP(GL11.GL_KEEP),
        ZERO(GL11.GL_ZERO),
        REPLACE(GL11.GL_REPLACE),
        INCR(GL11.GL_INCR),
        DECR(GL11.GL_DECR),
        INVERT(GL11.GL_INVERT);

        public int getConstant ()
        {
            return _constant;
        }

        Op (int constant)
        {
            _constant = constant;
        }

        protected int _constant;
    }

    /** The stencil test function. */
    @Editable(hgroup="t")
    public TestFunc testFunc = TestFunc.ALWAYS;

    /** The stencil test reference value. */
    @Editable(min=0, hgroup="t")
    public int testRef;

    /** The stencil fail operation. */
    @Editable(hgroup="o")
    public Op failOp = Op.KEEP;

    /** The stencil depth fail operation. */
    @Editable(hgroup="o")
    public Op depthFailOp = Op.KEEP;

    /** The stencil pass operation. */
    @Editable(hgroup="o")
    public Op passOp = Op.KEEP;

    /** The stencil test mask. */
    @Editable(hgroup="m")
    public int testMask = 0x7FFFFFFF;

    /** The stencil write mask. */
    @Editable(hgroup="m")
    public int writeMask = 0x7FFFFFFF;

    /**
     * Returns the corresponding stencil state.
     */
    public StencilState getState ()
    {
        return StencilState.getInstance(
            testFunc.getConstant(), testRef, testMask, failOp.getConstant(),
            depthFailOp.getConstant(), passOp.getConstant(), writeMask);
    }
}
