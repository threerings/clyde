//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2009 Three Rings Design, Inc.
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

import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.Tuple;

import com.threerings.config.ConfigEvent;
import com.threerings.config.ConfigReference;
import com.threerings.config.ConfigUpdateListener;
import com.threerings.expr.Bound;
import com.threerings.expr.ExpressionDefinition;
import com.threerings.expr.Scope;
import com.threerings.expr.SimpleScope;
import com.threerings.expr.Updater;

import com.threerings.opengl.model.Model;
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
     * A variable definer implementation.
     */
    public static class Definer extends Implementation
        implements TudeySceneView.TickParticipant
    {
        /**
         * Creates a new definer implementation.
         */
        public Definer (TudeyContext ctx, Scope parentScope, SceneGlobalConfig.Definer config)
        {
            super(ctx, parentScope);
            setConfig(config);
            _view.addTickParticipant(this);
        }

        /**
         * (Re)configures the implementation.
         */
        public void setConfig (SceneGlobalConfig.Definer config)
        {
            ArrayList<Tuple<String, Object>> definitions = Lists.newArrayList();
            ArrayList<Updater> updaters = Lists.newArrayList();
            for (ExpressionDefinition definition : config.definitions) {
                definitions.add(Tuple.newTuple(
                    definition.name, definition.getValue(this, updaters)));
            }
            @SuppressWarnings("unchecked") Tuple<String, Object>[] array =
                (Tuple<String, Object>[])new Tuple[definitions.size()];
            _definitions = definitions.toArray(array);
            _updaters = updaters.toArray(new Updater[updaters.size()]);
        }

        // documentation inherited from interface TickParticipant
        public boolean tick (int delayedTime)
        {
            for (Updater updater : _updaters) {
                updater.update();
            }
            return true;
        }

        @Override // documentation inherited
        public void dispose ()
        {
            super.dispose();
            _view.removeTickParticipant(this);
        }

        /** The definitions. */
        protected Tuple<String, Object>[] _definitions;

        /** Updaters for the definitions. */
        protected Updater[] _updaters;

        /** The view to which we add our definitions. */
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
