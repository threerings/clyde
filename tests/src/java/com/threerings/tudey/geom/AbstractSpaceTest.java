//
// $Id$

package com.threerings.tudey.geom;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.util.RandomUtil;

import junit.framework.TestCase;

/**
 * Tests {@link Space}s.
 */
public abstract class AbstractSpaceTest extends TestCase
{
    public AbstractSpaceTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    public void setUp ()
    {
        // create the space implementation to test
        _space = createSpace();
        assertNotNull(_space);
        // create the test shapes
        createTestShapes();
        // use a fixed seed so that our results are reproducible
        RandomUtil.rand.setSeed(1199325877849L);
    }

    public void testEmpty ()
    {
        assertTrue(_space.isEmpty());
    }

    public void testAdd ()
    {
        assertTrue(_space.add(_s1));
        assertTrue(_space.add(_s2));
        assertTrue(_space.add(_s3));
        assertEquals(3, _space.size());
        assertFalse(_space.isEmpty());

        assertTrue(_space.contains(_s1));
        assertTrue(_space.contains(_s2));
        assertTrue(_space.contains(_s3));
        assertFalse(_space.contains(_s4));

        // make sure we can't readd a space
        assertFalse(_space.add(_s1));
    }

    public void testRemove ()
    {
        _space.add(_s1);
        _space.add(_s2);
        _space.add(_s3);

        assertTrue(_space.contains(_s1));
        assertTrue(_space.remove(_s1));
        assertFalse(_space.remove(_s1));

        assertEquals(2, _space.size());
        assertFalse(_space.isEmpty());

        assertTrue(_space.add(_s1)); // test re-adding it

        assertFalse(_space.remove(_s4));
    }

    public void testClear ()
    {
        _space.add(_s1);
        _space.add(_s2);
        _space.add(_s3);

        assertFalse(_space.isEmpty());
        _space.clear();
        assertEquals(0, _space.size());
        assertTrue(_space.isEmpty());
    }

    public void testIntersects ()
    {
        addTestShapes();

        Circle circle = new Circle(0f, 0f, 2f);
        Line line = new Line(-10f, 0f, 10f, 0f);
        Point point = new Point(3f, 3f);
        Capsule cap = new Capsule(-7f, 7f, -4f, 4f, 2f);

        assertTrue(_space.intersects(circle));
        assertTrue(_space.intersects(line));
        assertTrue(_space.intersects(point));
        assertTrue(_space.intersects(cap));
        _space.remove(_s2);
        assertFalse(_space.intersects(point));
        ((Line)_s5).set(100f, 100f, 101f, 101f);
        assertFalse(_space.intersects(cap));
        _space.clear();
        assertFalse(_space.intersects(circle));
        assertFalse(_space.intersects(line));
    }

    public void testGetIntersectingShape ()
    {
        addTestShapes();

        Circle circle = new Circle(0f, 0f, 1.8f);
        Line line = new Line(-10f, 0f, 10f, 0f);
        Point point = new Point(3f, 3f);
        Capsule cap = new Capsule(-7f, 7f, -4f, 4f, 2f);

        testIntersection(circle, _s1, _s3, _s4, _s7);
        testIntersection(line, _s2, _s3, _s7);
        testIntersection(point, _s2);
        testIntersection(cap, _s5);
        _space.remove(_s2);
        testIntersection(point);
        ((Line)_s5).set(100f, 100f, 101f, 101f);
        testIntersection(cap);
    }

    public void testGetIntersectingSelf ()
    {
        addTestShapes();

        // no conflicts initially
        testSelfIntersection();

        // create a bunch of conflicts
        Rectangle rect = new Rectangle();
        _space.add(rect);
        rect.set(-2f, -2f, 2f, 2f); // mark the shape active
        testSelfIntersection(new Intersection(rect, _s1), new Intersection(rect, _s2),
                             new Intersection(rect, _s3), new Intersection(rect, _s4),
                             new Intersection(rect, _s7), new Intersection(rect, _s9));

        // move things around
        ((Point)_s1).set(10f, 10f);
        _space.remove(_s2);
        _space.remove(_s3);
        ((Circle)_s4).setLocation(-6f, 5f);
        testSelfIntersection(new Intersection(_s5, _s4),
                             new Intersection(rect, _s7), new Intersection(rect, _s9));
    }

