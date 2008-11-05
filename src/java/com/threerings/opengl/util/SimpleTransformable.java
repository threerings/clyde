//
// $Id$

package com.threerings.opengl.util;

import com.threerings.math.Transform3D;
import com.threerings.math.Vector3f;

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
        this(ctx, RenderQueue.OPAQUE);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     */
    public SimpleTransformable (GlContext ctx, String queue)
    {
        this(ctx, queue, 0);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     */
    public SimpleTransformable (GlContext ctx, String queue, int priority)
    {
        this(ctx, queue, priority, false, 0);
    }

    /**
     * Creates a new simple transformable.
     *
     * @param queue the name of the queue into which we place the batch.
     * @param priority the priority level at which to enqueue the batch.
     * @param modifiesColorState if true, invalidate the color state after calling the
     * {@link #draw} method.
     * @param primitiveCount the primitive count to report to the renderer.
     */
    public SimpleTransformable (
        GlContext ctx, String queue, int priority, boolean modifiesColorState, int primitiveCount)
    {
        super(ctx, queue, priority, modifiesColorState, primitiveCount);
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
        Transform3D modelview = tstate.getModelview();
        _ctx.getCompositor().getCamera().getViewTransform().compose(_transform, modelview);
        tstate.setDirty(true);

        // update the batch depth
        _batch.depth = modelview.transformPointZ(getCenter());

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

    /**
     * Returns the transformable's model space center.
     */
    protected Vector3f getCenter ()
    {
        return Vector3f.ZERO;
    }

    /** The world space transform. */
    protected Transform3D _transform = new Transform3D(Transform3D.UNIFORM);
}
