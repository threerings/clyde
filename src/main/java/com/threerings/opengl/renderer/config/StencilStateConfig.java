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

        protected final int _constant;
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

        protected final int _constant;
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

    /** If true, do not use a shared instance. */
    @Editable
    public boolean uniqueInstance;

    /**
     * Returns the corresponding stencil state.
     */
    public StencilState getState ()
    {
        return uniqueInstance ?
            new StencilState(
                testFunc.getConstant(), testRef, testMask, failOp.getConstant(),
                depthFailOp.getConstant(), passOp.getConstant(), writeMask) :
            StencilState.getInstance(
                testFunc.getConstant(), testRef, testMask, failOp.getConstant(),
                depthFailOp.getConstant(), passOp.getConstant(), writeMask);
    }
}
