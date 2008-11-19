//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.tudey.server.TudeySceneManager;

/**
 * Controls the state of an actor on the server.
 */
public abstract class Logic
{
    /**
     * Creates a new logic object.
     */
    public Logic (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;
    }

    /** The scene manager. */
    protected TudeySceneManager _scenemgr;
}
