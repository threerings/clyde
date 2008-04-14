//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;
import java.util.ArrayList;

import com.threerings.tudey.util.CoordIntMap;

/**
 * A space that uses spatial hashing to accelerate collision detection.
 */
public class HashSpace extends Space
{
    @Override // documentation inherited
    public boolean add (Shape shape)
    {
        return false;
    }

    @Override // documentation inherited
    public boolean remove (Shape shape)
    {
        return false;
    }

    @Override // documentation inherited
    public int size ()
    {
        return 0;
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
    protected CoordIntMap _static = new CoordIntMap();

    /** The spatial hash containing the active shapes. */
    protected CoordIntMap _active = new CoordIntMap();
}
