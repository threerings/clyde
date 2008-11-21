//
// $Id$

package com.threerings.tudey.server.logic;

import com.threerings.tudey.server.TudeySceneManager;

/**
 * Handles the server-side processing for some entity.
 */
public abstract class Logic
{
    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;
    }

    /** The scene manager. */
    protected TudeySceneManager _scenemgr;
}
