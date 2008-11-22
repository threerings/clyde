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

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.SceneGlobalConfig;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;
import com.threerings.tudey.util.TudeyContext;

/**
 * Represents a global entry.
 */
public class GlobalSprite extends EntrySprite
    implements ConfigUpdateListener<SceneGlobalConfig>
{
    /**
     * The actual sprite implementation.
     */
    public static abstract class Implementation extends SimpleScope
    {
        /**
         * Creates a new implementation.
         */
        public Implementation (TudeyContext ctx, Scope parentScope)
        {
            super(parentScope);
            _ctx = ctx;
        }

        @Override // documentation inherited
        public String getScopeName ()
        {
            return "impl";
        }

        /** The renderer context. */
        protected TudeyContext _ctx;
    }

    /**
     * An environment model implementation.
     */
    public static class EnvironmentModel extends Implementation
    {
        /**
         * Creates a new environment model implementation.
         */
        public EnvironmentModel (
            TudeyContext ctx, Scope parentScope, SceneGlobalConfig.EnvironmentModel config)
        {
            super(ctx, parentScope);
            _scene.add(_model = new Model(ctx));
            _model.setUserObject((GlobalSprite)parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SceneGlobalConfig.EnvironmentModel config)
        {
            _model.setConfig(config.model);
            _model.setLocalTransform(config.transform);
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
     * Creates a new global sprite.
     */
    public GlobalSprite (TudeyContext ctx, TudeySceneView view, GlobalEntry entry)
    {
        super(ctx, view);
        update(entry);
    }

    // documentation inherited from interface ConfigUpdateListener
    public void configUpdated (ConfigEvent<SceneGlobalConfig> event)
    {
        updateFromConfig();
    }

    @Override // documentation inherited
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override // documentation inherited
    public void update (Entry entry)
    {
        _entry = (GlobalEntry)entry;
        setConfig(_entry.sceneGlobal);
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
     * Sets the configuration of this global.
     */
    protected void setConfig (ConfigReference<SceneGlobalConfig> ref)
    {
        setConfig(_ctx.getConfigManager().getConfig(SceneGlobalConfig.class, ref));
    }

    /**
     * Sets the configuration of this global.
     */
    protected void setConfig (SceneGlobalConfig config)
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
     * Updates the global to match its new or modified configuration.
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
    protected GlobalEntry _entry;

    /** The global configuration. */
    protected SceneGlobalConfig _config;

    /** The global implementation. */
    protected Implementation _impl = NULL_IMPLEMENTATION;

    /** An implementation that does nothing. */
    protected static final Implementation NULL_IMPLEMENTATION = new Implementation(null, null) {
    };
}
