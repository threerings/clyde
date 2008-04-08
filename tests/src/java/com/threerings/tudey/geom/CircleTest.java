//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests the {@link Circle}.
 */
public class CircleTest extends TestCase
{
    public CircleTest (String name)
    {
        super(name);
    }

    public void testSet ()
    {
        Circle circle1 = new Circle(1f, 1f, 2f);
        Circle circle2 = new Circle();
        circle2.set(1f, 1f, 2f);
        assertEquals(circle1, circle2);

        circle1.setLocation(3f, 5f);
        circle1.setRadius(8f);
        circle2.set(circle1);
        assertEquals(circle1, circle2);
    }

    public void testBadSet ()
    {
        try {
            Circle circle = new Circle(0f, 0f, -1f);
        } catch (IllegalArgumentException iae) {
            return;
        }
        fail("Expected IllegalArgumentException");
    }

    public void testClone ()
    {
        Circle circle1 = new Circle(1f, 1f, 2f);
        Circle circle2 = (Circle)circle1.clone();
        assertEquals(circle1, circle2);
        assertEquals(circle1.getBounds(), circle2.getBounds());
    }

    public void testBounds ()
    {
        Circle circle = new Circle(1f, 2f, 3f);
        Bounds bounds = new Bounds(-2f, -1f, 4f, 5f);
        assertEquals(circle.getBounds(), bounds);
    }

    public void testPointIntersection ()
    {
        // check inside
        Circle circle = new Circle(0f, 0f, 3f);
        Point point = new Point(1f, 1f);
        assertTrue(circle.intersects(point));

        // check outside
        point.set(5f, 5f);
        assertFalse(circle.intersects(point));

        // check on circle
        point.set(1f, 0f);
        assertTrue(circle.intersects(point));
    }

    public void testCircleIntersection ()
    {
        // check coincident
        Circle circle1 = new Circle(0f, 0f, 1f);
        Circle circle2 = new Circle(0f, 0f, 1f);
        assertTrue(circle1.intersects(circle2));

        // check overlapping
        circle1.set(0.5f, 0f, 1f);
        assertTrue(circle1.intersects(circle2));

        // check not overlapping
        circle1.set(5f, 0f, 1f);
        assertFalse(circle1.intersects(circle2));
    }

    public void testLineIntersection ()
    {
        // check inside
        Circle circle = new Circle(0f, 0f, 1f);
        Line line = new Line(-0.5f, 0.5f, 0.5f, -0.5f);
        assertTrue(circle.intersects(line));
        line.set(-0.5f, -0.5f, 0.5f, -0.25f);
        assertTrue(circle.intersects(line));

        // check one end inside
        line.set(-2f, 1f, 0f, 0f);
        assertTrue(circle.intersects(line));
        line.set(-0.25f, 0.5f, 2f, 0.75f);
        assertTrue(circle.intersects(line));
        line.set(0.5f, 0.25f, 2f, 1.75f);
        assertTrue(circle.intersects(line));

        // check through
        line.set(-2f, 0.5f, 2f, 0.6f);
        assertTrue(circle.intersects(line));
        line.set(0.75f, 2f, 0.75f, -2f);
        assertTrue(circle.intersects(line));

        // check outside
        line.set(3f, 0f, 5f, 2f);
        assertFalse(circle.intersects(line));

        // check one point on edge
        line.set(1f, 0f, 2f, 4f);
        assertTrue(circle.intersects(line));
    }
}
