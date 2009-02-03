//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.lang.ref.SoftReference;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an actor sprite.
 */
@EditorTypes({ ActorSpriteConfig.Default.class, ActorSpriteConfig.Moving.class })
public abstract class ActorSpriteConfig extends DeepObject
    implements Exportable
{
    /**
     * The default sprite.
     */
    public static class Default extends ActorSpriteConfig
    {
        @Override // documentation inherited
        public ActorSprite.Implementation getImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            if (impl != null && impl.getClass() == ActorSprite.Original.class) {
                ((ActorSprite.Original)impl).setConfig(this);
            } else {
                impl = new ActorSprite.Original(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A sprite with idle and movement animations.
     */
    public static class Moving extends Default
    {
        /** The idle animations for the sprite. */
        @Editable
        public WeightedAnimation[] idles = new WeightedAnimation[0];

        /** The sets of movement animations for the sprite. */
        @Editable
        public MovementSet[] movements = new MovementSet[0];

        /** The name of the interaction animation, if any. */
        @Editable
        public String interact = "use_interact";

        /**
         * Returns the cached idle weight array.
         */
        public float[] getIdleWeights ()
        {
            float[] weights = (_idleWeights == null) ? null : _idleWeights.get();
            if (weights == null) {
                _idleWeights = new SoftReference<float[]>(weights = createWeights(idles));
            }
            return weights;
        }

        @Override // documentation inherited
        public ActorSprite.Implementation getImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            if (impl != null && impl.getClass() == ActorSprite.Moving.class) {
                ((ActorSprite.Moving)impl).setConfig(this);
            } else {
                impl = new ActorSprite.Moving(ctx, scope, this);
            }
            return impl;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            _idleWeights = null;
        }

        /** The cached idle weights. */
        protected transient SoftReference<float[]> _idleWeights;
    }

    /**
     * Contains the name of an animation and an associated weight.
     */
    public static class WeightedAnimation extends DeepObject
        implements Exportable
    {
        /** The name of the animation. */
        @Editable(hgroup="n")
        public String name = "";

        /** The weight of the animation (affects how often it occurs). */
        @Editable(min=0, step=0.01, hgroup="n")
        public float weight = 1f;
    }

    /**
     * Represents a set of movement animations.
     */
    public static class MovementSet extends DeepObject
        implements Exportable
    {
        /** The speed threshold at and beyond which this set is used. */
        @Editable(min=0, step=0.01)
        public float speedThreshold;

        /** The forward movement animation. */
        @Editable(hgroup="fl")
        public String forward = "";

        /** The left movement animation. */
        @Editable(hgroup="fl")
        public String left = "";

        /** The backward movement animation. */
        @Editable(hgroup="br")
        public String backward = "";

        /** The right movement animation. */
        @Editable(hgroup="br")
        public String right = "";
    }

    /** The actor model. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> model;

    /** A transient to spawn when the actor is created. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> creationTransient;

    /** A transient to spawn when the actor is destroyed. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> destructionTransient;

    /**
     * Adds the resources to preload for this sprite into the provided set.
     */
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
        preloads.add(new Preloadable.Model(model));
        preloads.add(new Preloadable.Model(creationTransient));
        preloads.add(new Preloadable.Model(destructionTransient));
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     */
    public abstract ActorSprite.Implementation getImplementation (
        TudeyContext ctx, Scope scope, ActorSprite.Implementation impl);

    /**
     * Invalidates any cached data.
     */
    public void invalidate ()
    {
        // nothing by default
    }

    /**
     * Creates the array of weights from the supplied weighted animation array.
     */
    protected static float[] createWeights (WeightedAnimation[] weighted)
    {
        float[] weights = new float[weighted.length];
        for (int ii = 0; ii < weighted.length; ii++) {
            weights[ii] = weighted[ii].weight;
        }
        return weights;
    }
}
