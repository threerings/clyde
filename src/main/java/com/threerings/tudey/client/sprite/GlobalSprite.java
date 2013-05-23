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

import com.threerings.opengl.model.Model;
import com.threerings.opengl.scene.Scene;

import com.threerings.tudey.client.TudeySceneView;
import com.threerings.tudey.config.CameraConfig;
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

        /**
         * Returns the model for this implementation, or <code>null</code> for none.
         */
        public Model getModel ()
        {
            return null;
        }

        @Override
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
            _model.setUserObject(parentScope);
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

        @Override
        public Model getModel ()
        {
            return _model;
        }

        @Override
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
     * A camera implementation.
     */
    public static class Camera extends Implementation
    {
        /**
         * Creates a new environment model implementation.
         */
        public Camera (TudeyContext ctx, Scope parentScope, SceneGlobalConfig.Camera config)
        {
            super(ctx, parentScope);
            setConfig(config);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SceneGlobalConfig.Camera config)
        {
            if (_camcfg != null) {
                _view.removeCameraConfig(_camcfg);
            }
            if (_camcfgs != null) {
                for (CameraConfig cc : _camcfgs) {
                    _view.removeCameraConfig(cc, 0f, null);
                }
            }
            _view.addCameraConfig(_camcfg = config.camera);
            _camcfgs = config.cameras;
            for (CameraConfig cc : _camcfgs) {
                _view.addCameraConfig(cc, 0f, null);
            }
        }

        @Override
        public void dispose ()
        {
            super.dispose();
            _view.removeCameraConfig(_camcfg);
            for (CameraConfig cc : _camcfgs) {
                _view.removeCameraConfig(cc, 0f, null);
            }
        }

        /** The added camera, if any. */
        protected CameraConfig _camcfg;

        /** The added cameras. */
        protected CameraConfig[] _camcfgs;

        /** The scene view. */
        @Bound
        protected TudeySceneView _view;
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

    @Override
    public Entry getEntry ()
    {
        return _entry;
    }

    @Override
    public void update (Entry entry)
    {
        _entry = (GlobalEntry)entry;
        setConfig(_entry.sceneGlobal);
    }

    @Override
    public Model getModel ()
    {
        return _impl.getModel();
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
