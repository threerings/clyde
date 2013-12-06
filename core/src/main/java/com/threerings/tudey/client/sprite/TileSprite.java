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

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.math.Transform3D;

import com.threerings.opengl.model.Model;
import com.threerings.opengl.scene.Scene;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.RectangleElement;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.util.Coord;
import com.threerings.tudey.util.TudeyContext;

import static com.threerings.tudey.Log.log;

/**
 * Represents a tile.
 */
public class TileSprite extends EntrySprite
    implements ConfigUpdateListener<TileConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Returns the model for this implementation, or <code>null</code> for none.
         */
        public Model getModel ()
        {
            return null;
        }

        /**
         * Returns the sprite's floor flags.
         */
        public int getFloorFlags ()
        {
            return 0x0;
        }

        /**
         * Updates the implementation to match the tile state.
         */
        public void update (TileEntry entry)
        {
            // nothing by default
        }

        @Override
        public String getScopeName ()
        {
            return "impl";
        }
    }

    /**
     * An original implementation.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (TudeyContext ctx, Scope parentScope, TileConfig.Original config)
        {
            super(parentScope);
            _ctx = ctx;
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (TileConfig.Original config)
        {
            // unmerge, set the config, then remerge/update
            maybeUnmerge();
            _config = config;
            TileSprite parent = (TileSprite)_parentScope;
            if (parent._entry != null) {
                updateModel(parent._entry);
            }

            // update the footprint
            boolean selected = parent.isSelected();
            if (selected && _footprint == null) {
                _footprint = new RectangleElement(_ctx, true);
                _footprint.getColor().set(SELECTED_COLOR);
                _scene.add(_footprint);
            } else if (!selected && _footprint != null) {
                _scene.remove(_footprint);
                _footprint = null;
            }
        }

        @Override
        public Model getModel ()
        {
            return _model;
        }

        @Override
        public int getFloorFlags ()
        {
            return _config.floorFlags;
        }

        @Override
        public void update (TileEntry entry)
        {
            maybeUnmerge();
            updateModel(entry);

            if (_footprint != null) {
                entry.getRegion(_config, _footprint.getRegion());
                _footprint.setElevation(entry.elevation);
                _footprint.updateBounds();
            }
        }

        @Override
        public void dispose ()
        {
            super.dispose();
            if (!maybeUnmerge()) {
                _scene.remove(_model);
            }
            if (_footprint != null) {
                _scene.remove(_footprint);
            }
        }

        /**
         * Ensures that the model is added to the scene and up-to-date.
         */
        protected void updateModel (TileEntry entry)
        {
            if (_model == null) {
                if (maybeMerge()) {
                    return;
                }
                _scene.add(_model = new Model(_ctx));
                _model.setUserObject(_parentScope);
            }
            entry.getTransform(_config, _model.getLocalTransform());
            _model.setConfig(_config.model);
            _model.updateBounds();
        }

        /**
         * Merges the model if appropriate.
         */
        protected boolean maybeMerge ()
        {
            TileSprite parent = (TileSprite)_parentScope;
            if (!(parent._view.canMerge() && _config.isMergeable(_ctx.getConfigManager()))) {
                return false;
            }
            TileEntry entry = parent._entry;
            Coord location = entry.getLocation();
            Transform3D transform = new Transform3D();
            entry.getTransform(_config, transform);
            if ((_model = parent._view.maybeMerge(location.x, location.y, _config.model,
                    transform, _config.floorFlags)) == null) {
                return false;
            }
            _mergeTransform = transform;
            return true;
        }

        /**
         * Unmerges the model if previously merged.
         */
        protected boolean maybeUnmerge ()
        {
            if (_mergeTransform == null) {
                return false;
            }
            TileSprite parent = (TileSprite)_parentScope;
            Coord location = parent._entry.getLocation();
            if (!parent._view.unmerge(location.x, location.y, _config.model, _mergeTransform)) {
                log.warning("Failed to unmerge static model.", "entry", parent._entry);
            }
            _mergeTransform = null;
            _model = null;
            return true;
        }

        /** The renderer context. */
        protected TudeyContext _ctx;

        /** The tile configuration. */
        protected TileConfig.Original _config;

        /** The model. */
        protected Model _model;

        /** The tile footprint. */
        protected RectangleElement _footprint;

        /** The transform under which we merged, if any. */
        protected Transform3D _mergeTransform;

        /** The scene to which we add our model. */
        @Bound
        protected Scene _scene;
    }

    /**
     * Creates a new tile sprite.
     */
    public TileSprite (TudeyContext ctx, TudeySceneView view, TileEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<TileConfig> event)
    {
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public int getFloorFlags ()
    {
        return _impl.getFloorFlags();
    }

    @Override
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override
    public void update (Entry entry)
    {
        TileEntry tentry = (TileEntry)entry;
        setConfig(tentry.tile);
        _impl.update(_entry = tentry);
    }

    @Override
    public Model getModel ()
    {
        return _impl.getModel();
    }

    @Override
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
        updateFromConfig();
        _impl.update(_entry);
    }

    @Override
    public void dispose ()
    {
        super.dispose();
        _impl.dispose();
        if (_config != null) {
            _config.removeListener(this);
        }
    }

    /**
     * Sets the configuration of this tile.
     */
    protected void setConfig (ConfigReference<TileConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(TileConfig.class, ref));
    }

    /**
     * Sets the configuration of this tile.
     */
    protected void setConfig (TileConfig config)
    {
        if (_config == config) {
            return;
        }
        if (_config != null) {
            _config.removeListener(this);
        }
        if ((_config = config) != null) {
            _config.addListener(this);
        }
        updateFromConfig();
    }

    /**
     * Updates the tile to match its new or modified configuration.
     */
    protected void updateFromConfig ()
    {
        TileConfig.Original original = (_config == null) ?
            null : _config.getOriginal(_ctx.getConfigManager());
        original = (original == null) ? TileConfig.NULL_ORIGINAL : original;
        Implementation nimpl = original.getSpriteImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The scene entry. */
    protected TileEntry _entry;

    /** The tile configuration. */
    protected TileConfig _config = INVALID_CONFIG;

    /** The tile implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An invalid config used to force an initial update. */
    protected static TileConfig INVALID_CONFIG = new TileConfig();

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
