//
// $Id$

package com.threerings.tudey.util;

import java.awt.Point;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.util.RandomUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.util.CoordIntMap.CoordIntEntry;

/**
 * Tests the {@link CoordIntMap}.
 */
public class CoordIntMapTest extends TestCase
{
    public CoordIntMapTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    public void setUp ()
    {
        // use a fixed seed so that our results are reproducible
        RandomUtil.rand.setSeed(1199331273112L);
    }

    public void testEmpty ()
    {
        CoordIntMap map = new CoordIntMap();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    public void testInsertion ()
    {
        CoordIntMap map = new CoordIntMap();
        HashMap<Point, Integer> ref = new HashMap<Point, Integer>();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);
        assertEquals(500, map.size());
        assertEquals(false, map.isEmpty());

        // make sure the maps contain the same contents
        assertEquivalent(map, ref);

        // test some random points and make sure they match up
        for (int ii = 0; ii < 1000; ii++) {
            Point pt = CoordMapTest.getRandomPoint(50);
            Integer v1 = ref.get(pt);
            Integer v2 = map.containsKey(pt.x, pt.y) ? map.get(pt.x, pt.y) : null;
            assertEquals(v1, v2);
        }

        // show some statistics
        map.printStatistics();

        // add some more points; this time with a bigger range
        populate(map, ref, 32768, 500);
        assertEquals(map.size(), 1000);
        assertEquivalent(map, ref);

        // show the stats again
        map.printStatistics();

        // replace some of the existing points
        int idx = 0;
        for (Map.Entry<Point, Integer> entry : ref.entrySet()) {
            if ((idx++ % 2) == 1) {
                Point pt = entry.getKey();
                int value = RandomUtil.rand.nextInt();
                entry.setValue(value);
                map.put(pt.x, pt.y, value);
            }
        }
        assertEquals(map.size(), 1000);
        assertEquivalent(map, ref);
    }

    public void testDeletion ()
    {
        CoordIntMap map = new CoordIntMap();
        HashMap<Point, Integer> ref = new HashMap<Point, Integer>();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // remove every other point as read from the reference map
        int idx = 0;
        for (Iterator<Map.Entry<Point, Integer>> it = ref.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Point, Integer> entry = it.next();
            if ((idx++ % 2) == 1) {
                it.remove();
                Point pt = entry.getKey();
                map.remove(pt.x, pt.y);
            }
        }
        assertEquals(250, map.size());
        assertEquivalent(map, ref);

        // now every other point as read from the test map
        idx = 0;
        for (Iterator<CoordIntEntry> it = map.entrySet().iterator(); it.hasNext(); ) {
            CoordIntEntry entry = it.next();
            if ((idx++ % 2) == 1) {
                it.remove();
                Point pt = entry.getKey();
                ref.remove(pt);
            }
        }
        assertEquals(125, map.size());
        assertEquivalent(map, ref);

        // clear it out
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    public void testStream ()
        throws IOException, ClassNotFoundException
    {
        CoordIntMap map = new CoordIntMap();
        HashMap<Point, Integer> ref = new HashMap<Point, Integer>();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // write the map out to a byte array
        ByteArrayOutInputStream bout = new ByteArrayOutInputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(map);
        out.close();

        // then read it back in and compare
        ObjectInputStream in = new ObjectInputStream(bout.getInputStream());
        map = (CoordIntMap)in.readObject();
        assertEquivalent(map, ref);
    }

    public void testExport ()
        throws IOException
    {
        CoordIntMap map = new CoordIntMap();
        HashMap<Point, Integer> ref = new HashMap<Point, Integer>();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // write the map out to a byte array
        ByteArrayOutInputStream bout = new ByteArrayOutInputStream();
        BinaryExporter out = new BinaryExporter(bout);
        out.writeObject(map);
        out.close();

        // then read it back in and compare
        BinaryImporter in = new BinaryImporter(bout.getInputStream());
        map = (CoordIntMap)in.readObject();
        assertEquivalent(map, ref);
    }

    protected static void populate (
        CoordIntMap map, HashMap<Point, Integer> ref, int range, int count)
    {
        for (int ii = 0; ii < count; ii++) {
            Point pt;
            do {
                pt = CoordMapTest.getRandomPoint(range);
            } while (ref.containsKey(pt));
            int value = RandomUtil.rand.nextInt();
            map.put(pt.x, pt.y, value);
            ref.put(pt, value);
        }
    }

    protected static void assertEquivalent (CoordIntMap map, HashMap<Point, Integer> ref)
    {
        assertEquals(map.size(), ref.size());
        for (Map.Entry<Point, Integer> entry : map.entrySet()) {
            Integer value = entry.getValue();
            assertEquals(value, (Integer)ref.get(entry.getKey()));
        }
        for (Map.Entry<Point, Integer> entry : ref.entrySet()) {
            Point pt = entry.getKey();
            int value = entry.getValue();
            assertTrue(map.containsKey(pt.x, pt.y));
            assertEquals(value, map.get(pt.x, pt.y));
        }
    }
}
