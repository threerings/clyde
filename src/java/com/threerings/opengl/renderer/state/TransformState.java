//
// $Id$

package com.threerings.opengl.renderer.state;

import com.threerings.math.Transform;

import com.threerings.opengl.renderer.Renderer;

/**
 * Contains the modelview transform state.
 */
public class TransformState extends RenderState
{
    /** The identity transform. */
    public static final TransformState IDENTITY = new TransformState();

    /**
     * Creates a new transform state with the values in the supplied transform.
     */
    public TransformState (Transform modelview)
    {
        _modelview.set(modelview);
    }

    /**
     * Creates a new transform state with the specified transform type ({@link Transform#GENERAL},
     * {@link Transform#AFFINE}, etc).
     */
    public TransformState (int type)
    {
        _modelview.setType(type);
    }

    /**
     * Creates a new transform state with an identity transform.
     */
    public TransformState ()
    {
    }

    /**
     * Returns a reference to the modelview transformation.
     */
    public Transform getModelview ()
    {
        return _modelview;
    }

    @Override // documentation inherited
    public int getType ()
    {
        return TRANSFORM_STATE;
    }

    @Override // documentation inherited
    public void apply (Renderer renderer)
    {
        renderer.setTransformState(_modelview);
    }

    /** The modelview transformation. */
    protected Transform _modelview = new Transform();
}
