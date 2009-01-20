//
// $Id$

package com.threerings.tudey.server.util;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.logic.Logic;
import com.threerings.tudey.util.CoordIntMap;

/**
 * A helper class for pathfinding and related business.
 */
public class Pathfinder
    implements TudeySceneModel.Observer
{
    /**
     * Creates a new pathfinder.
     */
    public Pathfinder (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;

        // initialize the entry flags and register as an observer
        TudeySceneModel model = (TudeySceneModel)_scenemgr.getScene().getSceneModel();
        _entryFlags.putAll(model.getCollisionFlags());
        model.addObserver(this);
    }

    /**
     * Shuts down the pathfinder.
     */
    public void shutdown ()
    {
        ((TudeySceneModel)_scenemgr.getScene().getSceneModel()).removeObserver(this);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
    }

    /** The owning scene manager. */
    protected TudeySceneManager _scenemgr;

    /** The collision flags corresponding to the scene entries. */
    protected CoordIntMap _entryFlags = new CoordIntMap(3, 0);
}
