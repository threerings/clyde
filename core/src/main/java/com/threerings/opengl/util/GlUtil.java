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

package com.threerings.opengl.util;

import java.util.Comparator;

/**
 * Various static methods of general utility.
 */
public class GlUtil
{
    /**
     * Returns the smallest power-of-two that is greater than or equal to the supplied (positive)
     * value.
     */
    public static int nextPowerOfTwo (int value)
    {
        return (Integer.bitCount(value) > 1) ? (Integer.highestOneBit(value) << 1) : value;
    }

    /**
     * Divides the provided array into two halves, so that the first half contains all of the
     * elements less than the median, and the second half contains all of the elements equal to
     * or greater than the median.  This code is adapted from Wikipedia's page on
     * <a href="http://en.wikipedia.org/wiki/Selection_algorithm">Selection algorithms</a>.
     */
    public static <T> void divide (T[] a, Comparator<? super T> comp)
    {
        int left = 0, right = a.length - 1, k = a.length / 2;
        while (true) {
            int pivotIndex = (left + right) / 2;
            int pivotNewIndex = partition(a, left, right, pivotIndex, comp);
            if (k == pivotNewIndex) {
                return;
            } else if (k < pivotNewIndex) {
                right = pivotNewIndex - 1;
            } else {
                left = pivotNewIndex + 1;
            }
        }
    }

    /**
     * Partitions the [left, right] sub-array about the element at the supplied pivot index.
     *
     * @return the new (final) location of the pivot.
     */
    protected static <T> int partition (
        T[] a, int left, int right, int pivotIndex, Comparator<? super T> comp)
    {
        T pivotValue = a[pivotIndex];
        swap(a, pivotIndex, right); // Move pivot to end
        int storeIndex = left;
        for (int ii = left; ii < right; ii++) {
            if (comp.compare(a[ii], pivotValue) <= 0) {
                swap(a, storeIndex, ii);
                storeIndex++;
            }
        }
        swap(a, right, storeIndex); // Move pivot to its final place
        return storeIndex;
    }

    /**
     * Swaps two elements in an array.
     */
    protected static <T> void swap (T[] a, int idx1, int idx2)
    {
        T tmp = a[idx1];
        a[idx1] = a[idx2];
        a[idx2] = tmp;
    }
}
