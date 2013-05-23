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

package com.threerings.opengl.util;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.state.AlphaState;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * A base class for simple overlays that draw themselves in immediate mode.
 */
public abstract class SimpleOverlay extends SimpleRenderable
{
    /**
     * Creates a new simple overlay.
     */
    public SimpleOverlay (GlContext ctx)
    {
        this(ctx, RenderQueue.OVERLAY);
    }

    /**
     * Creates a new simple overlay.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleOverlay (GlContext ctx, String queue)
    {
        this(ctx, queue, 0);
    }

    /**
     * Creates a new simple overlay.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleOverlay (GlContext ctx, String queue, int priority)
    {
        this(ctx, queue, priority, false, 0);
    }

    /**
     * Creates a new simple overlay.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleOverlay (
        GlContext ctx, String queue, int priority, boolean modifiesColorState, int primitiveCount)
    {
        super(ctx, queue, priority, modifiesColorState, primitiveCount);
    }

    @Override
    public void composite ()
    {
        // only add as enqueueable on primary render
        _ctx.getCompositor().addEnqueueable(this, 0);
    }

    @Override
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.ALPHA_STATE] = AlphaState.PREMULTIPLIED;
        states[RenderState.COLOR_STATE] = null;
        states[RenderState.DEPTH_STATE] = DepthState.DISABLED;
        states[RenderState.TEXTURE_STATE] = null;
        return states;
    }
}
