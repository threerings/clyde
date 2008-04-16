//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;
import java.util.ArrayList;

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
        if (!_shapes.remove(shape)) {
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
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        return false;
    }

    @Override // documentation inherited
    public void getIntersecting (Shape shape, List<Shape> results)
    {
        results.clear();
    }

    @Override // documentation inherited
    public void getIntersecting (List<Intersection> results)
    {
        results.clear();
    }

    protected void hash (Shape shape, CoordMultiMap<Shape> map)
    {
        Bounds bounds = shape.getBounds();
        int minx = (int)(bounds.minX / _size);
        int miny = (int)(bounds.minY / _size);
        int maxx = (int)(bounds.maxX / _size);
        int maxy = (int)(bounds.maxY / _size);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
            }
        }
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

    /** The shapes in the space. */
    protected ArrayList<Shape> _shapes = new ArrayList<Shape>();

    /** The spatial hash containing the static shapes. */
    protected CoordMultiMap<Shape> _static = new CoordMultiMap<Shape>();

    /** The spatial hash containing the active shapes. */
    protected CoordMultiMap<Shape> _active = new CoordMultiMap<Shape>();

    /** The size of the grid squares. */
    protected final float _size;
}
