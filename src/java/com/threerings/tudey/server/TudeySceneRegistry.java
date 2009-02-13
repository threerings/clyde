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

package com.threerings.tudey.server;

import java.util.Iterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.IntMap.IntEntry;
import com.samskivert.util.IntMaps;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.server.SceneRegistry;

/**
 * Provides special handling for moving between Tudey scenes.
 */
@Singleton
public class TudeySceneRegistry extends SceneRegistry
{
    /**
     * Constructs a Tudey scene registry.
     */
    @Inject public TudeySceneRegistry (PresentsDObjectMgr omgr, InvocationManager invmgr)
    {
        super(invmgr);
        _omgr = omgr;

        // create the interval to prune the portal mappings
        new Interval(_omgr) {
            public void expired () {
                prunePortalMappings();
            }
        }.schedule(PORTAL_PRUNE_INTERVAL, true);
    }

    /**
     * Forcibly moves a player to a new scene.
     *
     * @param portalKey the key of the destination portal.
     */
    public void moveBody (BodyObject source, int sceneId, Object portalKey)
    {
        // store the portal mapping
        _portals.put(source.getOid(), new PortalMapping(sceneId, portalKey));
        super.moveBody(source, sceneId);
    }

    // from interface SceneService
    public void moveTo (
        ClientObject caller, int sceneId, int sceneVer, SceneService.SceneMoveListener listener)
    {
        // look for a stored portal key
        PortalMapping mapping = _portals.remove(caller.getOid());
        if (mapping != null && mapping.getSceneId() == sceneId &&
                mapping.getExpiry() > System.currentTimeMillis()) {
            resolveScene(sceneId, new TudeySceneMoveHandler(
                _locman, (BodyObject)caller, sceneVer, mapping.getPortalKey(), listener));
        } else {
            super.moveTo(caller, sceneId, sceneVer, listener);
        }
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

    /** Maps body oids to the keys of their destination portals. */
    protected HashIntMap<PortalMapping> _portals = IntMaps.newHashIntMap();

    /** The interval after which portal mappings expire. */
    protected static final long PORTAL_MAPPING_LIFESPAN = 30 * 1000L;

    /** The interval at which we prune expired portal mappings. */
    protected static final long PORTAL_PRUNE_INTERVAL = 5 * 60 * 1000L;
}
