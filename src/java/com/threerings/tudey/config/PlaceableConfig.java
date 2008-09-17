//
// $Id$

package com.threerings.tudey.config;

import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.util.DeepObject;

import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.cursor.PlaceableCursor;
import com.threerings.tudey.client.sprite.PlaceableSprite;

/**
 * The configuration of a placeable object.
 */
public class PlaceableConfig extends ParameterizedConfig
{
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
         * Creates or updates a cursor implementation for this configuration.
         *
         * @param scope the placeable's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PlaceableCursor.Implementation getCursorImplementation (
            GlContext ctx, Scope scope, PlaceableCursor.Implementation impl);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the placeable's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract PlaceableSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PlaceableSprite.Implementation impl);
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

        @Override // documentation inherited
        public PlaceableCursor.Implementation getCursorImplementation (
            GlContext ctx, Scope scope, PlaceableCursor.Implementation impl)
        {
            if (impl instanceof PlaceableCursor.Original) {
                ((PlaceableCursor.Original)impl).setConfig(this);
            } else {
                impl = new PlaceableCursor.Original(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A prop implementation.
     */
    public static class Prop extends Original
    {
        /** Whether or not actors can walk through the prop. */
        @Editable(hgroup="p")
        public boolean passable;

        /** Whether or not actors can shoot through the prop. */
        @Editable(hgroup="p")
        public boolean penetrable;

        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PlaceableSprite.Implementation impl)
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
            GlContext ctx, Scope scope, PlaceableSprite.Implementation impl)
        {
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
        public PlaceableCursor.Implementation getCursorImplementation (
            GlContext ctx, Scope scope, PlaceableCursor.Implementation impl)
        {
            PlaceableConfig config = ctx.getConfigManager().getConfig(
                PlaceableConfig.class, placeable);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public PlaceableSprite.Implementation getSpriteImplementation (
            GlContext ctx, Scope scope, PlaceableSprite.Implementation impl)
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
     * Creates or updates a cursor implementation for this configuration.
     *
     * @param scope the placeable's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public PlaceableCursor.Implementation getCursorImplementation (
        GlContext ctx, Scope scope, PlaceableCursor.Implementation impl)
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
        GlContext ctx, Scope scope, PlaceableSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
