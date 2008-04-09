//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests the {@link Capsule}.
 */
public class CapsuleTest extends TestCase
{
    public CapsuleTest (String name)
    {
        super(name);
    }

    public void testSet ()
    {
        Capsule cap1 = new Capsule(1f, 1f, 2f, 3f, 5f);
        Capsule cap2 = new Capsule();
        cap2.set(1f, 1f, 2f, 3f, 5f);
        assertEquals(cap1, cap2);;
    }

    public void testBadSet ()
    {
        try {
            Capsule cap = new Capsule(1f, 1f, 2f, 3f, -1f);
        } catch (IllegalArgumentException iae) {
            return;
        }
        fail("Expected IllegalArgumentException");
    }

    public void testClone ()
    {
        Capsule cap1 = new Capsule(1f, 1f, 2f, 3f, 5f);
        Capsule cap2 = (Capsule)cap1.clone();
        assertEquals(cap1, cap2);
        assertEquals(cap1.getBounds(), cap2.getBounds());
    }

    public void testBounds ()
    {
        Capsule cap = new Capsule(1f, 1f, 2f, 3f, 5f);
        Bounds bounds = new Bounds(-4f, -4f, 7f, 8f);
        assertEquals(cap.getBounds(), bounds);
    }

    public void testPointIntersection ()
    {
        // check inside
        Capsule cap = new Capsule(-1f, 0f, 1f, 0f, 1f);
        Point point = new Point(0f, 0f);
        assertTrue(cap.intersects(point));

        // check outside
        point.set(3f, 3f);
        assertFalse(cap.intersects(point));

        // check edge
        point.set(0f, 1f);
        assertTrue(cap.intersects(point));
    }

    public void testLineIntersection ()
    {
        // check inside
        Capsule cap = new Capsule(-2f, 0f, 2f, 0f, 1f);
        Line line = new Line(-0.5f, 0.5f, 0.5f, -0.5f);
        assertTrue(cap.intersects(line));
        line.set(-0.5f, -0.5f, 0.5f, -0.25f);
        assertTrue(cap.intersects(line));

        // check one end inside
        line.set(-3f, 2f, 0f, 0f);
        assertTrue(cap.intersects(line));
        line.set(-0.25f, 0.5f, 3f, 0.75f);
        assertTrue(cap.intersects(line));

        // check through
        line.set(-3f, 0.5f, 3f, 0.6f);
//TODO        assertTrue(cap.intersects(line));
        line.set(0.75f, 2f, 0.75f, -2f);
        assertTrue(cap.intersects(line));

        // check outside
        line.set(4f, 0f, 5f, 2f);
        assertFalse(cap.intersects(line));

        // check one point on edge
        line.set(2f, 0f, 3f, 4f);
        assertTrue(cap.intersects(line));
    }

    public void testCircleIntersection ()
    {
        // check inside
        // check outside
        // check edge
        // check enclosing
    }

    public void testRectangleIntersection ()
    {
        // check coincident
        // check overlapping
        // check not overlapping
    }

    public void testCapsuleIntersection ()
    {
        // check coincident
        // check overlapping
        // check not overlapping
    }
}
