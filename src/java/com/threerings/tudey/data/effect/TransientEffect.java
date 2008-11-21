//
// $Id$

package com.threerings.tudey.data.effect;

import com.threerings.config.ConfigReference;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * An effect that spawns a transient model in the scene.
 */
public abstract class TransientEffect extends Effect
{
    /**
     * Creates a new transient effect.
     */
    public TransientEffect (int timestamp)
    {
        super(timestamp);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public TransientEffect ()
    {
    }

    @Override // documentation inherited
    public EffectSprite createSprite (TudeyContext ctx, final TudeySceneView view)
    {
        return new EffectSprite(ctx, view, this) {
            protected boolean liveTick (float elapsed) {
                view.getScene().spawnTransient(getModel(), getTransform());
                return false;
            }
        };
    }

    /**
     * Returns the configuration of the transient model to spawn.
     */
    public abstract ConfigReference<ModelConfig> getModel ();

    /**
     * Returns a reference to the transform at which to spawn the transient.
     */
    public abstract Transform3D getTransform ();
}
