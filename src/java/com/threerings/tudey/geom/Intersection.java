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

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        if (!(other instanceof Intersection)) {
            return false;
        }
        Intersection oisect = (Intersection)other;
        return (_shape1.equals(oisect._shape1) && _shape2.equals(oisect._shape2)) ||
            (_shape1.equals(oisect._shape2) && _shape2.equals(oisect._shape1));
    }

    @Override // documentation inherited
    public String toString ()
    {
        return "Intersection[s1=" + _shape1 + ", s2=" + _shape2 + "]";
    }

    /** The intersecting shapes. */
    protected Shape _shape1, _shape2;
}
