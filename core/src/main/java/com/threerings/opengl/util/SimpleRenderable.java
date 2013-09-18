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

import com.threerings.opengl.compositor.Compositable;
import com.threerings.opengl.compositor.Enqueueable;
import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * A base class for renderable objects based on {@link SimpleBatch}es.
 */
public abstract class SimpleRenderable
    implements Compositable, Enqueueable
{
    /**
     * Creates a new simple renderable.
     */
    public SimpleRenderable (GlContext ctx)
    {
        this(ctx, RenderQueue.OPAQUE);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleRenderable (GlContext ctx, String queue)
    {
        this(ctx, queue, 0);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleRenderable (GlContext ctx, String queue, int priority)
    {
        this(ctx, queue, priority, false, 0);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleRenderable (
        GlContext ctx, String queue, int priority, boolean modifiesColorState, int primitiveCount)
    {
        _ctx = ctx;
        _queue = ctx.getCompositor().getQueue(queue);
        _batch = createBatch(modifiesColorState, primitiveCount);
        _priority = priority;
    }

    /**
     * Returns a reference to the state set.
     */
    public RenderState[] getStates ()
    {
        return _batch.getStates();
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        _ctx.getCompositor().addEnqueueable(this);
    }

    // documentation inherited from interface Enqueueable
    public void enqueue ()
    {
        _queue.add(_batch, _priority);
    }

    /**
     * Creates an uninitialized renderable.
     */
    protected SimpleRenderable ()
    {
    }

    /**
     * Creates the batch to draw.
     */
    protected SimpleBatch createBatch (final boolean modifiesColorState, final int primitiveCount)
    {
        return new SimpleBatch(createStates(), new SimpleBatch.DrawCommand() {
            public boolean call () {
                draw();
                return modifiesColorState;
            }
            public int getPrimitiveCount () {
                return primitiveCount;
            }
        });
    }

    /**
     * Creates the state set for this object.
     */
    protected RenderState[] createStates ()
    {
        RenderState[] states = RenderState.createDefaultSet();
        states[RenderState.ARRAY_STATE] = null;
        states[RenderState.DEPTH_STATE] = DepthState.TEST_WRITE;
        states[RenderState.MATERIAL_STATE] = null;
        return states;
    }

    /**
     * Draws the geometry in immediate mode.
     */
    protected void draw ()
    {
        throw new RuntimeException("Override draw method to render.");
    }

    /** The renderer context. */
    protected GlContext _ctx;

    /** The queue into which we place our batch. */
    protected RenderQueue _queue;

    /** The batch that we submit to the renderer. */
    protected SimpleBatch _batch;

    /** The priority level of the batch. */
    protected int _priority;
}
