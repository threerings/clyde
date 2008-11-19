//
// $Id$

package com.threerings.tudey.client.handler;

import com.threerings.expr.SimpleScope;

import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.data.effect.Effect;

/**
 * Handles the display of a stateless effect.
 */
public abstract class EffectHandler extends SimpleScope
    implements TudeySceneView.TickParticipant
{
    /**
     * Creates a new handler.
     */
    public EffectHandler (GlContext ctx, TudeySceneView view, Effect effect)
    {
        super(view);
        _ctx = ctx;
        _effect = effect;

        // register as tick participant
        view.addTickParticipant(this);
    }

    // documentation inherited from interface TudeySceneView.TickParticipant
    public boolean tick (long delayedTime)
    {
        long timestamp = _effect.getTimestamp();
        return delayedTime < timestamp || liveTick((delayedTime - timestamp) / 1000f);
    }

    @Override // documentation inherited
    public String getScopeName ()
    {
        return "handler";
    }

    /**
     * Called to tick the handler once the delayed time has reached the effect's timestamp.
     *
     * @param elapsed the time elapsed since the start of the effect.
     * @return true to continue ticking the handler, false to remove it from the tick list.
     */
    protected abstract boolean liveTick (float elapsed);

    /** The renderer context. */
    protected GlContext _ctx;

    /** The effect object. */
    protected Effect _effect;
}
