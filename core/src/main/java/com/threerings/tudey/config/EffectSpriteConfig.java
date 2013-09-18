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
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.AnimationConfig;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.EffectSprite;
import com.threerings.tudey.data.effect.Effect;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an effect sprite.
 */
@EditorTypes({ EffectSpriteConfig.Default.class, EffectSpriteConfig.Animator.class })
public abstract class EffectSpriteConfig extends DeepObject
    implements Exportable
{
    /**
     * The default sprite.
     */
    public static class Default extends EffectSpriteConfig
    {
        @Override
        public EffectSprite.Implementation createImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            return new EffectSprite.Original(ctx, scope, this, effect);
        }
    }

    /**
     * A sprite that plays an animation on another sprite.
     */
    public static class Animator extends EffectSpriteConfig
    {
        /** The animation to play. */
        @Editable(nullable=true)
        public ConfigReference<AnimationConfig> animation;

         @Override
        public EffectSprite.Implementation createImplementation (
            TudeyContext ctx, Scope scope, Effect effect)
        {
            return new EffectSprite.Animator(ctx, scope, this, effect);
        }
    }

    /** Determines which floor categories the effect lies over. */
    @Editable(editor="mask", mode="floor", hgroup="a")
    public int floorMask = 0x01;

    /** Whether or not to attach the transient to the target (if it has one). */
    @Editable(hgroup="a")
    public boolean attachToTarget;

    /** Whether or not to rotate the transient with the target (if attached). */
    @Editable
    public boolean rotateWithTarget = true;

    /** The transient to fire off for the effect. */
    @Editable(nullable=true)
    public ConfigReference<ModelConfig> model;

    /**
     * Adds the resources to preload for this sprite into the provided set.
     */
    public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
    {
        preloads.add(new Preloadable.Model(model));
    }

    /**
     * Creates a sprite implementation for this configuration.
     */
    public abstract EffectSprite.Implementation createImplementation (
        TudeyContext ctx, Scope scope, Effect effect);
}
