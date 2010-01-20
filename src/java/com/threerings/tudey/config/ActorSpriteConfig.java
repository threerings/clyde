//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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
import com.threerings.util.DeepOmit;

import com.threerings.opengl.model.Animation;
import com.threerings.opengl.model.Model;
import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an actor sprite.
 */
@EditorTypes({
    ActorSpriteConfig.Default.class, ActorSpriteConfig.Moving.class,
    ActorSpriteConfig.StatefulEntry.class })
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
        /** A scale to apply to the sprite (affects the movement animation speeds). */
        @Editable(min=0, step=0.01, hgroup="s")
        public float scale = 1f;

        /** A scale to apply to attached models. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float attachedScale = 1f;

        /** The idle animations for the sprite. */
        @Editable
        public WeightedAnimation[] idles = new WeightedAnimation[0];

        /** The sets of movement animations for the sprite. */
        @Editable
        public MovementSet[] movements = new MovementSet[0];

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
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            super.getPreloads(cfgmgr, preloads);
            for (WeightedAnimation idle : idles) {
                preloads.add(new Preloadable.Animation(idle.animation));
            }
            for (MovementSet movement : movements) {
                movement.getPreloads(cfgmgr, preloads);
            }
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
        @DeepOmit
        protected transient SoftReference<float[]> _idleWeights;
    }

    /**
     * Contains an animation reference and an associated weight.
     */
    public static class WeightedAnimation extends DeepObject
        implements Exportable
    {
        /** The weight of the animation (affects how often it occurs). */
        @Editable(min=0, step=0.01)
        public float weight = 1f;

        /** The animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /**
     * Represents a set of movement animations.
     */
    @EditorTypes({ SingleMovement.class, QuadMovement.class })
    public static abstract class MovementSet extends DeepObject
        implements Exportable
    {
        /** The movement speed of this animation set. */
        @Editable(min=0, step=0.01)
        public float speed;

        /**
         * Adds the resources to preload for this sprite into the provided set.
         */
        public abstract void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads);

        /**
         * Resolves the movement set animations for the supplied model.
         */
        public abstract Animation[] resolve (Model model);
    }

    /**
     * A movement set with a single animation.
     */
    public static class SingleMovement extends MovementSet
    {
        /** The movement animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            preloads.add(new Preloadable.Animation(animation));
        }

        @Override // documentation inherited
        public Animation[] resolve (Model model)
        {
            Animation anim = model.createAnimation(animation);
            return new Animation[] { anim, anim, anim, anim };
        }
    }

    /**
     * A movement set with separate animations for the four directions.
     */
    public static class QuadMovement extends MovementSet
    {
        /** The forward animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> forward;

        /** The left animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> left;

        /** The backward animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> backward;

        /** The right animation reference. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> right;

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            preloads.add(new Preloadable.Animation(forward));
            preloads.add(new Preloadable.Animation(left));
            preloads.add(new Preloadable.Animation(backward));
            preloads.add(new Preloadable.Animation(right));
        }

        @Override // documentation inherited
        public Animation[] resolve (Model model)
        {
            return new Animation[] {
                model.createAnimation(backward), model.createAnimation(right),
                model.createAnimation(forward), model.createAnimation(left) };
        }
    }

    /**
     * Manipulates an entry sprite to reflect the state of the entry's corresponding actor.
     */
    public static class StatefulEntry extends Default
    {
        /** The entry's states. */
        @Editable
        public State[] states = new State[0];

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            super.getPreloads(cfgmgr, preloads);
            for (State state : states) {
                preloads.add(new Preloadable.Animation(state.animation));
            }
        }

        @Override // documentation inherited
        public ActorSprite.Implementation getImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            if (impl != null && impl.getClass() == ActorSprite.StatefulEntry.class) {
                ((ActorSprite.StatefulEntry)impl).setConfig(this);
            } else {
                impl = new ActorSprite.StatefulEntry(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Represents a single animated state.
     */
    public static class State extends DeepObject
        implements Exportable
    {
        /** The state animation. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;
    }

    /** Determines which floor categories the sprite belongs to. */
    @Editable(editor="mask", mode="floor", hgroup="f")
    public int floorFlags;

    /** Determines which floor categories the actor walks over. */
    @Editable(editor="mask", mode="floor", hgroup="f")
    public int floorMask = 0xFF;

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
