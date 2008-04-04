//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests the {@link Point}.
 */
public class PointTest extends TestCase
{
    public PointTest (String name)
    {
        super(name);
    }

    public void testSet ()
    {
        Point point1 = new Point(1f, 1f);
        Point point2 = new Point();
        point2.set(1f, 1f);
        assertEquals(point1, point2);
    }

    public void testClone ()
    {
        Point point1 = new Point(1f, 1f);
        Point point2 = (Point)point1.clone();
        assertEquals(point1, point2);
        assertEquals(point1.getBounds(), point2.getBounds());
    }

    public void testBounds ()
    {
        Point point = new Point(1f, 1f);
        Bounds bounds = new Bounds(1f, 1f, 1f, 1f);
        assertEquals(point.getBounds(), bounds);
    }
}
