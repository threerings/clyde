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
 * Tests some things with Streams.
 */
public class StreamsTest extends TestCase
{
  public StreamsTest (String name)
  {
    super(name);
  }

  public void testValues ()
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

      Streams.writeVarLong(out, value);
      assertEquals(expectedCount, out.size());
      long readBack = Streams.readVarLong(out.getInputStream());
      assertEquals(value, readBack);
      out.reset();
    }

    // write them all!
    for (Long value : testValues) {
      Streams.writeVarLong(out, value);
    }
    // read them all back!
    InputStream in = out.getInputStream();
    for (Long value : testValues) {
      assertEquals((long)value, Streams.readVarLong(in));
    }

    // try one more read: we should get -1
    assertEquals(-1L, Streams.readVarLong(in));
  }
}
