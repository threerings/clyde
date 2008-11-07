//
// $Id$

package com.threerings.tudey.server;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.server.SceneManager;

import com.threerings.tudey.data.TudeySceneObject;

/**
 * Manager for Tudey scenes.
 */
public class TudeySceneManager extends SceneManager
{
    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return (_tsobj = new TudeySceneObject());
    }

    /** A casted reference to the Tudey scene object. */
    protected TudeySceneObject _tsobj;
}
