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
 *
 * <p>Be aware that moving shapes are not automatically rehashed. (Implying the
 * intersection methods will not always be accurate.) Rather, it is assumed that
 * intersections will be calculated once per frame via {@link
 * getIntersecting(List<Intersection>)} at which point the entire space is
 * efficiently rehashed. Clients can, however, force a rehash by calling {@link
 * rehash()}.</p>
 *
 * <p>Also be aware that this space makes extensive use of the automatic
 * classification of shapes as active or static. Moving an otherwise stationary
 * shape will permanently and irrevocably reclassify it as active and, in the
 * process, degrade collision detection performance.</p>
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
        _cellSize = gridsize;
    }

    /**
     * Forces a rehash of all the shapes in this space.
     */
    public void rehash ()
    {
        // rehash the statics
        CoordMultiMap<Shape> nstatic = new CoordMultiMap<Shape>();
        for (Shape shape : _static.values()) {
            map(shape, nstatic);
        }
        _static = nstatic;
        // rehash the actives
        CoordMultiMap<Shape> nactive = new CoordMultiMap<Shape>();
        for (Shape shape : _active.values()) {
            map(shape, nactive);
        }
        _active = nactive;
    }

    @Override // documentation inherited
    public boolean add (Shape shape)
    {
        if (shape.getSpace() == this) {
            return false;
        }
        shape.setSpace(this);
        map(shape, shape.isActive() ? _active : _static);
        _size++;
        return true;
    }

    @Override // documentation inherited
    public boolean remove (Shape shape)
    {
        if (shape.getSpace() != this) {
            return false;
        }
        shape.setSpace(null);
        unmap(shape, shape.isActive() ? _active : _static);
        _size--;
        return true;
    }

    @Override // documentation inherited
    public int size ()
    {
        return _size;
    }

    @Override // documentation inherited
    public void clear ()
    {
        _active.clear();
        _static.clear();
        _size = 0;
    }

    @Override // documentation inherited
    public boolean intersects (Shape other)
    {
        Bounds bounds = other.getBounds();
        int minx = (int)(bounds.minX / _cellSize);
        int miny = (int)(bounds.minY / _cellSize);
        int maxx = (int)(bounds.maxX / _cellSize);
        int maxy = (int)(bounds.maxY / _cellSize);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                // check against the active shapes
                for (Iterator<Shape> ii = _active.getAll(xx, yy); ii.hasNext(); ) {
                    if (other.intersects(ii.next())) {
                        return true;
                    }
                }
                // check against the static shapes
                for (Iterator<Shape> ii = _static.getAll(xx, yy); ii.hasNext(); ) {
                    if (other.intersects(ii.next())) {
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
        int minx = (int)(bounds.minX / _cellSize);
        int miny = (int)(bounds.minY / _cellSize);
        int maxx = (int)(bounds.maxX / _cellSize);
        int maxy = (int)(bounds.maxY / _cellSize);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                // check against the active shapes
                for (Iterator<Shape> ii = _active.getAll(xx, yy); ii.hasNext(); ) {
                    Shape shape = ii.next();
                    if (shape.testIntersectionFlags(other) && shape.intersects(other)) {
                        results.add(shape);
                    }
                }
                // check against the static shapes
                for (Iterator<Shape> ii = _static.getAll(xx, yy); ii.hasNext(); ) {
                    Shape shape = ii.next();
                    if (shape.testIntersectionFlags(other) && shape.intersects(other)) {
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
        CoordMultiMap<Shape> nactive = new CoordMultiMap<Shape>();
        Set<Shape> actives = new HashSet<Shape>(_active.values()); // only want the unique shapes
        // only look for collisions with active shapes
        for (Shape shape : actives) {
            Bounds bounds = shape.getBounds();
            int minx = (int)(bounds.minX / _cellSize);
            int miny = (int)(bounds.minY / _cellSize);
            int maxx = (int)(bounds.maxX / _cellSize);
            int maxy = (int)(bounds.maxY / _cellSize);
            for (int xx = minx; xx <= maxx; xx++) {
                for (int yy = miny; yy <= maxy; yy++) {
                    // collide with the static
                    for (Iterator<Shape> ii = _static.getAll(xx, yy); ii.hasNext(); ) {
                        Shape sshape = ii.next();
                        if (shape.testIntersectionFlags(sshape) && shape.intersects(sshape)) {
                            results.add(new Intersection(shape, sshape));
                        }
                    }
                    // collide with the active and rehash in the process (this
                    // also lets use avoid filtering duplicate collisions)
                    for (Iterator<Shape> ii = nactive.getAll(xx, yy); ii.hasNext(); ) {
                        Shape ashape = ii.next();
                        if (shape.testIntersectionFlags(ashape) && shape.intersects(ashape)) {
                            results.add(new Intersection(shape, ashape));
                        }
                    }
                    nactive.put(xx, yy, shape);
                }
            }
        }
        // use our newly rehashed set of active shapes
        _active = nactive;
    }

    /**
     * Adds the shape to the specified map.
     */
    protected void map (Shape shape, CoordMultiMap<Shape> map)
    {
        Bounds bounds = shape.getBounds();
        int minx = (int)(bounds.minX / _cellSize);
        int miny = (int)(bounds.minY / _cellSize);
        int maxx = (int)(bounds.maxX / _cellSize);
        int maxy = (int)(bounds.maxY / _cellSize);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                map.put(xx, yy, shape);
            }
        }
    }

    /**
     * Removes the shape from the specified map.
     */
    protected void unmap (Shape shape, CoordMultiMap<Shape> map)
    {
        Bounds bounds = shape.getBounds();
        int minx = (int)(bounds.minX / _cellSize);
        int miny = (int)(bounds.minY / _cellSize);
        int maxx = (int)(bounds.maxX / _cellSize);
        int maxy = (int)(bounds.maxY / _cellSize);
        for (int xx = minx; xx <= maxx; xx++) {
            for (int yy = miny; yy <= maxy; yy++) {
                map.remove(xx, yy, shape);
            }
        }
    }

    @Override // documentation inherited
    protected void shapeWillMove (Shape shape)
    {
        if (!shape.isActive()) {
            unmap(shape, _static);
        }
    }

    @Override // documentation inherited
    protected void shapeDidMove (Shape shape)
    {
        // check if the shape should be reclassified as active
        if (!shape.isActive()) {
            map(shape, _active);
        }
    }

    /** The spatial hash containing the static shapes. */
    protected CoordMultiMap<Shape> _static = new CoordMultiMap<Shape>();

    /** The spatial hash containing the active shapes. */
    protected CoordMultiMap<Shape> _active = new CoordMultiMap<Shape>();

    /** The number of shapes in the space. */
    protected int _size;

    /** The width/height of each grid cell. */
    protected final float _cellSize;
}
