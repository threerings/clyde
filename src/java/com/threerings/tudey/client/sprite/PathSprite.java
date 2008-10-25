//
// $Id$

package com.threerings.tudey.client.sprite;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.opengl.util.GlContext;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.PathConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.PathEntry;

/**
 * Represents a path entry.
 */
public class PathSprite extends EntrySprite
    implements ConfigUpdateListener<PathConfig>
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
         * Updates the implementation to match the path state.
         */
        public void update (PathEntry entry)
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
        public Original (GlContext ctx, Scope parentScope, PathConfig.Original config)
        {
            super(parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (PathConfig.Original config)
        {
            _config = config;
        }

        @Override // documentation inherited
        public void update (PathEntry entry)
        {
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
        }

        /** The path configuration. */
        protected PathConfig.Original _config;
    }

    /**
     * Creates a new path sprite.
     */
    public PathSprite (GlContext ctx, TudeySceneView view, PathEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<PathConfig> event)
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
        setConfig((_entry = (PathEntry)entry).path);
        _impl.update(_entry);
    }

    @Override // documentation inherited
    public void setSelected (boolean selected)
    {
        super.setSelected(selected);
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
     * Sets the configuration of this path.
     */
    protected void setConfig (ConfigReference<PathConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(PathConfig.class, ref));
    }

    /**
     * Sets the configuration of this path.
     */
    protected void setConfig (PathConfig config)
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
     * Updates the path to match its new or modified configuration.
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
    protected PathEntry _entry;

    /** The path configuration. */
    protected PathConfig _config;

    /** The path implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null) {
    };
}
