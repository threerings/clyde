//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.math.FloatMath;
import com.threerings.math.Ray2D;
import com.threerings.math.Rect;
import com.threerings.math.Vector2f;

import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.util.Coord;

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

    @Override
    public SpaceElement getIntersection (
        Ray2D ray, Vector2f location, Predicate<? super SpaceElement> filter)
    {
        // check for an intersection with the oversized elements
        SpaceElement closest = getIntersection(_oversizedElements, ray, location, filter);

        // get the point of intersection with the top-level bounds
        if (!_bounds.getIntersection(ray, _pt)) {
            return closest;
        }

        // increment the visit counter
        _visit++;

        // determine the integer directions on each axis
        Vector2f origin = ray.getOrigin();
        Vector2f dir = ray.getDirection();
        int xdir = (int)Math.signum(dir.x);
        int ydir = (int)Math.signum(dir.y);

        // find the starting lines
        float rgran = 1f / _granularity;
        float px = _pt.x * rgran, py = _pt.y * rgran;
        int lx = (xdir < 0) ? FloatMath.iceil(px) : FloatMath.ifloor(px);
        int ly = (ydir < 0) ? FloatMath.iceil(py) : FloatMath.ifloor(py);

        // step through each cell that the ray intersects, returning the first hit or bailing
        // out when we exceed the bounds
        Vector2f result = new Vector2f();
        do {
            _coord.set(
                lx - (xdir < 0 ? 1 : 0),
                ly - (ydir < 0 ? 1 : 0));
            Node<SpaceElement> root = _elements.get(_coord);
            if (root != null) {
                SpaceElement element = root.getIntersection(ray, result, filter);
                if (element != null) {
                    if (closest == null || origin.distanceSquared(result) <
                            origin.distanceSquared(location)) {
                        closest = element;
                        location.set(result);
                    }
                    return closest;
                }
            }
            float xt = (xdir == 0) ? Float.MAX_VALUE :
                ((lx + xdir) * _granularity - origin.x) / dir.x;
            float yt = (ydir == 0) ? Float.MAX_VALUE :
                ((ly + ydir) * _granularity - origin.y) / dir.y;
            float t = (xt < yt) ? xt : yt;
            if (xt == t) {
                lx += xdir;
            }
            if (yt == t) {
                ly += ydir;
            }
        } while (
            _coord.x >= _minCoord.x && _coord.x <= _maxCoord.x &&
            _coord.y >= _minCoord.y && _coord.y <= _maxCoord.y);

        // no luck
        return closest;
    }

    @Override
    public void getIntersecting (Shape shape, Predicate<? super SpaceElement> filter,
            Collection<SpaceElement> results)
    {
        // get the oversized elements
        getIntersecting(_oversizedElements, shape, filter, results);

        // get the intersection with the top-level bounds
        shape.getBounds().intersect(_bounds, _rect);
        if (_rect.isEmpty()) {
            return;
        }

        // increment the visit counter
        _visit++;

        // visit the intersecting roots
        Vector2f min = _rect.getMinimumExtent(), max = _rect.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                Node<SpaceElement> root = _elements.get(_coord.set(xx, yy));
                if (root != null) {
                    root.get(shape, filter, results);
                }
            }
        }
    }

    @Override
    public void getElements (Rect bounds, Collection<SpaceElement> results)
    {
        getIntersecting(_elements, _oversizedElements, bounds, results);
    }

    @Override
    public void boundsWillChange (SpaceElement element)
    {
        super.boundsWillChange(element);
        removeFromSpatial(element);
    }

    @Override
    public void boundsDidChange (SpaceElement element)
    {
        addToSpatial(element);
        super.boundsDidChange(element);
    }

    @Override
    protected void addToSpatial (SpaceElement element)
    {
        add(_elements, _oversizedElements, element);
    }

    @Override
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
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                Node<T> root = roots.get(_coord.set(xx, yy));
                if (root == null) {
                    roots.put(_coord.clone(), root = createRoot(xx, yy));
                    addBounds(_coord, root);
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
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
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
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
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
        int level = FloatMath.round(
            FloatMath.log(bounds.getLongestEdge() / _granularity) / FloatMath.log(0.5f));
        return Math.min(Math.max(level, 0), _levels - 1);
    }

    /**
     * Creates a root node for the specified coordinates.
     */
    protected <T extends SpaceObject> Node<T> createRoot (int x, int y)
    {
        Node<T> root = getFromNodePool(_levels);
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
        _minCoord.set(Integer.MAX_VALUE, Integer.MAX_VALUE);
        _maxCoord.set(Integer.MIN_VALUE, Integer.MIN_VALUE);
        addBounds(_elements);
    }

    /**
     * Adds the bounds of the specified roots.
     */
    protected <T extends SpaceObject> void addBounds (HashMap<Coord, Node<T>> roots)
    {
        for (Map.Entry<Coord, Node<T>> entry : roots.entrySet()) {
            addBounds(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the bounds of the specified coordinate/node mapping.
     */
    protected <T extends SpaceObject> void addBounds (Coord coord, Node<T> node)
    {
        _bounds.addLocal(node.getBounds());
        _minCoord.set(
            Math.min(coord.x, _minCoord.x),
            Math.min(coord.y, _minCoord.y));
       _maxCoord.set(
            Math.max(coord.x, _maxCoord.x),
            Math.max(coord.y, _maxCoord.y));
    }

    /**
     * Returns an internal or leaf node from the pool as appropriate for the number of remaining
     * levels.
     */
    protected <T extends SpaceObject> Node<T> getFromNodePool (int levels)
    {
        if (levels > 1) {
            return getFromInternalNodePool(levels - 1);
        } else {
            return getFromLeafNodePool();
        }
    }

    /**
     * Obtains an internal node through the pool.
     */
    protected <T extends SpaceObject> InternalNode<T> getFromInternalNodePool (int levels)
    {
        int size = _internalNodePool.size();
        if (size == 0) {
            return new InternalNode<T>(levels);
        }
        @SuppressWarnings("unchecked") InternalNode<T> node =
            (InternalNode<T>)_internalNodePool.remove(size - 1);
        node.reinit(levels);
        return node;
    }

    /**
     * Obtains a leaf node through the pool.
     */
    protected <T extends SpaceObject> LeafNode<T> getFromLeafNodePool ()
    {
        int size = _leafNodePool.size();
        if (size == 0) {
            return new LeafNode<T>();
        }
        @SuppressWarnings("unchecked") LeafNode<T> node =
            (LeafNode<T>)_leafNodePool.remove(size - 1);
        return node;
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
        public T getIntersection (Ray2D ray, Vector2f location, Predicate<? super T> filter)
        {
            T closest = null;
            Vector2f origin = ray.getOrigin();
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (filter.apply(object) && object.updateLastVisit(_visit) &&
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
         * Retrieves all objects intersecting the provided shape.
         */
        public void get (Shape shape, Collection<T> results)
        {
            get(shape, Predicates.alwaysTrue(), results);
        }

        /**
         * Retrieves all objects intersecting the provided shape.
         */
        public void get (Shape shape, Predicate<? super T> filter, Collection<T> results)
        {
            if (shape.getIntersectionType(_bounds) != Shape.IntersectionType.NONE) {
                getIntersecting(shape, filter, results);
            }
        }

        /**
         * Retrieves all objects intersecting the provided bounds.
         */
        public void get (Rect bounds, Collection<T> results)
        {
            get(bounds, Predicates.alwaysTrue(), results);
        }

        /**
         * Retrieves all objects intersecting the provided bounds.
         */
        public void get (Rect bounds, Predicate<? super T> filter, Collection<T> results)
        {
            if (bounds.contains(_bounds)) {
                getAll(filter, results);
            } else if (bounds.intersects(_bounds)) {
                getIntersecting(bounds, filter, results);
            }
        }

        /**
         * Returns this node to the pool.
         */
        public abstract void returnToPool ();

        /**
         * Gets all objects in this node.
         */
        protected void getAll (Predicate<? super T> filter, Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) && filter.apply(object)) {
                    results.add(object);
                }
            }
        }

        /**
         * Gets all objects in this node intersecting the provided shape.
         */
        protected void getIntersecting (
                Shape shape, Predicate<? super T> filter, Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) && filter.apply(object) &&
                        shape.intersects((SpaceElement)object)) {
                    results.add(object);
                }
            }
        }

        /**
         * Gets all objects in this node intersecting the provided bounds.
         */
        protected void getIntersecting (
                Rect bounds, Predicate<? super T> filter, Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) && filter.apply(object) &&
                        object.getBounds().intersects(bounds)) {
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

        /**
         * Reinitializes the node with a new level count.
         */
        public void reinit (int levels)
        {
            _levels = levels;
        }

        @Override
        public boolean isEmpty ()
        {
            if (!super.isEmpty()) {
                return false;
            }
            for (Node<T> child : _children) {
                if (child != null) {
                    return false;
                }
            }
            return true;
        }

        @Override
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
                        _children[ii] = child = getFromNodePool(_levels);
                        child.getBounds().set(_rect);
                        child.add(object, level);
                    }
                } else if (child.getBounds().intersects(bounds)) {
                    child.add(object, level);
                }
            }
        }

        @Override
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
                        child.returnToPool();
                        _children[ii] = null;
                    }
                }
            }
        }

        @Override
        public T getIntersection (Ray2D ray, Vector2f location, Predicate<? super T> filter)
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

        @Override
        public void returnToPool ()
        {
            _internalNodePool.add(this);
        }

        @Override
        protected void getAll (Predicate<? super T> filter, Collection<T> results)
        {
            super.getAll(filter, results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.getAll(filter, results);
                }
            }
        }

        @Override
        protected void getIntersecting (
                Shape shape, Predicate<? super T> filter, Collection<T> results)
        {
            super.getIntersecting(shape, filter, results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.get(shape, filter, results);
                }
            }
        }

        @Override
        protected void getIntersecting (
                Rect bounds, Predicate<? super T> filter, Collection<T> results)
        {
            super.getIntersecting(bounds, filter, results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.get(bounds, filter, results);
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
        public Node<T>[] _children = (Node<T>[])new Node<?>[4];
    }

    /**
     * A leaf node.
     */
    protected class LeafNode<T extends SpaceObject> extends Node<T>
    {
        @Override
        public void returnToPool ()
        {
            _leafNodePool.add(this);
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

    /** The minimum coordinate. */
    protected Coord _minCoord = new Coord(Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** The maximum coordinate. */
    protected Coord _maxCoord = new Coord(Integer.MIN_VALUE, Integer.MIN_VALUE);

    /** The visit counter. */
    protected int _visit;

    /** A reusable coord object for queries. */
    protected Coord _coord = new Coord();

    /** A reusable rect. */
    protected Rect _rect = new Rect();

    /** Reusable location vector. */
    protected Vector2f _pt = new Vector2f();

    /** A pool of internal nodes to reuse. */
    protected List<InternalNode<?>> _internalNodePool = Lists.newArrayList();

    /** A pool of leaf nodes to reuse. */
    protected List<LeafNode<?>> _leafNodePool = Lists.newArrayList();
}
