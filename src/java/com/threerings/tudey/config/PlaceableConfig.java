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

import com.samskivert.util.StringUtil;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.cursor.PlaceableCursor;
import com.threerings.tudey.client.sprite.PlaceableSprite;
import com.threerings.tudey.shape.config.ShapeConfig;
import com.threerings.tudey.util.TudeyContext;

/**
 * The configuration of a placeable object.
 */
public class PlaceableConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the placeable's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Marker();

    /**
     * Contains the actual implementation of the placeable.
     */
    @EditorTypes({ Prop.class, ClickableProp.class, Marker.class, Derived.class })
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
         * Creates or updates a cursor implementation for this configuration.
         *
         * @param scope the placeable's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PlaceableCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PlaceableCursor.Implementation impl);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the placeable's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl);

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
    public static abstract class Original extends Implementation
    {
        /** Whether or not the placeable should be used as a default entrance. */
        @Editable
        public boolean defaultEntrance;

        /** The model to use to represent the placeable on the client. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The shape of the placeable. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        /** Tags used to identify the placeable within the scene. */
        @Editable
        public String[] tags = new String[0];

        /** The area's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /**
         * Returns the placeable's collision flags.
         */
        public int getCollisionFlags ()
        {
            return 0;
        }

        /**
         * Returns the name of the server-side logic class to use for the placeable, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return (tags.length == 0 && handlers.length == 0 && !defaultEntrance) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
        }

        /**
         * Adds the resources to preload for this placeable into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
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
        public PlaceableCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PlaceableCursor.Implementation impl)
        {
            if (impl instanceof PlaceableCursor.Original) {
                ((PlaceableCursor.Original)impl).setConfig(this);
            } else {
                impl = new PlaceableCursor.Original(ctx, scope, this);
            }
            return impl;
        }

        @Override // documentation inherited
        public void invalidate ()
        {
            shape.invalidate();
        }
    }

    /**
     * A prop implementation.
     */
    public static class Prop extends Original
    {
        /** The prop's collision flags. */
        @Editable
        public int collisionFlags = 0x01;

        @Override // documentation inherited
        public int getCollisionFlags ()
        {
            return collisionFlags;
        }

        @Override // documentation inherited
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            super.getPreloads(cfgmgr, preloads);
            preloads.add(new Preloadable.Model(model));
        }

        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
            if (impl != null && impl.getClass() == PlaceableSprite.Prop.class) {
                ((PlaceableSprite.Prop)impl).setConfig(this);
            } else {
                impl = new PlaceableSprite.Prop(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * Clickable prop implementation.
     */
    public static class ClickableProp extends Prop
    {
        /** The color to use when not hovering over the prop. */
        @Editable(mode="alpha", hgroup="c")
        public Color4f defaultColor = new Color4f(0.5f, 0.5f, 0.5f, 1f);

        /** The color to use when hovering over the prop. */
        @Editable(mode="alpha", hgroup="c")
        public Color4f hoverColor = new Color4f(Color4f.WHITE);

        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
            if (impl != null && impl.getClass() == PlaceableSprite.ClickableProp.class) {
                ((PlaceableSprite.ClickableProp)impl).setConfig(this);
            } else {
                impl = new PlaceableSprite.ClickableProp(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A marker implementation.
     */
    public static class Marker extends Original
    {
        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
            if (!ScopeUtil.resolve(scope, "markersVisible", false)) {
                return null;
            }
            if (impl instanceof PlaceableSprite.Marker) {
                ((PlaceableSprite.Marker)impl).setConfig(this);
            } else {
                impl = new PlaceableSprite.Marker(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The placeable reference. */
        @Editable(nullable=true)
        public ConfigReference<PlaceableConfig> placeable;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(PlaceableConfig.class, placeable);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            PlaceableConfig config = cfgmgr.getConfig(PlaceableConfig.class, placeable);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public PlaceableCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, PlaceableCursor.Implementation impl)
        {
            PlaceableConfig config = ctx.getConfigManager().getConfig(
                PlaceableConfig.class, placeable);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
            PlaceableConfig config = ctx.getConfigManager().getConfig(
                PlaceableConfig.class, placeable);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual placeable implementation. */
    @Editable
    public Implementation implementation = new Prop();

    /**
     * Returns a reference to the config's underlying original implementation.
     */
    public Original getOriginal (ConfigManager cfgmgr)
    {
        return implementation.getOriginal(cfgmgr);
    }

    /**
     * Creates or updates a cursor implementation for this configuration.
     *
     * @param scope the placeable's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PlaceableCursor.Implementation getCursorImplementation (
        TudeyContext ctx, Scope scope, PlaceableCursor.Implementation impl)
    {
        return implementation.getCursorImplementation(ctx, scope, impl);
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the placeable's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PlaceableSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
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
