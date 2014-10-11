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

package com.threerings.opengl.scene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Ray3D;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;
import com.threerings.opengl.util.Intersectable;

/**
 * A scene that uses a hybrid spatial hashing/octree scheme to store scene elements.
 */
public class HashScene extends Scene
{
    /**
     * Creates a new hash scene.
     *
     * @param granularity the size of the top-level cells.
     * @param levels the (maximum) number of octree levels.
     */
    public HashScene (GlContext ctx, float granularity, int levels)
    {
        this(ctx, granularity, levels, DEFAULT_SOURCES);
    }

    /**
     * Creates a new hash scene.
     *
     * @param granularity the size of the top-level cells.
     * @param levels the (maximum) number of octree levels.
     * @param sources the number of simultaneous sound sources to allow.
     */
    public HashScene (GlContext ctx, float granularity, int levels, int sources)
    {
        super(ctx, sources);
        _granularity = granularity;
        _levels = levels;
        _lresults = new Vector3f[levels];
        for (int ii = 0; ii < levels; ii++) {
            _lresults[ii] = new Vector3f();
        }
    }

    // documentation inherited from interface Compositable
    public void composite ()
    {
        // composite the oversized elements
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        composite(_oversizedElements, frustum);

        // make sure the frustum intersects the top-level bounds
        if (frustum.getIntersectionType(_bounds) == Frustum.IntersectionType.NONE) {
            return;
        }

        // increment the visit counter
        _visit++;

        // visit the intersecting roots
        frustum.getBounds().intersect(_bounds, _box);
        Vector3f min = _box.getMinimumExtent(), max = _box.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        int minz = FloatMath.ifloor(min.z * rgran);
        int maxz = FloatMath.ifloor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node<SceneElement> root = _elements.get(_coord.set(xx, yy, zz));
                    if (root != null) {
                        root.composite(frustum);
                    }
                }
            }
        }
    }

    @Override
    public SceneElement getIntersection (
        Ray3D ray, Vector3f location, Predicate<? super SceneElement> filter)
    {
        // check for an intersection with the oversized elements
        SceneElement closest = getIntersection(_oversizedElements, ray, location, filter);

        // get the point of intersection with the top-level bounds
        if (!_bounds.getIntersection(ray, _pt)) {
            return closest;
        }

        // increment the visit counter
        _visit++;

        // determine the integer directions on each axis
        Vector3f origin = ray.getOrigin();
        Vector3f dir = ray.getDirection();
        int xdir = (int)Math.signum(dir.x);
        int ydir = (int)Math.signum(dir.y);
        int zdir = (int)Math.signum(dir.z);

        // find the starting lines
        float rgran = 1f / _granularity;
        float px = _pt.x * rgran, py = _pt.y * rgran, pz = _pt.z * rgran;
        int lx = (xdir < 0) ? FloatMath.iceil(px) : FloatMath.ifloor(px);
        int ly = (ydir < 0) ? FloatMath.iceil(py) : FloatMath.ifloor(py);
        int lz = (zdir < 0) ? FloatMath.iceil(pz) : FloatMath.ifloor(pz);

        // step through each cell that the ray intersects, returning the first hit or bailing
        // out when we exceed the bounds
        Vector3f result = _lresults[0];
        do {
            _coord.set(
                lx - (xdir < 0 ? 1 : 0),
                ly - (ydir < 0 ? 1 : 0),
                lz - (zdir < 0 ? 1 : 0));
            Node<SceneElement> root = _elements.get(_coord);
            if (root != null) {
                SceneElement element = root.getIntersection(ray, result, filter);
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
            float zt = (zdir == 0) ? Float.MAX_VALUE :
                ((lz + zdir) * _granularity - origin.z) / dir.z;
            float t = (xt < yt) ? (xt < zt ? xt : zt) : (yt < zt ? yt : zt);
            if (xt == t) {
                lx += xdir;
            }
            if (yt == t) {
                ly += ydir;
            }
            if (zt == t) {
                lz += zdir;
            }
        } while (
            _coord.x >= _minCoord.x && _coord.x <= _maxCoord.x &&
            _coord.y >= _minCoord.y && _coord.y <= _maxCoord.y &&
            _coord.z >= _minCoord.z && _coord.z <= _maxCoord.z);

        // no luck
        return closest;
    }

    @Override
    public void getElements (Box bounds, Collection<SceneElement> results)
    {
        getIntersecting(_elements, _oversizedElements, bounds, results);
    }

    @Override
    public void getInfluences (Box bounds, Collection<SceneInfluence> results)
    {
        getIntersecting(_influences, _oversizedInfluences, bounds, results);
    }

    @Override
    public void getEffects (Box bounds, Collection<ViewerEffect> results)
    {
        getIntersecting(_effects, _oversizedEffects, bounds, results);
    }

    @Override
    public void boundsWillChange (SceneElement element)
    {
        super.boundsWillChange(element);
        removeFromSpatial(element);
    }

    @Override
    public void boundsDidChange (SceneElement element)
    {
        super.boundsDidChange(element);
        addToSpatial(element);
    }

    @Override
    public void boundsWillChange (SceneInfluence influence)
    {
        super.boundsWillChange(influence);
        removeFromSpatial(influence);
    }

    @Override
    public void boundsDidChange (SceneInfluence influence)
    {
        super.boundsDidChange(influence);
        addToSpatial(influence);
    }

    @Override
    public void boundsWillChange (ViewerEffect effect)
    {
        super.boundsWillChange(effect);
        removeFromSpatial(effect);
    }

    @Override
    public void boundsDidChange (ViewerEffect effect)
    {
        super.boundsDidChange(effect);
        addToSpatial(effect);
    }

    @Override
    protected void addToSpatial (SceneElement element)
    {
        add(_elements, _oversizedElements, element);
    }

    @Override
    protected void removeFromSpatial (SceneElement element)
    {
        remove(_elements, _oversizedElements, element);
    }

    @Override
    protected void addToSpatial (SceneInfluence influence)
    {
        add(_influences, _oversizedInfluences, influence);
    }

    @Override
    protected void removeFromSpatial (SceneInfluence influence)
    {
        remove(_influences, _oversizedInfluences, influence);
    }

    @Override
    protected void addToSpatial (ViewerEffect effect)
    {
        add(_effects, _oversizedEffects, effect);
    }

    @Override
    protected void removeFromSpatial (ViewerEffect effect)
    {
        remove(_effects, _oversizedEffects, effect);
    }

    /**
     * Adds the specified object to the provided map.
     */
    protected <T extends SceneObject> void add (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, T object)
    {
        Box bounds = object.getBounds();
        if (areOversized(bounds)) {
            oversized.add(object);
            return;
        }
        int level = getLevel(bounds);
        Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        int minz = FloatMath.ifloor(min.z * rgran);
        int maxz = FloatMath.ifloor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node<T> root = roots.get(_coord.set(xx, yy, zz));
                    if (root == null) {
                        roots.put(_coord.clone(), root = createRoot(xx, yy, zz));
                        addBounds(_coord, root);
                    }
                    root.add(object, level);
                }
            }
        }
    }

    /**
     * Removes the specified object from the provided map.
     */
    protected <T extends SceneObject> void remove (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, T object)
    {
        Box bounds = object.getBounds();
        if (areOversized(bounds)) {
            oversized.remove(object);
            return;
        }
        int level = getLevel(bounds);
        Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        int minz = FloatMath.ifloor(min.z * rgran);
        int maxz = FloatMath.ifloor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node<T> root = roots.get(_coord.set(xx, yy, zz));
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
    }

    /**
     * Determines whether the specified bounds qualify as "oversized" with respect to the
     * scene granularity.
     */
    protected boolean areOversized (Box bounds)
    {
        return bounds.getLongestEdge() > (_granularity * 2f);
    }

    /**
     * Adds all objects from the provided map that intersect the given bounds to the specified
     * results list.
     */
    protected <T extends SceneObject> void getIntersecting (
        HashMap<Coord, Node<T>> roots, ArrayList<T> oversized, Box bounds, Collection<T> results)
    {
        // get the oversized elements
        getIntersecting(oversized, bounds, results);

        // get the intersection with the top-level bounds
        bounds.intersect(_bounds, _box);
        if (_box.isEmpty()) {
            return;
        }

        // increment the visit counter
        _visit++;

        // visit the intersecting roots
        Vector3f min = _box.getMinimumExtent(), max = _box.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = FloatMath.ifloor(min.x * rgran);
        int maxx = FloatMath.ifloor(max.x * rgran);
        int miny = FloatMath.ifloor(min.y * rgran);
        int maxy = FloatMath.ifloor(max.y * rgran);
        int minz = FloatMath.ifloor(min.z * rgran);
        int maxz = FloatMath.ifloor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node<T> root = roots.get(_coord.set(xx, yy, zz));
                    if (root != null) {
                        root.get(bounds, results);
                    }
                }
            }
        }
    }

    /**
     * Returns the level for the supplied bounds.
     */
    protected int getLevel (Box bounds)
    {
        int level = FloatMath.round(
            FloatMath.log(bounds.getLongestEdge() / _granularity) / FloatMath.log(0.5f));
        return Math.min(Math.max(level, 0), _levels - 1);
    }

    /**
     * Creates a root node for the specified coordinates.
     */
    protected <T extends SceneObject> Node<T> createRoot (int x, int y, int z)
    {
        Node<T> root = getFromNodePool(_levels);
        Box bounds = root.getBounds();
        bounds.getMinimumExtent().set(
            x * _granularity, y * _granularity, z * _granularity);
        bounds.getMaximumExtent().set(
            (x + 1) * _granularity, (y + 1) * _granularity, (z + 1) * _granularity);
        return root;
    }

    /**
     * Recomputes the bounds of the roots.
     */
    protected void recomputeBounds ()
    {
        _bounds.setToEmpty();
        _minCoord.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        _maxCoord.set(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        addBounds(_elements);
        addBounds(_influences);
        addBounds(_effects);
    }

    /**
     * Adds the bounds of the specified roots.
     */
    protected <T extends SceneObject> void addBounds (HashMap<Coord, Node<T>> roots)
    {
        for (Map.Entry<Coord, Node<T>> entry : roots.entrySet()) {
            addBounds(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds the bounds of the specified coordinate/node mapping.
     */
    protected <T extends SceneObject> void addBounds (Coord coord, Node<T> node)
    {
        _bounds.addLocal(node.getBounds());
        _minCoord.set(
            Math.min(coord.x, _minCoord.x),
            Math.min(coord.y, _minCoord.y),
            Math.min(coord.z, _minCoord.z));
       _maxCoord.set(
            Math.max(coord.x, _maxCoord.x),
            Math.max(coord.y, _maxCoord.y),
            Math.max(coord.z, _maxCoord.z));
    }

    /**
     * Returns an internal or leaf node from the pool as appropriate for the number of remaining
     * levels.
     */
    protected <T extends SceneObject> Node<T> getFromNodePool (int levels)
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
    protected <T extends SceneObject> InternalNode<T> getFromInternalNodePool (int levels)
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
    protected <T extends SceneObject> LeafNode<T> getFromLeafNodePool ()
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
     * Represents a node in an octree.
     */
    protected abstract class Node<T extends SceneObject>
    {
        /**
         * Returns a reference to the bounds of the node.
         */
        public Box getBounds ()
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
         * Composites the elements in this node.
         */
        public void composite (Frustum frustum)
        {
            Frustum.IntersectionType type = frustum.getIntersectionType(_bounds);
            if (type == Frustum.IntersectionType.CONTAINS) {
                compositeAll();
            } else if (type == Frustum.IntersectionType.INTERSECTS) {
                compositeIntersecting(frustum);
            }
        }

        /**
         * Checks for an intersection with this node.
         */
        public T getIntersection (Ray3D ray, Vector3f location, Predicate<? super T> filter)
        {
            T closest = null;
            Vector3f origin = ray.getOrigin();
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (filter.apply(object) && object.updateLastVisit(_visit) &&
                        ((Intersectable)object).getIntersection(ray, _result) &&
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
        public void get (Box bounds, Collection<T> results)
        {
            if (bounds.contains(_bounds)) {
                getAll(results);
            } else if (bounds.intersects(_bounds)) {
                getIntersecting(bounds, results);
            }
        }

        /**
         * Returns this node to the pool.
         */
        public abstract void returnToPool ();

        /**
         * Composites all elements in this node.
         */
        protected void compositeAll ()
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit)) {
                    HashScene.this.composite((SceneElement)object);
                }
            }
        }

        /**
         * Composites the elements intersecting the given frustum.
         */
        protected void compositeIntersecting (Frustum frustum)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) &&
                        frustum.getIntersectionType(object.getBounds()) !=
                            Frustum.IntersectionType.NONE) {
                    HashScene.this.composite((SceneElement)object);
                }
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
        protected void getIntersecting (Box bounds, Collection<T> results)
        {
            for (int ii = 0, nn = _objects.size(); ii < nn; ii++) {
                T object = _objects.get(ii);
                if (object.updateLastVisit(_visit) && object.getBounds().intersects(bounds)) {
                    results.add(object);
                }
            }
        }

        /** The bounds of the node. */
        public Box _bounds = new Box();

        /** The objects in the node. */
        public ArrayList<T> _objects = new ArrayList<T>(4);
    }

    /**
     * An internal node with (up to) eight children.
     */
    protected class InternalNode<T extends SceneObject> extends Node<T>
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
            Box bounds = object.getBounds();
            for (int ii = 0; ii < _children.length; ii++) {
                Node<T> child = _children[ii];
                if (child == null) {
                    getChildBounds(ii, _box);
                    if (_box.intersects(bounds)) {
                        _children[ii] = child = getFromNodePool(_levels);
                        child.getBounds().set(_box);
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
            Box bounds = object.getBounds();
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
        public T getIntersection (Ray3D ray, Vector3f location, Predicate<? super T> filter)
        {
            T closest = super.getIntersection(ray, location, filter);
            Vector3f origin = ray.getOrigin();
            Vector3f result = _lresults[_levels];
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
        protected void compositeAll ()
        {
            super.compositeAll();
            for (Node<T> child : _children) {
                if (child != null) {
                    child.compositeAll();
                }
            }
        }

        @Override
        protected void compositeIntersecting (Frustum frustum)
        {
            super.compositeIntersecting(frustum);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.composite(frustum);
                }
            }
        }

        @Override
        protected void getAll (Collection<T> results)
        {
            super.getAll(results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.getAll(results);
                }
            }
        }

        @Override
        protected void getIntersecting (Box bounds, Collection<T> results)
        {
            super.getIntersecting(bounds, results);
            for (Node<T> child : _children) {
                if (child != null) {
                    child.get(bounds, results);
                }
            }
        }

        /**
         * Populates the specified box with the bounds of the indexed child.
         */
        protected void getChildBounds (int idx, Box box)
        {
            Vector3f pmin = _bounds.getMinimumExtent(), pmax = _bounds.getMaximumExtent();
            Vector3f cmin = box.getMinimumExtent(), cmax = box.getMaximumExtent();
            float hsize = (pmax.x - pmin.x) * 0.5f;
            if ((idx & (1 << 2)) == 0) {
                cmin.x = pmin.x;
                cmax.x = pmin.x + hsize;
            } else {
                cmin.x = pmin.x + hsize;
                cmax.x = pmax.x;
            }
            if ((idx & (1 << 1)) == 0) {
                cmin.y = pmin.y;
                cmax.y = pmin.y + hsize;
            } else {
                cmin.y = pmin.y + hsize;
                cmax.y = pmax.y;
            }
            if ((idx & (1 << 0)) == 0) {
                cmin.z = pmin.z;
                cmax.z = pmin.z + hsize;
            } else {
                cmin.z = pmin.z + hsize;
                cmax.z = pmax.z;
            }
        }

        /** The number of levels under this node. */
        public int _levels;

        /** The children of the node. */
        @SuppressWarnings("unchecked")
        public Node<T>[] _children = (Node<T>[])new Node<?>[8];
    }

    /**
     * A leaf node.
     */
    protected class LeafNode<T extends SceneObject> extends Node<T>
    {
        @Override
        public void returnToPool ()
        {
            _leafNodePool.add(this);
        }
    }

    /**
     * The coordinates of a hash cell.
     */
    protected static class Coord
        implements Cloneable
    {
        /** The coordinates of the cell. */
        public int x, y, z;

        /**
         * Creates a coordinate with the specified values.
         */
        public Coord (int x, int y, int z)
        {
            set(x, y, z);
        }

        /**
         * Creates a zero coordinate.
         */
        public Coord ()
        {
        }

        /**
         * Sets the fields of the coord.
         *
         * @return a reference to this coord, for chaining.
         */
        public Coord set (int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public Coord clone ()
        {
            try {
                return (Coord) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public int hashCode ()
        {
            return x + 31*(y + 31*z);
        }

        @Override
        public boolean equals (Object other)
        {
            Coord ocoord = (Coord)other;
            return x == ocoord.x && y == ocoord.y && z == ocoord.z;
        }
    }

    /** The size of the root nodes. */
    protected float _granularity;

    /** The (maximum) number of tree levels. */
    protected int _levels;

    /** The top level element nodes. */
    protected HashMap<Coord, Node<SceneElement>> _elements = Maps.newHashMap();

    /** Oversized elements. */
    protected ArrayList<SceneElement> _oversizedElements = new ArrayList<SceneElement>();

    /** The top level influence nodes. */
    protected HashMap<Coord, Node<SceneInfluence>> _influences = Maps.newHashMap();

    /** Oversized influences. */
    protected ArrayList<SceneInfluence> _oversizedInfluences = new ArrayList<SceneInfluence>();

    /** The top level effect nodes. */
    protected HashMap<Coord, Node<ViewerEffect>> _effects = Maps.newHashMap();

    /** Oversized effects. */
    protected ArrayList<ViewerEffect> _oversizedEffects = new ArrayList<ViewerEffect>();

    /** The bounds of the roots (does not include the oversized objects). */
    protected Box _bounds = new Box();

    /** The minimum coordinate. */
    protected Coord _minCoord = new Coord(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /** The maximum coordinate. */
    protected Coord _maxCoord = new Coord(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    /** The visit counter. */
    protected int _visit;

    /** A reusable coord object for queries. */
    protected Coord _coord = new Coord();

    /** A reusable box. */
    protected Box _box = new Box();

    /** Reusable location vector. */
    protected Vector3f _pt = new Vector3f();

    /** Per-level result vectors. */
    protected Vector3f[] _lresults;

    /** A pool of internal nodes to reuse. */
    protected List<InternalNode<?>> _internalNodePool = Lists.newArrayList();

    /** A pool of leaf nodes to reuse. */
    protected List<LeafNode<?>> _leafNodePool = Lists.newArrayList();
}
