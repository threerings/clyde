//
// $Id$

package com.threerings.tudey.client;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.tudey.util.TudeyContext;

/**
 * The basic Tudey scene controller class.
 */
public class TudeySceneController extends SceneController
{
    @Override // documentation inherited
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_tsview = new TudeySceneView((TudeyContext)ctx));
    }

    /** A casted reference to the scene view. */
    protected TudeySceneView _tsview;
}
