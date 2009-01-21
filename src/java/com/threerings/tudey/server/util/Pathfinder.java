//
// $Id$

package com.threerings.tudey.server.util;

import java.awt.Point;

import java.util.List;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.math.Vector2f;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.util.CoordIntMap;

/**
 * A helper class for pathfinding.
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

    /**
     * Computes a path for the specified actor from its current location, considering only the
     * scene entries (not the actors).
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getEntryPath (
        ActorLogic actor, float longest, float bx, float by, boolean partial)
    {
        Vector2f translation = actor.getTranslation();
        return getEntryPath(actor, longest, translation.x, translation.y, bx, by, partial);
    }

    /**
     * Computes a path for the specified actor, considering only the scene entries (not the
     * actors).
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getEntryPath (
        ActorLogic actor, float longest, float ax, float ay, float bx, float by, boolean partial)
    {
        return null;
    }

    /**
     * Computes a path for the specified actor from its current location.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getPath (
        ActorLogic actor, float longest, float bx, float by, boolean partial)
    {
        Vector2f translation = actor.getTranslation();
        return getPath(actor, longest, translation.x, translation.y, bx, by, partial);
    }

    /**
     * Computes a path for the specified actor.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getPath (
        ActorLogic actor, float longest, float ax, float ay, float bx, float by, boolean partial)
    {
        return null;
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
