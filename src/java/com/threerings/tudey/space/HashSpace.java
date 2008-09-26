//
// $Id$

package com.threerings.tudey.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.google.common.collect.Maps;

import com.samskivert.util.Predicate;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

/**
 * A space that uses a hybrid spatial hashing/quadtree scheme to store elements.
 */
public class HashSpace extends Space
{
    /**
     * Creates a new hash space.
     *
     * @param granularity the size of the top-level cells.
     * @param levels the (maximum) number of quadtree levels.
     */
    public HashSpace (float granularity, int levels)
    {
        _granularity = granularity;
        _levels = levels;
    }

    @Override // documentation inherited
    public SpaceElement getIntersection (
        Ray2D ray, Vector2f location, Predicate<SpaceElement> filter)
    {
        // check for an intersection with the oversized elements
        SpaceElement closest = getIntersection(_oversizedElements, ray, location, filter);

        // get the point of intersection with the top-level bounds
        if (!_bounds.getIntersection(ray, _pt)) {
            return closest;
        }

        // increment the visit counter
        _visit++;

        // find the starting cell
        float rgran = 1f / _granularity;
        int xx = (int)FloatMath.floor(_pt.x * rgran);
        int yy = (int)FloatMath.floor(_pt.y * rgran);

        // determine the integer directions on each axis
        Vector2f dir = ray.getDirection();
        int xdir = (int)Math.signum(dir.x);
        int ydir = (int)Math.signum(dir.y);

        // step through each cell that the ray intersects, returning the first hit or bailing
        // out when we exceed the bounds
        Vector2f result = new Vector2f();
        do {
            Node<SpaceElement> root = _elements.get(_coord.set(xx, yy));
            if (root != null) {
                SpaceElement element = root.getIntersection(ray, result, filter);
                if (element != null) {
                    Vector2f origin = ray.getOrigin();
                    if (closest == null || origin.distanceSquared(result) <
                            origin.distanceSquared(location)) {
                        closest = element;
                        location.set(result);
                    }
                    return closest;
                }
            }
            float xt = (xdir == 0) ? Float.MAX_VALUE :
                ((xx + xdir) * _granularity - _pt.x) / dir.x;
            float yt = (ydir == 0) ? Float.MAX_VALUE :
                ((yy + ydir) * _granularity - _pt.y) / dir.y;
            float t = (xt < yt) ? xt : yt;
            if (xt == t) {
                xx += xdir;
            }
            if (yt == t) {
                yy += ydir;
            }
            _pt.addScaledLocal(dir, t);

        } while (_bounds.contains(_pt));

        // no luck
        return closest;
    }

    @Override // documentation inherited
    public void getElements (Rect bounds, Collection<SpaceElement> results)
    {
        getIntersecting(_elements, _oversizedElements, bounds, results);
    }

    @Override // documentation inherited
    public void boundsWillChange (SpaceElement element)
    {
        super.boundsWillChange(element);
        removeFromSpatial(element);
    }

    @Override // documentation inherited
    public void boundsDidChange (SpaceElement element)
    {
        super.boundsDidChange(element);
        addToSpatial(element);
    }

    @Override // documentation inherited
    protected void addToSpatial (SpaceElement element)
    {
        add(_elements, _oversizedElements, element);
    }

    @Override // documentation inherited
    protected void removeFromSpatial (SpaceElement element)
    {
        remove(_elements, _oversizedElements, element);
    }

