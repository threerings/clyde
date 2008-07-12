//
// $Id$

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

    @Override // documentation inherited
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
