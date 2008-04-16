//
// $Id$

package com.threerings.tudey.util;

import java.awt.Point;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

import org.apache.commons.collections.map.MultiValueMap;

import junit.framework.TestCase;

import com.samskivert.io.ByteArrayOutInputStream;
import com.samskivert.util.RandomUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.export.BinaryExporter;
import com.threerings.export.BinaryImporter;

import com.threerings.tudey.util.CoordMap.CoordEntry;

/**
 * Tests the {@link CoordMultiMap}.
 */
public class CoordMultiMapTest extends TestCase
{
    public CoordMultiMapTest (String name)
    {
        super(name);
    }

    @Override // documentation inherited
    public void setUp ()
    {
        // use a fixed seed so that our results are reproducible
        RandomUtil.rand.setSeed(1199325877849L);
    }

    public void testEmpty ()
    {
        CoordMultiMap<Object> map = new CoordMultiMap<Object>();
        assertEquals(map.size(), 0);
        assertTrue(map.isEmpty());
    }

    public void testInsertion ()
    {
        CoordMultiMap<Integer> map = new CoordMultiMap<Integer>();
        MultiValueMap ref = new MultiValueMap();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);
        assertEquals(map.size(), 500);
        assertEquals(false, map.isEmpty());

        // make sure the maps match up
        assertEquivalent(map, ref);

        // test some random points and make sure they match up
        for (int ii = 0; ii < 1000; ii++) {
            Point pt = getRandomPoint(50);
            assertEquivalent(map.getAll(pt.x, pt.y), (Collection)ref.get(pt));
        }

        // add some more points; this time with a bigger range
        populate(map, ref, 32768, 500);
        assertEquals(map.size(), 1000);
        assertEquivalent(map, ref);
    }

    public void testSimpleDeletion ()
    {
        CoordMultiMap<Integer> map = new CoordMultiMap<Integer>();
        map.put(0, 0, 1);
        map.put(0, 0, 2);
        map.put(0, 0, 3);
        map.put(0, 0, 4);
        assertEquals(4, map.size());
        map.removeAll(0, 0);
        assertEquals(0, map.size());
        assertFalse(map.containsKey(0, 0));
    }

    public void testDeletion ()
    {
        CoordMultiMap<Integer> map = new CoordMultiMap<Integer>();
        MultiValueMap ref = new MultiValueMap();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // remove some random points and make sure they match up
        for (int ii = 0; ii < 500; ii++) {
            Point pt = getRandomPoint(50);
            map.removeAll(pt.x, pt.y);
            ref.remove(pt);
        }
        assertEquivalent(map, ref);

        // clear it out
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    public void testStream ()
        throws IOException, ClassNotFoundException
    {
        CoordMultiMap<Integer> map = new CoordMultiMap<Integer>();
        MultiValueMap ref = new MultiValueMap();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // write the map out to a byte array
        ByteArrayOutInputStream bout = new ByteArrayOutInputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(map);
        out.close();

        // then read it back in and compare
        ObjectInputStream in = new ObjectInputStream(bout.getInputStream());
        @SuppressWarnings("unchecked") CoordMultiMap<Integer> nmap = (CoordMultiMap<Integer>)in.readObject();
        assertEquivalent(nmap, ref);
    }

    public void testExport ()
        throws IOException
    {
        CoordMultiMap<Integer> map = new CoordMultiMap<Integer>();
        MultiValueMap ref = new MultiValueMap();

        // insert a bunch of points into both maps
        populate(map, ref, 50, 500);

        // write the map out to a byte array
        ByteArrayOutInputStream bout = new ByteArrayOutInputStream();
        BinaryExporter out = new BinaryExporter(bout);
        out.writeObject(map);
        out.close();

        // then read it back in and compare
        BinaryImporter in = new BinaryImporter(bout.getInputStream());
        @SuppressWarnings("unchecked") CoordMultiMap<Integer> nmap = (CoordMultiMap<Integer>)in.readObject();
        assertEquivalent(nmap, ref);
    }

    protected static void populate (
        CoordMultiMap<Integer> map, MultiValueMap ref, int range, int count)
    {
        for (int ii = 0; ii < count; ii++) {
            Point pt = getRandomPoint(range);
            Integer value = RandomUtil.rand.nextInt();
            map.put(pt.x, pt.y, value);
            ref.put(pt, value);
        }
    }

    protected static void assertEquivalent (CoordMultiMap<Integer> map, MultiValueMap ref)
    {
        // ensure the sizes are the same
        assertEquals(ref.totalSize(), map.size());
        // ensure everything in the map is in the ref (no extra elements)
        for (Map.Entry<Point, Integer> entry : map.entrySet()) {
            if (!ref.containsKey(entry.getKey())) {
                fail("CoordMultiMap has " + entry.getKey() + " but reference map does not");
            }
        }
        // ensure the map contains everything in the ref (and the stuff matches)
        for (Object obj : ref.keySet()) {
            Point pt = (Point)obj;
            assertEquivalent(map.getAll(pt.x, pt.y), (Collection)ref.get(pt));
        }
    }

    protected static void assertEquivalent (Iterator<Integer> it, Collection col)
    {
        while (it.hasNext()) {
            Integer ii = it.next();
            if (!col.contains(ii)) {
                fail("CoordMultiMap has " + ii + " but reference map does not");
            }
        }
    }

    protected static Point getRandomPoint (int range)
    {
        return new Point(
            RandomUtil.getInt(+range, -range),
            RandomUtil.getInt(+range, -range));
    }
}
