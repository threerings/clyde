//
// $Id$

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
            if (impl instanceof ActorSprite.Original) {
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
        public String interact = "";

        /**
         * Returns the cached idle weight array.
         */
        public float[] getIdleWeights ()
        {
            float[] weights = (_idleWeights == null) ? null : _idleWeights.get();
            if (weights == null) {
                _idleWeights = new SoftReference<float[]>(weights = new float[idles.length]);
                for (int ii = 0; ii < idles.length; ii++) {
                    weights[ii] = idles[ii].weight;
                }
            }
            return weights;
        }

        @Override // documentation inherited
        public ActorSprite.Implementation getImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            if (impl instanceof ActorSprite.Moving) {
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
        protected SoftReference<float[]> _idleWeights;
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
}
