//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.client.sprite;

import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Transform3D;
import com.threerings.math.Vector2f;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;

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

        @Override
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

            // determine the target model, if any
            Sprite sprite = _view.getSprite(effect.getTarget());
            _targetModel = (sprite == null) ? null : sprite.getModel();

            // spawn the effect transient, if any
            if (config.model == null) {
                return;
            }
            if (sprite instanceof ActorSprite && config.attachToTarget) {
                ((ActorSprite)sprite).spawnAttachedTransientModel(
                    config.model, config.rotateWithTarget);
                return;
            }
            Transform3D transform;
            if (_targetModel != null) {
                transform = _targetModel.getLocalTransform();
            } else {
                Vector2f translation = effect.getTranslation();
                transform = _view.getFloorTransform(
                    translation.x, translation.y, effect.getRotation(), config.floorMask);
            }
            _view.getScene().spawnTransient(config.model, transform);
        }

        /** The target model, if any. */
        protected Model _targetModel;

        /** The owning view. */
        @Bound
        protected TudeySceneView _view;
    }

    /**
     * Plays an animation on a target sprite.
     */
    public static class Animator extends Original
    {
        /**
         * Creates a new implementation.
         */
        public Animator (
            TudeyContext ctx, Scope parentScope, EffectSpriteConfig.Animator config, Effect effect)
        {
            super(ctx, parentScope, config, effect);

            // play the animation, if any
            if (_targetModel != null && config.animation != null) {
                Animation anim = _targetModel.createAnimation(config.animation);
                if (anim != null) {
                    anim.start();
                }
            }
        }
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

    /**
     * Returns the effect.
     */
    public Effect getEffect ()
    {
        return _effect;
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
