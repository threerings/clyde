//
// $Id$

package com.threerings.opengl.util;

import com.threerings.opengl.renderer.SimpleBatch;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;

/**
 * A base class for {@link Renderable} objects that simply require a set of states and a call to
 * their own {@link #draw} method (which draws its geometry in world space).
 */
public abstract class SimpleRenderable
    implements Renderable
{
    /**
     * Creates a new simple renderable.
     */
    public SimpleRenderable (GlContext ctx)
    {
        this(ctx, false);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param transparent if true, render the object in the transparent queue.
     */
    public SimpleRenderable (GlContext ctx, boolean transparent)
    {
        this(ctx, transparent, false, 0);
    }

    /**
     * Creates a new simple renderable.
     *
     * @param transparent if true, render the object in the transparent queue.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleRenderable (
        GlContext ctx, boolean transparent, final boolean modifiesColorState,
        final int primitiveCount)
    {
        _ctx = ctx;
        _transparent = transparent;
        _batch = new SimpleBatch(createStates(), new SimpleBatch.DrawCommand() {
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
     * Returns a reference to the state set.
     */
    public RenderState[] getStates ()
    {
        return _batch.getStates();
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // update the transform state
        TransformState tstate = (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
        tstate.getModelview().set(_ctx.getRenderer().getCamera().getViewTransform());
        tstate.setDirty(true);

        // queue up the batch
        if (_transparent) {
            _ctx.getRenderer().enqueueTransparent(_batch);
        } else {
            _ctx.getRenderer().enqueueOpaque(_batch);
        }
    }

    /**
     * Creates the state set for this object.
     */
    protected RenderState[] createStates ()
    {
        RenderState[] states = (RenderState[])RenderState.DEFAULTS.clone();
        states[RenderState.DEPTH_STATE] = DepthState.TEST_WRITE;
        states[RenderState.TRANSFORM_STATE] = new TransformState();
        return states;
    }

    /**
     * Draws the geometry in world space.
     */
    protected abstract void draw ();

    /** The renderer context. */
    protected GlContext _ctx;

    /** Whether to add the batch to the transparent queue. */
    protected boolean _transparent;

    /** The batch that we submit to the renderer. */
    protected SimpleBatch _batch;
}
