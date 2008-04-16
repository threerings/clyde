//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.threerings.tudey.util.CoordMultiMap;

/**
 * A space that uses spatial hashing to accelerate collision detection.
 */
public class HashSpace extends Space
{
    /**
     * Creates a new spatial hashing structure with a size-1 grid.
     */
    public HashSpace ()
    {
        this(1f);
    }

    /**
     * Creates a new spatial hashing structure.
     *
     * @param gridsize the dimensions of the grid squares
     */
    public HashSpace (float gridsize)
    {
        if (gridsize <= 0f) {
            throw new IllegalArgumentException("Grid size must be greater than zero.");
        }
        _size = gridsize;
    }

    @Override // documentation inherited
    public boolean add (Shape shape)
    {
        if (shape.getSpace() == this) {
            return false;
        }
        shape.setSpace(this);
        return true;
    }

    @Override // documentation inherited
    public boolean remove (Shape shape)
    {
        if (shape.getSpace() != this) {
            return false;
        }
        shape.setSpace(null);
        return true;
    }

    @Override // documentation inherited
    public int size ()
    {
        return _shapes.size();
    }

    @Override // documentation inherited
    public void clear ()
    {
        _shapes.clear();
        _active.clear();
        _static.clear();
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        Bounds bounds = other.getBounds();
        int minx = (int)(bounds.minX / _size);
        int miny = (int)(bounds.minY / _size);
        int maxx = (int)(bounds.maxX / _size);
        int maxy = (int)(bounds.maxY / _size);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                // check against the active shapes
                for (Iterator<Shape> ii = _active.getAll(xx, yy); ii.hasNext(); ) {
                    if (checkIntersects(other, ii.next())) {
                        return true;
                    }
                }
                // check against the static shapes
                for (Iterator<Shape> ii = _static.getAll(xx, yy); ii.hasNext(); ) {
                    if (checkIntersects(other, ii.next())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override // documentation inherited
    public void getIntersecting (Shape other, List<Shape> results)
    {
        results.clear();
        Bounds bounds = other.getBounds();
        int minx = (int)(bounds.minX / _size);
        int miny = (int)(bounds.minY / _size);
        int maxx = (int)(bounds.maxX / _size);
        int maxy = (int)(bounds.maxY / _size);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                // check against the active shapes
                for (Iterator<Shape> ii = _active.getAll(xx, yy); ii.hasNext(); ) {
                    Shape shape = ii.next();
                    if (checkIntersects(other, shape)) {
                        results.add(shape);
                    }
                }
                // check against the static shapes
                for (Iterator<Shape> ii = _static.getAll(xx, yy); ii.hasNext(); ) {
                    Shape shape = ii.next();
                    if (checkIntersects(other, shape)) {
                        results.add(shape);
                    }
                }
            }
        }
    }

    @Override // documentation inherited
    public void getIntersecting (List<Intersection> results)
    {
        results.clear();
    }

    /**
     * Adds the shape to the specified map.
     */
    protected void map (Shape shape, CoordMultiMap<Shape> map)
    {
        Bounds bounds = shape.getBounds();
        int minx = (int)(bounds.minX / _size);
        int miny = (int)(bounds.minY / _size);
        int maxx = (int)(bounds.maxX / _size);
        int maxy = (int)(bounds.maxY / _size);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                map.put(xx, yy, shape);
            }
        };
    }

    /**
     * Removes the shape from the specified map.
     */
    protected void unmap (Shape shape, CoordMultiMap<Shape> map)
    {
        Bounds bounds = shape.getBounds();
        int minx = (int)(bounds.minX / _size);
        int miny = (int)(bounds.minY / _size);
        int maxx = (int)(bounds.maxX / _size);
        int maxy = (int)(bounds.maxY / _size);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                map.remove(xx, yy, shape);
            }
        };
    }

    /**
     * Checks if the two shapes intersect.
     */
    protected boolean checkIntersects (Shape s1, Shape s2)
    {
        return s1.testIntersectionFlags(s2) && s1.intersects(s2);
    }

    @Override // documentation inherited
    protected void shapeWillMove (Shape shape)
    {
        // remove from wherever
    }

    @Override // documentation inherited
    protected void shapeDidMove (Shape shape)
    {
        // add to active
    }

    /** A wrapper for shapes. */
    protected static final class ShapeWrapper
    {
        public Shape shape;
        public boolean active;

        public ShapeWrapper (Shape shape)
        {
            this.shape = shape;
            this.active = false;
        }

        @Override // documentation inherited
        public boolean equals (Object other)
        {
            return shape.equals(other);
        }
        
        @Override // documentation inherited
        public int hashCode ()
        {
            return shape.hashCode();
        }
    }

    /** The shapes in the space. */
    protected Set<ShapeWrapper> _shapes = new HashSet<ShapeWrapper>();

    /** The spatial hash containing the static shapes. */
    protected CoordMultiMap<Shape> _static = new CoordMultiMap<Shape>();

    /** The spatial hash containing the active shapes. */
    protected CoordMultiMap<Shape> _active = new CoordMultiMap<Shape>();

    /** The size of the grid squares. */
    protected final float _size;
}
