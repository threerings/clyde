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

package com.threerings.tudey.server;

import java.util.Iterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.IntMap.IntEntry;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Lifecycle;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneMoveHandler;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.util.UpdateList;

import com.threerings.config.ConfigManager;

import com.threerings.tudey.data.TudeyCodes;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.server.util.SceneTicker;

/**
 * Provides special handling for moving between Tudey scenes.
 */
@Singleton
public class TudeySceneRegistry extends SceneRegistry
    implements TudeyCodes
{
    /**
     * Constructs a Tudey scene registry.
     */
    @Inject public TudeySceneRegistry (
        InvocationManager invmgr, PresentsDObjectMgr omgr, Lifecycle lifecycle)
    {
        super(invmgr);
        _omgr = omgr;

        lifecycle.addComponent(new Lifecycle.InitComponent() {
            public void init () {
                // create the default scene ticker
                _defaultTicker = createDefaultTicker();

                // create the interval to prune the portal mappings
                new Interval(_omgr) {
                    public void expired () {
                        prunePortalMappings();
                    }
                }.schedule(PORTAL_PRUNE_INTERVAL, true);
            }
        });
    }

    /**
     * Forcibly moves a player to a new scene.
     *
     * @param portalKey the key of the destination portal.
     */
    public void moveBody (BodyObject source, int sceneId, Object portalKey)
    {
        // store the portal mapping
        addPortalMapping(source, sceneId, portalKey);
        super.moveBody(source, sceneId);
    }

    /**
     * Resolves a scene for the specified caller.
     */
    public void resolveScene (ClientObject caller, int sceneId, ResolutionListener listener)
    {
        resolveScene(sceneId, listener);
    }

    /**
     * Returns a reference to the default scene ticker.
     */
    public SceneTicker getDefaultTicker ()
    {
        return _defaultTicker;
    }

    @Override
    public void moveTo (
        ClientObject caller, int sceneId, int sceneVer, SceneService.SceneMoveListener listener)
    {
        // look for a stored portal key
        BodyObject body = (BodyObject)caller;
        Object portalKey = removePortalMapping(body, sceneId);
        SceneMoveHandler handler;
        if (portalKey != null) {
            handler = new TudeySceneMoveHandler(_locman, body, sceneVer, portalKey, listener);
        } else {
            handler = new SceneMoveHandler(_locman, body, sceneVer, listener);
        }
        resolveScene(caller, sceneId, handler);
    }

    @Override
    protected void processSuccessfulResolution (
        SceneModel model, UpdateList updates, Object extras)
    {
        // initialize the scene model
        ((TudeySceneModel)model).init(_cfgmgr);

        super.processSuccessfulResolution(model, updates, extras);
    }

    /**
     * Creates the default scene ticker.
     */
    protected SceneTicker createDefaultTicker ()
    {
        return new SceneTicker.EventThread(_omgr, DEFAULT_TICK_INTERVAL);
    }

    /**
     * Adds a portal mapping.
     */
    protected void addPortalMapping (BodyObject source, int sceneId, Object portalKey)
    {
        _portals.put(source.getOid(), new PortalMapping(sceneId, portalKey));
    }

    /**
     * Removes and returns the portal mapping for the specified body and scene, or returns
     * <code>null</code> if there is none or it has expired.
     */
    protected Object removePortalMapping (BodyObject source, int sceneId)
    {
        PortalMapping mapping = _portals.remove(source.getOid());
        return (mapping != null && mapping.getSceneId() == sceneId &&
            mapping.getExpiry() > System.currentTimeMillis()) ? mapping.getPortalKey() : null;
    }

    /**
     * Removes all expired portal mappings.
     */
    protected void prunePortalMappings ()
    {
        // remove any mappings that have expired or lost their client objects
        long now = System.currentTimeMillis();
        for (Iterator<IntEntry<PortalMapping>> it = _portals.intEntrySet().iterator();
                it.hasNext(); ) {
            IntEntry<PortalMapping> entry = it.next();
            if (_omgr.getObject(entry.getIntKey()) == null ||
                    now >= entry.getValue().getExpiry()) {
                it.remove();
            }
        }
    }

    /**
     * Contains stored destination portal information.
     */
    protected static class PortalMapping
    {
        /**
         * Creates a new portal mapping.
         */
        public PortalMapping (int sceneId, Object portalKey)
        {
            _sceneId = sceneId;
            _portalKey = portalKey;
            _expiry = System.currentTimeMillis() + PORTAL_MAPPING_LIFESPAN;
        }

        /**
         * Returns the id of the scene associated with this mapping.
         */
        public int getSceneId ()
        {
            return _sceneId;
        }

        /**
         * Returns the key of the destination portal.
         */
        public Object getPortalKey ()
        {
            return _portalKey;
        }

        /**
         * Returns the time at which the mapping expires.
         */
        public long getExpiry ()
        {
            return _expiry;
        }

        /** The scene id. */
        protected int _sceneId;

        /** The destination portal key. */
        protected Object _portalKey;

        /** The time at which the mapping expires. */
        protected long _expiry;
    }

    /** The server object manager. */
    protected PresentsDObjectMgr _omgr;

    /** The config manager. */
    @Inject protected ConfigManager _cfgmgr;

    /** Maps body oids to the keys of their destination portals. */
    protected HashIntMap<PortalMapping> _portals = IntMaps.newHashIntMap();

    /** The default scene ticker. */
    protected SceneTicker _defaultTicker;

    /** The interval after which portal mappings expire. */
    protected static final long PORTAL_MAPPING_LIFESPAN = 30 * 1000L;

    /** The interval at which we prune expired portal mappings. */
    protected static final long PORTAL_PRUNE_INTERVAL = 5 * 60 * 1000L;
}
