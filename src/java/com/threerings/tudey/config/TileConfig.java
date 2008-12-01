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
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.client.cursor.TileCursor;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The configuration of a tile.
 */
public class TileConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the tile's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Original();

    /**
     * Contains the actual implementation of the tile.
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
         * Creates or updates a cursor implementation for this configuration.
         *
         * @param scope the tile's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract TileCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, TileCursor.Implementation impl);

        /**
         * Creates or updates a sprite implementation for this configuration.
         *
         * @param scope the tile's expression scope.
         * @param impl an existing implementation to reuse, if possible.
         * @return either a reference to the existing implementation (if reused), a new
         * implementation, or <code>null</code> if no implementation could be created.
         */
        public abstract TileSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, TileSprite.Implementation impl);
    }

    /**
     * An original tile implementation.
     */
    public static class Original extends Implementation
    {
        /** The width of the tile. */
        @Editable(min=1, hgroup="d")
        public int width = 1;

        /** The height of the tile. */
        @Editable(min=1, hgroup="d")
        public int height = 1;

        /** The model to use for the tile. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** Indicates where the tile is passable. */
        @Editable(width=3)
        public boolean[][] passable = new boolean[][] { { false } };

        /** Indicates where the tile is penetrable. */
        @Editable(width=3)
        public boolean[][] penetrable = new boolean[][] { { false } };

        /**
         * Finds the transform of the tile at the specified coordinates.
         */
        public void getTransform (int x, int y, int elevation, int rotation, Transform3D result)
        {
            TudeySceneMetrics.getTileTransform(width, height, x, y, elevation, rotation, result);
        }

        /**
         * Finds the region covered by the tile at the specified coordinates.
         */
        public void getRegion (int x, int y, int rotation, Rectangle result)
        {
            TudeySceneMetrics.getTileRegion(width, height, x, y, rotation, result);
        }

        /**
         * Returns the width of the tile under the supplied rotation.
         */
        public int getWidth (int rotation)
        {
            return TudeySceneMetrics.getTileWidth(width, height, rotation);
        }

        /**
         * Returns the height of the tile under the supplied rotation.
         */
        public int getHeight (int rotation)
        {
            return TudeySceneMetrics.getTileHeight(width, height, rotation);
        }

        /**
         * Checks whether the tile at the specified coordinate is passable at the given location.
         */
        public boolean isPassable (int x, int y, int rotation, int tx, int ty)
        {
            return true;
        }

        /**
         * Checks whether the tile at the specified coordinates is penetrable at the given location.
         */
        public boolean isPenetrable (int x, int y, int rotation, int tx, int ty)
        {
            return true;
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override // documentation inherited
        public TileCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, TileCursor.Implementation impl)
        {
            if (impl instanceof TileCursor.Original) {
                ((TileCursor.Original)impl).setConfig(this);
            } else {
                impl = new TileCursor.Original(ctx, scope, this);
            }
            return impl;
        }

        @Override // documentation inherited
        public TileSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, TileSprite.Implementation impl)
        {
            if (impl instanceof TileSprite.Original) {
                ((TileSprite.Original)impl).setConfig(this);
            } else {
                impl = new TileSprite.Original(ctx, scope, this);
            }
            return impl;
        }
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The tile reference. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        @Override // documentation inherited
        public void getUpdateReferences (ConfigReferenceSet refs)
        {
            refs.add(TileConfig.class, tile);
        }

        @Override // documentation inherited
        public Original getOriginal (ConfigManager cfgmgr)
        {
            TileConfig config = cfgmgr.getConfig(TileConfig.class, tile);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override // documentation inherited
        public TileCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, TileCursor.Implementation impl)
        {
            TileConfig config = ctx.getConfigManager().getConfig(TileConfig.class, tile);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override // documentation inherited
        public TileSprite.Implementation getSpriteImplementation (
            TudeyContext ctx, Scope scope, TileSprite.Implementation impl)
        {
            TileConfig config = ctx.getConfigManager().getConfig(TileConfig.class, tile);
            return (config == null) ? null : config.getSpriteImplementation(ctx, scope, impl);
        }
    }

    /** The actual tile implementation. */
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
     * Creates or updates a cursor implementation for this configuration.
     *
     * @param scope the tile's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public TileCursor.Implementation getCursorImplementation (
        TudeyContext ctx, Scope scope, TileCursor.Implementation impl)
    {
        return implementation.getCursorImplementation(ctx, scope, impl);
    }

    /**
     * Creates or updates a sprite implementation for this configuration.
     *
     * @param scope the tile's expression scope.
     * @param impl an existing implementation to reuse, if possible.
     * @return either a reference to the existing implementation (if reused), a new
     * implementation, or <code>null</code> if no implementation could be created.
     */
    public TileSprite.Implementation getSpriteImplementation (
        TudeyContext ctx, Scope scope, TileSprite.Implementation impl)
    {
        return implementation.getSpriteImplementation(ctx, scope, impl);
    }

    @Override // documentation inherited
    protected void getUpdateReferences (ConfigReferenceSet refs)
    {
        implementation.getUpdateReferences(refs);
    }
}
