//
// $Id$

package com.threerings.tudey.client.cursor;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;

import com.threerings.opengl.mod.Model;
import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Renderable;
import com.threerings.opengl.util.Tickable;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;

/**
 * A cursor for a placeable object.
 */
public class PlaceableCursor extends Cursor
    implements ConfigUpdateListener<PlaceableConfig>
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
        public Implementation (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
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

        /** The renderer context. */
        protected GlContext _ctx;
    }

    /**
     * The normal implementation.
     */
    public static class Normal extends Implementation
    {
        /**
         * Creates a new normal implementation.
         */
        public Normal (GlContext ctx, Scope parentScope, PlaceableConfig.Original config)
        {
            super(ctx, parentScope);
            _model = new Model(ctx);
            _model.setParentScope(this);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Original config)
        {
            _model.setConfig(config.model);
        }

        @Override // documentation inherited
        public void tick (float elapsed)
        {
            _model.tick(elapsed);
            _model.updateBounds();
        }

        @Override // documentation inherited
        public void enqueue ()
        {
            _model.enqueue();
        }

        /** The model. */
        protected Model _model;
    }

    /**
     * Creates a new placeable cursor.
     */
    public PlaceableCursor (GlContext ctx, TudeySceneView view, PlaceableEntry entry)
    {
        super(ctx, view);
        _entry = entry;
        updateFromEntry();
    }

    /**
     * Updates the cursor from the entry.
     */
    public void updateFromEntry ()
    {
        setConfig(_entry.placeable);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PlaceableConfig> event)
    {
        updateFromConfig();
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
     * Sets the configuration of the placeable.
     */
    protected void setConfig (ConfigReference<PlaceableConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(PlaceableConfig.class, ref));
    }

    /**
     * Sets the configuration of the placeable.
     */
    protected void setConfig (PlaceableConfig config)
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
        _impl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
    }

    /** The prototype entry. */
    protected PlaceableEntry _entry;

    /** The placeable config. */
    protected PlaceableConfig _config;

    /** The cursor implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
    };
}
