//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.config.ConfigReference;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

/**
 * Handles an effect on the server.
 */
public class EffectLogic extends Logic
{
    /**
     * Initializes the logic.
     */
    public void init (
        TudeySceneManager scenemgr, ConfigReference<EffectConfig> ref,
        EffectConfig.Original config, int timestamp, Vector2f translation, float rotation)
    {
        super.init(scenemgr);
        _effect = createEffect(ref, timestamp, translation, rotation);
        _effect.init(scenemgr.getConfigManager());
        _shape = config.shape.getShape().transform(new Transform2D(translation, rotation));
    }

    /**
     * Returns a reference to the effect fired.
     */
    public Effect getEffect ()
    {
        return _effect;
    }

    /**
     * Returns a reference to the shape of the effect.
     */
    public Shape getShape ()
    {
        return _shape;
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _effect.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _effect.getRotation();
    }

    /**
     * Creates the effect for this logic.
     */
    protected Effect createEffect (
        ConfigReference<EffectConfig> ref, int timestamp, Vector2f translation, float rotation)
    {
        return new Effect(ref, timestamp, translation, rotation);
    }

    /** The effect fired. */
    protected Effect _effect;

    /** The shape of the effect. */
    protected Shape _shape;
}
