//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Vector2f;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.EffectConfig;
import com.threerings.tudey.config.EffectSpriteConfig;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.util.TudeyContext;

/**
 * Handles the display of a stateless effect.
 */
public class EffectSprite extends Sprite
    implements TudeySceneView.TickParticipant
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Updates the state of the sprite.
         *
         * @return true to keep the sprite in the tick list, false to remove it.
         */
        public boolean tick (float elapsed)
        {
            return false;
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (
            TudeyContext ctx, Scope parentScope, EffectSpriteConfig config, Effect effect)
        {
            super(parentScope);

            // spawn the effect transient, if any
            if (config.model != null) {
                Vector2f translation = effect.getTranslation();
                _view.getScene().spawnTransient(config.model, _view.getFloorTransform(
                    translation.x, translation.y, effect.getRotation()));
            }
        }

        /** The owning view. */
        @Bound
        protected TudeySceneView _view;
    }

    /**
     * Creates a new effect sprite.
     */
    public EffectSprite (TudeyContext ctx, TudeySceneView view, Effect effect)
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
        if (delayedTime < timestamp) {
            return true; // not yet fired
        }
        if (_impl == null) {
            EffectConfig config = _ctx.getConfigManager().getConfig(
                EffectConfig.class, _effect.getConfig());
            _impl = (config == null) ? null : config.createSpriteImplementation(
                _ctx, this, _effect);
            _impl = (_impl == null) ? NULL_IMPLEMENTATION : _impl;
        }
        return _impl.tick((delayedTime - timestamp) / 1000f);
    }

    /** The effect object. */
    protected Effect _effect;

    /** The effect implementation (<code>null</code> until actually created). */
    protected Implementation _impl;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
