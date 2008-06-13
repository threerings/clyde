//
// $Id$

package com.threerings.opengl.renderer.config;

import org.lwjgl.opengl.GL11;

import com.threerings.editor.Editable;
import com.threerings.export.Exportable;
import com.threerings.util.DeepObject;

import com.threerings.opengl.renderer.state.DepthState;

/**
 * Configurable depth state.
 */
public class DepthStateConfig extends DeepObject
    implements Exportable
{
    /** Depth test function constants. */
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

    /** The depth test function. */
    @Editable(hgroup="d")
    public TestFunc testFunc = TestFunc.LEQUAL;

    /** Whether or not to write to the depth buffer. */
    @Editable(hgroup="d")
    public boolean mask = true;

    /**
     * Returns the corresponding depth state.
     */
    public DepthState getState ()
    {
        return DepthState.getInstance(testFunc.getConstant(), mask);
    }
}
