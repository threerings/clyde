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

package com.threerings.tudey.server.logic;

import java.util.ArrayList;
import java.util.Map;

import com.threerings.config.ConfigManager;
import com.threerings.config.ConfigReference;
import com.threerings.math.Vector2f;

import com.threerings.opengl.model.config.ModelConfig;

import com.threerings.tudey.config.HandlerConfig;
import com.threerings.tudey.config.PlaceableConfig;
import com.threerings.tudey.config.SceneGlobalConfig;
import com.threerings.tudey.data.EntityKey;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.GlobalEntry;
import com.threerings.tudey.data.TudeySceneModel.PlaceableEntry;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.TudeySceneMetrics;

/**
 * A logic object associated with a scene entry.
 */
public class EntryLogic extends Logic
{
    /**
     * Special camera logic.
     */
    public static class Camera extends EntryLogic
    {
        @Override
        protected void wasAdded ()
        {
            GlobalEntry gentry = (GlobalEntry)_entry;
            SceneGlobalConfig.Camera config =
                (SceneGlobalConfig.Camera)gentry.getConfig(_scenemgr.getConfigManager());
            _scenemgr.setDefaultLocalInterest(TudeySceneMetrics.getLocalInterest(
                config.camera, 4f / 3f));
        }

        @Override
        protected void wasRemoved (boolean endScene)
        {
            _scenemgr.setDefaultLocalInterest(TudeySceneMetrics.getDefaultLocalInterest());
        }
    }

    /**
     * Logic for stateful props.
     */
    public static class StatefulProp extends EntryLogic
    {
        @Override
        public void transfer (Logic source, Map<Object, Object> refs)
        {
            super.transfer(source, refs);
            _actor = (ActorLogic)refs.get(((StatefulProp)source)._actor);
        }

        @Override
        protected void wasAdded ()
        {
            PlaceableEntry pentry = (PlaceableEntry)_entry;
            PlaceableConfig.StatefulProp config =
                (PlaceableConfig.StatefulProp)pentry.getConfig(_scenemgr.getConfigManager());
            if (config.actor != null) {
                _actor = _scenemgr.spawnActor(
                    _scenemgr.getNextTimestamp(), _translation, _rotation, config.actor);
                if (_actor instanceof EntryStateLogic) {
                    ((EntryStateLogic)_actor).setEntry(this);
                }
            }
        }

        @Override
        protected void wasRemoved (boolean endScene)
        {
            if (_actor != null) {
                _actor.destroy(_scenemgr.getNextTimestamp(), this, endScene);
            }
        }

        /** The logic for the state actor. */
        protected ActorLogic _actor;
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, Entry entry)
    {
        super.init(scenemgr);
        _entityKey = new EntityKey.Entry(entry.getKey());
        _entry = entry;
        ConfigManager cfgmgr = scenemgr.getConfigManager();
        _translation = entry.getTranslation(cfgmgr);
        _rotation = entry.getRotation(cfgmgr);
        _tags = entry.getTags(cfgmgr);
        _defaultEntrance = entry.isDefaultEntrance(cfgmgr);
        _shape = entry.createShape(cfgmgr);

        // create the handler logic objects
        ArrayList<HandlerLogic> handlers = new ArrayList<HandlerLogic>();
        for (HandlerConfig config : entry.getHandlers(cfgmgr)) {
            HandlerLogic handler = createHandler(config, this);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        _handlers = handlers.toArray(new HandlerLogic[handlers.size()]);

        // give subclasses a chance to set up
        didInit();
    }

    /**
     * Returns a reference to the scene entry corresponding to this logic.
     */
    public Entry getEntry ()
    {
        return _entry;
    }

    /**
     * Notes that the entry has been added to the scene.
     */
    public void added ()
    {
        // notify the handlers
        int timestamp = _scenemgr.getNextTimestamp();
        for (HandlerLogic handler : _handlers) {
            handler.startup(timestamp);
        }

        // give subclasses a chance to initialize
        wasAdded();
    }

    /**
     * Notes that the entry has been removed from the scene.
     *
     * @param endScene Set to true if the scene is being destroyed (will prevent some shutdown
     * handles from running)
     */
    public void removed (boolean endScene)
    {
        // notify the handlers
        int timestamp = _scenemgr.getNextTimestamp();
        for (HandlerLogic handler : _handlers) {
            handler.shutdown(timestamp, this, endScene);
            handler.removed();
        }

        // give subclasses a chance to cleanup
        wasRemoved(endScene);
    }

    @Override
    public String[] getTags ()
    {
        return _tags;
    }

    @Override
    public boolean isDefaultEntrance ()
    {
        return _defaultEntrance;
    }

    @Override
    public boolean isActive ()
    {
        return _scenemgr.getEntryLogic(_entry.getKey()) == this;
    }

    @Override
    public EntityKey getEntityKey ()
    {
        return _entityKey;
    }

    @Override
    public Vector2f getTranslation ()
    {
        return _translation;
    }

    @Override
    public float getRotation ()
    {
        return _rotation;
    }

    @Override
    public Shape getShape ()
    {
        return _shape;
    }

    @Override
    public Vector2f[] getPatrolPath ()
    {
        return _entry.createPatrolPath(_shape);
    }

    @Override
    public ConfigReference<ModelConfig> getModel ()
    {
        return _entry.getModel(_scenemgr.getConfigManager());
    }

    @Override
    public void signal (int timestamp, Logic source, String name)
    {
        for (HandlerLogic handler : _handlers) {
            handler.signal(timestamp, source, name);
        }
    }

    @Override
    public void setVariable (int timestamp, Logic source, String name, Object value)
    {
        super.setVariable(timestamp, source, name, value);
        for (HandlerLogic handler : _handlers) {
            handler.variableChanged(timestamp, source, name);
        }
    }

    @Override
    public void request (int timestamp, PawnLogic source, String name)
    {
        for (HandlerLogic handler : _handlers) {
            handler.request(timestamp, source, name);
        }
    }

    @Override
    public void transfer (Logic source, Map<Object, Object> refs)
    {
        super.transfer(source, refs);

        // transfer the handler state
        HandlerLogic[] shandlers = ((EntryLogic)source)._handlers;
        for (int ii = 0; ii < _handlers.length; ii++) {
            _handlers[ii].transfer(shandlers[ii], refs);
        }
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /**
     * Override to perform custom startup.
     */
    protected void wasAdded ()
    {
        // nothing by default
    }

    /**
     * Override to perform custom cleanup.
     */
    protected void wasRemoved (boolean endScene)
    {
        // nothing by default
    }

    /** The entity key. */
    protected EntityKey.Entry _entityKey;

    /** The scene entry. */
    protected Entry _entry;

    /** The entry's tags. */
    protected String[] _tags;

    /** Whether or not the entry represents a default entrance. */
    protected boolean _defaultEntrance;

    /** The entry's approximate translation. */
    protected Vector2f _translation;

    /** The entry's approximate rotation. */
    protected float _rotation;

    /** The entry's shape. */
    protected Shape _shape;

    /** The entry's patrol path. */
    protected Vector2f[] _patrolPath;

    /** The entry's event handlers. */
    protected HandlerLogic[] _handlers;
}
