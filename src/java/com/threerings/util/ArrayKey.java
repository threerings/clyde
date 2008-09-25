//
// $Id$

package com.threerings.util;

import java.util.Arrays;

/**
 * A hash key that uses {@link Arrays#deepHashCode} and {@link Arrays#deepEquals} to hash/compare
 * an array of elements.
 */
public class ArrayKey
{
    /**
     * Creates a new key with the supplied elements.
     */
    public ArrayKey (Object... elements)
    {
        _elements = elements;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return Arrays.deepHashCode(_elements);
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof ArrayKey &&
            Arrays.deepEquals(_elements, ((ArrayKey)other)._elements);
    }

    /** The elements to compare. */
    protected Object[] _elements;
}
