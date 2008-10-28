//
// $Id$

package com.threerings.tudey.client.cursor;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.Bound;
import com.threerings.expr.Scoped;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.compositor.RenderScheme;
import com.threerings.opengl.mod.Model;
import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.state.ColorState;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.client.util.RectangleElement;
import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;

/**
 * A cursor for tiles.
 */
public class TileCursor extends EntryCursor
    implements ConfigUpdateListener<TileConfig>
{
    /**
     * The actual cursor implementation.
     */
    public static abstract class Implementation extends SimpleScope
        implements Tickable, Renderable
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Updates the cursor state.
         */
        public void update (TileEntry entry)
        {
            // nothing by default
        }

        // documentation inherited from interface Tickable
        public void tick (float elapsed)
        {
            // nothing by default
        }

        // documentation inherited from interface Renderable
        public void enqueue ()
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
     * The original implementation.
     */
    public static class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (GlContext ctx, Scope parentScope, TileConfig.Original config)
        {
            super(parentScope);
            _model = new Model(ctx);
            _model.setParentScope(this);
            _model.setRenderScheme(RenderScheme.TRANSLUCENT);
            _model.setColorState(new ColorState());
            _model.getColorState().getColor().set(0.5f, 0.5f, 0.5f, 0.45f);

            _footprint = new RectangleElement(ctx, true);
            _footprint.getColor().set(Color4f.GREEN);
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

            entry.getRegion(_config, _footprint.getRegion());
            _footprint.setElevation(entry.elevation);
        }

        @Override // documentation inherited
        public void tick (float elapsed)
        {
            _model.tick(elapsed);
        }

        @Override // documentation inherited
        public void enqueue ()
        {
            _model.enqueue();
            _footprint.enqueue();
        }

        /** The tile configuration. */
        protected TileConfig.Original _config;

        /** The model. */
        protected Model _model;

        /** The tile footprint. */
        protected RectangleElement _footprint;
    }

    /**
     * Creates a new tile cursor.
     */
    public TileCursor (GlContext ctx, TudeySceneView view, TileEntry entry)
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
    public void tick (float elapsed)
    {
        _impl.tick(elapsed);
    }

    @Override // documentation inherited
    public void enqueue ()
    {
        _impl.enqueue();
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
     * Sets the configuration of the tile.
     */
    protected void setConfig (ConfigReference<TileConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(TileConfig.class, ref));
    }

    /**
     * Sets the configuration of the tile.
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
     * Updates this cursor to match its configuration.
     */
    protected void updateFromConfig ()
    {
        Implementation nimpl = (_config == null) ?
            null : _config.getCursorImplementation(_ctx, this, _impl);
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The prototype entry. */
    protected TileEntry _entry;

    /** The tile config. */
    protected TileConfig _config;

    /** The cursor implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