    public void testStress ()
    {
        // add one metric truckload of static rectangles (tiles&blocks)
        for (int ii = 0; ii < 1000; ii++) {
            int minx = RandomUtil.getInt(200, -200), miny = RandomUtil.getInt(200, -200);
            int w = RandomUtil.getInt(4, 1), h = RandomUtil.getInt(4, 1);
            Rectangle rect = new Rectangle(minx, miny, minx+w, miny+h);
            _space.add(rect);
        }

        // add (e^-(i*pi))*(-100) active circles (players&monsters)
        for (int ii = 0; ii < 50; ii++) {
            float x = RandomUtil.getFloat(400f) - 200f, y = RandomUtil.getFloat(400f) - 200f;
            float r = RandomUtil.getFloat(4f) + 0.1f;
            Circle circle = new Circle();
            _space.add(circle);
            circle.set(x, y, r); // get it marked as active
        }

        // intersect 'em! a lot! (because typically we're doing intersections, not adding/removing)
        for (int ii = 0; ii < 25; ii++) {
            _space.getIntersecting(new ArrayList<Intersection>());
        }
    }

    /**
     * Creates some test shapes.
     */
    protected void createTestShapes ()
    {
        _s1 = new Point (1f, 1f);
        _s2 = new Rectangle(2f, -1f, 4f, 4f);
        _s3 = new Rectangle(-3.5f, -0.5f, -1.5f, 0.5f);
        _s4 = new Circle(-1f, 1f, 0.25f);
        _s5 = new Line(-6f, 5f, -2f, 3f);
        _s6 = new Capsule(-1f, 3f, 1f, 3f, 0.5f);
        _s7 = new Line(0.5f, 2f, 0.5f, -1f);
        _s8 = new Circle(0f, -8f, 2f);
        _s9 = new Capsule(-1f, -3f, 3f, -5f, 1f);
    }

    /**
     * Adds the test shapes to the space.
     */
    protected void addTestShapes ()
    {
        _space.add(_s1);
        _space.add(_s2);
        _space.add(_s3);
        _space.add(_s4);
        _space.add(_s5);
        _space.add(_s6);
        _space.add(_s7);
        _space.add(_s8);
        _space.add(_s9);
    }

    /**
     * Performs a single intersection test and verifies the results.
     */
    protected void testIntersection (Shape test, Shape... results)
    {
        List<Shape> actual = new ArrayList<Shape>();
        List<Shape> expected = Arrays.asList(results);
        _space.getIntersecting(test, actual);
        assertEquivalent(expected, actual);
    }

    /**
     * Performs a single self intersection test and verifies the results.
     */
    protected void testSelfIntersection (Intersection... results)
    {
        List<Intersection> actual = new ArrayList<Intersection>();
        List<Intersection> expected = Arrays.asList(results);
        _space.getIntersecting(actual);
        assertEquivalent(expected, actual);
    }

    /**
     * Returns whether the two lists contain the same items.
     */
    @SuppressWarnings("unchecked")
    protected void assertEquivalent (List expected, List actual)
    {
        for (int ii = 0, nn = expected.size(); ii < nn; ii++) {
            Object expect = expected.get(ii);
            if (!actual.contains(expect)) {
                fail("Actual list missing expected item " + expect);
            }
        }
        for (int ii = 0, nn = actual.size(); ii < nn; ii++) {
            Object got = actual.get(ii);
            if (!expected.contains(got)) {
                fail("Actual list contains unexpected item " + got);
            }
        }
    }

    /**
     * Creates the space used in this test.
     */
    protected abstract Space createSpace ();

    /** The space to test. */
    protected Space _space;

    /** Some shapes for use in the tests. */
    protected Shape _s1, _s2, _s3, _s4, _s5, _s6, _s7, _s8, _s9;
}
