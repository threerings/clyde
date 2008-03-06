//
// $Id$

package com.threerings.tudey.server;

import com.threerings.tudey.data.TudeyPlaceObject;

/**
 * The superclass of server-side objects that handle scene element state.
 */
public class ServerLogic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeyPlaceManagerDelegate delegate)
    {
        _delegate = delegate;
        _tobj = _delegate.getTudeyPlaceObject();
    }

    /** The owning delete. */
    protected TudeyPlaceManagerDelegate _delegate;

    /** The place object. */
    protected TudeyPlaceObject _tobj;
}
