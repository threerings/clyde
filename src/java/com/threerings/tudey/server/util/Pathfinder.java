//
// $Id$

package com.threerings.tudey.server.util;

import java.awt.Point;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.config.ConfigManager;
import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.server.logic.Logic;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.space.Space;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.CoordIntMap;

/**
 * A helper class for pathfinding.  Currently the pathfinding strategy is to divide the world up
 * into unit cells and track the collision flags of all scene entries and actors whose shapes
 * intersect those cells.  An alternate method that may be worth exploring would be to have the
 * traversal predicate perform a full intersection query (it seems likely that this would be more
 * expensive than maintaining the collision map for all actors, but it's not entirely clear).
 */
public class Pathfinder
    implements TudeySceneModel.Observer, Space.Observer
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
        _combinedFlags.putAll(_entryFlags);
        for (SpaceElement element : model.getElements().values()) {
            addFlags(element);
        }
        model.addObserver(this);
        model.getSpace().addObserver(this);

        // listen for actor element updates
        _scenemgr.getActorSpace().addObserver(this);
    }

    /**
     * Shuts down the pathfinder.
     */
    public void shutdown ()
    {
        TudeySceneModel model = (TudeySceneModel)_scenemgr.getScene().getSceneModel();
        model.removeObserver(this);
        model.getSpace().removeObserver(this);
        _scenemgr.getActorSpace().removeObserver(this);
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
        return getPath(_combinedFlags, actor, longest, ax, ay, bx, by, partial);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addFlags(entry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        updateFlags(oentry);
        addFlags(nentry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        updateFlags(oentry);
    }

    // documentation inherited from interface Space.Observer
    public void elementAdded (SpaceElement element)
    {
        addFlags(element);
    }

    // documentation inherited from interface Space.Observer
    public void elementRemoved (SpaceElement element)
    {
        updateFlags(element);
    }

    // documentation inherited from interface Space.Observer
    public void elementBoundsWillChange (SpaceElement element)
    {
        updateFlags(element);
    }

    // documentation inherited from interface Space.Observer
    public void elementBoundsDidChange (SpaceElement element)
    {
        addFlags(element);
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
     * Adds the specified entry's flags to the flag maps.
     */
    protected void addFlags (Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            return; // taken care of when the space changes
        }
        TileEntry tentry = (TileEntry)entry;
        TileConfig.Original config = tentry.getConfig(_scenemgr.getConfigManager());
        tentry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                int flags = tentry.getCollisionFlags(config, xx, yy);
                if (flags != 0) {
                    _entryFlags.setBits(xx, yy, flags);
                    _combinedFlags.setBits(xx, yy, flags);
                }
            }
        }
    }

    /**
     * Updates the flags underneath the area of the specified entry.
     */
    protected void updateFlags (Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            return; // taken care of when the space changes
        }
        TileEntry tentry = (TileEntry)entry;
        TileConfig.Original config = tentry.getConfig(_scenemgr.getConfigManager());
        tentry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                int flags = tentry.getCollisionFlags(config, xx, yy);
                if (flags != 0) {
                    updateQuad(xx, yy);
                    updateFlags(xx, yy, true, null);
                }
            }
        }
    }

    /**
     * Adds the specified element's flags to the flag map(s).
     */
    protected void addFlags (SpaceElement element)
    {
        Object object = element.getUserObject();
        boolean entry;
        int flags;
        if (object instanceof Entry) {
            entry = true;
            flags = ((Entry)object).getCollisionFlags(_scenemgr.getConfigManager());

        } else { // object instanceof ActorLogic
            entry = false;
            flags = ((ActorLogic)object).getActor().getCollisionFlags();
        }
        if (flags == 0) {
            return; // nothing to do
        }
        Rect bounds = element.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (element.intersects(_quad)) {
                    if (entry) {
                        _entryFlags.setBits(xx, yy, flags);
                    }
                    _combinedFlags.setBits(xx, yy, flags);
                }
            }
        }
    }

    /**
     * Updates the flags underneath the specified element.
     */
    protected void updateFlags (SpaceElement element)
    {
        Object object = element.getUserObject();
        boolean entry;
        int flags;
        if (object instanceof Entry) {
            entry = true;
            flags = ((Entry)object).getCollisionFlags(_scenemgr.getConfigManager());

        } else { // object instanceof ActorLogic
            entry = false;
            flags = ((ActorLogic)object).getActor().getCollisionFlags();
        }
        if (flags == 0) {
            return; // nothing to do
        }
        Rect bounds = element.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (element.intersects(_quad)) {
                    updateFlags(xx, yy, entry, element);
                }
            }
        }
    }

    /**
     * Updates the coordinates of the quad to encompass the specified grid cell.
     */
    protected void updateQuad (int x, int y)
    {
        float lx = x, ly = y, ux = lx + 1f, uy = ly + 1f;
        _quad.getVertex(0).set(lx, ly);
        _quad.getVertex(1).set(ux, ly);
        _quad.getVertex(2).set(ux, uy);
        _quad.getVertex(3).set(lx, uy);
        _quad.getBounds().getMinimumExtent().set(lx, ly);
        _quad.getBounds().getMaximumExtent().set(ux, uy);
    }

    /**
     * Updates the flags at the specified location ({@link #_quad} should be set to the cell
     * boundaries).
     *
     * @param skip an element to skip, or null for none.
     */
    protected void updateFlags (int x, int y, boolean entry, SpaceElement skip)
    {
        // if we're updating an entry, recompute its flags; otherwise, retrieve from map
        int flags;
        if (entry) {
            TudeySceneModel model = (TudeySceneModel)_scenemgr.getScene().getSceneModel();
            flags = model.getCollisionFlags().get(x, y);
            model.getSpace().getIntersecting(_quad, _elements);
            ConfigManager cfgmgr = _scenemgr.getConfigManager();
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                if (element != skip) {
                    flags |= ((Entry)element.getUserObject()).getCollisionFlags(cfgmgr);
                }
            }
            _elements.clear();
            _entryFlags.put(x, y, flags);

        } else {
            flags = _entryFlags.get(x, y);
        }

        // add the flags for the actors
        _scenemgr.getActorSpace().getIntersecting(_quad, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            if (element != skip) {
                flags |= ((ActorLogic)element.getUserObject()).getActor().getCollisionFlags();
            }
        }
        _elements.clear();

        // store the combined flags
        _combinedFlags.put(x, y, flags);
    }

    /** The owning scene manager. */
    protected TudeySceneManager _scenemgr;

    /** The collision flags corresponding to the scene entries. */
    protected CoordIntMap _entryFlags = new CoordIntMap(3, 0);

    /** The collision flags corresponding to the scene entries and the actors. */
    protected CoordIntMap _combinedFlags = new CoordIntMap(3, 0);

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);

    /** Holds elements during intersection testing. */
    protected List<SpaceElement> _elements = Lists.newArrayList();

    /** Region object to reuse. */
    protected transient Rectangle _region = new Rectangle();
}
