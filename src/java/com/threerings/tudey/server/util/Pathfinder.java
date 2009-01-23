//
// $Id$

package com.threerings.tudey.server.util;

import java.awt.Point;

import java.util.List;
import java.util.Map;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.space.SpaceElement;
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
        for (SpaceElement element : model.getElements().values()) {
            addEntryElement(element);
        }
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
        return getPath(_entryFlags, actor, longest, ax, ay, bx, by, partial);
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
        return getPath(_flags, actor, longest, ax, ay, bx, by, partial);
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

    /**
     * Computes a path for the specified actor.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @return the computed path, or null if unreachable.
     */
    protected Vector2f[] getPath (
        final CoordIntMap flags, ActorLogic logic, float longest, float ax, float ay,
        float bx, float by, boolean partial)
    {
        // determine the actor's extents
        Rect bounds = logic.getShape().getBounds();
        int width = Math.max(1, (int)Math.ceil(bounds.getWidth()));
        int height = Math.max(1, (int)Math.ceil(bounds.getHeight()));

        // create the traversal predicate
        AStarPathUtil.TraversalPred pred;
        final Actor actor = logic.getActor();
        if (width == 1 && height == 1) {
            // simpler predicate for the common case of 1x1 actors
            pred = new AStarPathUtil.TraversalPred() {
                public boolean canTraverse (Object traverser, int x, int y) {
                    return !actor.canCollide(flags.get(x, y));
                }
            };
        } else {
            final int left = width / 2, right = (width - 1) / 2;
            final int bottom = height / 2, top = (height - 1) / 2;
            pred = new AStarPathUtil.TraversalPred() {
                public boolean canTraverse (Object traverser, int x, int y) {
                    for (int yy = y - bottom, yymax = y + top; yy <= yymax; yy++) {
                        for (int xx = x - left, xxmax = x + right; xx <= xxmax; xx++) {
                            if (actor.canCollide(flags.get(xx, yy))) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            };
        }

        // compute the offsets for converting to/from integer coordinates
        float xoff = (width % 2) * 0.5f;
        float yoff = (height % 2) * 0.5f;

        // compute the path
        List<Point> path = AStarPathUtil.getPath(
            pred, actor, (int)longest, Math.round(ax - xoff), Math.round(ay - yoff),
            Math.round(bx - xoff), Math.round(by - yoff), partial);
        if (path == null) {
            return null;
        }

        // convert to fractional coordinates
        Vector2f[] waypoints = new Vector2f[path.size()];
        for (int ii = 0; ii < waypoints.length; ii++) {
            Point pt = path.get(ii);
            waypoints[ii] = new Vector2f(pt.x + xoff, pt.y + yoff);
        }
        return waypoints;
    }

    /**
     * Adds an entry element to the flag map.
     */
    protected void addEntryElement (SpaceElement element)
    {
        Entry entry = (Entry)element.getUserObject();
        int flags = entry.getCollisionFlags(_scenemgr.getConfigManager());
        if (flags == 0) {
            return;
        }
        Rect bounds = element.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                float lx = xx, ly = yy, ux = lx + 1f, uy = ly + 1f;
                _quad.getVertex(0).set(lx, ly);
                _quad.getVertex(1).set(ux, ly);
                _quad.getVertex(2).set(ux, uy);
                _quad.getVertex(3).set(lx, uy);
                _quad.getBounds().getMinimumExtent().set(lx, ly);
                _quad.getBounds().getMaximumExtent().set(ux, uy);
                if (element.intersects(_quad)) {
                    _entryFlags.setBits(xx, yy, flags);
                }
            }
        }
    }

    /** The owning scene manager. */
    protected TudeySceneManager _scenemgr;

    /** The collision flags corresponding to the scene entries. */
    protected CoordIntMap _entryFlags = new CoordIntMap(3, 0);

    /** The collision flags corresponding to the scene entries and the actors. */
    protected CoordIntMap _flags = new CoordIntMap(3, 0);

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);
}
