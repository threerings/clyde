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

package com.threerings.opengl.model;

import java.util.Comparator;

import com.samskivert.util.ArrayUtil;
import com.threerings.math.Box;
import com.threerings.math.Ray3D;
import com.threerings.math.Triangle;
import com.threerings.math.Vector3f;

import com.threerings.export.Exportable;

import com.threerings.opengl.util.GlUtil;

/**
 * A mesh used for collision detection.
 */
public class CollisionMesh
    implements Exportable
{
    /**
     * Creates the mesh from an array of vertices, where each set of three vertices represents a
     * triangle.
     */
    public CollisionMesh (Vector3f... vertices)
    {
        Triangle[] triangles = new Triangle[vertices.length / 3];
        for (int ii = 0; ii < triangles.length; ii++) {
            triangles[ii] = new Triangle(vertices[ii*3], vertices[ii*3+1], vertices[ii*3+2]);
        }
        _root = createNode(triangles);
    }

    /**
     * No-arg constructor for deserialization.
     */
    public CollisionMesh ()
    {
    }

    /**
     * Returns a reference to the bounds of the mesh.
     */
    public Box getBounds ()
    {
        return _root.getBounds();
    }

    /**
     * Finds the intersection of the given ray with this mesh and places it in the supplied result
     * object.
     *
     * @return true if the ray hit the mesh, in which case the result will be placed in the object
     * supplied.
     */
    public boolean getIntersection (Ray3D ray, Vector3f result)
    {
        return _root.getIntersection(ray, result);
    }

    /**
     * Creates a node to hold the given array of triangles.
     */
    protected static Node createNode (Triangle[] triangles)
    {
        // compute the triangles' bounding box
        Box bounds = new Box(Vector3f.MAX_VALUE, Vector3f.MIN_VALUE);
        for (Triangle triangle : triangles) {
            bounds.addLocal(triangle.getFirstVertex());
            bounds.addLocal(triangle.getSecondVertex());
            bounds.addLocal(triangle.getThirdVertex());
        }

        // if there's only one triangle, we can create a leaf
        if (triangles.length == 1) {
            return new LeafNode(bounds, triangles[0]);
        }

        // otherwise, we must create an internal node that splits the list in two
        // on the largest axis
        Vector3f bsize = bounds.getMaximumExtent().subtract(bounds.getMinimumExtent());
        float max = Math.max(bsize.x, Math.max(bsize.y, bsize.z));
        if (bsize.x == max) {
            GlUtil.divide(triangles, Triangle.X_COMPARATOR);
        } else if (bsize.y == max) {
            GlUtil.divide(triangles, Triangle.Y_COMPARATOR);
        } else { // bsize.z == max
            GlUtil.divide(triangles, Triangle.Z_COMPARATOR);
        }
        return new InternalNode(bounds,
            createNode(ArrayUtil.splice(triangles, triangles.length / 2)),
            createNode(ArrayUtil.splice(triangles, 0, triangles.length / 2)));
    }

    /**
     * A node in the axis-aligned bounding box tree.
     */
    protected static abstract class Node
        implements Exportable
    {
        public Node (Box bounds)
        {
            _bounds = bounds;
        }

        public Node ()
        {
        }

        /**
         * Returns a reference to the bounds of the node.
         */
        public Box getBounds ()
        {
            return _bounds;
        }

        /**
         * Finds the intersection of the given ray with this node and places it in the supplied
         * result object.
         *
         * @return true if the ray hit a triangle within the node, in which case the result will be
         * placed in the object supplied.
         */
        public boolean getIntersection (Ray3D ray, Vector3f result)
        {
            return _bounds.intersects(ray) && computeIntersection(ray, result);
        }

        /**
         * Computes the actual intersection between the ray and this node's geometry, once it has
         * been established that the ray intersects the node's bounding box.
         *
         * @return true if the ray hit a triangle within the node, in which case the result will be
         * placed in the object supplied.
         */
        protected abstract boolean computeIntersection (Ray3D ray, Vector3f result);

        /** The bounds of the node. */
        protected Box _bounds;
    }

    /**
     * An internal node containing two children.
     */
    protected static class InternalNode extends Node
    {
        public InternalNode (Box bounds, Node left, Node right)
        {
            super(bounds);
            _left = left;
            _right = right;
        }

        public InternalNode ()
        {
        }

        @Override
        protected boolean computeIntersection (Ray3D ray, Vector3f result)
        {
            // check both children; if they both intersect, use closest intersection
            if (_left.getIntersection(ray, result)) {
                Vector3f lresult = new Vector3f(result);
                if (_right.getIntersection(ray, result)) {
                    Vector3f origin = ray.getOrigin();
                    if (lresult.distanceSquared(origin) < result.distanceSquared(origin)) {
                        result.set(lresult);
                    }
                }
                return true;
            }
            return _right.getIntersection(ray, result);
        }

        /** The children of this node. */
        protected Node _left, _right;
    }

    /**
     * A leaf node containing a single triangle.
     */
    protected static class LeafNode extends Node
    {
        public LeafNode (Box bounds, Triangle triangle)
        {
            super(bounds);
            _triangle = triangle;
        }

        public LeafNode ()
        {
        }

        @Override
        public boolean computeIntersection (Ray3D ray, Vector3f result)
        {
            return _triangle.getIntersection(ray, result);
        }

        /** The triangle in the leaf. */
        protected Triangle _triangle;
    }

    /**
     * Compares triangles based on one of the coordinates of their centers.
     */
    protected static class CenterComparator
        implements Comparator<Triangle>
    {
        public CenterComparator (int idx)
        {
            _idx = idx;
        }

        // documentation inherited from interface Comparator
        public int compare (Triangle t1, Triangle t2)
        {
            return Float.compare(t1.getCenter().get(_idx), t2.getCenter().get(_idx));
        }

        /** The index of the coordinate to compare. */
        protected int _idx;
    }

    /** The root node of the AABB tree. */
    protected Node _root;
}
