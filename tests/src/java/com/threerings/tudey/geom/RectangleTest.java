//
// $Id$

package com.threerings.tudey.geom;

import junit.framework.TestCase;

/**
 * Tests the {@link Rectangle}.
 */
public class RectangleTest extends TestCase
{
    public RectangleTest (String name)
    {
        super(name);
    }

    public void testSet ()
    {
        Rectangle rect1 = new Rectangle(1f, 1f, 2f, 3f);
        Rectangle rect2 = new Rectangle();
        rect2.set(1f, 1f, 2f, 3f);
        assertEquals(rect1, rect2);
    }

    public void testBadSet ()
    {
        try {
            Rectangle rect = new Rectangle(2f, 3f, 0f, 0f);
        } catch (IllegalArgumentException iae) {
            return;
        }
        fail("Expected IllegalArgumentException");
    }

    public void testClone ()
    {
        Rectangle rect1 = new Rectangle(1f, 1f, 2f, 3f);
        Rectangle rect2 = (Rectangle)rect1.clone();
        assertEquals(rect1, rect2);
        assertEquals(rect1.getBounds(), rect2.getBounds());
    }

    public void testBounds ()
    {
        Rectangle rect = new Rectangle(1f, 1f, 2f, 3f);
        Bounds bounds = new Bounds(1f, 1f, 2f, 3f);
        assertEquals(rect.getBounds(), bounds);
    }

    public void testPointIntersection ()
    {
        // check inside
        Rectangle rect = new Rectangle(-2f, -1f, 2f, 1f);
        Point point = new Point(0.5f, -0.75f);
        assertTrue(rect.intersects(point));

        // check outside
        point.set(10f, 10f);
        assertFalse(rect.intersects(point));

        // check edge
        point.set(0f, 1f);
        assertTrue(rect.intersects(point));
    }

    public void testLineIntersection ()
    {
        // check inside
        Rectangle rect = new Rectangle(-2f, -1f, 2f, 1f);
        Line line = new Line(-0.5f, 0.5f, 0.5f, -0.5f);
        assertTrue(rect.intersects(line));
        line.set(-0.5f, -0.5f, 0.5f, -0.25f);
        assertTrue(rect.intersects(line));

        // check one end inside
        line.set(-3f, 2f, 0f, 0f);
        assertTrue(rect.intersects(line));
        line.set(-0.25f, 0.5f, 3f, 0.75f);
        assertTrue(rect.intersects(line));

        // check through
        line.set(-3f, 0.5f, 3f, 0.6f);
        assertTrue(rect.intersects(line));
        line.set(0.75f, 2f, 0.75f, -2f);
        assertTrue(rect.intersects(line));

        // check outside
        line.set(3f, 0f, 5f, 2f);
        assertFalse(rect.intersects(line));

        // check on edge
        line.set(2f, 0f, 3f, 4f);
        assertTrue(rect.intersects(line)); // one point on edge
        line.set(2f, -2f, 2f, 2f);
        assertTrue(rect.intersects(line)); // along vertical edge
        line.set(3f, -1f, -3f, -1f);
        assertTrue(rect.intersects(line)); // along horizontal edge
    }

    public void testCircleIntersection ()
    {
        // check completely
        Rectangle rect = new Rectangle(-2f, -1f, 2f, 1f);
        Circle circle = new Circle (0f, 0f, 0.75f);
        assertTrue(rect.intersects(circle));

        // check outside
        circle.setLocation(10f, 10f);
        assertFalse(rect.intersects(circle));

        // check edge
        circle.set(3f, 0f, 1f);
        assertTrue(rect.intersects(circle));

        // check overlapping
        circle.set(1.25f, 1.25f, 0.4f);
        assertTrue(rect.intersects(circle)); // center outside rect
        circle.set(-1.8f, 0.1f, 0.6f);
        assertTrue(rect.intersects(circle)); // center inside rect

        // check enclosing
        circle.set(0f, 0f, 20f);
        assertTrue(rect.intersects(circle));
    }

    public void testRectangleIntersection ()
    {
        // check coincident
        Rectangle rect1 = new Rectangle(-2f, -1f, 2f, 1f);
        Rectangle rect2 = new Rectangle(-2f, -1f, 2f, 1f);
        assertTrue(rect1.intersects(rect2));

        // check overlapping
        rect1.set(0.5f, 0.5f, 3f, 3f);
        assertTrue(rect1.intersects(rect2));

        // check not overlapping
        rect1.set(10f, 10f, 11f, 11f);
        assertFalse(rect1.intersects(rect2));
    }
}
