//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.scene.Scene;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

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
         * Updates the implementation to match the tile state.
         */
        public void update (TileEntry entry)
        {
            // nothing by default
        }

        @Override // documentation inherited
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
        public Original (GlContext ctx, Scope parentScope, TileConfig.Original config)
        {
            super(parentScope);
            _scene.add(_model = new Model(ctx));
            _model.setUserObject(parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (TileConfig.Original config)
        {
            _model.setConfig((_config = config).model);
        }

        @Override // documentation inherited
        public void update (TileEntry entry)
        {
            entry.getTransform(_config, _model.getLocalTransform());
            _model.updateBounds();
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            _scene.remove(_model);
        }

        /** The tile configuration. */
        protected TileConfig.Original _config;

        /** The model. */
        protected Model _model;

        /** The scene to which we add our model. */
        @Bound
        protected Scene _scene;
    }

    /**
     * Creates a new tile sprite.
     */
    public TileSprite (GlContext ctx, TudeySceneView view, TileEntry entry)
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

    @Override // documentation inherited
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override // documentation inherited
    public void update (Entry entry)
    {
        setConfig((_entry = (TileEntry)entry).tile);
        _impl.update(_entry);
    }

    @Override // documentation inherited
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
        Implementation nimpl = (_config == null) ?
            null : _config.getSpriteImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The scene entry. */
    protected TileEntry _entry;

    /** The tile configuration. */
    protected TileConfig _config;

    /** The tile implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
