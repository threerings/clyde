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
import com.threerings.config.ConfigReferenceSet;
import com.threerings.config.ParameterizedConfig;
import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.editor.Strippable;
import com.threerings.export.Exportable;
import com.threerings.expr.Scope;
import com.threerings.math.Transform3D;
import com.threerings.util.DeepObject;
import com.threerings.util.DeepOmit;

import com.threerings.opengl.gui.util.Rectangle;
import com.threerings.opengl.model.config.ModelConfig;
import com.threerings.opengl.model.config.StaticConfig;
import com.threerings.opengl.model.config.StaticSetConfig;
import com.threerings.opengl.util.Preloadable;
import com.threerings.opengl.util.PreloadableSet;

import com.threerings.tudey.client.cursor.TileCursor;
import com.threerings.tudey.client.sprite.TileSprite;
import com.threerings.tudey.util.DirectionUtil;
import com.threerings.tudey.util.TudeyContext;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * The configuration of a tile.
 */
public class TileConfig extends ParameterizedConfig
{
    /** Used when we can't resolve the tile's underlying original implementation. */
    public static final Original NULL_ORIGINAL = new Original("editor/error/model.dat");

    /**
     * Contains the actual implementation of the tile.
     */
    @EditorTypes({ Original.class, Derived.class })
    public static abstract class Implementation extends DeepObject
        implements Exportable
    {
        @Deprecated
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

        /**
         * Invalidates any cached data.
         */
        public void invalidate ()
        {
            // nothing by default
        }
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

        /** Whether or not the tile should be used as a default entrance. */
        @Editable(hgroup="d")
        @Strippable
        public boolean defaultEntrance;

        /** The model to use for the tile. */
        @Editable(nullable=true)
        public ConfigReference<ModelConfig> model;

        /** The tile's collision flags. */
        @Editable(width=3)
        public int[][] collisionFlags = new int[][] { { 0x01 } };

        /** The tile's direction flags. */
        @Editable(width=3)
        public int[][] directionFlags = new int[][] { { 0x00 } };

        /** The tile's floor flags. */
        @Editable(editor="mask", mode="floor", hgroup="f")
        public int floorFlags = 0x01;

        /** Allows control over whether the tile can be merged with others. */
        @Editable(hgroup="f")
        public boolean mergeable = true;

        /** Tags used to identify the tile within the scene. */
        @Editable
        @Strippable
        public TagConfig tags = new TagConfig();

        /** The tile's event handlers. */
        @Editable
        public HandlerConfig[] handlers = new HandlerConfig[0];

        /**
         * Default constructor.
         */
        public Original ()
        {
        }

        /**
         * Creates an implementation with the specified model.
         */
        public Original (String model)
        {
            this.model = new ConfigReference<ModelConfig>(model);
        }

        /**
         * Checks whether we can merge this tile's model with others in the scene.
         */
        public boolean isMergeable (ConfigManager cfgmgr)
        {
            if (!(mergeable && getLogicClassName() == null)) {
                return false;
            }
            if (_modelStatic == null) {
                ModelConfig config = cfgmgr.getConfig(ModelConfig.class, model);
                ModelConfig.Implementation original =
                    (config == null) ? null : config.getOriginal();
                _modelStatic = (original instanceof StaticConfig ||
                    original instanceof StaticSetConfig);
            }
            return _modelStatic;
        }

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
         * Returns the collision flags for the given location for a tile at the specified
         * coordinates.
         */
        public int getCollisionFlags (int x, int y, int rotation, int tx, int ty)
        {
            return getRotatedValue(collisionFlags, x, y, rotation, tx, ty);
        }

        /**
         * Returns the direction flags for the given location for a tile at the specified
         * coordinates.
         */
        public int getDirectionFlags (int x, int y, int rotation, int tx, int ty)
        {
            return DirectionUtil.rotateCardinal(
                    getRotatedValue(directionFlags, x, y, rotation, tx, ty), rotation);
        }

        /**
         * Returns the name of the server-side logic class to use for the tile, or
         * <code>null</code> for none.
         */
        public String getLogicClassName ()
        {
            return (tags.getLength() == 0 && handlers.length == 0 && !defaultEntrance) ? null :
                "com.threerings.tudey.server.logic.EntryLogic";
        }

        /**
         * Adds the resources to preload for this tile into the provided set.
         */
        public void getPreloads (ConfigManager cfgmgr, PreloadableSet preloads)
        {
            preloads.add(new Preloadable.Model(model));
            for (HandlerConfig handler : handlers) {
                handler.getPreloads(cfgmgr, preloads);
            }
        }

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            return this;
        }

        @Override
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

        @Override
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

        @Override
        public void invalidate ()
        {
            for (HandlerConfig handler : handlers) {
                handler.invalidate();
            }
            _modelStatic = null;
        }

        /**
         * Returns the value in the 2D array based on the rotated coordinates.
         */
        protected int getRotatedValue (int[][] values, int x, int y, int rotation, int tx, int ty)
        {
            if (values.length == 0) {
                return 0;
            }
            int fx, fy;
            switch (rotation) {
                default:
                case 0:
                    fx = tx - x;
                    fy = (height - 1) - (ty - y);
                    break;
                case 1:
                    fx = ty - y;
                    fy = tx - x;
                    break;
                case 2:
                    fx = (width - 1) - (tx - x);
                    fy = ty - y;
                    break;
                case 3:
                    fx = (width - 1) - (ty - y);
                    fy = (height - 1) - (tx - x);
                    break;
            }
            int[] row = values[fy % values.length];
            return (row.length == 0) ? 0 : row[fx % row.length];
        }

        /** Cached flag indicating whether or not the model is static. */
        @DeepOmit
        protected transient Boolean _modelStatic;
    }

    /**
     * A derived implementation.
     */
    public static class Derived extends Implementation
    {
        /** The tile reference. */
        @Editable(nullable=true)
        public ConfigReference<TileConfig> tile;

        @Override
        public Original getOriginal (ConfigManager cfgmgr)
        {
            TileConfig config = cfgmgr.getConfig(TileConfig.class, tile);
            return (config == null) ? null : config.getOriginal(cfgmgr);
        }

        @Override
        public TileCursor.Implementation getCursorImplementation (
            TudeyContext ctx, Scope scope, TileCursor.Implementation impl)
        {
            TileConfig config = ctx.getConfigManager().getConfig(TileConfig.class, tile);
            return (config == null) ? null : config.getCursorImplementation(ctx, scope, impl);
        }

        @Override
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

    @Override
    protected void fireConfigUpdated ()
    {
        // invalidate the implementation
        implementation.invalidate();
        super.fireConfigUpdated();
    }
}
