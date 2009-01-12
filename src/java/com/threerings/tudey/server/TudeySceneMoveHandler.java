//
// $Id$

package com.threerings.tudey.server;

import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.LocationManager;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneMoveHandler;

/**
 * Provides special handling for traversing portals.
 */
public class TudeySceneMoveHandler extends SceneMoveHandler
{
    /**
     * Creates a new move handler.
     */
    public TudeySceneMoveHandler (
        LocationManager locman, BodyObject body, int sceneVer, Object portalKey,
        SceneService.SceneMoveListener listener)
    {
        super(locman, body, sceneVer, listener);
        _portalKey = portalKey;
    }

    @Override // documentation inherited
    protected void effectSceneMove (SceneManager scmgr)
        throws InvocationException
    {
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
