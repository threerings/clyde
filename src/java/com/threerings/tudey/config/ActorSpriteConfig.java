//
// $Id$

package com.threerings.tudey.config;

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
@EditorTypes({ ActorSpriteConfig.Default.class })
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
}
