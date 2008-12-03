//
// $Id$

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
    @EditorTypes({ Prop.class, Marker.class, Derived.class })
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
        /** The model to use to represent the placeable on the client. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The shape of the placeable. */
        @Editable
        public ShapeConfig shape = new ShapeConfig.Point();

        /** A tag to use to identify the placeable within the scene. */
        @Editable
        public String tag = "";

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
            return (StringUtil.isBlank(tag) && handlers.length == 0) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
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
        public PlaceableSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
            if (impl instanceof PlaceableSprite.Prop) {
                ((PlaceableSprite.Prop)impl).setConfig(this);
            } else {
                impl = new PlaceableSprite.Prop(ctx, scope, this);
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
