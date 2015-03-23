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

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Vector2f;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an effect.
 */
public class EffectConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the effect.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            // nothing by default
        }

        /**
         * Returns a reference to the config's underlying original implementation.
         */
        public abstract Original getOriginal (ConfigManager cfgmgr);

        /**
         * Creates a sprite implementation for this configuration.
         *
         * @param scope the effect's expression scope.
         * @return the created implementation, or <code>null</code> if no implementation could be
         * created.
         */
        public abstract EffectSprite.Implementation createSpriteImplementation (
            TudeyContext ctx, Scope scope, Effect effect);

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
    }

    /**
     * Superclass of the original implementations.
     */
    public static class Original extends Implementation
    {
        /** The type of sprite to use for the effect. */
        @Editable
        public EffectSpriteConfig sprite = new EffectSpriteConfig.Default();

        /** The shape of the effect. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        /** The action associated with the effect, if any. */
        @Editable(nullable=true)
        public ActionConfig action;

        /** The lifespan of the effect. */
        @Editable(min=0, hgroup="l")
        public int lifespan = 1000;

        /** If true, only show the effect to its target. */
        @Editable(hgroup="l")
        public boolean targetOnly;

        /**
         * Returns the name of the server-side logic class to use for the effect.
         */
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.EffectLogic";
        }

        /**
         * Creates a new effect of the type associated with this config.
         */
        public Effect createEffect (
            ConfigReference<EffectConfig> config, int timestamp, EntityKey target,
            Vector2f translation, float rotation)
        {
            return new Effect(config, timestamp, target, translation, rotation);
        }

        /**
         * Adds the resources to preload for this effect into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            sprite.getPreloads(cfgmgr, preloads);
            if (action != null) {
                action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override
        public EffectSprite.Implementation createSpriteImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            return sprite.createImplementation(ctx, scope, effect);
        }

        @Override
        public void invalidate ()
        {
            shape.invalidate();
            if (action != null) {
                action.invalidate();
            }
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The effect reference. */
        @Editable(nullable=true)
        public ConfigReference<EffectConfig> effect;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            EffectConfig config = cfgmgr.getConfig(EffectConfig.class, effect);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override
        public EffectSprite.Implementation createSpriteImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            EffectConfig config = ctx.getConfigManager().getConfig(
                EffectConfig.class, this.effect);
            return (config == null) ? null : config.createSpriteImplementation(ctx, scope, effect);
        }
    }

    /** The actual effect implementation. */
    @Editable
    public Implementation implementation = new Original();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    /**
     * Creates a sprite implementation for this configuration.
     *
     * @param scope the effect's expression scope.
     * @return the created implementation, or <code>null</code> if no implementation could be
     * created.
     */
    public EffectSprite.Implementation createSpriteImplementation (
        TudeyContext ctx, Scope scope, Effect effect)
    {
        return implementation.createSpriteImplementation(ctx, scope, effect);
    }

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }
}
