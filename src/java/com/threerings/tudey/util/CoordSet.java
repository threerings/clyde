//
// $Id$

package com.threerings.tudey.util;

import java.awt.Point;

import com.samskivert.util.ArrayIntSet;

/**
 * Contains a set of packed coordinates.
 */
public class CoordSet extends ArrayIntSet
{
    /**
     * Adds the specified coordinates to the set.
     */
    public boolean add (int x, int y)
    {
        return add(CoordUtil.getCoord(x, y));
    }

    /**
     * Checks whether the set contains the specified value.
     */
    public boolean contains (int x, int y)
    {
        return contains(CoordUtil.getCoord(x, y));
    }

    /**
     * Removes the specified coordinates from the set.
     */
    public boolean remove (int x, int y)
    {
        return remove(CoordUtil.getCoord(x, y));
    }

    /**
     * Finds and returns the point in the set closest to the one given.
     */
    public Point getClosestPoint (int x, int y)
    {
        int cdist = Integer.MAX_VALUE;
        Point pt = new Point();
        for (int ii = 0; ii < _size; ii++) {
            int coord = _values[ii], cx = CoordUtil.getX(coord), cy = CoordUtil.getY(coord);
            int dist = CoordUtil.getDistance(x, y, cx, cy);
            if (dist < cdist) {
                pt.setLocation(cx, cy);
                cdist = dist;
            }
        }
        return (cdist < Integer.MAX_VALUE) ? pt : null;
    }
}
