//
// $Id$

package com.threerings.opengl.util;

import com.threerings.math.Transform3D;

import com.threerings.opengl.compositor.RenderQueue;
import com.threerings.opengl.renderer.state.DepthState;
import com.threerings.opengl.renderer.state.RenderState;
import com.threerings.opengl.renderer.state.TransformState;

/**
 * Extends {@link SimpleRenderable} to include a world space transform so that subclasses can
 * render themselves in model space.
 */
public abstract class SimpleTransformable extends SimpleRenderable
{
    /**
     * Creates a new simple transformable.
     */
    public SimpleTransformable (GlContext ctx)
    {
        this(ctx, RenderQueue.DEFAULT);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleTransformable (GlContext ctx, String queue)
    {
        this(ctx, queue, false, 0);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param transparent if true, enqueue the batch as transparent.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleTransformable (GlContext ctx, String queue, boolean transparent, int priority)
    {
        this(ctx, queue, transparent, priority, false, 0);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param transparent if true, enqueue the batch as transparent.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleTransformable (
        GlContext ctx, String queue, boolean transparent, int priority,
        boolean modifiesColorState, int primitiveCount)
    {
        super(ctx, queue, transparent, priority, modifiesColorState, primitiveCount);
    }

    /**
     * Returns a reference to the world space transform.
     */
    public Transform3D getTransform ()
    {
        return _transform;
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        // update the transform state
        TransformState tstate = (TransformState)_batch.getStates()[RenderState.TRANSFORM_STATE];
        _ctx.getCompositor().getCamera().getViewTransform().compose(
            _transform, tstate.getModelview());
        tstate.setDirty(true);

        // queue up the batch
        super.enqueue();
    }

    @Override // documentation inherited
    protected RenderState[] createStates ()
    {
        RenderState[] states = super.createStates();
        states[RenderState.TRANSFORM_STATE] = new TransformState();
        return states;
    }

    /**
     * Creates an uninitialized transformable.
     */
    protected SimpleTransformable ()
    {
    }

    /** The world space transform. */
    protected Transform3D _transform = new Transform3D(Transform3D.UNIFORM);
}
