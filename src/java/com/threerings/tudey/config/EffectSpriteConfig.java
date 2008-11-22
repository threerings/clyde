//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an effect sprite.
 */
@EditorTypes({ EffectSpriteConfig.Default.class })
public abstract class EffectSpriteConfig extends DeepObject
    implements Exportable
{
    /**
     * The default sprite.
     */
    public static class Default extends EffectSpriteConfig
    {
        @Override // documentation inherited
        public EffectSprite.Implementation createImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            return new EffectSprite.Original(ctx, scope, this, effect);
        }
    }

    /** The transient to fire off for the effect. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> model;

    /**
     * Creates a sprite implementation for this configuration.
     */
    public abstract EffectSprite.Implementation createImplementation (
        TudeyContext ctx, Scope scope, Effect effect);
}
