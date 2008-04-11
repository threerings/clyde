//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;

/**
 * An intersection between two shapes.
 */
public class Intersection
{
    public Intersection (Shape shape1, Shape shape2)
    {
        _shape1 = shape1;
        _shape2 = shape2;
    }

    /**
     * Returns the first intersecting shape.
     */
    public Shape getShape1 ()
    {
        return _shape1;
    }

    /**
     * Returns the second intersecting shape.
     */
    public Shape getShape2 ()
    {
        return _shape2;
    }

    /** The intersecting shapes. */
    protected Shape _shape1, _shape2;
}
