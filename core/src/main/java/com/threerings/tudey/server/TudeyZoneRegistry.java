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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.zone.client.ZoneService;
import com.threerings.whirled.zone.data.ZonedBodyObject;
import com.threerings.whirled.zone.server.ZoneManager;
import com.threerings.whirled.zone.server.ZoneMoveHandler;
import com.threerings.whirled.zone.server.ZoneRegistry;

/**
 * Provides special handling for moving between Tudey zones.
 */
@Singleton
public class TudeyZoneRegistry extends ZoneRegistry
{
    /**
     * Creates a new zone registry.
     */
    @Inject public TudeyZoneRegistry (InvocationManager invmgr)
    {
        super(invmgr);
    }

    /**
     * Forcibly moves a player to a new zoned scene.
     *
     * @param portalKey the key of the destination portal.
     */
    public String moveBody (ZonedBodyObject source, int zoneId, int sceneId, Object portalKey)
    {
        _tscreg.addPortalMapping((BodyObject)source, sceneId, portalKey);
        return moveBody(source, zoneId, sceneId);
    }

    @Override
    protected ZoneMoveHandler createZoneMoveHandler (
        ZoneManager zmgr, BodyObject body, int sceneId, int sceneVer,
        ZoneService.ZoneMoveListener listener)
    {
        Object portalKey = _tscreg.removePortalMapping(body, sceneId);
        return createZoneMoveHandler(zmgr, body, sceneId, sceneVer, portalKey, listener);
    }

    /**
     * Creates a zone move handler with a portal key.
     */
    protected ZoneMoveHandler createZoneMoveHandler (
        ZoneManager zmgr, BodyObject body, int sceneId, int sceneVer,
        Object portalKey, ZoneService.ZoneMoveListener listener)
    {
        return new TudeyZoneMoveHandler(
            _locman, zmgr, _screg, body, sceneId, sceneVer, portalKey, listener);
    }

    /** The Tudey scene registry. */
    @Inject protected TudeySceneRegistry _tscreg;
}
