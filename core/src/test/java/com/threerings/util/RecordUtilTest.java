//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.util;

import junit.framework.TestCase;

/**
 * Tests the {@link RecordUtil} methods.
 */
public class RecordUtilTest extends TestCase
{
  public void testValues ()
  {
    Pair pair = new Pair(7, "seven");
    Object[] values = RecordUtil.getValues(pair);
    assertEquals(2, values.length);
    assertEquals(7, values[0]);
    assertEquals("seven", values[1]);
    assertEquals(pair, RecordUtil.newInstance(Pair.class, values));
  }

  public void testConstructorExceptions ()
  {
    // whatever the canonical constructor throws is passed along as is
    try {
      RecordUtil.newInstance(Pair.class, new Object[] { -1, "negative" });
      fail("Constructed an invalid Pair.");
    } catch (IllegalArgumentException iae) {
      assertEquals("Negative count", iae.getMessage());
    }
  }

  public void testNonPublic ()
  {
    try {
      RecordUtil.getValues(new Hidden(1));
      fail("Read a non-public record.");
    } catch (IllegalArgumentException iae) {
      // expected: we don't get around access checks
    }
  }

  public record Pair (int count, String name)
  {
    public Pair {
      if (count < 0) {
        throw new IllegalArgumentException("Negative count");
      }
    }
  }

  record Hidden (int x) {}
}
