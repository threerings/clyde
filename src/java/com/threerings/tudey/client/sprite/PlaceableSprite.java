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
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;

/**
 * Represents a placeable entry.
 */
public class PlaceableSprite extends EntrySprite
    implements ConfigUpdateListener<PlaceableConfig>
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
         * Updates the implementation to match the entry state.
         */
        public void update (PlaceableEntry entry)
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
     * Superclass of the original implementations.
     */
    public static abstract class Original extends Implementation
    {
        /**
         * Creates a new implementation.
         */
        public Original (GlContext ctx, Scope parentScope)
        {
            super(parentScope);
            _scene.add(_model = new Model(ctx));
            _model.setUserObject((PlaceableSprite)parentScope);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PlaceableConfig.Original config)
        {
            _model.setConfig(config.model);
        }

        /**
         * Updates the implementation to match the entry state.
         */
        public void update (PlaceableEntry entry)
        {
            _model.setLocalTransform(entry.transform);
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            _scene.remove(_model);
        }

        /** The model. */
        protected Model _model;

        /** The scene to which we add our model. */
        @Bound
        protected Scene _scene;
    }

    /**
     * A prop implementation.
     */
    public static class Prop extends Original
    {
        /**
         * Creates a new prop implementation.
         */
        public Prop (GlContext ctx, Scope parentScope, PlaceableConfig.Prop config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }
    }

    /**
     * A marker implementation.
     */
    public static class Marker extends Original
    {
        /**
         * Creates a new marker implementation.
         */
        public Marker (GlContext ctx, Scope parentScope, PlaceableConfig.Marker config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }
    }

    /**
     * Creates a new placeable sprite.
     */
    public PlaceableSprite (GlContext ctx, TudeySceneView view, PlaceableEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PlaceableConfig> event)
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
        _entry = (PlaceableEntry)entry;
        setConfig(_entry.placeable);
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
     * Sets the configuration of this placeable.
     */
    protected void setConfig (ConfigReference<PlaceableConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(PlaceableConfig.class, ref));
    }

    /**
     * Sets the configuration of this placeable.
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
     * Updates the placeable to match its new or modified configuration.
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
    protected PlaceableEntry _entry;

    /** The placeable configuration. */
    protected PlaceableConfig _config;

    /** The placeable implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
