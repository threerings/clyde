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
        // check outside
        // check edge
    }

    public void testLineIntersection ()
    {
        // check inside
        // check one end inside
        // check through
        // check outside
        // check one point on edge
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
