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

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.FloatMath;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.sprite.ActorSprite;
import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of an actor.
 */
public class ActorConfig extends ParameterizedConfig
{
    /**
     * Contains the actual implementation of the actor.
     */
    @EditorTypes({
        Original.class, Mobile.class, Pawn.class, Agent.class, Derived.class })
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
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the actor's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract ActorSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl);

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
        /** The type of sprite to use for the actor. */
        @Editable
        public ActorSpriteConfig sprite = new ActorSpriteConfig.Default();

        /** The shape of the actor. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        /** Tags used to identify the actor within the scene. */
        @Editable
        public String[] tags = new String[0];

        /** The actor's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /** Whether or not the actor should be used as a default entrance. */
        @Editable(hgroup="d")
        public boolean defaultEntrance;

        /** If non-zero, the spawn point will be adjusted so that the actor doesn't collide with
         * anything that matches the mask (if possible). */
        @Editable(hgroup="d")
        public int spawnMask;

        /** Determines which collision categories the actor belongs to. */
        @Editable(hgroup="c")
        public int collisionFlags = 0x01;

        /** Determines which collision categories the actor collides with. */
        @Editable(hgroup="c")
        public int collisionMask = 0x01;

        /**
         * Returns the name of the server-side logic class to use for the actor.
         */
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.ActorLogic";
        }

        /**
         * Adds the resources to preload for this actor into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            sprite.getPreloads(cfgmgr, preloads);
            for (HandlerConfig handler : handlers) {
                handler.action.getPreloads(cfgmgr, preloads);
            }
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public ActorSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            return sprite.getImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            sprite.invalidate();
            shape.invalidate();
            for (HandlerConfig handler : handlers) {
                handler.invalidate();
            }
        }
    }

    /**
     * Base class for mobile implementations.
     */
    public static class Mobile extends Original
    {
        /** The speed at which the actor (normally) travels. */
        @Editable(min=0, step=0.01, hgroup="s")
        public float speed = 6f;

        /** If true, set the actor in motion after spawning. */
        @Editable(hgroup="s")
        public boolean moving;

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.MobileLogic";
        }
    }

    /**
     * Implementation for user-controlled actors.
     */
    public static class Pawn extends Mobile
    {
        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.PawnLogic";
        }
    }

    /**
     * Implementation for computer-controlled actors.
     */
    public static class Agent extends Mobile
    {
        /** The agent's turn rate. */
        @Editable(min=0.0, scale=Math.PI/180.0)
        public float turnRate = FloatMath.PI;

        /** The agent's behavior. */
        @Editable
        public BehaviorConfig behavior = new BehaviorConfig.Idle();

        @Override // documentation inherited
        public String getLogicClassName ()
        {
            return "com.threerings.tudey.server.logic.AgentLogic";
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            super.invalidate();
            behavior.invalidate();
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The actor reference. */
        @Editable(nullable=true)
        public ConfigReference<ActorConfig> actor;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(ActorConfig.class, actor);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            ActorConfig config = cfgmgr.getConfig(ActorConfig.class, actor);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public ActorSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
        {
            ActorConfig config = ctx.getConfigManager().getConfig(ActorConfig.class, actor);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual actor implementation. */
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
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the actor's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public ActorSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, ActorSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
