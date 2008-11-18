//
// $Id$

package com.threerings.tudey.data.effect;

import com.threerings.config.ConfigReference;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.ModelConfig;

/**
 * A transient effect that includes explicit values for the model reference and transform
 * parameters.  This is less compact than subclassing {@link TransientEffect}, but more
 * versatile.
 */
public class SimpleTransientEffect extends TransientEffect
{
    /**
     * Creates a new simple transient effect.
     */
    public SimpleTransientEffect (
        long timestamp, ConfigReference<ModelConfig> model, Transform3D transform)
    {
        super(timestamp);
        _model = model;
        _transform = transform;
    }

    /**
     * No-arg constructor for deserialization.
     */
    public SimpleTransientEffect ()
    {
    }

    @Override // documentation inherited
    public ConfigReference<ModelConfig> getModel ()
    {
        return _model;
    }

    @Override // documentation inherited
    public Transform3D getTransform ()
    {
        return _transform;
    }

    /** The transient model to spawn. */
    protected ConfigReference<ModelConfig> _model;

    /** The transform at which to spawn the model. */
    protected Transform3D _transform;
}
