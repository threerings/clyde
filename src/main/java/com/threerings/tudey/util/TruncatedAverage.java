//
// $Id$
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

package com.threerings.tudey.util;

import java.util.Arrays;

import com.threerings.media.util.TrailingAverage;

/**
 * Like {@link com.threerings.media.util.TrailingAverage}, but attempts to exclude outliers
 * (caused by periodic garbage collection, e.g.) by removing the lowest and highest values.
 */
public class TruncatedAverage extends TrailingAverage
{
    /**
     * Creates a new truncated average that records the last ten values and omits the three highest
     * and lowest when computing the average.
     */
    public TruncatedAverage ()
    {
        this(10, 3, 3);
    }

    /**
     * Creates a new truncated average that records the specified number of values and omits the
     * given number of values at the higher and lower ends.
     */
    public TruncatedAverage (int history, int omitLowest, int omitHighest)
    {
        super(history);
        _omitLowest = omitLowest;
        _omitHighest = omitHighest;
        _sorted = new int[history];
    }

    @Override
    public void record (int value)
    {
        super.record(value);

        // copy the current values and sort
        int end = Math.min(_history.length, _index);
        System.arraycopy(_history, 0, _sorted, 0, end);
        Arrays.sort(_sorted, 0, end);

        // add up the values, less the ones at the end
        int total = 0;
        int first = (_omitLowest * end) / _history.length;
        int last = end - (_omitHighest * end) / _history.length;
        for (int ii = first; ii < last; ii++) {
            total += _sorted[ii];
        }
        _value = total / (last - first);
    }

    @Override
    public int value ()
    {
        return _value;
    }

    /** The number of values to omit on the lower and higher ends. */
    protected int _omitLowest, _omitHighest;

    /** An array for the sorted values. */
    protected int[] _sorted;

    /** The current value. */
    protected int _value;
}
