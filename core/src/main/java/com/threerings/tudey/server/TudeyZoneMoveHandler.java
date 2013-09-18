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

import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.LocationManager;

import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.zone.client.ZoneService;
import com.threerings.whirled.zone.server.ZoneManager;
import com.threerings.whirled.zone.server.ZoneMoveHandler;

/**
 * Provides special handling for traversing portals and client-specific resolution.
 */
public class TudeyZoneMoveHandler extends ZoneMoveHandler
{
    /**
     * Creates a new zone move handler.
     */
    public TudeyZoneMoveHandler (
        LocationManager locmgr, ZoneManager zmgr, SceneRegistry screg, BodyObject body,
        int sceneId, int sceneVer, Object portalKey, ZoneService.ZoneMoveListener listener)
    {
        super(locmgr, zmgr, screg, body, sceneId, sceneVer, listener);
        _portalKey = portalKey;
    }

    @Override
    protected void resolveScene ()
    {
        ((TudeySceneRegistry)_screg).resolveScene(_body, _sceneId, this);
    }

    @Override
    protected void effectSceneMove (SceneManager scmgr)
        throws InvocationException
    {
        // if there's no portal key, just call normal implementation
        if (_portalKey == null) {
            super.effectSceneMove(scmgr);
            return;
        }
        // let the destination scene manager know that we're coming in
        TudeySceneManager destmgr = (TudeySceneManager)scmgr;
        destmgr.mapEnteringBody(_body, _portalKey);

        try {
            super.effectSceneMove(destmgr);
        } catch (InvocationException ie) {
            // if anything goes haywire, clear out our entering status
            destmgr.clearEnteringBody(_body);
            throw ie;
        }
    }

    /** Identifies the destination portal. */
    protected Object _portalKey;
}
