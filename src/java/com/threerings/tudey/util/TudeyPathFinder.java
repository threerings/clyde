//
// $Id$

package com.threerings.tudey.util;

import java.util.List;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.geom.Point;

/**
 * Finds paths through a Tudey scene using the A* algorithm.
 */
public class TudeyPathFinder
{
    /**
     * Creates a new path finder for the given scene.
     */
    public TudeyPathFinder (TudeySceneModel scene)
    {
        _scene = scene;
    }

    /**
     * Finds a path through the scene. Assumes the staring and ending
     * coordinates are traversable.
     *
     * @param start the staring coordinate
     * @param end the ending coordinate
     * @return a list of point objects representing a path from coordinates
     * start to end, inclusive
     */
    public List<Point> findPath (Point start, Point end)
    {
        return null;
    }

    /** The scene in with to path find. */
    protected TudeySceneModel _scene;
}
