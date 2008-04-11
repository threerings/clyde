//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

import com.threerings.math.FloatMath;

/**
 * Tests the {@link Line}.
 */
public class LineTest extends TestCase
{
    public LineTest (String name)
    {
        super(name);
    }

    public void testSet ()
    {
        Line line1 = new Line(1f, 1f, 2f, 3f);
        Line line2 = new Line();
        line2.set(1f, 1f, 2f, 3f);
        assertEquals(line1, line2);
    }

    public void testClone ()
    {
        Line line1 = new Line(1f, 1f, 2f, 3f);
        Line line2 = (Line)line1.clone();
        assertEquals(line1, line2);
        assertEquals(line1.getBounds(), line2.getBounds());
    }

    public void testBounds ()
    {
        Line line = new Line(0f, 0f, 10f, 10f);
        Bounds bounds = new Bounds(0f, 0f, 10f, 10f);
        assertEquals(line.getBounds(), bounds);

        line.set(-1f, 1f, 1f, -1f);
        bounds.set(-1f, -1f, 1f, 1f);
        assertEquals(line.getBounds(), bounds);

        line.set(0f, 1f, 0f, -1f);
        bounds.set(0f, -1f, 0f, 1f);
        assertEquals(line.getBounds(), bounds);
    }

    public void testIntersection ()
    {
        // check conincident
        Line line1 = new Line(0f, 0f, 1f, 1f);
        Line line2 = new Line(0f, 0f, 1f, 1f);
        assertTrue(line1.intersects(line2));

        // check parallel
        line1.set(0f, 0f, 2f, 2f);
        line2.set(0f, -2f, 2f, 0f);
        assertFalse(line1.intersects(line2));

        // check non-intersecting
        line1.set(-1f, -1f, 1f, 1f);
        line2.set(3f, 4f, 5f, 6f);
        assertFalse(line1.intersects(line2));

        // check intersecting
        line1.set(0f, 1f, 1f, 0f);
        line2.set(0f, 0f, 1f, 1f);
        assertTrue(line1.intersects(line2));

        line1.set(-1f, -1f, 1f, 1f);
        line2.set(0f, -1f, 0f, 1f);
        assertTrue(line1.intersects(line2));
    }
}
