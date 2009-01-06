//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.sprite.EffectSprite;
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
        /**
         * Adds the implementation's update references to the provided set.
         */
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

        /** The lifespan of the effect. */
        @Editable(min=0)
        public int lifespan = 1000;

        /**
         * Returns the name of the server-side logic class to use for the effect.
         */
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.EffectLogic";
        }

        /**
         * Adds the resources to preload for this effect into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            sprite.getPreloads(cfgmgr, preloads);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public EffectSprite.Implementation createSpriteImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            return sprite.createImplementation(ctx, scope, effect);
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

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(EffectConfig.class, effect);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            EffectConfig config = cfgmgr.getConfig(EffectConfig.class, effect);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
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

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
