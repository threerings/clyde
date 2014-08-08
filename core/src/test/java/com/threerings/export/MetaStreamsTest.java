//
// $Id$

package com.threerings.export;

import java.io.InputStream;
import java.io.IOException;

import java.util.List;

import com.google.common.collect.ImmutableList;

import com.samskivert.io.ByteArrayOutInputStream;

import junit.framework.TestCase;

/**
 * Tests some things with the MetaStreams.
 */
public class MetaStreamsTest extends TestCase
{
    public MetaStreamsTest (String name)
    {
        super(name);
    }

    public void testLengths ()
        throws IOException
    {
        List<Long> testValues = ImmutableList.of(0L, Long.MAX_VALUE, (long)Integer.MAX_VALUE, 127L);
        List<Integer> byteCounts = ImmutableList.of(1, 9, 5, 1);
        assertEquals(testValues.size(), byteCounts.size());

        ByteArrayOutInputStream out = new ByteArrayOutInputStream();

        // first try writing each one separately
        for (int ii = 0, nn = testValues.size(); ii < nn; ii++) {
            long value = testValues.get(ii);
            int expectedCount = byteCounts.get(ii);
            System.err.println("etc, etc: " + value + " / " + expectedCount);

            MetaStreams.writeLength(out, value);
            assertEquals(expectedCount, out.size());
            long readBack = MetaStreams.readLength(out.getInputStream());
            assertEquals(value, readBack);
            out.reset();
        }

        // write them all!
        for (Long value : testValues) {
            MetaStreams.writeLength(out, value);
        }
        // read them all back!
        InputStream in = out.getInputStream();
        for (Long value : testValues) {
            assertEquals((long)value, MetaStreams.readLength(in));
        }

        // try one more read: we should get -1
        assertEquals(-1L, MetaStreams.readLength(in));
    }
}
