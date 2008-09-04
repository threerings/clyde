//
// $Id$

package com.threerings.opengl.scene;

import java.util.ArrayList;
import java.util.HashMap;

import com.threerings.math.Box;
import com.threerings.math.FloatMath;
import com.threerings.math.Frustum;
import com.threerings.math.Ray;
import com.threerings.math.Vector3f;

import com.threerings.opengl.util.GlContext;

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
        super(ctx);
        _granularity = granularity;
        _levels = levels;
    }

    // documentation inherited from interface Renderable
    public void enqueue ()
    {
        // make sure it intersects the top-level bounds
        Frustum frustum = _ctx.getCompositor().getCamera().getWorldVolume();
        if (frustum.getIntersectionType(_bounds) == Frustum.IntersectionType.NONE) {
            return;
        }

        // increment the visit counter
        _visit++;

        // visit the intersecting roots
        Box bounds = frustum.getBounds();
        Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        int minz = (int)FloatMath.floor(min.z * rgran);
        int maxz = (int)FloatMath.floor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node root = _roots.get(_coord.set(xx, yy, zz));
                    if (root != null) {
                        root.enqueue(frustum);
                    }
                }
            }
        }
    }

    @Override // documentation inherited
    public SceneElement getIntersection (Ray ray, Vector3f location)
    {
        // get the point of intersection with the top-level bounds
        if (!_bounds.getIntersection(ray, _pt)) {
            return null;
        }

        // increment the visit counter
        _visit++;

        // find the starting cell
        float rgran = 1f / _granularity;
        int xx = (int)FloatMath.floor(_pt.x * rgran);
        int yy = (int)FloatMath.floor(_pt.y * rgran);
        int zz = (int)FloatMath.floor(_pt.z * rgran);

        // determine the integer directions on each axis
        Vector3f dir = ray.getDirection();
        int xdir = (int)Math.signum(dir.x);
        int ydir = (int)Math.signum(dir.y);
        int zdir = (int)Math.signum(dir.z);

        // step through each cell that the ray intersects, returning the first hit or bailing
        // out when we exceed the bounds
        do {
            Node root = _roots.get(_coord.set(xx, yy, zz));
            if (root != null) {
                SceneElement element = root.getIntersection(ray, location);
                if (element != null) {
                    return element;
                }
            }
            float xt = (xdir == 0) ? Float.MAX_VALUE :
                ((xx + xdir) * _granularity - _pt.x) / dir.x;
            float yt = (ydir == 0) ? Float.MAX_VALUE :
                ((yy + ydir) * _granularity - _pt.y) / dir.y;
            float zt = (zdir == 0) ? Float.MAX_VALUE :
                ((zz + zdir) * _granularity - _pt.z) / dir.z;
            float t = (xt < yt) ? (xt < zt ? xt : zt) : (yt < zt ? yt : zt);
            if (xt == t) {
                xx += xdir;
            }
            if (yt == t) {
                yy += ydir;
            }
            if (zt == t) {
                zz += zdir;
            }
            _pt.addScaledLocal(dir, t);

        } while (_bounds.contains(_pt));

        // no luck
        return null;
    }

    @Override // documentation inherited
    public void boundsWillChange (SceneElement element)
    {
        removeFromSpatial(element);
    }

    @Override // documentation inherited
    public void boundsDidChange (SceneElement element)
    {
        addToSpatial(element);
    }

    @Override // documentation inherited
    protected void addToSpatial (SceneElement element)
    {
        Box bounds = element.getBounds();
        int level = getLevel(bounds);
        Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        int minz = (int)FloatMath.floor(min.z * rgran);
        int maxz = (int)FloatMath.floor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node root = _roots.get(_coord.set(xx, yy, zz));
                    if (root == null) {
                        _roots.put((Coord)_coord.clone(), root = createRoot(xx, yy, zz));
                        _bounds.addLocal(root.getBounds());
                    }
                    root.add(element, level);
                }
            }
        }
    }

    @Override // documentation inherited
    protected void removeFromSpatial (SceneElement element)
    {
        Box bounds = element.getBounds();
        int level = getLevel(bounds);
        Vector3f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        float rgran = 1f / _granularity;
        int minx = (int)FloatMath.floor(min.x * rgran);
        int maxx = (int)FloatMath.floor(max.x * rgran);
        int miny = (int)FloatMath.floor(min.y * rgran);
        int maxy = (int)FloatMath.floor(max.y * rgran);
        int minz = (int)FloatMath.floor(min.z * rgran);
        int maxz = (int)FloatMath.floor(max.z * rgran);
        for (int zz = minz; zz <= maxz; zz++) {
            for (int yy = miny; yy <= maxy; yy++) {
                for (int xx = minx; xx <= maxx; xx++) {
                    Node root = _roots.get(_coord.set(xx, yy, zz));
                    if (root == null) {
                        continue;
                    }
                    root.remove(element, level);
                    if (root.isEmpty()) {
                        _roots.remove(_coord);
                        recomputeBounds();
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
        int level = Math.round(
            FloatMath.log(bounds.getLongestEdge() / _granularity) / FloatMath.log(0.5f));
        return Math.min(Math.max(level, 0), _levels - 1);
    }

    /**
     * Creates a root node for the specified coordinates.
     */
    protected Node createRoot (int x, int y, int z)
    {
        Node root = (_levels > 1) ? new InternalNode(_levels - 1) : new LeafNode();
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
        for (Node root : _roots.values()) {
            _bounds.addLocal(root.getBounds());
        }
    }

    /**
     * Represents a node in an octree.
     */
    protected abstract class Node
    {
        /**
         * Returns a reference to the bounds of the node.
         */
        public Box getBounds ()
        {
            return _bounds;
        }

        /**
         * Determines whether the node is empty (that is, has no elements).
         */
        public boolean isEmpty ()
        {
            return _elements.isEmpty();
        }

        /**
         * Adds an element to this node.
         */
        public void add (SceneElement element, int level)
        {
            _elements.add(element);
        }

        /**
         * Removes an element from this node.
         */
        public void remove (SceneElement element, int level)
        {
            _elements.remove(element);
        }

        /**
         * Enqueues the elements in this node.
         */
        public void enqueue (Frustum frustum)
        {
            Frustum.IntersectionType type = frustum.getIntersectionType(_bounds);
            if (type == Frustum.IntersectionType.CONTAINS) {
                enqueueAll();
            } else if (type == Frustum.IntersectionType.INTERSECTS) {
                enqueueIntersecting(frustum);
            }
        }

        /**
         * Checks for an intersection with this node.
         */
        public SceneElement getIntersection (Ray ray, Vector3f location)
        {
            SceneElement closest = null;
            Vector3f origin = ray.getOrigin();
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SceneElement element = _elements.get(ii);
                if (element.updateLastVisit(_visit) && element.getIntersection(ray, _result) &&
                        (closest == null || origin.distanceSquared(_result) <
                            origin.distanceSquared(location))) {
                    closest = element;
                    location.set(_result);
                }
            }
            return closest;
        }

        /**
         * Enqueues all elements in this node.
         */
        protected void enqueueAll ()
        {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SceneElement element = _elements.get(ii);
                if (element.updateLastVisit(_visit)) {
                    HashScene.this.enqueue(element);
                }
            }
        }

        /**
         * Enqueues the elements intersecting the given frustum.
         */
        protected void enqueueIntersecting (Frustum frustum)
        {
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SceneElement element = _elements.get(ii);
                if (element.updateLastVisit(_visit) &&
                        frustum.getIntersectionType(element.getBounds()) !=
                            Frustum.IntersectionType.NONE) {
                    HashScene.this.enqueue(element);
                }
            }
        }

        /** The bounds of the node. */
        public Box _bounds = new Box();

        /** The elements in the node. */
        public ArrayList<SceneElement> _elements = new ArrayList<SceneElement>(4);
    }

    /**
     * An internal node with (up to) eight children.
     */
    protected class InternalNode extends Node
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
        public void add (SceneElement element, int level)
        {
            if (level == 0) {
                super.add(element, level);
                return;
            }
            level--;
            Box bounds = element.getBounds();
            for (int ii = 0; ii < _children.length; ii++) {
                Node child = _children[ii];
                if (child == null) {
                    getChildBounds(ii, _box);
                    if (_box.intersects(bounds)) {
                        _children[ii] = child = (_levels > 1) ?
                            new InternalNode(_levels - 1) : new LeafNode();
                        child.getBounds().set(_box);
                        child.add(element, level);
                    }
                } else if (child.getBounds().intersects(bounds)) {
                    child.add(element, level);
                }
            }
        }

        @Override // documentation inherited
        public void remove (SceneElement element, int level)
        {
            if (level == 0) {
                super.remove(element, level);
                return;
            }
            level--;
            Box bounds = element.getBounds();
            for (int ii = 0; ii < _children.length; ii++) {
                Node child = _children[ii];
                if (child != null && child.getBounds().intersects(bounds)) {
                    child.remove(element, level);
                    if (child.isEmpty()) {
                        _children[ii] = null;
                    }
                }
            }
        }

        @Override // documentation inherited
        public SceneElement getIntersection (Ray ray, Vector3f location)
        {
            SceneElement closest = super.getIntersection(ray, location);
            Vector3f origin = ray.getOrigin();
            Vector3f result = new Vector3f();
            for (Node child : _children) {
                if (child == null || !child.getBounds().intersects(ray)) {
                    continue;
                }
                SceneElement element = child.getIntersection(ray, result);
                if (element != null && (closest == null ||
                        origin.distanceSquared(result) < origin.distanceSquared(location))) {
                    closest = element;
                    location.set(result);
                }
            }
            return closest;
        }

        @Override // documentation inherited
        protected void enqueueAll ()
        {
            super.enqueueAll();
            for (Node child : _children) {
                if (child != null) {
                    child.enqueueAll();
                }
            }
        }

        @Override // documentation inherited
        protected void enqueueIntersecting (Frustum frustum)
        {
            super.enqueueIntersecting(frustum);
            for (Node child : _children) {
                if (child != null) {
                    child.enqueue(frustum);
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
        public Node[] _children = new Node[8];
    }

    /**
     * A leaf node.
     */
    protected class LeafNode extends Node
    {
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
            return x + 31*(y + 31*z);
        }

        @Override // documentation inherited
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

    /** The top level nodes. */
    protected HashMap<Coord, Node> _roots = new HashMap<Coord, Node>();

    /** The bounds of the roots. */
    protected Box _bounds = new Box();

    /** The visit counter. */
    protected int _visit;

    /** A reusable coord object for queries. */
    protected Coord _coord = new Coord();

    /** A reusable box. */
    protected Box _box = new Box();

    /** Reusable location vector. */
    protected Vector3f _pt = new Vector3f();
}
