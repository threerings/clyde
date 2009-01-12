//
// $Id$

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
    @Inject public TudeySceneRegistry (InvocationManager invmgr)
    {
        super(invmgr);

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
    @Inject protected PresentsDObjectMgr _omgr;

    /** Maps body oids to the keys of their destination portals. */
    protected HashIntMap<PortalMapping> _portals = IntMaps.newHashIntMap();

    /** The interval after which portal mappings expire. */
    protected static final long PORTAL_MAPPING_LIFESPAN = 30 * 1000L;

    /** The interval at which we prune expired portal mappings. */
    protected static final long PORTAL_PRUNE_INTERVAL = 5 * 60 * 1000L;
}
