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
import com.threerings.tudey.client.util.ShapeElement;
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
        public Implementation (Scope parentScope)
        {
            super(parentScope);
        }

        /**
         * Updates the cursor state.
         */
        public void update (PlaceableEntry entry)
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
        public Original (GlContext ctx, Scope parentScope, PlaceableConfig.Original config)
        {
            super(parentScope);
            _model = new Model(ctx);
            _model.setParentScope(this);
            _model.setRenderScheme(RenderScheme.TRANSLUCENT);
            _model.setColorState(new ColorState());
            _model.getColorState().getColor().set(0.5f, 0.5f, 0.5f, 0.45f);
            _footprint = new ShapeElement(ctx);
            _footprint.getColor().set(Color4f.GREEN);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Original config)
        {
            _model.setConfig(config.model);
            _footprint.setConfig(config.shape, true);
        }

        @Override // documentation inherited
        public void update (PlaceableEntry entry)
        {
            _model.setLocalTransform(entry.transform);
            _footprint.setTransform(entry.transform);
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

        /** The model. */
        protected Model _model;

        /** The footprint. */
        protected ShapeElement _footprint;
    }

    /**
     * Creates a new placeable cursor.
     */
    public PlaceableCursor (GlContext ctx, TudeySceneView view, PlaceableEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    /**
     * Updates the cursor with new entry state.
     */
    public void update (PlaceableEntry entry)
    {
        setConfig((_entry = entry).placeable);
        _impl.update(_entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PlaceableConfig> event)
    {
        updateFromConfig();
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
        nimpl = (nimpl == null) ? NULL_IMPLEMENTATION : nimpl;
        if (_impl != nimpl) {
            _impl.dispose();
            _impl = nimpl;
        }
    }

    /** The prototype entry. */
    protected PlaceableEntry _entry;

    /** The placeable config. */
    protected PlaceableConfig _config;

    /** The cursor implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