    /**
     * Adds the specified object to the provided map.
     */
    protected <T extends SpaceObject> void add (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, T object)
    {
        Rect bounds = object.getBounds();
        if (areOversized(bounds)) {
            oversized.add(object);
            return;
        }
        int level = getLevel(bounds);
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                Node<T> root = roots.get(_coord.set(xx, yy));
                if (root == null) {
                    roots.put((Coord)_coord.clone(), root = createRoot(xx, yy));
                    _bounds.addLocal(root.getBounds());
                }
                root.add(object, level);
            }
        }
    }

    /**
     * Removes the specified object from the provided map.
     */
    protected <T extends SpaceObject> void remove (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, T object)
    {
        Rect bounds = object.getBounds();
        if (areOversized(bounds)) {
            oversized.remove(object);
            return;
        }
        int level = getLevel(bounds);
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                Node<T> root = roots.get(_coord.set(xx, yy));
                if (root == null) {
                    continue;
                }
                root.remove(object, level);
                if (root.isEmpty()) {
                    roots.remove(_coord);
                    recomputeBounds();
                }
            }
        }
    }

    /**
     * Determines whether the specified bounds qualify as "oversized" with respect to the
     * scene granularity.
     */
    protected boolean areOversized (Rect bounds)
    {
        return bounds.getLongestEdge() > (_granularity * 2f);
    }

    /**
     * Adds all objects from the provided map that intersect the given bounds to the specified
     * results list.
     */
    protected <T extends SpaceObject> void getIntersecting (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, Rect bounds, Collection<T> results)
    {
        // get the oversized elements
        getIntersecting(oversized, bounds, results);

        // get the intersection with the top-level bounds
        bounds.intersect(_bounds, _rect);
        if (_rect.isEmpty()) {
            return;
        }

        // increment the visit counter
        _visit++;

        // visit the intersecting roots
        Vector2f min = _rect.getMinimumExtent(), max = _rect.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                Node<T> root = roots.get(_coord.set(xx, yy));
                if (root != null) {
                    root.get(bounds, results);
                }
            }
        }
    }

    /**
     * Returns the level for the supplied bounds.
     */
    protected int getLevel (Rect bounds)
    {
        int level = Math.round(
            FloatMath.log(bounds.getLongestEdge() / _granularity) / FloatMath.log(0.5f));
        return Math.min(Math.max(level, 0), _levels - 1);
    }

    /**
     * Creates a root node for the specified coordinates.
     */
    protected <T extends SpaceObject> Node<T> createRoot (int x, int y)
    {
        Node<T> root = (_levels > 1) ? new InternalNode<T>(_levels - 1) : new LeafNode<T>();
        Rect bounds = root.getBounds();
        bounds.getMinimumExtent().set(x * _granularity, y * _granularity);
        bounds.getMaximumExtent().set((x + 1) * _granularity, (y + 1) * _granularity);
        return root;
    }

    /**
     * Recomputes the bounds of the roots.
     */
    protected void recomputeBounds ()
    {
        _bounds.setToEmpty();
        for (Node root : _elements.values()) {
            _bounds.addLocal(root.getBounds());
        }
    }

    /**
     * Represents a node in a quadtree.
     */
    protected abstract class Node<T extends SpaceObject>
    {
        /**
         * Returns a reference to the bounds of the node.
         */
        public Rect getBounds ()
        {
            return _bounds;
        }

        /**
         * Determines whether the node is empty (that is, has no objects).
         */
        public boolean isEmpty ()
        {
            return _objects.isEmpty();
        }

        /**
         * Adds an object to this node.
         */
        public void add (T object, int level)
        {
            _objects.add(object);
        }

        /**
         * Removes an object from this node.
         */
        public void remove (T object, int level)
        {
            _objects.remove(object);
        }

        /**
         * Checks for an intersection with this node.
         */
        public T getIntersection (Ray2D ray, Vector2f location, Predicate<T> filter)
        {
            T closest = null;
            Vector2f origin = ray.getOrigin();
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (filter.isMatch(object) && object.updateLastVisit(_visit) &&
                        ((SpaceElement)object).getIntersection(ray, _result) &&
                            (closest == null || origin.distanceSquared(_result) <
                                origin.distanceSquared(location))) {
                    closest = object;
                    location.set(_result);
                }
            }
            return closest;
        }

        /**
         * Retrieves all objects intersecting the provided bounds.
         */
        public void get (Rect bounds, Collection<T> results)
        {
            if (bounds.contains(_bounds)) {
                getAll(results);
            } else if (bounds.intersects(_bounds)) {
                getIntersecting(bounds, results);
            }
        }

        /**
         * Gets all objects in this node.
         */
        protected void getAll (Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit)) {
                    results.add(object);
                }
            }
        }

        /**
         * Gets all objects in this node intersecting the provided bounds.
         */
        protected void getIntersecting (Rect bounds, Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) && object.getBounds().intersects(bounds)) {
                    results.add(object);
                }
            }
        }

        /** The bounds of the node. */
        public Rect _bounds = new Rect();

        /** The objects in the node. */
        public ArrayList<T> _objects = new ArrayList<T>(4);
    }

    /**
     * An internal node with (up to) four children.
     */
    protected class InternalNode<T extends SpaceObject> extends Node<T>
    {
        public InternalNode (int levels)
        {
            _levels = levels;
        }

        @Override // documentation inherited
        public boolean isEmpty ()
        {
            if (!super.isEmpty()) {
                return false;
            }
            for (Node child : _children) {
                if (child != null) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
        public void add (T object, int level)
        {
            if (level == 0) {
                super.add(object, level);
                return;
            }
            level--;
            Rect bounds = object.getBounds();
            for (int ii = 0; ii < _children.length; ii++) {
                Node<T> child = _children[ii];
                if (child == null) {
                    getChildBounds(ii, _rect);
                    if (_rect.intersects(bounds)) {
                        _children[ii] = child = (_levels > 1) ?
                            new InternalNode<T>(_levels - 1) : new LeafNode<T>();
                        child.getBounds().set(_rect);
                        child.add(object, level);
                    }
                } else if (child.getBounds().intersects(bounds)) {
                    child.add(object, level);
                }
            }
        }

        @Override // documentation inherited
        public void remove (T object, int level)
        {
            if (level == 0) {
                super.remove(object, level);
                return;
            }
            level--;
            Rect bounds = object.getBounds();
            for (int ii = 0; ii < _children.length; ii++) {
                Node<T> child = _children[ii];
                if (child != null && child.getBounds().intersects(bounds)) {
                    child.remove(object, level);
                    if (child.isEmpty()) {
                        _children[ii] = null;
                    }
                }
            }
        }

        @Override // documentation inherited
        public T getIntersection (Ray2D ray, Vector2f location, Predicate<T> filter)
        {
            T closest = super.getIntersection(ray, location, filter);
            Vector2f origin = ray.getOrigin();
            Vector2f result = new Vector2f();
            for (Node<T> child : _children) {
                if (child == null || !child.getBounds().intersects(ray)) {
                    continue;
                }
                T object = child.getIntersection(ray, result, filter);
                if (object != null && (closest == null ||
                        origin.distanceSquared(result) < origin.distanceSquared(location))) {
                    closest = object;
                    location.set(result);
                }
            }
            return closest;
        }

        @Override // documentation inherited
        protected void getAll (Collection<T> results)
        {
            super.getAll(results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.getAll(results);
                }
            }
        }

        @Override // documentation inherited
        protected void getIntersecting (Rect bounds, Collection<T> results)
        {
            super.getIntersecting(bounds, results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.getIntersecting(bounds, results);
                }
            }
        }

        /**
         * Populates the specified rect with the bounds of the indexed child.
         */
        protected void getChildBounds (int idx, Rect rect)
        {
            Vector2f pmin = _bounds.getMinimumExtent(), pmax = _bounds.getMaximumExtent();
            Vector2f cmin = rect.getMinimumExtent(), cmax = rect.getMaximumExtent();
            float hsize = (pmax.x - pmin.x) * 0.5f;
            if ((idx & (1 << 1)) == 0) {
                cmin.x = pmin.x;
                cmax.x = pmin.x + hsize;
            } else {
                cmin.x = pmin.x + hsize;
                cmax.x = pmax.x;
            }
            if ((idx & (1 << 0)) == 0) {
                cmin.y = pmin.y;
                cmax.y = pmin.y + hsize;
            } else {
                cmin.y = pmin.y + hsize;
                cmax.y = pmax.y;
            }
        }

        /** The number of levels under this node. */
        public int _levels;

        /** The children of the node. */
        @SuppressWarnings("unchecked")
        public Node<T>[] _children = (Node<T>[])new Node[4];
    }

    /**
     * A leaf node.
     */
    protected class LeafNode<T extends SpaceObject> extends Node<T>
    {
    }

    /**
     * The coordinates of a hash cell.
     */
    protected static class Coord
        implements Cloneable
    {
        /** The coordinates of the cell. */
        public int x, y;

        /**
         * Sets the fields of the coord.
         *
         * @return a reference to this coord, for chaining.
         */
        public Coord set (int x, int y)
        {
            this.x = x;
            this.y = y;
            return this;
        }

        @Override // documentation inherited
        public Object clone ()
        {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null; // won't happen
            }
        }

        @Override // documentation inherited
        public int hashCode ()
        {
            return x + 31*y;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            Coord ocoord = (Coord)other;
            return x == ocoord.x && y == ocoord.y;
        }
    }

    /** The size of the root nodes. */
    protected float _granularity;

    /** The (maximum) number of tree levels. */
    protected int _levels;

    /** The top level element nodes. */
    protected HashMap<Coord, Node<SpaceElement>> _elements = Maps.newHashMap();

    /** Oversized elements. */
    protected ArrayList<SpaceElement> _oversizedElements = new ArrayList<SpaceElement>();

    /** The bounds of the roots (does not include the oversized objects). */
    protected Rect _bounds = new Rect();

    /** The visit counter. */
    protected int _visit;

    /** A reusable coord object for queries. */
    protected Coord _coord = new Coord();

    /** A reusable rect. */
    protected Rect _rect = new Rect();

    /** Reusable location vector. */
    protected Vector2f _pt = new Vector2f();
}
