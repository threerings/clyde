//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;
import java.util.ArrayList;

/**
 * A space that doesn't do anything special to accelerate intersection testing.  Useful for very
 * simple spaces, and for debugging.
 */
public class SimpleSpace extends Space
{
    @Override // documentation inherited
    public boolean add (Shape shape)
    {
        if (shape.getSpace() == this) {
            return false;
        }
        shape.setSpace(this);
        _shapes.add(shape);
        if (size() == 1) {
            _bounds.set(shape.getBounds());
        } else {
            _bounds.addLocal(shape.getBounds());
        }
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
    public boolean intersects (Shape other)
    {
        Bounds obounds = other.getBounds();
        for (int ii = 0, nn = _shapes.size(); ii < nn; ii++) {
            Shape shape = _shapes.get(ii);
            if (shape.getBounds().intersects(obounds) && shape.intersects(other)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public void getIntersecting (Shape shape, List<Shape> results)
    {
        results.clear();
        Bounds bounds = shape.getBounds();
        for (int ii = 0, nn = _shapes.size(); ii < nn; ii++) {
            Shape oshape = _shapes.get(ii);
            if (shape.testIntersectionFlags(oshape) && bounds.intersects(oshape.getBounds()) &&
                    shape.intersects(oshape)) {
                results.add(oshape);
            }
        }
    }

    @Override // documentation inherited
    public abstract void getIntersecting (List<Intersection> results)
    {
        results.clear();
    }

    @Override // documentation inherited
    protected void shapeDidMove (Shape shape)
    {
        // expand the bounds
        _bounds.addLocal(shape.getBounds());
    }

    /** The shapes in the space. */
    protected List<Shape> _shapes = new ArrayList<Shape>();
}
