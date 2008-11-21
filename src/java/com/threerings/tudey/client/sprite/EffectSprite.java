//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.effect.Effect;

/**
 * Handles the display of a stateless effect.
 */
public abstract class EffectSprite extends Sprite
    implements TudeySceneView.TickParticipant
{
    /**
     * Creates a new effect sprite.
     */
    public EffectSprite (GlContext ctx, TudeySceneView view, Effect effect)
    {
        super(ctx, view);
        _effect = effect;

        // register as tick participant
        view.addTickParticipant(this);
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (int delayedTime)
    {
        int timestamp = _effect.getTimestamp();
        return delayedTime < timestamp || liveTick((delayedTime - timestamp) / 1000f);
    }

    /**
     * Called to tick the handler once the delayed time has reached the effect's timestamp.
     *
     * @param elapsed the time elapsed since the start of the effect.
     * @return true to continue ticking the handler, false to remove it from the tick list.
     */
    protected abstract boolean liveTick (float elapsed);

    /** The effect object. */
    protected Effect _effect;
}
