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

        protected final int _constant;
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

        protected final int _constant;
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

        protected final int _constant;
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

    /** If true, do not use a shared instance. */
    @Editable
    public boolean uniqueInstance;

    /**
     * Returns the corresponding alpha state.
     */
    public AlphaState getState ()
    {
        return uniqueInstance ?
            new AlphaState(
                testFunc.getConstant(), testRef, srcBlendFactor.getConstant(),
                destBlendFactor.getConstant()) :
            AlphaState.getInstance(
                testFunc.getConstant(), testRef, srcBlendFactor.getConstant(),
                destBlendFactor.getConstant());
    }
}
