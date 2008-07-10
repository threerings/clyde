//
// $Id$

package com.threerings.opengl.util;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;

/**
 * A base class for {@link Renderable} objects based on {@link SimpleBatch}es.
 */
public abstract class SimpleRenderable
    implements Renderable
{
    /**
     * Creates a new simple renderable.
     */
    public SimpleRenderable (GlContext ctx)
    {
        this(ctx, RenderQueue.DEFAULT);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleRenderable (GlContext ctx, String queue)
    {
        this(ctx, queue, false, 0);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param transparent if true, enqueue the batch as transparent.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleRenderable (GlContext ctx, String queue, boolean transparent, int priority)
    {
        this(ctx, queue, transparent, priority, false, 0);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param transparent if true, enqueue the batch as transparent.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleRenderable (
        GlContext ctx, String queue, boolean transparent, int priority,
        boolean modifiesColorState, int primitiveCount)
    {
        _ctx = ctx;
        _queue = ctx.getCompositor().getQueue(queue);
        _batch = createBatch(modifiesColorState, primitiveCount);
        _transparent = transparent;
        _priority = priority;
    }

    /**
     * Returns a reference to the state set.
     */
    public RenderState[] getStates ()
    {
        return _batch.getStates();
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        _queue.add(_batch, _transparent, _priority);
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
        RenderState[] states = RenderState.DEFAULTS.clone();
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

    /** Whether to enqueue the batch as transparent. */
    protected boolean _transparent;

    /** The priority level of the batch. */
    protected int _priority;
}
