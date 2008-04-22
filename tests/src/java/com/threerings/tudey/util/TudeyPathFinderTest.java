//
// $Id$

package com.threerings.tudey.util;

import java.util.List;

import junit.framework.TestCase;

import com.threerings.tudey.geom.Circle;
import com.threerings.tudey.geom.Capsule;
import com.threerings.tudey.geom.Point;
import com.threerings.tudey.geom.Rectangle;
import com.threerings.tudey.geom.Space;
import com.threerings.tudey.geom.HashSpace;

/**
 * Tests the {@link TudeyPathFinder}.
 */
public class TudeyPathFinderTest extends TestCase
{
    public TudeyPathFinderTest (String name)
    {
        super(name);
    }

    public void testPath ()
    {
        Space space = new HashSpace(2f);
        TudeyPathFinder pather = new TudeyPathFinder(space);
        populateSpace(space);

        Point start = new Point(), end = new Point();
        List<Point> path;

        // get out of the U
        start.set(3.5f, -0.5f);
        end.set(7f, 0.5f);
        path = pather.findPath(start, end, 0.45f, 20, true);
        assertValidPath(path, start, end, 0.45f, space);

        // top left to bottom right
        start.set(-5f, 7f);
        end.set(6.5f, -2.5f);
        path = pather.findPath(start, end, 0.45f, 50, true);
        assertValidPath(path, start, end, 0.45f, space);

        // through the corridor
        start.set(-2f, 3f);
        end.set(-2f, 7f);
        path = pather.findPath(start, end, 0.95f, 15, true);
        assertValidPath(path, start, end, 0.95f, space);

        // through the north-east quadrant
        start.set(8f, 8f);
        end.set(1f, 3f);
        path = pather.findPath(start, end, 0.45f, 15, true);
        assertValidPath(path, start, end, 0.45f, space);
    }

    /**
     * Asserts that the specified path can be followed in the space.
     */
    protected void assertValidPath (List<Point> path, Point start, Point end, float radius, Space space)
    {
        for (int ii = 0, nn = path.size() - 1; ii < nn; ii++) {
            Point cur = path.get(ii);
            Point next = path.get(ii+1);
            Capsule cap = new Capsule(cur.getX(), cur.getY(), next.getX(), next.getY(), radius);
            // make sure there's a clear path between path points
            assertFalse(space.intersects(cap));
        }
    }

    /**
     * Creates a model of the world.
     */
    protected void populateSpace (Space space)
    {
        // wide U
        space.add(createTile(1, 1, 1, 1));
        space.add(createTile(1, 0, 1, 1));
        space.add(createTile(1, -1, 1, 1));
        space.add(createTile(1, -2, 1, 1));
        space.add(createTile(2, -2, 1, 1));
        space.add(createTile(3, -2, 1, 1));
        space.add(createTile(4, -2, 1, 1));
        space.add(createTile(5, -2, 1, 1));
        space.add(createTile(5, -1, 1, 1));
        space.add(createTile(5, 0, 1, 1));
        space.add(createTile(5, 1, 1, 1));

        // corridor
        space.add(createTile(-4, 3, 1, 1));
        space.add(createTile(-4, 4, 1, 1));
        space.add(createTile(-4, 5, 1, 1));
        space.add(createTile(-4, 6, 1, 1));
        space.add(createTile(-1, 3, 1, 1));
        space.add(createTile(-1, 4, 1, 1));
        space.add(createTile(-1, 5, 1, 1));
        space.add(createTile(-1, 6, 1, 1));

        // props
        space.add(createProp(1.5f, 4.5f, 0.5f));
        space.add(createProp(1f, 8f, 0.5f));

        // randoms
        space.add(createTile(-3, -3, 1, 2));
        space.add(createTile(-4, 1, 4, 1));
        space.add(createTile(3, 4, 2, 1));
        space.add(createTile(5, 4, 2, 1));
        space.add(createTile(2, 6, 3, 1));
    }

    /**
     * Creates a rectangle representing a tile at the specified coordinates having the given size.
     */
    protected Rectangle createTile (int x, int y, int w, int h)
    {
        return new Rectangle(x, y, x+w, y+h);
    }

    /**
     * Creates a circle representing a tile at the specified coordinates having the given size.
     */
    protected Circle createProp (float x, float y, float r)
    {
        return new Circle(x, y, r);
    }
}
