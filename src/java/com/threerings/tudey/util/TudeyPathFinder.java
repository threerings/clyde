//
// $Id$

package com.threerings.tudey.util;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import com.threerings.math.FloatMath;

import com.threerings.media.util.AStarPathUtil;
import com.threerings.media.util.AStarPathUtil.TraversalPred;
import com.threerings.media.util.AStarPathUtil.Stepper;

import com.threerings.tudey.geom.Circle;
import com.threerings.tudey.geom.Point;
import com.threerings.tudey.geom.Space;
import com.threerings.tudey.geom.Shape;

/**
 * Finds paths through a Tudey space using the A* algorithm. Paths are
 * calculated along a grid aligned at (0,0) with half-unit resolution.
 */
public class TudeyPathFinder
{
    /**
     * Creates a new path finder for the given space.
     */
    public TudeyPathFinder (Space space)
    {
        _space = space;
    }

    /**
     * Finds a path through the space.
     *
     * @param start the staring coordinate
     * @param end the ending coordinate
     * @param radius the size of the object moving through the space
     * @param longest the longest allowable path, in scene grid spaces
     * @param partial if true, a partial path path will be returned that gets
     * the traverser as close to the goal as possible in the event that a
     * complete path cannot be located
     * @return a list of point objects representing a path from coordinates
     * start to end, inclusive or <code>null</code> if no path could be found
     */
    public List<Point> findPath (Point start, Point end, float radius, int longest, boolean partial)
    {
        final Circle circle = new Circle(0f, 0f, radius); // create a dummy circle
        final int dist = longest * 2; // convert scene space to grid space

        // find the path
        List<java.awt.Point> path = AStarPathUtil.getPath(
            new TraversalPred() {
                public boolean canTraverse (Object trav, int x, int y) {
                    // check if there is a collision in the scene space
                    circle.setLocation(gridToScene(x), gridToScene(y));
                    _space.getIntersecting(circle, _isect);
                    return _isect.size() == 0;
                }
            },
            new Stepper() {
                public void considerSteps (int x, int y) {
                    // consider the four cardinal directions
                    considerStep(x - 1, y, AStarPathUtil.ADJACENT_COST);
                    considerStep(x + 1, y, AStarPathUtil.ADJACENT_COST);
                    considerStep(x, y - 1, AStarPathUtil.ADJACENT_COST);
                    considerStep(x, y + 1, AStarPathUtil.ADJACENT_COST);
                }
            },
            null,
            dist,
            sceneToGrid(start.getX()), sceneToGrid(start.getY()),
            sceneToGrid(end.getX()), sceneToGrid(end.getY()),
            partial);

        // then convert from our funky grid space to scene points again
        ArrayList<Point> npath = new ArrayList<Point>(path.size());
        for (int ii = 0, nn = path.size(); ii < nn; ii++) {
            java.awt.Point pt = path.get(ii);
            npath.add(new Point(gridToScene(pt.x), gridToScene(pt.y)));
        }

        return npath;
    }

    /**
     * Converts a one component of a coordinate from scene to grid space.
     */
    protected int sceneToGrid (float coord)
    {
        return (int)(FloatMath.roundNearest(coord, 0.5f) * 2);
    }

    /**
     * Converts a one component of a coordinate from grid to scene space.
     */
    protected float gridToScene (int coord)
    {
        return coord / 2f;
    }

    /** The space in with to path find. */
    protected Space _space;

    /** Dummy list for intersection testing. */ 
    protected List<Shape> _isect = new LinkedList<Shape>();
}
